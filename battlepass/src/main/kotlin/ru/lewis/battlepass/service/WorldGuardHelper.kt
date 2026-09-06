package ru.lewis.battlepass.service

import com.google.inject.Singleton
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion
import org.bukkit.Location
import com.sk89q.worldedit.bukkit.BukkitAdapter

@Singleton
class WorldGuardHelper @jakarta.inject.Inject constructor() {

    fun createRegion(
        center: Location,
        radius: Int,
        name: String = "fragments_event"
    ) {
        val world = center.world
            ?: throw IllegalArgumentException("World is null")

        val regionManager = WorldGuard.getInstance()
            .platform
            .regionContainer
            .get(BukkitAdapter.adapt(world))
            ?: throw IllegalStateException("RegionManager is null")


        val min = BlockVector3.at(
            center.blockX - radius,
            world.minHeight,
            center.blockZ - radius
        )

        val max = BlockVector3.at(
            center.blockX + radius,
            world.maxHeight,
            center.blockZ + radius
        )


        val region = ProtectedCuboidRegion(
            name,
            min,
            max
        )

        regionManager.addRegion(region)
        regionManager.save()
    }


    fun deleteRegion(
        name: String = "fragments_event",
        world: org.bukkit.World
    ) {
        val regionManager = WorldGuard.getInstance()
            .platform
            .regionContainer
            .get(BukkitAdapter.adapt(world))
            ?: return

        regionManager.removeRegion(name)
        regionManager.save()
    }
}
