package com.hjk321.secondwind.nms

import org.bukkit.WorldBorder

/// Utility functions for version-specific code or things that may be version-specific in the future.
/// Essentially, anything that is not native bukkit/spigot/paper api.
interface NMS {
    fun isWorldBorderMoving(worldBorder: WorldBorder) : Boolean
    fun getWorldBorderRemainingTime(worldBorder: WorldBorder) : Long
    fun getWorldBorderTargetSize(worldBorder: WorldBorder) : Double
}
