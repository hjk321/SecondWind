package gg.hjk.secondwind

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import kotlin.collections.component1
import kotlin.collections.component2

internal class DyingBossBarHandler(private val plugin: SecondWind) : Listener {

    fun updateBossBars() {
        this.bars.forEach { (uuid, bar) ->
            run {
                val player = Bukkit.getPlayer(uuid) ?: return
                var ticks = this.plugin.dyingPlayerHandler.getDyingTicks(player) -
                        this.plugin.dyingGracePeriodTicks
                if (ticks < 0)
                    ticks = 0
                bar.progress(ticks / this.plugin.dyingTicks.toFloat())
            }
        }
    }

    private val bars : HashMap<UUID, BossBar> = HashMap()

    private fun constructDyingBossBar() : BossBar {
        val name = MiniMessage.miniMessage().deserialize("<bold><red>You are dying!</red></bold>")
        return BossBar.bossBar(name, 1.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS)
            .addFlag(BossBar.Flag.DARKEN_SCREEN) // .addFlag(BossBar.Flag.CREATE_WORLD_FOG) // TODO configurable
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
            .addFlag(BossBar.Flag.DARKEN_SCREEN) // .addFlag(BossBar.Flag.CREATE_WORLD_FOG) // TODO configurable
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
       player.hideBossBar(bar)
        bars.remove(player.uniqueId)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun removeBarOnLeave(event: PlayerQuitEvent) {
        if (bars.containsKey(event.player.uniqueId))
            stopDyingBossBar(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun addBarOnJoin(event: PlayerJoinEvent) {
        bars.remove(event.player.uniqueId)
        if (plugin.dyingPlayerHandler.checkDyingTag(event.player))
            startDyingBossBar(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun removeBarOnDeath(event: PlayerDeathEvent) {
        if (event.isCancelled)
            return
        if (bars.containsKey(event.player.uniqueId))
            stopDyingBossBar(event.player)
    }
}
