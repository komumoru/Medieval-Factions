package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.player.MfPlayerId
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK

class TerritoryRestrictionsListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val restrictions = getRestrictions()
        if (!restrictions.enabled) {
            return
        }
        if (!restrictions.restrictedPlace.matches(event.block.type)) {
            return
        }
        val player = event.player
        if (hasBypass(player)) {
            return
        }
        val claimService = plugin.services.claimService
        val claim = claimService.getClaim(event.block.chunk)
        if (!isPlayerAllowed(player, claim, restrictions.treatAlliesAsMembers)) {
            event.isCancelled = true
            sendMessage(player, restrictions.message)
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val restrictions = getRestrictions()
        if (!restrictions.enabled) {
            return
        }
        if (!restrictions.restrictedBreak.matches(event.block.type)) {
            return
        }
        val player = event.player
        if (hasBypass(player)) {
            return
        }
        val claimService = plugin.services.claimService
        val claim = claimService.getClaim(event.block.chunk)
        if (!isPlayerAllowed(player, claim, restrictions.treatAlliesAsMembers)) {
            event.isCancelled = true
            sendMessage(player, restrictions.message)
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val restrictions = getRestrictions()
        if (!restrictions.enabled) {
            return
        }
        if (event.action != RIGHT_CLICK_BLOCK) {
            return
        }
        val clickedBlock = event.clickedBlock ?: return
        val material = clickedBlock.type
        if (!restrictions.restrictedInteraction.matches(material)) {
            return
        }
        if (restrictions.interactionWhitelist.matches(material)) {
            return
        }
        val player = event.player
        if (hasBypass(player)) {
            return
        }
        val claimService = plugin.services.claimService
        val claim = claimService.getClaim(clickedBlock.chunk)
        if (!isPlayerAllowed(player, claim, restrictions.treatAlliesAsMembers)) {
            event.isCancelled = true
            sendMessage(player, restrictions.message)
        }
    }

    private fun hasBypass(player: Player): Boolean {
        if (player.hasPermission("medievalfactions.bypassrestrictions")) {
            return true
        }
        val mfPlayer = plugin.services.playerService.getPlayer(player)
        return mfPlayer?.isBypassEnabled == true && player.hasPermission("mf.bypass")
    }

    private fun isPlayerAllowed(player: Player, claim: MfClaimedChunk?, treatAlliesAsMembers: Boolean): Boolean {
        if (claim == null) {
            return false
        }
        val claimService = plugin.services.claimService
        val playerId = MfPlayerId.fromBukkitPlayer(player)
        return claimService.isPlayerMemberOrAlly(playerId, claim, treatAlliesAsMembers)
    }

    private fun sendMessage(player: Player, message: String) {
        if (message.isNotBlank()) {
            player.sendMessage(message)
        }
    }

    private fun getRestrictions(): Restrictions {
        val config = plugin.config
        val message = ChatColor.translateAlternateColorCodes(
            '&',
            config.getString("territoryRestrictions.messages.notInOwnTerritory")
                ?: "&cYou can only do that with this block inside your faction's territory."
        )
        return Restrictions(
            enabled = config.getBoolean("territoryRestrictions.enabled"),
            treatAlliesAsMembers = config.getBoolean("territoryRestrictions.treatAlliesAsMembers"),
            restrictedPlace = parseMaterialMatcher("territoryRestrictions.restrictedPlace"),
            restrictedBreak = parseMaterialMatcher("territoryRestrictions.restrictedBreak"),
            interactionWhitelist = parseMaterialMatcher("territoryRestrictions.interactionWhitelist"),
            message = message
        )
    }

    private fun parseMaterialMatcher(path: String): MaterialMatcher {
        val config = plugin.config
        val exact = mutableSetOf<Material>()
        val patterns = mutableSetOf<String>()
        config.getStringList(path).forEach { entry ->
            val value = entry.trim()
            if (value.isEmpty()) {
                return@forEach
            }
            val material = Material.matchMaterial(value, true)
            if (material != null) {
                exact += material
            } else {
                patterns += value.uppercase()
            }
        }
        return MaterialMatcher(exact, patterns)
    }

    private data class Restrictions(
        val enabled: Boolean,
        val treatAlliesAsMembers: Boolean,
        val restrictedPlace: MaterialMatcher,
        val restrictedBreak: MaterialMatcher,
        val interactionWhitelist: MaterialMatcher,
        val message: String
    ) {
        val restrictedInteraction: MaterialMatcher = restrictedPlace + restrictedBreak
    }

    private data class MaterialMatcher(
        val exact: Set<Material>,
        val patterns: Set<String>
    ) {
        fun matches(material: Material): Boolean {
            if (exact.contains(material)) {
                return true
            }
            if (patterns.isEmpty()) {
                return false
            }
            val name = material.name.uppercase()
            return patterns.any { pattern -> name.contains(pattern) }
        }

        operator fun plus(other: MaterialMatcher): MaterialMatcher {
            return MaterialMatcher(
                (exact + other.exact).toSet(),
                (patterns + other.patterns).toSet()
            )
        }
    }
}
