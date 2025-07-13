package gg.hjk.secondwind.nms

import net.kyori.adventure.text.Component
import org.bukkit.WorldBorder
import org.bukkit.damage.DamageSource
import org.bukkit.entity.Player

/// Utility functions for internals or version-specific API.
internal interface NMS {
    fun isWorldBorderMoving(worldBorder: WorldBorder) : Boolean
    fun getWorldBorderRemainingTime(worldBorder: WorldBorder) : Long
    fun getWorldBorderTargetSize(worldBorder: WorldBorder) : Double
    fun getDeathMessage(player: Player, damage: Double, source: DamageSource) : Component
}
