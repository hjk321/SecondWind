package com.hjk321.secondwind

import org.bukkit.plugin.java.JavaPlugin

@Suppress("unused") // Class invoked by paper-plugin.yml
class SecondWind : JavaPlugin() {
    override fun onEnable() {
        this.logger.info("Enabled!")
    }
}
