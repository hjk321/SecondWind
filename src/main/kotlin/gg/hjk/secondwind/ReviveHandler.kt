package gg.hjk.secondwind

import gg.hjk.secondwind.util.UUIDPersistentDataType
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType
import java.nio.ByteBuffer
import java.util.UUID

internal class ReviveHandler(private val plugin: SecondWind) : Listener {

    data class PlayerReviveProgress(val uuid: UUID, val ticks: Int)
    inner class PlayerReviveProgressType : PersistentDataType<ByteArray, PlayerReviveProgress> {
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

    private fun getRevivedBy(player: Player): PlayerReviveProgress? {
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
}
