@MixinConfigOption(
        description = "Skips repeated checks whether the equipment of an entity changed. " +
        "Equipment updates are detected instead.",
        depends = {
                @MixinConfigDependency(dependencyPath = "mixin.util.item_component_and_count_tracking"),
                @MixinConfigDependency(dependencyPath = "mixin.util.in_world_tracking.entity")
        }
)
package net.caffeinemc.mods.lithium.mixin.entity.equipment_tracking;

import net.caffeinemc.gradle.MixinConfigDependency;
import net.caffeinemc.gradle.MixinConfigOption;