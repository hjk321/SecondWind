package com.hjk321.secondwind

import com.google.gson.JsonParseException
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.damage.DamageSource
import org.bukkit.entity.Player
import org.bukkit.entity.Pose
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.event.entity.EntityResurrectEvent
import org.bukkit.event.player.PlayerGameModeChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType

const val DYING_NOW = 0
const val NOT_DYING = -1

internal class DyingPlayerHandler(private val plugin: SecondWind) : Listener {

    private class DyingPlayerHandlerTask(private val handler: DyingPlayerHandler) : Runnable {
        override fun run() {
            // TODO only iterate over dying players
            handler.plugin.server.onlinePlayers.forEach { player ->
                if (handler.decrementDyingTicks(player) == DYING_NOW) {
                    player.health = 0.0
                }
            }
        }
    }

    private val task = DyingPlayerHandlerTask(this)
    fun startTask() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, task, 1, 1)
    }

    private val dyingKey = NamespacedKey(this.plugin, "dying")
    private val deathMessageKey = NamespacedKey(this.plugin, "death_message")

    fun checkDyingTag(player: Player): Boolean {
        return (player.persistentDataContainer.get(dyingKey, PersistentDataType.INTEGER) ?: NOT_DYING) >= DYING_NOW
    }

    fun getDyingTicks(player: Player) : Int {
        return player.persistentDataContainer.get(dyingKey, PersistentDataType.INTEGER) ?: NOT_DYING
    }

    private fun removeDyingTag(player: Player) {
        player.persistentDataContainer.remove(dyingKey)
    }

    private fun addDyingTag(player: Player) {
        player.persistentDataContainer.remove(dyingKey) // In case it's a bad value or the wrong type
        player.persistentDataContainer.set(dyingKey, PersistentDataType.INTEGER, plugin.dyingTicks
                + plugin.dyingGracePeriodTicks)
    }

    /// Returns the new value after decrementing.
    private fun decrementDyingTicks(player: Player) : Int {
        val ticks = getDyingTicks(player)
        if (ticks <= NOT_DYING)
            return NOT_DYING
        player.persistentDataContainer.set(dyingKey, PersistentDataType.INTEGER, ticks - 1)
        return ticks - 1
    }

    private fun storeDeathMessage(player: Player, damage: Double, damageSource: DamageSource) {
        val deathMessage = plugin.nms.getDeathMessage(player, damage, damageSource)
        val serialized = GsonComponentSerializer.gson().serialize(deathMessage)
        player.persistentDataContainer.set(deathMessageKey, PersistentDataType.STRING, serialized)
    }

    private fun getDeathMessage(player: Player) : Component? {
        val serialized = player.persistentDataContainer.get(deathMessageKey, PersistentDataType.STRING) ?: return null
        return try {
            GsonComponentSerializer.gson().deserialize(serialized)
        } catch (ex: JsonParseException) {
            null
        }
    }

    private fun removeDeathMessage(player: Player) {
        player.persistentDataContainer.remove(deathMessageKey)
    }

    private fun startDying(player: Player) {
        addDyingTag(player)
        player.health = 0.5
        player.addPotionEffect(PotionEffect(
            PotionEffectType.SLOWNESS, PotionEffect.INFINITE_DURATION,
            4, false, false, false)
        )
        player.addPotionEffect(PotionEffect(
            PotionEffectType.MINING_FATIGUE, PotionEffect.INFINITE_DURATION,
            2, false, false, false)
        )
        player.addPotionEffect(PotionEffect( // Note, this prevents damaging entities in certain situations
            PotionEffectType.WEAKNESS, PotionEffect.INFINITE_DURATION,
            0, false, false, false)
        )
        player.setPose(Pose.SWIMMING, true)
        plugin.redScreenHandler.sendDyingRedScreenEffect(player)
        plugin.dyingBossBarHandler.startDyingBossBar(player)
    }

    private fun secondWind(player: Player) {
        if (!checkDyingTag(player))
            return
        removeDyingTag(player)
        removeDeathMessage(player)
        player.removePotionEffect(PotionEffectType.SLOWNESS)
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE)
        player.removePotionEffect(PotionEffectType.WEAKNESS)
        // TODO remove other bad effects
        // TODO multiple effects of the same type are supported, so we should only remove the infinite versions?

        player.health = 1.0
        player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION,
            100, 0, false, false, false))
        player.setPose(Pose.STANDING, false)
        plugin.redScreenHandler.clearDyingScreenEffect(player)
        plugin.dyingBossBarHandler.stopDyingBossBar(player)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun checkPlayerLethalDamage(event: EntityDamageEvent) {
        if (event.entity !is Player)
            return
        val player = event.entity as Player
        if (event.cause == EntityDamageEvent.DamageCause.KILL)
            return // the kill command bypasses second wind
        if ((player.gameMode == GameMode.CREATIVE) || (player.gameMode == GameMode.SPECTATOR))
            return
        if (event.finalDamage >= player.health) { // TODO more robust check
            if ((player.inventory.itemInOffHand.type == Material.TOTEM_OF_UNDYING)
                || (player.inventory.itemInMainHand.type == Material.TOTEM_OF_UNDYING)) {
                return // Defer to vanilla totem logic. TODO config for alternate totem behavior
                // FIXME the totem is currently popped when taking any damage from dying instead of just when we want
            }
            if (checkDyingTag(player)) {
                // prevent damage while dying
                event.isCancelled = true
                return
            }

            storeDeathMessage(player, event.damage, event.damageSource)
            event.damage = 0.0
            startDying(player)
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun setDeathMessage(event: PlayerDeathEvent) {
        if (event.isCancelled)
            return
        event.deathMessage(getDeathMessage(event.player) ?: return)
        // TODO make a config option to print the death message when player is knocked down, then set it to null here
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun deadPlayerCleanup(event: PlayerDeathEvent) {
        if (event.isCancelled)
            return
        removeDyingTag(event.player)
        removeDeathMessage(event.player)
        event.player.setPose(Pose.DYING, false)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun reviveOnGamemodeChange(event: PlayerGameModeChangeEvent) {
        if (event.isCancelled)
            return
        val gamemode = event.newGameMode
        if ((gamemode == GameMode.CREATIVE) || (gamemode == GameMode.SPECTATOR))
            secondWind(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun reviveOnTotem(event: EntityResurrectEvent) {
        if ((event.entity !is Player) || (event.isCancelled))
            return
        secondWind(event.entity as Player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun popTotemEarly(event: PlayerInteractEvent) {
        if (event.hasItem() && (event.item?.type == Material.TOTEM_OF_UNDYING) && checkDyingTag(event.player)) {
            event.player.damage(event.player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value) // todo dont assert
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun noRegenWhileDying(event: EntityRegainHealthEvent) {
        if (event.entity is Player && checkDyingTag(event.entity as Player))
            event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun killIfLeaveGameWhileDying(event: PlayerQuitEvent) {
        if (plugin.killOnQuit && checkDyingTag(event.player)) {
            event.player.health = 0.0 // Bypasses damage event but still triggers death event
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun handlePlayerJoinWhileDying(event: PlayerJoinEvent) {
        if (!plugin.killOnQuit) {
            if (checkDyingTag(event.player)) {
                event.player.setPose(Pose.SWIMMING, true)
            }
        } else {
            if (!event.player.isDead)
                event.player.health = 0.0 // Shouldn't happen, but perhaps the config changed since they last logged in.
        }
    }
}
