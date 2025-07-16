package gg.hjk.secondwind

import gg.hjk.secondwind.nms.NMS
import gg.hjk.secondwind.nms.SimpleNMS
import org.bstats.bukkit.Metrics
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.annotations.ApiStatus.Internal
import kotlin.math.pow

@Internal
class SecondWind : JavaPlugin() {

    companion object {
        const val BSTATS_ID = 22438
    }

    internal lateinit var nms : NMS
    internal lateinit var dyingPlayerHandler : DyingPlayerHandler
    internal lateinit var redScreenHandler : RedScreenHandler
    internal lateinit var dyingBossBarHandler: DyingBossBarHandler
    internal lateinit var reviveHandler : ReviveHandler
    private lateinit var playerTickTask: PlayerTickTask
    private lateinit var metrics : Metrics

    // TODO these should eventually be configurable
    var killOnQuit = false // FIXME broken if true
    var dyingTicks = 200
    var dyingGracePeriodTicks = 6
    var invulnTicks = 30
    var reviveTicks = 80
    var reviveRadiusSquared = 5.0.pow(2)
    var failedReviveMaxGracePeriodTicks = 30

    override fun onEnable() {
        nms = SimpleNMS() // For now, all supported versions can use the same nms code

        dyingPlayerHandler = DyingPlayerHandler(this)
        server.pluginManager.registerEvents(dyingPlayerHandler, this)

        redScreenHandler = RedScreenHandler(this)
        server.pluginManager.registerEvents(redScreenHandler, this)

        dyingBossBarHandler = DyingBossBarHandler(this)
        server.pluginManager.registerEvents(dyingBossBarHandler, this)

        reviveHandler = ReviveHandler(this)
        server.pluginManager.registerEvents(reviveHandler, this)

        playerTickTask = PlayerTickTask(this)
        playerTickTask.register()

        metrics = Metrics(this, BSTATS_ID)
        this.logger.info("Enabled!")
    }

    override fun onDisable() {
        if (this::metrics.isInitialized)
            metrics.shutdown()
    }

    private class PlayerTickTask(private val plugin: SecondWind) : Runnable {
        private var isEvenTick = true
        override fun run() {
            plugin.server.onlinePlayers.forEach { player ->
                if (!player.isValid || player.isDead)
                    return@forEach
                plugin.reviveHandler.tickRevive(player)
                plugin.dyingPlayerHandler.tickDyingPlayer(player)
            }
            isEvenTick = !isEvenTick
            if (isEvenTick)
                plugin.dyingBossBarHandler.updateBossBars()
        }

        fun register() {
            Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, 1, 1)
        }
    }
}
