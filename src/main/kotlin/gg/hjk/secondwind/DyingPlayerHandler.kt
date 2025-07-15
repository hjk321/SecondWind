package gg.hjk.secondwind

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent
import com.google.gson.JsonParseException
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.damage.DamageSource
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.entity.Pose
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.event.entity.EntityResurrectEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.player.PlayerGameModeChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType

internal class DyingPlayerHandler(private val plugin: SecondWind) : Listener {

    companion object {
        const val DYING_NOW = 0
        const val NOT_DYING = -1
        const val POPPING_TOTEM = -1337

        const val INVULN_WEARING_OFF = 0
        const val NOT_INVULNERABLE = -1

        const val STAND_TICKS = 12
    }

    private class DyingPlayerHandlerTask(private val handler: DyingPlayerHandler) : Runnable {
        override fun run() {
            handler.plugin.server.onlinePlayers.forEach { player ->
                if (!player.isValid || player.isDead)
                    return@forEach

                handler.decrementInvulnTicks(player)

                if (handler.getStandForAttackTicks(player) > DYING_NOW) {
                    if (handler.decrementStandForAttackTicks(player) == DYING_NOW) {
                        player.setPose(Pose.SWIMMING, true)
                    }
                }

                if (handler.decrementDyingTicks(player) == DYING_NOW) {
                    // If we're holding a totem, we'd rather use it than die
                    if ((player.inventory.itemInOffHand.type == Material.TOTEM_OF_UNDYING)
                                || (player.inventory.itemInMainHand.type == Material.TOTEM_OF_UNDYING)) {
                        handler.setPoppingTotem(player)
                        player.damage(player.getAttribute(Attribute.MAX_HEALTH)!!.value) // todo dont assert
                        return@forEach
                    }

                    // rip
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
    private val standForAttackKey = NamespacedKey(this.plugin, "stand_for_attack")
    private val invulnKey = NamespacedKey(this.plugin, "invuln")

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
        } catch (_: JsonParseException) {
            null
        }
    }

    private fun removeDeathMessage(player: Player) {
        player.persistentDataContainer.remove(deathMessageKey)
    }

    private fun getStandForAttackTicks(player: Player): Int {
        return player.persistentDataContainer.get(standForAttackKey, PersistentDataType.INTEGER) ?: NOT_DYING
    }

    private fun setStandForAttackTicks(player: Player, ticks: Int) {
        player.persistentDataContainer.set(standForAttackKey, PersistentDataType.INTEGER, ticks)
    }

    /// Returns the new value after decrementing.
    private fun decrementStandForAttackTicks(player: Player): Int {
        val ticks = getStandForAttackTicks(player)
        if (ticks <= NOT_DYING)
            return NOT_DYING
        player.persistentDataContainer.set(standForAttackKey, PersistentDataType.INTEGER, ticks - 1)
        return ticks - 1
    }

    private fun checkPoppingTotem(player: Player): Boolean {
        return (player.persistentDataContainer.get(dyingKey, PersistentDataType.INTEGER) ?: NOT_DYING) == POPPING_TOTEM
    }

    fun setPoppingTotem(player: Player) {
        player.persistentDataContainer.remove(dyingKey) // In case it's a bad value or the wrong type
        player.persistentDataContainer.set(dyingKey, PersistentDataType.INTEGER, POPPING_TOTEM)
        removeInvulnTag(player)
    }

    fun checkInvulnTag(player: Player): Boolean {
        return ((player.persistentDataContainer.get(invulnKey, PersistentDataType.INTEGER) ?: NOT_INVULNERABLE) >= INVULN_WEARING_OFF)
                && (!checkDyingTag(player))
    }

    fun getInvulnTicks(player: Player) : Int {
        return if (!checkDyingTag(player)) player.persistentDataContainer.get(invulnKey, PersistentDataType.INTEGER) ?: NOT_INVULNERABLE
            else NOT_INVULNERABLE
    }

    private fun removeInvulnTag(player: Player) {
        player.persistentDataContainer.remove(invulnKey)
    }

    private fun addInvulnTag(player: Player) {
        player.persistentDataContainer.remove(invulnKey) // In case it's a bad value or the wrong type
        player.persistentDataContainer.set(invulnKey, PersistentDataType.INTEGER, plugin.invulnTicks)
    }

    /// Returns the new value after decrementing.
    private fun decrementInvulnTicks(player: Player) : Int {
        val ticks = getInvulnTicks(player)
        if (ticks <= NOT_INVULNERABLE)
            return NOT_INVULNERABLE
        player.persistentDataContainer.set(invulnKey, PersistentDataType.INTEGER, ticks - 1)
        return ticks - 1
    }

    private fun startDying(player: Player) {
        addDyingTag(player)
        removeInvulnTag(player)
        player.health = 0.1
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

    private fun secondWind(player: Player, playSound: Boolean) {
        if (!checkDyingTag(player) && !checkPoppingTotem(player))
            return
        removeDyingTag(player)
        removeDeathMessage(player)
        setStandForAttackTicks(player, NOT_DYING)
        addInvulnTag(player)
        player.removePotionEffect(PotionEffectType.SLOWNESS)
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE)
        player.removePotionEffect(PotionEffectType.WEAKNESS)
        // TODO remove other bad effects?
        // TODO multiple effects of the same type are supported, so we should only remove the infinite versions?

        player.health = 1.0 // TODO configurable
        player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION,
            100, 0, false, false, false))
        stopForcedCrawl(player)
        plugin.redScreenHandler.clearDyingScreenEffect(player)
        plugin.dyingBossBarHandler.stopDyingBossBar(player)
        if (playSound)
            player.playSound(player, Sound.ITEM_TOTEM_USE, 0.5f, 2.0f) // TODO config
    }

    private fun stopForcedCrawl(player: Player) {
        player.setPose(Pose.STANDING, false)
        plugin.nms.updatePlayerPose(player)
    }

    private fun standForAttack(player: Player) {
        stopForcedCrawl(player)
        setStandForAttackTicks(player, STAND_TICKS)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun checkPlayerLethalDamage(event: EntityDamageEvent) {
        if (event.entity !is Player)
            return
        val player = event.entity as Player
        if (event.cause == EntityDamageEvent.DamageCause.KILL) // TODO configurable ignored damage types
            return // the kill command bypasses second wind
        if ((player.gameMode == GameMode.CREATIVE) || (player.gameMode == GameMode.SPECTATOR))
            return
        if (event.finalDamage >= player.health) { // TODO more robust check
            if (((player.inventory.itemInOffHand.type == Material.TOTEM_OF_UNDYING)
                || (player.inventory.itemInMainHand.type == Material.TOTEM_OF_UNDYING))
                && ((!checkDyingTag(player) || checkPoppingTotem(player)) && !checkInvulnTag(player))) {
                return // Defer to vanilla totem logic.
            }
            if (checkDyingTag(player) || checkInvulnTag(player)) {
                // prevent damage while dying or invulnerable
                event.isCancelled = true
                return
            }

            // Start dying
            storeDeathMessage(player, event.damage, event.damageSource)
            event.damage = 0.0
            startDying(player)

            // Revive killer if a dying player
            val killer = event.damageSource.causingEntity
            if (killer is Player && checkDyingTag(killer))
                secondWind(killer, true)
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
        removeInvulnTag(event.player)
        setStandForAttackTicks(event.player, NOT_DYING)
        event.player.setPose(Pose.DYING, false)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun reviveOnGamemodeChange(event: PlayerGameModeChangeEvent) {
        if (event.isCancelled)
            return
        val gamemode = event.newGameMode
        if ((gamemode == GameMode.CREATIVE) || (gamemode == GameMode.SPECTATOR)) {
            secondWind(event.player, false)
            removeInvulnTag(event.player)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun reviveOnTotem(event: EntityResurrectEvent) {
        if ((event.entity !is Player) || (event.isCancelled))
            return
        secondWind(event.entity as Player, false)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun popTotem(event: PlayerInteractEvent) {
        if (event.action.isRightClick && event.hasItem() && (event.item?.type == Material.TOTEM_OF_UNDYING)
            && checkDyingTag(event.player)) {
            setPoppingTotem(event.player)
            event.player.damage(event.player.getAttribute(Attribute.MAX_HEALTH)!!.value) // todo dont assert
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun noRegenWhileDying(event: EntityRegainHealthEvent) {
        if (event.entity is Player && checkDyingTag(event.entity as Player))
            event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun killIfLeaveGameWhileDying(event: PlayerQuitEvent) {
        removeInvulnTag(event.player)
        if (plugin.killOnQuit && checkDyingTag(event.player)) {
            event.player.health = 0.0 // Bypasses damage event but still triggers death event
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun handlePlayerJoinWhileDying(event: PlayerJoinEvent) {
        removeInvulnTag(event.player)
        if (!plugin.killOnQuit) {
            if (checkDyingTag(event.player)) {
                event.player.setPose(Pose.SWIMMING, true)
            }
        } else {
            if (!event.player.isDead)
                event.player.health = 0.0 // Shouldn't happen, but perhaps the config changed since they last logged in.
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun revivePlayerOnMobKill(event: EntityDeathEvent) {
        if (event.isCancelled)
            return
        if (event.entity !is Mob)
            return
        // TODO config for only direct kills
        val killer = event.damageSource.causingEntity
        if (killer is Player && checkDyingTag(killer)) {
            secondWind(killer, true)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun dyingPlayerStandsOnShootBow(event: EntityShootBowEvent) {
        if (event.isCancelled)
            return
        val shooter = event.entity
        if (shooter !is Player || !checkDyingTag(shooter))
            return

        var eye = shooter.eyeHeight
        standForAttack(shooter)
        eye = shooter.eyeHeight - eye

        val arrow = event.projectile
        val loc = arrow.location
        loc.y += eye
        arrow.teleport(loc)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun dyingPlayerStandsOnLaunchProjectile(event: PlayerLaunchProjectileEvent) {
        if (event.isCancelled)
            return
        if (!checkDyingTag(event.player))
            return

        var eye = event.player.eyeHeight
        standForAttack(event.player)
        eye = event.player.eyeHeight - eye

        val projectile = event.projectile
        val loc = projectile.location
        loc.y += eye
        projectile.teleport(loc)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun dyingPlayerStandsOnMeleeAttack(event: PrePlayerAttackEntityEvent) {
        if (event.isCancelled)
            return
        if (!checkDyingTag(event.player))
            return
        if (event.attacked !is Mob && event.attacked !is Player)
            return

        standForAttack(event.player)
    }
}
