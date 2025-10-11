package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.RemoFactions
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFactionId
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.block.BlockFormEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockGrowEvent
import org.bukkit.event.block.BlockMultiPlaceEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntityChangeBlockEvent

class OfflineProtectionListener(private val plugin: RemoFactions) : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        handleBlockChange(listOf(event.block), originChunk = null, isBlockDamage = true) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        handleBlockChange(listOf(event.block), originChunk = null, isBlockDamage = false) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockMultiPlace(event: BlockMultiPlaceEvent) {
        val blocks = mutableSetOf<Block>()
        blocks += event.block
        event.replacedBlockStates.mapTo(blocks) { it.block }
        handleBlockChange(blocks, originChunk = null, isBlockDamage = false) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockBurn(event: BlockBurnEvent) {
        handleBlockChange(listOf(event.block), originChunk = null, isBlockDamage = true) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockFade(event: BlockFadeEvent) {
        handleBlockChange(listOf(event.block), originChunk = null, isBlockDamage = true) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockForm(event: BlockFormEvent) {
        handleBlockChange(listOf(event.block), originChunk = null, isBlockDamage = false) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockSpread(event: BlockSpreadEvent) {
        handleBlockChange(listOf(event.block), originChunk = null, isBlockDamage = false) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockGrow(event: BlockGrowEvent) {
        handleBlockChange(listOf(event.block), originChunk = null, isBlockDamage = false) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockFromTo(event: BlockFromToEvent) {
        handleBlockChange(listOfNotNull(event.toBlock), originChunk = null, isBlockDamage = false) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockPistonExtend(event: BlockPistonExtendEvent) {
        val affectedBlocks = mutableSetOf<Block>()
        affectedBlocks += event.block
        event.blocks.forEach { block ->
            affectedBlocks += block
            affectedBlocks += block.getRelative(event.direction)
        }
        handleBlockChange(affectedBlocks, originChunk = null, isBlockDamage = false) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockPistonRetract(event: BlockPistonRetractEvent) {
        val affectedBlocks = mutableSetOf<Block>()
        affectedBlocks += event.block
        event.blocks.forEach { block ->
            affectedBlocks += block
            affectedBlocks += block.getRelative(event.direction.oppositeFace)
        }
        handleBlockChange(affectedBlocks, originChunk = null, isBlockDamage = false) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
        handleBlockChange(listOf(event.block), originChunk = null, isBlockDamage = true) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        val location = event.location
        handleExplosion(event.blockList(), location.world, location.chunk) {
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
        if (world == null) {
            return
        }
        handleBlockChange(blocks, originChunk, isBlockDamage = true) {
            cancel()
        }
    }

    private fun handleBlockChange(
        blocks: Collection<Block>,
        originChunk: Chunk?,
        isBlockDamage: Boolean,
        cancel: () -> Unit
    ) {
        val settings = getSettings()
        if (!settings.enabled) {
            return
        }
        if (settings.onlyBlockDamage && !isBlockDamage) {
            return
        }
        val claimService = plugin.services.claimService
        val allowWhenAnyMemberOnline = settings.allowWhenAnyMemberOnline
        val factionsToProtect = mutableMapOf<MfFactionId, Boolean>()

        val protectedBlocksDetected = blocks.any { block ->
            val world = block.world
            if (settings.exemptWorlds.contains(world.name.lowercase())) {
                return@any false
            }
            val claim = claimService.getClaim(block.chunk) ?: return@any false
            shouldProtect(claim, allowWhenAnyMemberOnline, factionsToProtect)
        }

        val originProtected = if (originChunk != null) {
            if (settings.exemptWorlds.contains(originChunk.world.name.lowercase())) {
                false
            } else {
                val claim = claimService.getClaim(originChunk)
                claim != null && shouldProtect(claim, allowWhenAnyMemberOnline, factionsToProtect)
            }
        } else {
            false
        }

        if (!protectedBlocksDetected && !originProtected) {
            return
        }

        cancel()
    }

    private data class ProtectionSettings(
        val enabled: Boolean,
        val onlyBlockDamage: Boolean,
        val allowWhenAnyMemberOnline: Boolean,
        val exemptWorlds: Set<String>
    )

    private fun getSettings(): ProtectionSettings {
        val config = plugin.config
        return ProtectionSettings(
            enabled = config.getBoolean("offlineBlastProtection.enabled"),
            onlyBlockDamage = config.getBoolean("offlineBlastProtection.onlyBlockDamage", true),
            allowWhenAnyMemberOnline = config.getBoolean("offlineBlastProtection.allowWhenAnyMemberOnline", true),
            exemptWorlds = config.getStringList("offlineBlastProtection.exemptWorlds")
                .map(String::lowercase)
                .toSet()
        )
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
