package gg.hjk.secondwind

import io.papermc.paper.plugin.loader.PluginClasspathBuilder
import io.papermc.paper.plugin.loader.PluginLoader
import org.jetbrains.annotations.ApiStatus.Internal

@Internal
@Suppress("unused") // Class invoked by paper-plugin.yml
class SecondWindLoader : PluginLoader {
    override fun classloader(classpathBuilder: PluginClasspathBuilder) {
    }
}
