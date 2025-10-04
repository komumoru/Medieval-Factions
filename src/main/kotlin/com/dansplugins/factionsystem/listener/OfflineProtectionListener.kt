package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFactionId
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.entity.EntityExplodeEvent

class OfflineProtectionListener(private val plugin: MedievalFactions) : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        val location = event.location
        handleExplosion(event.blockList(), location?.world, location?.chunk) {
            event.blockList().clear()
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        handleExplosion(event.blockList(), event.block.world, event.block.chunk) {
            event.blockList().clear()
            event.isCancelled = true
        }
    }

    private fun handleExplosion(
        blocks: MutableList<Block>,
        world: World?,
        originChunk: Chunk?,
        cancel: () -> Unit
    ) {
        val config = plugin.config
        if (!config.getBoolean("offlineBlastProtection.enabled")) {
            return
        }
        if (world == null) {
            return
        }
        val exemptWorlds = config.getStringList("offlineBlastProtection.exemptWorlds").map(String::lowercase)
        if (exemptWorlds.contains(world.name.lowercase())) {
            return
        }
        val claimService = plugin.services.claimService
        val allowWhenAnyMemberOnline = config.getBoolean("offlineBlastProtection.allowWhenAnyMemberOnline", true)
        val onlyBlockDamage = config.getBoolean("offlineBlastProtection.onlyBlockDamage", true)
        val factionsToProtect = mutableMapOf<MfFactionId, Boolean>()
        val protectedBlocks = if (blocks.isEmpty()) {
            emptyList()
        } else {
            blocks.filter { block ->
                val claim = claimService.getClaim(block.chunk) ?: return@filter false
                shouldProtect(claim, allowWhenAnyMemberOnline, factionsToProtect)
            }
        }
        val originProtected = if (!onlyBlockDamage && originChunk != null) {
            val claim = claimService.getClaim(originChunk)
            claim != null && shouldProtect(claim, allowWhenAnyMemberOnline, factionsToProtect)
        } else {
            false
        }
        if (protectedBlocks.isEmpty() && !originProtected) {
            return
        }
        if (onlyBlockDamage) {
            blocks.removeAll(protectedBlocks.toSet())
        } else {
            cancel()
        }
    }

    private fun shouldProtect(
        claim: MfClaimedChunk,
        allowWhenAnyMemberOnline: Boolean,
        cache: MutableMap<MfFactionId, Boolean>
    ): Boolean {
        return cache.getOrPut(claim.factionId) {
            if (!allowWhenAnyMemberOnline) {
                return@getOrPut true
            }
            val factionService = plugin.services.factionService
            !factionService.hasOnlineMember(claim.factionId)
        }
    }
}
