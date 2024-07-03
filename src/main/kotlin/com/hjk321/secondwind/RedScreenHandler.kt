package com.hjk321.secondwind

import io.papermc.paper.event.world.border.WorldBorderBoundsChangeEvent
import io.papermc.paper.event.world.border.WorldBorderBoundsChangeFinishEvent
import io.papermc.paper.event.world.border.WorldBorderCenterChangeEvent
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldBorder
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.concurrent.TimeUnit

internal class RedScreenHandler(private val plugin: SecondWind) : Listener {

    fun sendDyingRedScreenEffect(player: Player) {
        val realBorder = player.world.worldBorder
        val newBorder = copyWorldBorderForDying(player.world)

        // If real border is shrinking or growing, imitate that in the new border
        if (plugin.nms.isWorldBorderMoving(realBorder))
            newBorder.setSize(plugin.nms.getWorldBorderTargetSize(realBorder), TimeUnit.MILLISECONDS,
                plugin.nms.getWorldBorderRemainingTime(realBorder))

        player.worldBorder = newBorder
        sendWitherHeartEffect(player)
    }

    private fun sendWitherHeartEffect(player: Player) {
        player.sendPotionEffectChange(player, PotionEffect( // Fake wither heart effect
            PotionEffectType.WITHER, PotionEffect.INFINITE_DURATION,
            0, false, false, false)
        )
    }

    fun clearDyingScreenEffect(player: Player) {
        player.worldBorder = null
        player.sendPotionEffectChangeRemove(player, PotionEffectType.WITHER)
    }

    private fun copyWorldBorderForDying(world: World) : WorldBorder {
        val worldBorder = world.worldBorder
        val newBorder = Bukkit.createWorldBorder()

        newBorder.size = worldBorder.size
        newBorder.center = worldBorder.center.multiply(world.coordinateScale)
        newBorder.damageAmount = worldBorder.damageAmount
        newBorder.damageBuffer = worldBorder.damageBuffer
        newBorder.warningDistance = Integer.MAX_VALUE
        newBorder.warningTime = 0

        return newBorder
    }

    private fun scheduleDyingWorldBorderUpdate(newBorder: WorldBorder, world: World) {
        plugin.server.scheduler.scheduleSyncDelayedTask(plugin) {
            // Iterating over all players is inefficient, but this will only ever be called when the worldborder updates
            // TODO if we have to make a dyingPlayers map somewhere, switch this to it as well
            plugin.server.onlinePlayers.forEach { player ->
                if (plugin.dyingPlayerHandler.checkDyingTag(player) && player.world == world) {
                    player.worldBorder = newBorder
                }
            }
        }
    }

    private fun scheduleSendNewDyingEffectToPlayer(player: Player) {
        plugin.server.scheduler.scheduleSyncDelayedTask(plugin) {
            if (plugin.dyingPlayerHandler.checkDyingTag(player)) {
                sendDyingRedScreenEffect(player)
                sendWitherHeartEffect(player)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun updateDyingWorldBorderOnChange(event: WorldBorderBoundsChangeEvent) {
        if (event.isCancelled)
            return
        val newBorder = copyWorldBorderForDying(event.world)
        newBorder.setSize(event.newSize, TimeUnit.MILLISECONDS, event.duration)
        scheduleDyingWorldBorderUpdate(newBorder, event.world)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun updateDyingWorldBorderOnFinish(event: WorldBorderBoundsChangeFinishEvent) {
        scheduleDyingWorldBorderUpdate(copyWorldBorderForDying(event.world), event.world)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun updateDyingWorldBorderOnCenterChange(event: WorldBorderCenterChangeEvent) {
        if (event.isCancelled)
            return
        // TODO what if the center changes while it is also growing/shrinking? need to investigate.
        scheduleDyingWorldBorderUpdate(copyWorldBorderForDying(event.world), event.world)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun updateDyingWorldBorderOnWorldChange(event: PlayerChangedWorldEvent) {
        scheduleSendNewDyingEffectToPlayer(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun updateDyingWorldBorderOnJoin(event: PlayerJoinEvent) {
        // TODO if kill-on-leave is on, we shouldn't bother here.
        scheduleSendNewDyingEffectToPlayer(event.player)
    }
}
