package gg.hjk.secondwind.util

import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType
import java.nio.ByteBuffer
import java.util.UUID

// TODO: If I ever make a "core" library, this should be moved to it.
class UUIDPersistentDataType : PersistentDataType<ByteArray, UUID> {
    companion object {
        val uuidType = UUIDPersistentDataType()
    }

    override fun getPrimitiveType(): Class<ByteArray> = ByteArray::class.java
    override fun getComplexType(): Class<UUID> = UUID::class.java

    override fun toPrimitive(complex: UUID, context: PersistentDataAdapterContext): ByteArray {
        val byteBuffer = ByteBuffer.wrap(ByteArray(16))
        byteBuffer.putLong(complex.mostSignificantBits)
        byteBuffer.putLong(complex.leastSignificantBits)
        return byteBuffer.array()
    }

    override fun fromPrimitive(primitive: ByteArray, context: PersistentDataAdapterContext): UUID {
        val byteBuffer = ByteBuffer.wrap(primitive)
        return UUID(byteBuffer.getLong(), byteBuffer.getLong())
    }
}
