package com.hjk321.secondwind

import io.papermc.paper.event.world.border.WorldBorderBoundsChangeEvent
import io.papermc.paper.event.world.border.WorldBorderBoundsChangeFinishEvent
import io.papermc.paper.event.world.border.WorldBorderCenterChangeEvent
import org.bukkit.Bukkit
import org.bukkit.WorldBorder
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.time.Instant
import java.util.concurrent.TimeUnit

class RedScreenHandler(private val plugin: SecondWind) : Listener {
    private var worldBorderChangeEnd: Instant? = null
    private var worldBorderChangeSize: Double? = null

    fun sendInitialDyingScreenEffect(player: Player) {
        val newBorder = copyWorldBorderForDying(player.world.worldBorder)
        worldBorderChangeEnd?.let { worldBorderChangeEnd ->
            val duration = worldBorderChangeEnd.toEpochMilli() - Instant.now().toEpochMilli()
            if (duration > 0) {
                worldBorderChangeSize?.let { worldBorderChangeSize ->
                    newBorder.setSize(worldBorderChangeSize, TimeUnit.MILLISECONDS, duration)
                }
            }
        }
        player.worldBorder = newBorder
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
            // Iterating over all players is inefficient, but this will only ever be called when the worldborder updates
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
        worldBorderChangeEnd = Instant.now().plusMillis(event.duration)
        worldBorderChangeSize = event.newSize
        scheduleDyingWorldBorderUpdate(newBorder)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @Suppress("unused") // Registered by Listener
    fun updateDyingWorldBorderOnFinish(event: WorldBorderBoundsChangeFinishEvent) {
        worldBorderChangeEnd = null
        worldBorderChangeSize = null
        scheduleDyingWorldBorderUpdate(copyWorldBorderForDying(event.worldBorder))
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @Suppress("unused") // Registered by Listener
    fun updateDyingWorldBorderOnCenterChange(event: WorldBorderCenterChangeEvent) {
        if (event.isCancelled)
            return
        scheduleDyingWorldBorderUpdate(copyWorldBorderForDying(event.worldBorder))
    }
}
