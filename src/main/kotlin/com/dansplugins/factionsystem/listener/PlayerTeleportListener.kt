package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.RemoFactions
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerTeleportEvent

class PlayerTeleportListener(private val plugin: RemoFactions) : Listener {

    @EventHandler
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        val teleportService = plugin.services.teleportService
        teleportService.cancelTeleportation(event.player)
    }
}
