package gg.hjk.secondwind

import gg.hjk.secondwind.nms.NMS
import gg.hjk.secondwind.nms.SimpleNMS
import org.bstats.bukkit.Metrics
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.annotations.ApiStatus.Internal

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
    private lateinit var metrics : Metrics

    // TODO these should eventually be configurable
    var killOnQuit = false
    var dyingTicks = 200
    var dyingGracePeriodTicks = 8
    var invulnTicks = 30

    override fun onEnable() {
        nms = SimpleNMS() // For now, all supported versions can use the same nms code

        dyingPlayerHandler = DyingPlayerHandler(this)
        server.pluginManager.registerEvents(dyingPlayerHandler, this)
        dyingPlayerHandler.startTask()

        redScreenHandler = RedScreenHandler(this)
        server.pluginManager.registerEvents(redScreenHandler, this)

        dyingBossBarHandler = DyingBossBarHandler(this)
        server.pluginManager.registerEvents(dyingBossBarHandler, this)
        dyingBossBarHandler.startTask()

        reviveHandler = ReviveHandler(this)
        server.pluginManager.registerEvents(reviveHandler, this)

        metrics = Metrics(this, BSTATS_ID)
        this.logger.info("Enabled!")
    }

    override fun onDisable() {
        if (this::metrics.isInitialized)
            metrics.shutdown()
    }
}
