package ru.lewis.battlepass.bootstrap

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Stage
import org.bukkit.plugin.java.JavaPlugin
import ru.lewis.battlepass.Main
import ru.lewis.battlepass.bootstrap.module.InjectionModule

class Bootstrap : JavaPlugin() {
    private var disabled: Boolean = false

    private lateinit var entryPoint: Main

    override fun onEnable() {
        try {
            injector = Guice.createInjector(
                Stage.PRODUCTION,
                InjectionModule(this)
            )
            entryPoint = injector.getInstance(Main::class.java)
            entryPoint.enable()
        } catch (e: Throwable) {
            slF4JLogger.error("Failed to enable", e)
            server.scheduler.runTask(this, Runnable { server.pluginManager.disablePlugin(this) })
        }
    }

    override fun onDisable() {
        if (::entryPoint.isInitialized && !disabled) {
            disabled = true
            entryPoint.disable()
        }
    }

    companion object {
        lateinit var injector: Injector
    }
}
