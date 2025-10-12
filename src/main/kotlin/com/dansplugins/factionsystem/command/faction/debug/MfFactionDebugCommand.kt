package com.dansplugins.factionsystem.command.faction.debug

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

class MfFactionDebugCommand(private val plugin: RemoFactions) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.debug")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDebugNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionDebugNotAPlayer"]}")
            return true
        }
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender) ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                    sender.sendMessage("$RED${plugin.language["CommandFactionBypassFailedToSavePlayer"]}")
                    plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                    return@Runnable
                }
                val factionService = plugin.services.factionService
                val relationshipService = plugin.services.factionRelationshipService
                val claimService = plugin.services.claimService

                val playerFaction = factionService.getFaction(mfPlayer.id)
                val role = playerFaction?.getRole(mfPlayer.id)
                val location = sender.location
                val worldName = location.world?.name ?: "Unknown"
                val chunk = location.chunk
                val claim = claimService.getClaim(chunk)
                val claimFaction = claim?.factionId?.let(factionService::getFaction)

                val yes = plugin.language["CommandFactionDebugYes"]
                val no = plugin.language["CommandFactionDebugNo"]
                fun format(value: Boolean) = if (value) yes else no

                sender.sendMessage("$AQUA${plugin.language["CommandFactionDebugHeader", sender.name]}")
                sender.sendMessage("$GRAY${plugin.language["CommandFactionDebugPlayerId", mfPlayer.id.value]}")
                if (playerFaction != null) {
                    sender.sendMessage("$GRAY${plugin.language["CommandFactionDebugFaction", playerFaction.name, playerFaction.id.value]}")
                    val roleName = role?.name ?: plugin.language["CommandFactionDebugNoRole"]
                    sender.sendMessage("$GRAY${plugin.language["CommandFactionDebugRole", roleName]}")
                } else {
                    sender.sendMessage("$GRAY${plugin.language["CommandFactionDebugNoFaction"]}")
                }
                sender.sendMessage(
                    "$GRAY${plugin.language["CommandFactionDebugBypass", format(mfPlayer.isBypassEnabled), format(sender.hasPermission("mf.bypass"))]}"
                )
                sender.sendMessage(
                    "$GRAY${plugin.language[
                        "CommandFactionDebugLocation",
                        worldName,
                        location.blockX.toString(),
                        location.blockY.toString(),
                        location.blockZ.toString(),
                        chunk.x.toString(),
                        chunk.z.toString()
                    ]}"
                )
                if (claim != null) {
                    if (claimFaction != null) {
                        sender.sendMessage(
                            "$GRAY${plugin.language["CommandFactionDebugChunkOwner", claimFaction.name, claimFaction.id.value]}"
                        )
                    } else {
                        sender.sendMessage(
                            "$GRAY${plugin.language["CommandFactionDebugChunkOwnerUnknown", claim.factionId.value]}"
                        )
                    }
                } else {
                    sender.sendMessage("$GRAY${plugin.language["CommandFactionDebugChunkOwnerWilderness"]}")
                }

                val interactionAllowed = when {
                    claim == null -> true
                    playerFaction == null -> false
                    else -> claimService.isInteractionAllowed(mfPlayer.id, claim)
                }
                sender.sendMessage("$GRAY${plugin.language["CommandFactionDebugInteractionAllowed", format(interactionAllowed)]}")

                if (claim != null && playerFaction != null) {
                    val sameFaction = playerFaction.id == claim.factionId
                    sender.sendMessage("$GRAY${plugin.language["CommandFactionDebugMemberMatch", format(sameFaction)]}")

                    val alliesFlag = claimFaction?.flags?.get(plugin.flags.alliesCanInteractWithLand) == true
                    val playerIsAlly = relationshipService.getAllies(claim.factionId).contains(playerFaction.id)
                    sender.sendMessage(
                        "$GRAY${plugin.language["CommandFactionDebugAllyAccess", format(alliesFlag), format(playerIsAlly)]}"
                    )

                    val vassalFlag = claimFaction?.flags?.get(plugin.flags.vassalageTreeCanInteractWithLand) == true
                    val playerInVassalTree = relationshipService.getVassalTree(claim.factionId).contains(playerFaction.id)
                    sender.sendMessage(
                        "$GRAY${plugin.language["CommandFactionDebugVassalAccess", format(vassalFlag), format(playerInVassalTree)]}"
                    )

                    val liegeFlag = claimFaction?.flags?.get(plugin.flags.liegeChainCanInteractWithLand) == true
                    val playerInLiegeChain = relationshipService.getLiegeChain(claim.factionId).contains(playerFaction.id)
                    sender.sendMessage(
                        "$GRAY${plugin.language["CommandFactionDebugLiegeAccess", format(liegeFlag), format(playerInLiegeChain)]}"
                    )

                    val wartimeEnabled = plugin.config.getBoolean("pvp.enableWartimeBlockDestruction")
                    val warOpponents = relationshipService.getFactionsAtWarWith(claim.factionId)
                    val atWarWithPlayer = playerFaction.id in warOpponents
                    sender.sendMessage(
                        "$GRAY${plugin.language["CommandFactionDebugWarAccess", format(wartimeEnabled), format(atWarWithPlayer)]}"
                    )
                }

                val restrictionsEnabled = plugin.config.getBoolean("territoryRestrictions.enabled")
                val treatAlliesAsMembers = plugin.config.getBoolean("territoryRestrictions.treatAlliesAsMembers")
                val restrictionsAllow = if (!restrictionsEnabled) {
                    true
                } else if (claim == null) {
                    false
                } else {
                    claimService.isPlayerMemberOrAlly(mfPlayer.id, claim, treatAlliesAsMembers)
                }

                sender.sendMessage(
                    "$GRAY${plugin.language["CommandFactionDebugRestrictionsEnabled", format(restrictionsEnabled)]}"
                )
                sender.sendMessage(
                    "$GRAY${plugin.language["CommandFactionDebugRestrictionsTreatAllies", format(treatAlliesAsMembers)]}"
                )
                sender.sendMessage(
                    "$GRAY${plugin.language["CommandFactionDebugRestrictionsAllow", format(restrictionsAllow)]}"
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
