package com.hjk321.secondwind

import org.bukkit.*
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
import org.bukkit.event.entity.EntityResurrectEvent
import org.bukkit.event.player.PlayerGameModeChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType

class DyingPlayerHandler(private val plugin: SecondWind) : Listener {
    private val dyingKey = NamespacedKey(this.plugin, "dying")

    fun checkDyingTag(player:Player): Boolean {
        return player.persistentDataContainer.has(dyingKey, PersistentDataType.BOOLEAN) &&
            (player.persistentDataContainer.get(dyingKey, PersistentDataType.BOOLEAN) == true)
    }

    fun removeDyingTag(player:Player) {
        player.persistentDataContainer.remove(dyingKey)
    }

    fun addDyingTag(player: Player) {
        player.persistentDataContainer.remove(dyingKey) // In case it's false or the wrong type
        player.persistentDataContainer.set(dyingKey, PersistentDataType.BOOLEAN, true)
    }

    private fun startDying(player: Player) {
        addDyingTag(player)
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
        plugin.redScreenHandler.sendDyingRedScreenEffect(player)
    }

    private fun secondWind(player: Player) {
        if (!checkDyingTag(player))
            return
        removeDyingTag(player)
        player.removePotionEffect(PotionEffectType.WITHER)
        player.removePotionEffect(PotionEffectType.SLOWNESS)
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE)
        // TODO remove other bad effects

        player.health = 1.0
        player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION,
            100, 0, false, false, false))
        player.setPose(Pose.STANDING, false)
        plugin.redScreenHandler.clearDyingScreenEffect(player)
    }

    @EventHandler(priority = EventPriority.HIGH)
    @Suppress("unused") // Registered by Listener
    fun checkPlayerLethalDamage(event: EntityDamageEvent) {
        if (event.entity is Player) {
            val player = event.entity as Player
            if (event.cause == EntityDamageEvent.DamageCause.KILL)
                return // the kill command bypasses second wind
            if ((player.gameMode == GameMode.CREATIVE) || (player.gameMode == GameMode.SPECTATOR))
                return
            if (event.damage >= player.health && !checkDyingTag(player)) { // TODO more robust check
                if ((player.inventory.itemInOffHand.type == Material.TOTEM_OF_UNDYING)
                    || (player.inventory.itemInMainHand.type == Material.TOTEM_OF_UNDYING)) {
                    return // Defer to vanilla totem logic. TODO config for alternate totem behavior
                }
                event.damage = 0.0
                startDying(player)
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @Suppress("unused") // Registered by Listener
    fun deadPlayerCleanup(event: PlayerDeathEvent) {
        if (event.isCancelled)
            return
        removeDyingTag(event.player)
        event.player.setPose(Pose.DYING, false)
        plugin.redScreenHandler.clearDyingScreenEffect(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @Suppress("unused") // Registered by Listener
    fun reviveOnGamemodeChange(event: PlayerGameModeChangeEvent) {
        if (event.isCancelled)
            return
        val gamemode = event.newGameMode
        if ((gamemode == GameMode.CREATIVE) || (gamemode == GameMode.SPECTATOR))
            secondWind(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @Suppress("unused") // Registered by Listener
    fun reviveOnTotem(event: EntityResurrectEvent) {
        if ((event.entity !is Player) || (event.isCancelled))
            return
        secondWind(event.entity as Player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @Suppress("unused") // Registered by Listener
    fun popTotemEarly(event: PlayerInteractEvent) {
        if (event.hasItem() && (event.item?.type == Material.TOTEM_OF_UNDYING) && checkDyingTag(event.player)) {
            event.player.damage(event.player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value) // todo dont assert
        }
    }
}
