package ru.lewis.battlepass.listener

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import ru.lewis.battlepass.menu.BattlePassMenu
import ru.lewis.battlepass.service.BattlePassService

@Singleton
class ServerLoadedListener @Inject constructor(
    private val battlePassService: BattlePassService,
    private val battlePassMenu: BattlePassMenu,
) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        battlePassService.onPlayerJoin(event.player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        battlePassService.onPlayerQuit(event.player)
    }
}
