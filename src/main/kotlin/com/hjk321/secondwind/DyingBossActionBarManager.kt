package com.hjk321.secondwind

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID

internal class DyingBossActionBarManager(private val plugin: SecondWind) : Listener {
    private val bars : HashMap<UUID, BossBar> = HashMap()

    private fun constructDyingBossBar() : BossBar {
        val name = MiniMessage.miniMessage().deserialize("<bold><red>You are dying!</red></bold>")
        return BossBar.bossBar(name, 1.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS)
    }

    private fun constructRevivingBossBar(revivedName : String) : BossBar {
        val name = MiniMessage.miniMessage().deserialize("<bold><green>Reviving <name>!</green></bold>",
            Placeholder.component("name", Component.text(revivedName)))
        return BossBar.bossBar(name, 0.0f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS)
    }

    private fun constructBeingRevivedBossBar(reviverName : String) : BossBar {
        val name = MiniMessage.miniMessage().deserialize("<bold><green>Being revived by <name>!</green></bold>",
            Placeholder.component("name", Component.text(reviverName)))
        return BossBar.bossBar(name, 0.0f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS)
    }

    fun startDyingBossBar(player: Player) {
        if (bars.containsKey(player.uniqueId))
            throw IllegalStateException() // TODO remove later, but during development I need to notice invalid state
        val bar = constructDyingBossBar()
        bars[player.uniqueId] = bar
        player.showBossBar(bar)
    }

    fun stopDyingBossBar(player: Player) {
        val bar = bars[player.uniqueId] ?: throw IllegalStateException()
        bar.removeViewer(player)
        bars.remove(player.uniqueId)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun removeBarOnLeave(event: PlayerQuitEvent) {
        if (bars.containsKey(event.player.uniqueId))
            stopDyingBossBar(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun addBarOnJoin(event: PlayerJoinEvent) {
        if (bars.containsKey(event.player.uniqueId))
            return
        startDyingBossBar(event.player)
    }
}
