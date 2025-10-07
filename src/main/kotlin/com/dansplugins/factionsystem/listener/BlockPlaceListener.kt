package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.RemoFactions
import com.dansplugins.factionsystem.area.MfBlockPosition
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.RED
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import java.util.Locale
import java.util.logging.Level

class BlockPlaceListener(private val plugin: RemoFactions) : Listener {

    private val invalidWildernessRestrictedBlockEntries = mutableSetOf<String>()

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val gateService = plugin.services.gateService
        val blockPosition = MfBlockPosition.fromBukkitBlock(event.block)
        val gates = gateService.getGatesAt(blockPosition)
        if (gates.isNotEmpty()) {
            event.isCancelled = true
            event.player.sendMessage("$RED${plugin.language["CannotPlaceBlockInGate"]}")
            return
        }

        val claimService = plugin.services.claimService
        val claim = claimService.getClaim(event.block.chunk)
        if (claim == null) {
            val preventAllPlacement = plugin.config.getBoolean("wilderness.place.prevent", false)
            val blockIsRestricted =
                !preventAllPlacement && getWildernessRestrictedBlocks().contains(event.block.type)
            if (preventAllPlacement || blockIsRestricted) {
                event.isCancelled = true
                if (plugin.config.getBoolean("wilderness.place.alert", true)) {
                    event.player.sendMessage("$RED${plugin.language["CannotPlaceBlockInWilderness"]}")
                }
            }
            return
        }
        val factionService = plugin.services.factionService
        val claimFaction = factionService.getFaction(claim.factionId) ?: return
        val relationshipService = plugin.services.factionRelationshipService
        val playerService = plugin.services.playerService
        val mfPlayer = playerService.getPlayer(event.player)
        if (mfPlayer == null) {
            event.isCancelled = true
            plugin.server.scheduler.runTaskAsynchronously(
                plugin,
                Runnable {
                    playerService.save(MfPlayer(plugin, event.player)).onFailure {
                        event.player.sendMessage("$RED${plugin.language["BlockPlaceFailedToSavePlayer"]}")
                        plugin.logger.log(Level.SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                }
            )
            return
        }
        val playerFaction = factionService.getFaction(mfPlayer.id)
        if (!claimService.isInteractionAllowed(mfPlayer.id, claim)) {
            if (mfPlayer.isBypassEnabled && event.player.hasPermission("mf.bypass")) {
                event.player.sendMessage("$RED${plugin.language["FactionTerritoryProtectionBypassed"]}")
            } else if (playerFaction != null && relationshipService.getFactionsAtWarWith(playerFaction.id).contains(claimFaction.id)) {
                if (event.block.type != Material.LADDER || !plugin.config.getBoolean("factions.laddersPlaceableInEnemyFactionTerritory")) {
                    event.isCancelled = true
                    event.player.sendMessage("$RED${plugin.language["CannotPlaceBlockInFactionTerritory", claimFaction.name]}")
                }
            } else {
                event.isCancelled = true
                event.player.sendMessage("$RED${plugin.language["CannotPlaceBlockInFactionTerritory", claimFaction.name]}")
            }
        }
    }

    private fun getWildernessRestrictedBlocks(): Set<Material> {
        val rawEntries: List<String>? = plugin.config.getStringList("wilderness.place.restrictedBlocks")
        val materials = mutableSetOf<Material>()
        rawEntries.orEmpty().forEach { rawEntry ->
            val entry = rawEntry.trim()
            if (entry.isEmpty()) {
                return@forEach
            }
            val normalizedName = entry.substringAfter(':', entry).uppercase(Locale.ROOT)
            val material = runCatching { Material.valueOf(normalizedName) }.getOrNull()
            if (material != null) {
                materials += material
            } else {
                if (invalidWildernessRestrictedBlockEntries.add(normalizedName)) {
                    plugin.logger.log(
                        Level.WARNING,
                        "Unknown material '$entry' in wilderness.place.restrictedBlocks; ignoring."
                    )
                }
            }
        }
        return materials
    }
}
