package com.hjk321.secondwind

import com.hjk321.secondwind.nms.NMS
import com.hjk321.secondwind.nms.SimpleNMS
import org.bukkit.plugin.java.JavaPlugin

class SecondWind : JavaPlugin() {
    lateinit var nms: NMS
    lateinit var dyingPlayerHandler : DyingPlayerHandler
    lateinit var redScreenHandler : RedScreenHandler

    override fun onEnable() {
        nms = SimpleNMS() // For now, all supported versions can use the same nms code
        dyingPlayerHandler = DyingPlayerHandler(this)
        server.pluginManager.registerEvents(dyingPlayerHandler, this)
        redScreenHandler = RedScreenHandler(this)
        server.pluginManager.registerEvents(redScreenHandler, this)
        this.logger.info("Enabled!")
    }
}
