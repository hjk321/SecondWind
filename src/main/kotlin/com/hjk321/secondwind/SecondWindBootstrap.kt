package com.hjk321.secondwind

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.jetbrains.annotations.ApiStatus.Internal

@Internal
@Suppress("unused") // Class invoked by paper-plugin.yml
class SecondWindBootstrap : PluginBootstrap {
    override fun bootstrap(context: BootstrapContext) {
        val manager = context.lifecycleManager
        manager.registerEventHandler<ReloadableRegistrarEvent<Commands?>>(
            LifecycleEvents.COMMANDS
        ) { event: ReloadableRegistrarEvent<Commands?> ->
            val commands = event.registrar()
            commands.register("secondwind", "SecondWind admin command", listOf("sw"),
                SecondWindCommand())
        }
    }
}
