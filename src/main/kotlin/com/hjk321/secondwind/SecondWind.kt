package com.hjk321.secondwind

import org.bukkit.plugin.java.JavaPlugin

@Suppress("unused") // Class invoked by paper-plugin.yml
class SecondWind : JavaPlugin() {
    lateinit var dyingPlayerHandler : DyingPlayerHandler
    lateinit var redScreenHandler : RedScreenHandler

    override fun onEnable() {
        dyingPlayerHandler = DyingPlayerHandler(this)
        server.pluginManager.registerEvents(dyingPlayerHandler, this)
        redScreenHandler = RedScreenHandler(this)
        server.pluginManager.registerEvents(redScreenHandler, this)
        this.logger.info("Enabled!")
    }
}
