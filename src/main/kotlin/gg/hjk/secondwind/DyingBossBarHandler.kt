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

    companion object {
        const val DYING_MESSAGE = "<bold><red>You are dying!</red></bold>"
        const val BEING_REVIVED_MESSAGE = "<bold><green>Being revived by <name>!</green></bold>"
        const val REVIVING_MESSAGE = "<bold><green>Reviving <name>!</green></bold>"
    }

    // TODO don't reconstruct boss bar names every time
    fun updateBossBars() {
        this.bars.toMap().forEach { (uuid, bar) ->
            run {
                val player = Bukkit.getPlayer(uuid) ?: return@forEach
                var reviveTarget: Player? = null
                plugin.reviveHandler.getReviveTarget(player)?.let { reviveTarget = plugin.server.getPlayer(it) }

                if (plugin.reviveHandler.isBeingRevived(player)) {
                    val ticks = plugin.reviveHandler.getReviveTicks(player)
                    bar.progress(ticks / this.plugin.reviveTicks.toFloat())
                    bar.color(BossBar.Color.GREEN)
                    bar.name(MiniMessage.miniMessage().deserialize(BEING_REVIVED_MESSAGE,
                        Placeholder.component("name", Component.text(
                        plugin.reviveHandler.getRevivedByPlayername(player) ?: "???"))))
                } else if (plugin.dyingPlayerHandler.checkDyingTag(player)) {
                    var ticks = this.plugin.dyingPlayerHandler.getDyingTicks(player) -
                            this.plugin.dyingGracePeriodTicks
                    if (ticks < 0)
                        ticks = 0
                    bar.progress(ticks / this.plugin.dyingTicks.toFloat())
                    bar.color(BossBar.Color.RED)
                    bar.name(MiniMessage.miniMessage().deserialize(DYING_MESSAGE))
                } else if (reviveTarget != null) {
                    val ticks = plugin.reviveHandler.getReviveTicks(reviveTarget)
                    bar.progress(ticks / this.plugin.reviveTicks.toFloat())
                    bar.color(BossBar.Color.GREEN)
                    bar.name(MiniMessage.miniMessage().deserialize(REVIVING_MESSAGE,
                        Placeholder.component("name", Component.text(reviveTarget.name))))
                } else {
                    // Clean up boss bar
                    stopBossBar(player)
                }
            }
        }
    }

    private val bars : HashMap<UUID, BossBar> = HashMap()

    private fun constructDyingBossBar() : BossBar {
        val name = MiniMessage.miniMessage().deserialize(DYING_MESSAGE)
        return BossBar.bossBar(name, 1.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS)
            .addFlag(BossBar.Flag.DARKEN_SCREEN) // .addFlag(BossBar.Flag.CREATE_WORLD_FOG) // TODO configurable
    }

    private fun constructRevivingBossBar(player: Player) : BossBar {
        var reviveTarget: Player? = null
        plugin.reviveHandler.getReviveTarget(player)?.let { reviveTarget = plugin.server.getPlayer(it) }
        val name = MiniMessage.miniMessage().deserialize(REVIVING_MESSAGE,
            Placeholder.component("name", Component.text(reviveTarget?.name ?: "???")))
        return BossBar.bossBar(name, 0.0f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS)
    }

    fun startBossBar(player: Player, isReviving: Boolean) {
        val oldBar = bars[player.uniqueId]
        if (oldBar != null) {
            player.hideBossBar(oldBar)
            bars.remove(player.uniqueId)
        }

        val bar = if (isReviving) constructRevivingBossBar(player) else constructDyingBossBar()
        bars[player.uniqueId] = bar
        player.showBossBar(bar)
    }

    private fun stopBossBar(player: Player) {
        val bar = bars[player.uniqueId] ?: return
        player.hideBossBar(bar)
        bars.remove(player.uniqueId)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun removeBarOnLeave(event: PlayerQuitEvent) {
        if (bars.containsKey(event.player.uniqueId))
            stopBossBar(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun addBarOnJoin(event: PlayerJoinEvent) {
        bars.remove(event.player.uniqueId)
        if (plugin.dyingPlayerHandler.checkDyingTag(event.player))
            startBossBar(event.player, false)
        else
            stopBossBar(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun removeBarOnDeath(event: PlayerDeathEvent) {
        if (event.isCancelled)
            return
        if (bars.containsKey(event.player.uniqueId))
            stopBossBar(event.player)
    }
}
