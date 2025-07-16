package gg.hjk.secondwind

import gg.hjk.secondwind.util.UUIDPersistentDataType
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType
import java.nio.ByteBuffer
import java.util.UUID

internal class ReviveHandler(private val plugin: SecondWind) : Listener {

    private fun cleanupReviveState(player: Player, reviver: Player?) {
        removeRevivedBy(player)
        reviver?.let {
            if (getReviveTarget(it) == player.uniqueId)
                removeReviveTarget(it)
        }
    }

    fun tickRevive(player: Player) {
        val reviveProgress = getRevivedBy(player) ?: return
        val reviver = plugin.server.getPlayer(reviveProgress.uuid)

        if (!plugin.dyingPlayerHandler.checkDyingTag(player)) {
            // Being revived but not dying? How did we get here?
            cleanupReviveState(player, reviver)
            return
        }

        // Invalidate if the reviver is offline, no longer targeting this player,
        // in a different world, dead, dyinig, or are too far away.
        if (reviver == null ||
            !reviver.isValid || reviver.isDead ||
            plugin.dyingPlayerHandler.checkDyingTag(reviver) ||
            getReviveTarget(reviver) != player.uniqueId ||
            player.world.uid != reviver.world.uid ||
            player.location.distanceSquared(reviver.location) > plugin.reviveRadiusSquared
        ) {
            // Add a few extra ticks to the dying player's timer
            var ticks = plugin.dyingPlayerHandler.getDyingTicks(player)
            if (ticks < plugin.dyingGracePeriodTicks)
                ticks = plugin.dyingGracePeriodTicks
            ticks += reviveProgress.ticks.coerceAtMost(plugin.failedReviveMaxGracePeriodTicks)
            if (ticks > plugin.dyingTicks + plugin.dyingGracePeriodTicks)
                ticks = plugin.dyingTicks + plugin.dyingGracePeriodTicks

            plugin.dyingPlayerHandler.setDyingTicks(player, ticks)
            cleanupReviveState(player, reviver)
            return
        }

        // Increment progress and check for completion
        if (incrementRevivedBy(player) >= plugin.reviveTicks) {
            cleanupReviveState(player, reviver)
            plugin.dyingPlayerHandler.secondWind(player, true)
        }
    }

    private data class PlayerReviveProgress(val uuid: UUID, var ticks: Int)
    private inner class PlayerReviveProgressType : PersistentDataType<ByteArray, PlayerReviveProgress> {
        override fun getPrimitiveType(): Class<ByteArray> = ByteArray::class.java
        override fun getComplexType(): Class<PlayerReviveProgress> = PlayerReviveProgress::class.java

        override fun toPrimitive(complex: PlayerReviveProgress, context: PersistentDataAdapterContext): ByteArray {
            val byteBuffer = ByteBuffer.wrap(ByteArray(20))
            byteBuffer.putLong(complex.uuid.mostSignificantBits)
            byteBuffer.putLong(complex.uuid.leastSignificantBits)
            byteBuffer.putInt(complex.ticks)
            return byteBuffer.array()
        }

        override fun fromPrimitive(primitive: ByteArray, context: PersistentDataAdapterContext): PlayerReviveProgress {
            val byteBuffer = ByteBuffer.wrap(primitive)
            val uuid = UUID(byteBuffer.getLong(), byteBuffer.getLong())
            val ticks = byteBuffer.getInt()
            return PlayerReviveProgress(uuid, ticks)
        }
    }
    private val playerReviveProgressType = PlayerReviveProgressType()

    private val reviveTargetKey = NamespacedKey(this.plugin, "revive_target")
    private val revivedByKey = NamespacedKey(this.plugin, "revived_by")

    private fun getReviveTarget(player: Player): UUID? {
        return player.persistentDataContainer.get(reviveTargetKey, UUIDPersistentDataType.uuidType)
    }

    private fun setReviveTarget(player: Player, target: Player) {
        player.persistentDataContainer.remove(reviveTargetKey) // In case it's a bad value or the wrong type
        player.persistentDataContainer.set(reviveTargetKey, UUIDPersistentDataType.uuidType, target.uniqueId)
    }

    private fun removeReviveTarget(player: Player) {
        player.persistentDataContainer.remove(reviveTargetKey)
    }

    fun isBeingRevived(player: Player) : Boolean {
        return getRevivedBy(player) != null
    }

    private fun getRevivedBy(player: Player) : PlayerReviveProgress? {
        return player.persistentDataContainer.get(revivedByKey, playerReviveProgressType)
    }

    private fun setRevivedBy(player: Player, revivedBy: Player) {
        player.persistentDataContainer.remove(revivedByKey) // In case it's a bad value or the wrong type
        player.persistentDataContainer.set(revivedByKey, playerReviveProgressType,
            PlayerReviveProgress(revivedBy.uniqueId, 0))
    }

    private fun removeRevivedBy(player: Player) {
        player.persistentDataContainer.remove(revivedByKey)
    }

    private fun incrementRevivedBy(player: Player) : Int {
        val revivedProgress = getRevivedBy(player) ?: return 0
        val ticks = ++revivedProgress.ticks
        player.persistentDataContainer.remove(revivedByKey) // In case it's a bad value or the wrong type
        player.persistentDataContainer.set(revivedByKey, playerReviveProgressType, revivedProgress)
        return ticks
    }

    private fun tryStartReviveTarget(player: Player, target: Player) {
        val dyingPlayerHandler = this.plugin.dyingPlayerHandler
        val previouslyRevivedBy = getRevivedBy(target)?.uuid
        if (dyingPlayerHandler.checkDyingTag(player) || !dyingPlayerHandler.checkDyingTag(target))
            return
        if (previouslyRevivedBy != null && player.uniqueId != previouslyRevivedBy) // Target being revived by someone else
            return
        
        setReviveTarget(player, target)
        if (previouslyRevivedBy == null)
            setRevivedBy(target, player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun tryReviveOnRightClick(event: PlayerInteractEntityEvent) {
        if (event.rightClicked !is Player)
            return
        tryStartReviveTarget(event.player, event.rightClicked as Player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun cleanupReviveOnLeave(event: PlayerQuitEvent) {
        removeReviveTarget(event.player)
        removeRevivedBy(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun cleanupReviveOnJoin(event: PlayerJoinEvent) {
        removeReviveTarget(event.player)
        removeRevivedBy(event.player)
    }
}
