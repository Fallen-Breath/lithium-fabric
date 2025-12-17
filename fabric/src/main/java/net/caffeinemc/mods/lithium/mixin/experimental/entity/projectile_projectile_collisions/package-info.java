@MixinConfigOption(
        description = "Optimize huge stacks of projectiles (e.g. 1000+ ender pearls in a single statis chamber) by " +
                "skipping projectile-projectile collision checks for projectile types that are unable to collide with " +
                "each other, e.g. ender pearls never collide with ender pearls.",
        depends = @MixinConfigDependency(dependencyPath = "mixin.chunk.entity_class_groups")
)
package net.caffeinemc.mods.lithium.mixin.experimental.entity.projectile_projectile_collisions;

import net.caffeinemc.gradle.MixinConfigDependency;
import net.caffeinemc.gradle.MixinConfigOption;