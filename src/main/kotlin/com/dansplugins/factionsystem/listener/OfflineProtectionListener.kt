package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.RemoFactions
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFactionId
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.block.BlockFertilizeEvent
import org.bukkit.event.block.BlockFormEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockGrowEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockMultiPlaceEvent
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockRedstoneEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.block.CauldronLevelChangeEvent
import org.bukkit.event.block.FluidLevelChangeEvent
import org.bukkit.event.block.LeavesDecayEvent
import org.bukkit.event.block.SpongeAbsorbEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.ExplosionPrimeEvent
import org.bukkit.event.world.StructureGrowEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.floor

class OfflineProtectionListener(private val plugin: RemoFactions) : Listener {

    private val primedExplosionRadii: MutableMap<UUID, Float> = ConcurrentHashMap()

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        handleBlockChange(listOf(event.block), originChunk = null, isBlockDamage = true) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockDamage(event: BlockDamageEvent) {
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
    fun onBlockIgnite(event: BlockIgniteEvent) {
        handleBlockChange(listOf(event.block), originChunk = null, isBlockDamage = false) {
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
    fun onBlockFertilize(event: BlockFertilizeEvent) {
        val blocks = event.blocks.map { it.block }
        handleBlockChange(blocks, originChunk = null, isBlockDamage = false) {
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
    fun onLeavesDecay(event: LeavesDecayEvent) {
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
    fun onFluidLevelChange(event: FluidLevelChangeEvent) {
        handleBlockChange(listOf(event.block), originChunk = null, isBlockDamage = false) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onCauldronLevelChange(event: CauldronLevelChangeEvent) {
        handleBlockChange(listOf(event.block), originChunk = null, isBlockDamage = false) {
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
    fun onBlockPhysics(event: BlockPhysicsEvent) {
        handleBlockChange(listOf(event.block), originChunk = null, isBlockDamage = false) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockDropItem(event: BlockDropItemEvent) {
        handleBlockChange(listOf(event.block), originChunk = null, isBlockDamage = true) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onSpongeAbsorb(event: SpongeAbsorbEvent) {
        handleBlockChange(listOf(event.block), originChunk = null, isBlockDamage = true) {
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
    fun onBlockRedstone(event: BlockRedstoneEvent) {
        handleBlockChange(listOf(event.block), originChunk = null, isBlockDamage = false) {
            event.newCurrent = event.oldCurrent
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onStructureGrow(event: StructureGrowEvent) {
        val blocks = event.blocks.map { it.block }
        val originChunk = event.location.chunk
        handleBlockChange(blocks, originChunk, isBlockDamage = false) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        val location = event.location
        val entity = event.entity as Entity?
        if (entity == null) {
            handleExplosion(event.blockList(), location.chunk, emptySet()) {
                event.blockList().clear()
                event.isCancelled = true
            }
            return
        }
        val radius = primedExplosionRadii.remove(entity.uniqueId)
        val additionalChunks = if (location.world != null && radius != null) {
            getChunksWithinRadius(location, radius)
        } else {
            emptySet()
        }
        handleExplosion(event.blockList(), location.chunk, additionalChunks) {
            event.blockList().clear()
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onExplosionPrime(event: ExplosionPrimeEvent) {
        val entity = event.entity
        val location = entity.location
        val originChunk = location.chunk
        val additionalChunks = getChunksWithinRadius(location, event.radius)
        var cancelled = false
        handleBlockChange(emptyList(), originChunk, additionalChunks, isBlockDamage = true) {
            cancelled = true
            event.isCancelled = true
        }
        if (!cancelled) {
            primedExplosionRadii[entity.uniqueId] = event.radius
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        handleExplosion(event.blockList(), event.block.chunk, emptySet()) {
            event.blockList().clear()
            event.isCancelled = true
        }
    }

    private fun handleExplosion(
        blocks: MutableList<Block>,
        originChunk: Chunk?,
        additionalChunks: Collection<Chunk>,
        cancel: () -> Unit
    ) {
        handleBlockChange(blocks, originChunk, additionalChunks, isBlockDamage = true) {
            cancel()
        }
    }

    private fun handleBlockChange(
        blocks: Collection<Block>,
        originChunk: Chunk?,
        additionalChunks: Collection<Chunk> = emptyList(),
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

        val chunksToEvaluate = mutableSetOf<Chunk>()
        blocks.mapTo(chunksToEvaluate, Block::getChunk)
        originChunk?.let(chunksToEvaluate::add)
        additionalChunks.mapTo(chunksToEvaluate) { it }

        val protectedDetected = chunksToEvaluate.any { chunk ->
            if (settings.exemptWorlds.contains(chunk.world.name.lowercase())) {
                return@any false
            }
            val claim = claimService.getClaim(chunk) ?: return@any false
            shouldProtect(claim, allowWhenAnyMemberOnline, factionsToProtect)
        }

        if (!protectedDetected) {
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
            onlyBlockDamage = config.getBoolean("offlineBlastProtection.onlyBlockDamage", false),
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

    private fun getChunksWithinRadius(location: Location, radius: Float): Set<Chunk> {
        val world = location.world ?: return emptySet()
        if (radius <= 0f) {
            return setOf(location.chunk)
        }
        val radiusBlocks = radius.toDouble()
        val minChunkX = floor((location.x - radiusBlocks) / 16.0).toInt()
        val maxChunkX = floor((location.x + radiusBlocks) / 16.0).toInt()
        val minChunkZ = floor((location.z - radiusBlocks) / 16.0).toInt()
        val maxChunkZ = floor((location.z + radiusBlocks) / 16.0).toInt()
        val chunks = mutableSetOf<Chunk>()
        for (chunkX in minChunkX..maxChunkX) {
            for (chunkZ in minChunkZ..maxChunkZ) {
                chunks += world.getChunkAt(chunkX, chunkZ)
            }
        }
        return chunks
    }
}
