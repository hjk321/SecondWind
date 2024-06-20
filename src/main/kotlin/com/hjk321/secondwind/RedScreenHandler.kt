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
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import java.util.concurrent.TimeUnit

class RedScreenHandler(private val plugin: SecondWind) : Listener {

    // FIXME there's something going screwy with the scale of the nether and the worldborders.

    fun sendDyingRedScreenEffect(player: Player) {
        val realBorder = player.world.worldBorder
        val newBorder = copyWorldBorderForDying(realBorder)

        // If real border is shrinking or growing, imitate that in the new border
        if (plugin.nms.isWorldBorderMoving(realBorder))
            newBorder.setSize(plugin.nms.getWorldBorderTargetSize(realBorder), TimeUnit.MILLISECONDS,
                plugin.nms.getWorldBorderRemainingTime(realBorder))

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
            // TODO if we have to make a dyingPlayers map somewhere, switch this to it as well
            plugin.server.onlinePlayers.forEach { player ->
                if (plugin.dyingPlayerHandler.checkDyingTag(player)) {
                    player.worldBorder = newBorder
                }
            }
        }
    }

    private fun scheduleSendNewDyingEffectToPlayer(player: Player) {
        plugin.server.scheduler.scheduleSyncDelayedTask(plugin) {
            if (plugin.dyingPlayerHandler.checkDyingTag(player))
                sendDyingRedScreenEffect(player)
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
    fun updateDyingWorldBorderOnCenterChange(event: WorldBorderCenterChangeEvent) {
        if (event.isCancelled)
            return
        // TODO what if the center changes while it is also growing/shrinking? need to investigate.
        scheduleDyingWorldBorderUpdate(copyWorldBorderForDying(event.worldBorder))
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @Suppress("unused") // Registered by Listener
    fun updateDyingWorldBorderOnWorldChange(event: PlayerChangedWorldEvent) {
        scheduleSendNewDyingEffectToPlayer(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @Suppress("unused") // Registered by Listener
    fun updateDyingWorldBorderOnJoin(event: PlayerJoinEvent) {
        // TODO if kill-on-leave is on, we shouldn't bother here.
        scheduleSendNewDyingEffectToPlayer(event.player)
    }
}
