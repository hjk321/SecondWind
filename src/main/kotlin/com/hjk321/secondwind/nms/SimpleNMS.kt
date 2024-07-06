package com.hjk321.secondwind.nms

import io.papermc.paper.adventure.PaperAdventure
import net.kyori.adventure.text.Component
import net.minecraft.world.level.border.BorderStatus
import org.bukkit.WorldBorder
import org.bukkit.craftbukkit.CraftWorldBorder
import org.bukkit.craftbukkit.damage.CraftDamageSource
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.damage.DamageSource
import org.bukkit.entity.Player

/// For now at least, there's no variance between versions, so we will just implement nms here
internal class SimpleNMS : NMS {
    override fun isWorldBorderMoving(worldBorder: WorldBorder) : Boolean {
        if (worldBorder !is CraftWorldBorder)
            return false // :(
        val wb = worldBorder.handle
        return (wb.status != BorderStatus.STATIONARY)
    }

    override fun getWorldBorderRemainingTime(worldBorder: WorldBorder) : Long {
        if (worldBorder !is CraftWorldBorder)
            return 0 // :(
        val wb = worldBorder.handle
        if (wb.status == BorderStatus.STATIONARY)
            return 0
        return wb.lerpRemainingTime
    }

    override fun getWorldBorderTargetSize(worldBorder: WorldBorder): Double {
        if (worldBorder !is CraftWorldBorder)
            return worldBorder.size // :(
        val wb = worldBorder.handle
        if (wb.status == BorderStatus.STATIONARY)
            return worldBorder.size
        return wb.lerpTarget
    }

    override fun getDeathMessage(player: Player, damage: Double, source: DamageSource): Component {
        if (player !is CraftPlayer || source !is CraftDamageSource)
            return Component.translatable("death.attack.generic", player.displayName()) // :(
        val tracker = player.handle.combatTracker
        tracker.recordDamage(source.handle, damage.toFloat())
        return PaperAdventure.asAdventure(tracker.deathMessage)
    }
}
