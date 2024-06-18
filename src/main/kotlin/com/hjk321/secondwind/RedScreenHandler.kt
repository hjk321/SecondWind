package com.hjk321.secondwind

import io.papermc.paper.event.world.border.WorldBorderBoundsChangeEvent
import io.papermc.paper.event.world.border.WorldBorderBoundsChangeFinishEvent
import org.bukkit.Bukkit
import org.bukkit.WorldBorder
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.util.concurrent.TimeUnit

class RedScreenHandler(private val plugin: SecondWind) : Listener {
    fun sendInitialDyingScreenEffect(player: Player) {
        player.worldBorder = copyWorldBorderForDying(player.world.worldBorder)
        // TODO if at all possible, we need to account for borders that are growing/shrinking here
    }

    fun clearDyingScreenEffect(player: Player) {
        player.worldBorder = null
    }

    private fun copyWorldBorderForDying(worldBorder: WorldBorder) : WorldBorder {
        val newBorder = Bukkit.createWorldBorder()

        newBorder.size = worldBorder.size
        newBorder.center = worldBorder.center
        newBorder.damageAmount = worldBorder.damageAmount
        newBorder.damageBuffer = worldBorder.damageBuffer
        newBorder.warningDistance = Integer.MAX_VALUE
        newBorder.warningTime = 0

        return newBorder
    }

    private fun scheduleDyingWorldBorderUpdate(newBorder: WorldBorder) {
        plugin.server.scheduler.scheduleSyncDelayedTask(plugin) {
            // TODO optimize? It's only being called when the world border changes, so might not be neccesary.
            plugin.server.onlinePlayers.forEach { player ->
                if (plugin.dyingPlayerHandler.checkDyingTag(player)) {
                    player.worldBorder = newBorder
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @Suppress("unused") // Registered by Listener
    fun updateDyingWorldBorderOnChange(event: WorldBorderBoundsChangeEvent) {
        if (event.isCancelled)
            return
        val newBorder = copyWorldBorderForDying(event.worldBorder)
        newBorder.setSize(event.newSize, TimeUnit.MILLISECONDS, event.duration)
        scheduleDyingWorldBorderUpdate(newBorder)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @Suppress("unused") // Registered by Listener
    fun updateDyingWorldBorderOnFinish(event: WorldBorderBoundsChangeFinishEvent) {
        scheduleDyingWorldBorderUpdate(copyWorldBorderForDying(event.worldBorder))
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @Suppress("unused") // Registered by Listener
    fun updateDyingWorldBorderOnCenterChange(event: WorldBorderBoundsChangeFinishEvent) {
        scheduleDyingWorldBorderUpdate(copyWorldBorderForDying(event.worldBorder))
    }
}
