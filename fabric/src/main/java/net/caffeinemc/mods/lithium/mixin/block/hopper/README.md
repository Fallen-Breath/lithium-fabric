# Lithium Hopper Optimizations
## Assumptions made about other inventories

- Some inventories (like ChiseledBookshelfBlockEntity) require stack size 1 for insertion tests.
  These inventories implement `LithiumTransferConditionInventory.lithium$itemInsertionTestRequiresStackSize1()` to signal
  this requirement, and Lithium creates a size-1 copy for testing in these cases.
- Hoppers always transfer a single item. For most inventories, the insertion test does not depend on the stack size
  (the receiving inventory slot is not filled to its limit and the item type is correct). This allows Lithium to
  avoid creating a copy of the transferred item stack when testing if the transfer would succeed, except for special
  inventories that explicitly require size-1 testing (see above).

## Inventory Stack List

LithiumStackList replaces the inventory stack lists (NonNullList). It keeps an inventory content modification counter, empty and full slot counts, and caches the
signal strength until the inventory content is modified.

## Lithium Inventory

LithiumInventory is any inventory that can have its stack list replaced with LithiumStackList. Making any inventory a
LithiumInventory just requires implementing the getters and setter of the stack list. This is done in the
InventoryAccessors class. An inventory implementing LithiumInventory allows hoppers to use the LithiumStackList's
additional data for some optimizations.

## Inventory Caching

Hoppers try to cache the inventory they are interacting with. If there is a cached inventory it can be used directly
without querying the world again. This is possible for BlockEntities, Composters and no block inventory presence. The
cache can be validated by checking whether the BlockEntity has not been removed from the world. Composters and no block
inventory presence will be invalidated when the hopper receives a shape update from the corresponding direction or a
block update from any direction (vanilla no longer provides the direction the update comes from). A
workaround for the update suppressing behavior when placing a hopper in a powered position
(cf. https://www.youtube.com/watch?v=QVOONJ1OY44 ) is included (cf. HopperBlockMixin). Entity inventories (storage
minecarts) are not cached, as they are randomly chosen every tick, may move or be destroyed at any time without notice
to the hopper.

### Compatibility Features

- Movable Block Entities: The cache validation includes position comparison checks to support mods that allow moving
block entities (e.g. Carpet). If a cached block entity's position no longer matches the expected interaction
position, the cache is invalidated and the inventory is queried again.

- Stack List Replacement Detection: Some operations (such as commands making a furnace read its inventory from NBT)
can silently replace an inventory's stack list. The hopper detects when the cached LithiumStackList reference no longer
matches the inventory's current stack list and invalidates the cache accordingly.

## Modification Counter Shortcuts and Comparator Updates

Hoppers keep track of the last inventory they interacted with, and its modification count, even if the inventory is an
entity which must not be cached. This data can still be used for shortcutting the item transfer, given the hopper
interacts with the same inventory again. If both the hopper the inventory the hopper interacts with were not modified
since the last failed transfer attempt, it is known that the current attempt will fail as well. Therefore, the whole
transfer logic can be skipped, however, the side effects (comparator updates) of the failed transfer have to be
mimicked. Hoppers use the inventory modification counters to determine whether the transfer attempt can be skipped. The
comparator side effects (ComparatorUpdatePattern) are computed in HopperHelper and cached in the LithiumStackList. The
side effect cache is invalidated when the inventory is modified. The ComparatorUpdatePattern is only guaranteed to be
calculated like vanilla if mods do not add item types with a non-power-of-two maximum stack size.

The inventory modification counters are updated in LithiumStackList and ItemStackMixin. ItemStacks keep a reference to
the LithiumStackList they are in, and they notify the LithiumStackList when their stack size is manipulated. Generally,
ItemStacks are in only one inventory at a time. Some technical players use a technique called "item shadowing", that allows
placing the same ItemStack object in multiple inventories by causing an exception to be thrown during the player packet
handling, e.g. a StackOverFlowError due to recursive block updates in older minecraft versions. Lithium handles this too,
by keeping track of all relevant inventories at the same time in the ItemStack using a Multi ChangeSubscriber instance.

### Comparator Updates on Failed Transfers

Vanilla transfer attempts always include taking out an item from the source inventory and attempting to insert it into
the target inventory. When failing to place the item into the target inventory, the item is returned to the source
inventory. This can cause comparator updates depending on the inventory type, possibly even with a reduced signal strength.

The `ComparatorUpdatePattern` captures the detectable pattern of comparator updates that would occur during failed extraction
attempts from different inventory types. Since many patterns with multiple consecutive updates are not distinguishable
with contraptions, only very few different patterns exist.
All patterns: NO_UPDATE, UPDATE, DECREMENT_UPDATE_INCREMENT_UPDATE, UPDATE_DECREMENT_UPDATE_INCREMENT_UPDATE

## Entity Interaction Shortcuts

The entity tracker engine makes each entity section (16x16x16 blocks) keep track on the last tick time an Entity of
class Inventory or ItemEntity have moved, appeared or disappeared inside them. Hoppers use the modification tick time to
determine whether any ItemEntity may have entered the pickup area after the last failed item pickup attempt. If the
hopper inventory did not change after the last failed item pickup attempt and no ItemEntities moved, the item pickup
attempt can be skipped, as it is guaranteed to fail like the previous attempt. Similarly, if there was no Inventory
Entity present last time, and no Inventory Entities have moved or appeared, the hopper does not need to search for
Inventory Entities.

# Comparator Caching

Block entities lazily initialize whether there is any comparator near them. Placing a comparator updates the value for
nearby block entities. Breaking a comparator does not reset it until the block entity is reloaded.

## Further Development

If any bugs show up, they will need to be fixed.

## How Lithium hoppers work different from vanilla

- Lithium Optimized Inventories:
    - Any vanilla block entity / storage entity inventory that vanilla hoppers can interact with (Block inventories like composters excluded!)
    - Custom Stack list (LithiumStackList):
      Replaces the vanilla stack list when a hopper accesses the inventory. Double chest stack lists are handled in a
      special case, as they must not store data themselves as they can be reinstantiated on any access. Lithium tries
      to reuse existing double inventory instances.
    
    - LithiumStackLists store additional data:
        - Cached signal strength (int)
            - Stored when signal strength is calculated
            - Invalidated when inventory content is changed
            - For double chests: Uses a modification counter to detect changes instead of immediate invalidation.
              The cached value is revalidated by comparing the current modification count to the stored count.
        - Cached Comparator Update Pattern (Enum)
            - Possible values: NO_UPDATE, UPDATE, DECR_UPDATE_INCR_UPDATE, UPDATE_DECR_UPDATE_INCR_UPDATE
            - Stored when accessed and not stored
                - accessed when failed transfer attempt is skipped due to unchanged inventories
            - Deleted when inventory content is changed
        - Signal Strength Override (boolean)
            - Sets the inventory signal strength to 0 when set
                - Only used to execute certain Comparator Update Patterns
        - Inventory Modification Count (long)
            - Increased when inventory content is changed
        - Number of occupied slots (int)
            - Updated when inventory content is changed (only checking the changed slot)
        - Number of full slots (int)
            - Updated when inventory content is changed (only checking the changed slot)
        - Parent Stack list
            - Only used when stack lists is a double chest half
            - Set when double stack list is created when double inventory is accessed by a hopper

- Lithium Sectioned Entity Movement Trackers:
    - Certain classes of Entities are configured to be tracked: Inventory and ItemEntity
    - Entity Sections store the timestamp of the last change for each tracked class
    - Entities of tracked classes notify their entity section every time their position change or when they are added or
      removed from the world
        - Updates their classes' timestamp of the entity section to the current time
    - Entity Sections store a set of movement trackers
        - When the section becomes accessible or inaccessible the movement trackers are notified
    - Movement trackers are a data structure used by hoppers.
        - Data stored:
            - Box of the observed sections
            - The observed world (dimension)
            - The tracked class (Inventory or ItemEntity)
            - List of observed entity sections
            - Mask whether the sections are accessible
            - Number of users (= hoppers) of this tracker
            - List of the accessible observed entity sections' timestamp (for more direct access)
            - Largest known change timestamp
        - Functionality:
            - Check whether it is guaranteed that no change (entity movement, creation, removal) happened after a given
              timestamp (includes usage and update of largest known timestamp)
            - Collect all entities of the tracked class inside a given box from the accessible observed entity sections.
        - Deduplication:
            - When a new movement tracker is created, and an equal one already exists, the already existing one is used
              instead. The number of users of the tracker is increased.
            - When the user no longer uses the tracker, it un-registers and the number of users will be decreased.
              When it reaches 0, it is removed from the sets of trackers and can be garbage collected.

- Lithium Hopper behavior:
    - Data:
        - Own inventory modification counter at last item collection attempt
        - Own inventory modification counter at last insert attempt
        - Own inventory modification counter at last extraction attempt
        - Current insertion cache state and current extraction cache state:
            - UNKNOWN: Not yet determined
            - BLOCK_STATE: Composter or similar
            - BLOCK_ENTITY: Block Entity inventory or double chest
            - REMOVAL_TRACKING_BLOCK_ENTITY: Block Entity inventory or double chest but with inventory tracking
            - NO_BLOCK_INVENTORY: Interact with entities
        - Current insertion / extraction inventory (only block inventories)
        - Current expected block entity removal count for each current insertion / extraction inventory (only
          BLOCK_ENTITY)
        - Current lithium insertion / extraction inventory (only lithium optimized inventories)
        - Last known stack list of each current lithium insertion / extraction inventory
        - Last known inventory modification count of each current lithium insertion / extraction inventory
            - Updated just before attempting to transfer items
            - Used to skip transfer attempts
            - Only used if the last known stack list is also the current stack list of the same inventory

        - Item entity movement tracker
            - Created when item entities are searched for the first time
            - Un-registered when the hopper is removed
        - Item pickup search space bounding boxes and encompassing box
        - Time of the last item collection attempt

        - Inventory entity movement tracker (one for extraction and insertion each)
            - Created before searching for extraction / insertion inventory entities for the first time
            - Un-registered when the hopper is removed
        - Extract / Insert inventory entity interaction search box
        - Time of last search with no extract / insert inventory inside search box
    - Behavior
        - Shortcut inventory emptiness and fullness checks by comparing the number of occupied or full slots to 0 or
          inventory size
        - UNKNOWN cache state:
            - get the block inventory like vanilla and initialize the cache
        - BLOCK_STATE:
            - use the cached inventory
        - BLOCK_ENTITY:
            - check the expected removal count of the block entity, invalidate the cache if it changed
            - use the cached inventory if not invalidated, otherwise UNKNOWN cache state
        - REMOVAL_TRACKING_BLOCK_ENTITY
            - use the cached inventory. Subscribe to the cached inventory for major changes (removed from world) and
              invalidate the cache when handling those events if necessary.
        - NO_BLOCK_INVENTORY:
            - skip interacting with block inventory
        - Hopper receives a block/observer update from top or hopper's face:
            - invalidate corresponding cache state to UNKNOWN if current state is BLOCK_STATE or NO_BLOCK_INVENTORY
        - Use the extract / insert inventory entity movement tracker to retrieve an inventory entity
            - Skip getting inventory entities if the search box was empty and the tracker guarantees that no inventory
              entity positions changed since the last search
            - Cache the selected inventory
        - If interacting with an optimized inventory, compare the inventory modification counters to the stored values
          to determine whether the transfer attempt is guaranteed to fail. If it is guaranteed to fail, skip the
          transfer attempt and execute its cached comparator update pattern (use no update pattern when inserting).
        - Transfer a single item from the source to the target inventory like vanilla. Avoid copying item stacks when
          possible.
        - Update stored modification counters after failed a transfer attempt
        - Use the item entity entity movement trackers to retrieve item entities to interact with
            - Skip getting item entities if the search box was empty and the tracker guarantees that no item entity
              positions changed since the last search
            - Skip getting item entities if the modification counter of the hopper is equal to the stored value, and the
              tracker guarantees that no item entity positions changed since the last search
            - Get item entities, update the stored modification counter, the search timestamp and whether the search box
              is empty.

        - Hopper decides to sleep at the end of it ticking if:
            - The hopper is not on cooldown
            - The hopper inventory wasn't modified during the current hopper ticking
            - When querying inventory entities: No inventory entity was found in the interaction area and no inventory
              entity moved in the nearby chunk sections in the last and current tick
            - When interacting with inventory blocks: The inventory block is blockstate-based (composter) or supports
              inventory change tracking (double inventory + vanilla block entities).
            - When interacting with item entities: No item entity changed in the nearby chunk sections in the last and
              current tick
            - When extracting from a block entity: The hopper is not constantly sending comparator updates around the
              block entity that it is extracting from, or the comparator updates are not detectable, because there are
              no comparators within 2 blocks horizontal distance around the block entity (comparator rotation or block
              in between are not checked)
        - Hopper starts sleeping by subscribing to the relevant tracked inventories and subscribing to the relevant
          entity trackers. It also subscribes to itself for inventory changes.
        - When the hopper receives any event from the subscribed trackers, it will wake up and invalidate cached
          information if needed (e.g. inventory removal event -> invalidate cached data for this inventory)
        - When the hopper is set on cooldown it wakes up. However, to handle the 7gt/8gt cooldown case:
          When a hopper receives an item from another hopper, the receiving hopper is set to an 8-tick cooldown by
          default. If the receiving hopper's tickedGameTime is >= the sending hopper's tickedGameTime (both ticked in
          the same game tick), the cooldown is reduced to 7 ticks instead. As Lithium implements block entity sleeping,
          the tickedGameTick may be inconsistent with the usual hopper ticking order.
          Thus, the case of a sleeping hopper being woken up by receiving an item and going into cooldown has to be handled
          carefully: 
          When a sleeping hopper receives a cooldown, a 7gt cooldown is used AND the hopper will only wake up in the next
          game tick to process the cooldown. Additionally, other updates cannot wake the hopper during this short
          sleep.
        - More hopper sleeping capabilities live in the general block entity sleeping code (Locked & Not on cooldown
          sleeping)