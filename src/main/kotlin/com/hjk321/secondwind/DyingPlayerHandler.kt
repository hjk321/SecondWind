package com.hjk321.secondwind

import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.entity.Pose
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*


class DyingPlayerHandler : Listener {
    private val dyingPlayers: ArrayList<UUID> = ArrayList()

    private fun startDying(player: Player) {
        dyingPlayers.add(player.uniqueId)
        player.health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value // todo don't assert
        player.addPotionEffect(PotionEffect(
            PotionEffectType.WITHER, PotionEffect.INFINITE_DURATION,
            1, false, false, false)
        )
        player.addPotionEffect(PotionEffect(
            PotionEffectType.SLOWNESS, PotionEffect.INFINITE_DURATION,
            4, false, false, false)
        )
        player.addPotionEffect(PotionEffect(
            PotionEffectType.MINING_FATIGUE, PotionEffect.INFINITE_DURATION,
            2, false, false, false)
        )
        player.setPose(Pose.SWIMMING, true)
    }

    private fun secondWind(player: Player) {
        player.removePotionEffect(PotionEffectType.WITHER)
        player.removePotionEffect(PotionEffectType.SLOWNESS)
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE)
        // TODO remove other bad effects
        player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION,
            100, 0, false, false, false))
    }

    @EventHandler(priority = EventPriority.HIGH)
    @Suppress("unused") // Registered by Listener
    fun checkPlayerVanillaDeath(event: EntityDamageEvent) {
        if (event.entity is Player) {
            val player = event.entity as Player
            if (event.damage >= player.health && !dyingPlayers.contains(player.uniqueId)) { // TODO more robust check
                event.damage = 0.0
                startDying(player)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @Suppress("unused") // Registered by Listener
    fun deadPlayerCleanup(event: PlayerDeathEvent) {
        dyingPlayers.remove(event.player.uniqueId)
        event.player.setPose(Pose.DYING, false)
    }
}
