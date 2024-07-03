package com.hjk321.secondwind

import com.hjk321.secondwind.nms.NMS
import com.hjk321.secondwind.nms.SimpleNMS
import org.bstats.bukkit.Metrics
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.annotations.ApiStatus.Internal

const val BSTATS_ID = 22438

@Internal
class SecondWind : JavaPlugin() {
    internal lateinit var nms : NMS
    internal lateinit var dyingPlayerHandler : DyingPlayerHandler
    internal lateinit var redScreenHandler : RedScreenHandler
    internal lateinit var dyingBossActionBarManager: DyingBossActionBarManager
    private lateinit var metrics : Metrics

    // TODO these should eventually be configurable
    var killOnQuit = false
    var dyingTicks = 200

    override fun onEnable() {
        nms = SimpleNMS() // For now, all supported versions can use the same nms code

        dyingPlayerHandler = DyingPlayerHandler(this)
        server.pluginManager.registerEvents(dyingPlayerHandler, this)
        dyingPlayerHandler.startTask()

        redScreenHandler = RedScreenHandler(this)
        server.pluginManager.registerEvents(redScreenHandler, this)

        dyingBossActionBarManager = DyingBossActionBarManager(this)
        server.pluginManager.registerEvents(dyingBossActionBarManager, this)

        metrics = Metrics(this, BSTATS_ID)
        this.logger.info("Enabled!")
    }

    override fun onDisable() {
        if (this::metrics.isInitialized)
            metrics.shutdown()
    }
}
