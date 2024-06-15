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

    private fun startDeath(player: Player) {
        dyingPlayers.add(player.uniqueId)
        player.health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value // todo don't assert
        player.addPotionEffect(PotionEffect(
            PotionEffectType.WITHER,
            PotionEffect.INFINITE_DURATION, 1)
        )
        player.addPotionEffect(PotionEffect(
                PotionEffectType.SLOWNESS,
            PotionEffect.INFINITE_DURATION, 4)
        )
        player.setPose(Pose.SWIMMING, true)
        // Hacky way to make player crawl client-side
        // player.sendBlockChange(player.location.add(0.0, 1.0, 0.0), Material.BARRIER.createBlockData())
    }

    @EventHandler(priority = EventPriority.HIGH)
    @Suppress("unused") // Registered by Listener
    fun checkPlayerVanillaDeath(event: EntityDamageEvent) {
        if (event.entity is Player) {
            val player = event.entity as Player
            if (event.damage >= player.health && !dyingPlayers.contains(player.uniqueId)) { // TODO more robust check
                event.damage = 0.0
                startDeath(player)
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
