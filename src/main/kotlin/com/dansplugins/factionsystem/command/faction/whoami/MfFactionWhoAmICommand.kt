package com.dansplugins.factionsystem.command.faction.whoami

import com.dansplugins.factionsystem.RemoFactions
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.AQUA
import org.bukkit.ChatColor.GRAY
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level.SEVERE

class MfFactionWhoAmICommand(private val plugin: RemoFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.whoami")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionWhoAmINoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionWhoAmINotAPlayer"]}")
            return true
        }
        val player = sender
        val worldId = player.location.world?.uid
        val chunkX = player.location.chunk.x
        val chunkZ = player.location.chunk.z
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(player)
                    ?: playerService.save(MfPlayer(plugin, player)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionWhoAmIFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val faction = factionService.getFaction(mfPlayer.id)
                val claimService = plugin.services.claimService
                val claim = if (worldId != null) {
                    claimService.getClaim(worldId, chunkX, chunkZ)
                } else {
                    null
                }
                val factionName = faction?.name ?: plugin.language["CommandFactionWhoAmINoFaction"]
                val factionId = faction?.id?.value ?: plugin.language["CommandFactionWhoAmINoFaction"]
                val isInOwnFaction = faction != null && claim?.factionId == faction.id
                sender.sendMessage("$AQUA${plugin.language["CommandFactionWhoAmITitle"]}")
                sender.sendMessage("$GRAY${plugin.language["CommandFactionWhoAmIPlayerName", player.name]}")
                sender.sendMessage("$GRAY${plugin.language["CommandFactionWhoAmIFactionName", factionName]}")
                sender.sendMessage("$GRAY${plugin.language["CommandFactionWhoAmIFactionId", factionId]}")
                sender.sendMessage(
                    "$GRAY${plugin.language["CommandFactionWhoAmIInOwnFaction", if (isInOwnFaction) "True" else "False"]}"
                )
            }
        )
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = emptyList<String>()
}
