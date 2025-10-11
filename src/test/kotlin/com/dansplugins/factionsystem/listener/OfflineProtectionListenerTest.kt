package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.RemoFactions
import com.dansplugins.factionsystem.claim.MfClaimService
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.faction.MfFactionService
import com.dansplugins.factionsystem.service.Services
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Entity
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockRedstoneEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.ExplosionPrimeEvent
import org.bukkit.event.world.StructureGrowEvent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OfflineProtectionListenerTest {

    private lateinit var plugin: RemoFactions
    private lateinit var claimService: MfClaimService
    private lateinit var factionService: MfFactionService
    private lateinit var uut: OfflineProtectionListener

    @BeforeEach
    fun setUp() {
        plugin = mock(RemoFactions::class.java)
        mockServices()
        uut = OfflineProtectionListener(plugin)
    }

    @Test
    fun onEntityExplode_originChunkProtected_onlyBlockDamageFalse_shouldCancelExplosion() {
        val config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(config.getBoolean("offlineBlastProtection.enabled")).thenReturn(true)
        `when`(config.getStringList("offlineBlastProtection.exemptWorlds")).thenReturn(emptyList())
        `when`(config.getBoolean("offlineBlastProtection.allowWhenAnyMemberOnline", true)).thenReturn(true)
        `when`(config.getBoolean("offlineBlastProtection.onlyBlockDamage", false)).thenReturn(false)

        val world = mock(World::class.java)
        val worldId = UUID.randomUUID()
        `when`(world.name).thenReturn("world")
        `when`(world.uid).thenReturn(worldId)
        val chunk = mock(Chunk::class.java)
        `when`(chunk.world).thenReturn(world)
        `when`(chunk.x).thenReturn(0)
        `when`(chunk.z).thenReturn(0)
        `when`(world.getChunkAt(0, 0)).thenReturn(chunk)
        val location = Location(world, 0.0, 64.0, 0.0)
        `when`(world.getChunkAt(location)).thenReturn(chunk)

        val factionId = MfFactionId.generate()
        val claim = MfClaimedChunk(worldId, 0, 0, factionId)
        `when`(claimService.getClaim(chunk)).thenReturn(claim)
        `when`(factionService.hasOnlineMember(factionId)).thenReturn(false)

        val event = mock(EntityExplodeEvent::class.java)
        val blocks = mutableListOf<Block>()
        `when`(event.blockList()).thenReturn(blocks)
        `when`(event.location).thenReturn(location)

        uut.onEntityExplode(event)

        verify(event).isCancelled = true
    }

    @Test
    fun onBlockPlace_blockProtected_onlyBlockDamageFalse_shouldCancelPlacement() {
        val config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(config.getBoolean("offlineBlastProtection.enabled")).thenReturn(true)
        `when`(config.getStringList("offlineBlastProtection.exemptWorlds")).thenReturn(emptyList())
        `when`(config.getBoolean("offlineBlastProtection.allowWhenAnyMemberOnline", true)).thenReturn(true)
        `when`(config.getBoolean("offlineBlastProtection.onlyBlockDamage", false)).thenReturn(false)

        val world = mock(World::class.java)
        val worldId = UUID.randomUUID()
        `when`(world.name).thenReturn("world")
        `when`(world.uid).thenReturn(worldId)

        val chunk = mock(Chunk::class.java)
        `when`(chunk.world).thenReturn(world)

        val factionId = MfFactionId.generate()
        val claim = MfClaimedChunk(worldId, 0, 0, factionId)
        `when`(claimService.getClaim(chunk)).thenReturn(claim)
        `when`(factionService.hasOnlineMember(factionId)).thenReturn(false)

        val block = mock(Block::class.java)
        `when`(block.world).thenReturn(world)
        `when`(block.chunk).thenReturn(chunk)

        val event = mock(BlockPlaceEvent::class.java)
        `when`(event.block).thenReturn(block)

        uut.onBlockPlace(event)

        verify(event).isCancelled = true
    }

    @Test
    fun onExplosionPrime_originChunkProtected_shouldCancelExplosion() {
        val config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(config.getBoolean("offlineBlastProtection.enabled")).thenReturn(true)
        `when`(config.getStringList("offlineBlastProtection.exemptWorlds")).thenReturn(emptyList())
        `when`(config.getBoolean("offlineBlastProtection.allowWhenAnyMemberOnline", true)).thenReturn(true)
        `when`(config.getBoolean("offlineBlastProtection.onlyBlockDamage", false)).thenReturn(false)

        val world = mock(World::class.java)
        val worldId = UUID.randomUUID()
        `when`(world.name).thenReturn("world")
        `when`(world.uid).thenReturn(worldId)

        val chunk = mock(Chunk::class.java)
        `when`(chunk.world).thenReturn(world)

        val location = Location(world, 1.0, 65.0, 1.0)
        `when`(world.getChunkAt(location)).thenReturn(chunk)
        `when`(world.getChunkAt(0, 0)).thenReturn(chunk)
        `when`(chunk.x).thenReturn(0)
        `when`(chunk.z).thenReturn(0)

        val factionId = MfFactionId.generate()
        val claim = MfClaimedChunk(worldId, 0, 0, factionId)
        `when`(claimService.getClaim(chunk)).thenReturn(claim)
        `when`(factionService.hasOnlineMember(factionId)).thenReturn(false)

        val entity = mock(Entity::class.java)
        `when`(entity.location).thenReturn(location)

        val event = mock(ExplosionPrimeEvent::class.java)
        `when`(event.entity).thenReturn(entity)
        `when`(event.radius).thenReturn(0.0f)

        uut.onExplosionPrime(event)

        verify(event).isCancelled = true
    }

    @Test
    fun onExplosionPrime_radiusIntersectsProtectedChunk_shouldCancelExplosion() {
        val config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(config.getBoolean("offlineBlastProtection.enabled")).thenReturn(true)
        `when`(config.getStringList("offlineBlastProtection.exemptWorlds")).thenReturn(emptyList())
        `when`(config.getBoolean("offlineBlastProtection.allowWhenAnyMemberOnline", true)).thenReturn(true)
        `when`(config.getBoolean("offlineBlastProtection.onlyBlockDamage", false)).thenReturn(false)

        val world = mock(World::class.java)
        val worldId = UUID.randomUUID()
        `when`(world.name).thenReturn("world")
        `when`(world.uid).thenReturn(worldId)

        val originChunk = mock(Chunk::class.java)
        `when`(originChunk.world).thenReturn(world)
        `when`(originChunk.x).thenReturn(1)
        `when`(originChunk.z).thenReturn(1)

        val claimedChunk = mock(Chunk::class.java)
        `when`(claimedChunk.world).thenReturn(world)
        `when`(claimedChunk.x).thenReturn(0)
        `when`(claimedChunk.z).thenReturn(0)

        val chunk01 = mock(Chunk::class.java)
        `when`(chunk01.world).thenReturn(world)
        `when`(chunk01.x).thenReturn(0)
        `when`(chunk01.z).thenReturn(1)

        val chunk10 = mock(Chunk::class.java)
        `when`(chunk10.world).thenReturn(world)
        `when`(chunk10.x).thenReturn(1)
        `when`(chunk10.z).thenReturn(0)

        val location = Location(world, 20.0, 65.0, 20.0)
        `when`(world.getChunkAt(location)).thenReturn(originChunk)
        `when`(world.getChunkAt(1, 1)).thenReturn(originChunk)
        `when`(world.getChunkAt(0, 0)).thenReturn(claimedChunk)
        `when`(world.getChunkAt(0, 1)).thenReturn(chunk01)
        `when`(world.getChunkAt(1, 0)).thenReturn(chunk10)

        val factionId = MfFactionId.generate()
        val claim = MfClaimedChunk(worldId, 0, 0, factionId)
        `when`(claimService.getClaim(claimedChunk)).thenReturn(claim)
        `when`(factionService.hasOnlineMember(factionId)).thenReturn(false)

        val entity = mock(Entity::class.java)
        `when`(entity.location).thenReturn(location)

        val event = mock(ExplosionPrimeEvent::class.java)
        `when`(event.entity).thenReturn(entity)
        `when`(event.radius).thenReturn(6.0f)

        uut.onExplosionPrime(event)

        verify(event).isCancelled = true
    }

    @Test
    fun onEntityExplode_blocksCleared_radiusStored_shouldCancelExplosion() {
        val config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(config.getBoolean("offlineBlastProtection.enabled")).thenReturn(true)
        `when`(config.getStringList("offlineBlastProtection.exemptWorlds")).thenReturn(emptyList())
        `when`(config.getBoolean("offlineBlastProtection.allowWhenAnyMemberOnline", true)).thenReturn(true)
        `when`(config.getBoolean("offlineBlastProtection.onlyBlockDamage", false)).thenReturn(false)

        val world = mock(World::class.java)
        val worldId = UUID.randomUUID()
        `when`(world.name).thenReturn("world")
        `when`(world.uid).thenReturn(worldId)

        val originChunk = mock(Chunk::class.java)
        `when`(originChunk.world).thenReturn(world)
        `when`(originChunk.x).thenReturn(1)
        `when`(originChunk.z).thenReturn(1)

        val claimedChunk = mock(Chunk::class.java)
        `when`(claimedChunk.world).thenReturn(world)
        `when`(claimedChunk.x).thenReturn(0)
        `when`(claimedChunk.z).thenReturn(0)

        val chunk01 = mock(Chunk::class.java)
        `when`(chunk01.world).thenReturn(world)
        `when`(chunk01.x).thenReturn(0)
        `when`(chunk01.z).thenReturn(1)

        val chunk10 = mock(Chunk::class.java)
        `when`(chunk10.world).thenReturn(world)
        `when`(chunk10.x).thenReturn(1)
        `when`(chunk10.z).thenReturn(0)

        val location = Location(world, 20.0, 65.0, 20.0)
        `when`(world.getChunkAt(location)).thenReturn(originChunk)
        `when`(world.getChunkAt(1, 1)).thenReturn(originChunk)
        `when`(world.getChunkAt(0, 0)).thenReturn(claimedChunk)
        `when`(world.getChunkAt(0, 1)).thenReturn(chunk01)
        `when`(world.getChunkAt(1, 0)).thenReturn(chunk10)

        val factionId = MfFactionId.generate()
        val claim = MfClaimedChunk(worldId, 0, 0, factionId)
        `when`(claimService.getClaim(claimedChunk)).thenReturn(claim)
        `when`(factionService.hasOnlineMember(factionId)).thenReturn(true, false)

        val entity = mock(Entity::class.java)
        val entityId = UUID.randomUUID()
        `when`(entity.uniqueId).thenReturn(entityId)
        `when`(entity.location).thenReturn(location)

        val primeEvent = mock(ExplosionPrimeEvent::class.java)
        `when`(primeEvent.entity).thenReturn(entity)
        `when`(primeEvent.radius).thenReturn(6.0f)

        uut.onExplosionPrime(primeEvent)

        verify(primeEvent, never()).isCancelled = true

        val blocks = mutableListOf<Block>()
        val explodeEvent = mock(EntityExplodeEvent::class.java)
        `when`(explodeEvent.blockList()).thenReturn(blocks)
        `when`(explodeEvent.location).thenReturn(location)
        `when`(explodeEvent.entity).thenReturn(entity)

        uut.onEntityExplode(explodeEvent)

        verify(explodeEvent).isCancelled = true
        assertTrue(blocks.isEmpty())
    }

    @Test
    fun onBlockPhysics_blockProtected_shouldCancelEvent() {
        val config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(config.getBoolean("offlineBlastProtection.enabled")).thenReturn(true)
        `when`(config.getStringList("offlineBlastProtection.exemptWorlds")).thenReturn(emptyList())
        `when`(config.getBoolean("offlineBlastProtection.allowWhenAnyMemberOnline", true)).thenReturn(true)
        `when`(config.getBoolean("offlineBlastProtection.onlyBlockDamage", false)).thenReturn(false)

        val world = mock(World::class.java)
        val worldId = UUID.randomUUID()
        `when`(world.name).thenReturn("world")
        `when`(world.uid).thenReturn(worldId)

        val chunk = mock(Chunk::class.java)
        `when`(chunk.world).thenReturn(world)

        val factionId = MfFactionId.generate()
        val claim = MfClaimedChunk(worldId, 0, 0, factionId)
        `when`(claimService.getClaim(chunk)).thenReturn(claim)
        `when`(factionService.hasOnlineMember(factionId)).thenReturn(false)

        val block = mock(Block::class.java)
        `when`(block.world).thenReturn(world)
        `when`(block.chunk).thenReturn(chunk)

        val event = mock(BlockPhysicsEvent::class.java)
        `when`(event.block).thenReturn(block)

        uut.onBlockPhysics(event)

        verify(event).isCancelled = true
    }

    @Test
    fun onBlockRedstone_blockProtected_shouldResetCurrent() {
        val config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(config.getBoolean("offlineBlastProtection.enabled")).thenReturn(true)
        `when`(config.getStringList("offlineBlastProtection.exemptWorlds")).thenReturn(emptyList())
        `when`(config.getBoolean("offlineBlastProtection.allowWhenAnyMemberOnline", true)).thenReturn(true)
        `when`(config.getBoolean("offlineBlastProtection.onlyBlockDamage", false)).thenReturn(false)

        val world = mock(World::class.java)
        val worldId = UUID.randomUUID()
        `when`(world.name).thenReturn("world")
        `when`(world.uid).thenReturn(worldId)

        val chunk = mock(Chunk::class.java)
        `when`(chunk.world).thenReturn(world)

        val factionId = MfFactionId.generate()
        val claim = MfClaimedChunk(worldId, 0, 0, factionId)
        `when`(claimService.getClaim(chunk)).thenReturn(claim)
        `when`(factionService.hasOnlineMember(factionId)).thenReturn(false)

        val block = mock(Block::class.java)
        `when`(block.world).thenReturn(world)
        `when`(block.chunk).thenReturn(chunk)

        val event = mock(BlockRedstoneEvent::class.java)
        `when`(event.block).thenReturn(block)
        `when`(event.oldCurrent).thenReturn(9)

        uut.onBlockRedstone(event)

        verify(event).newCurrent = 9
    }

    @Test
    fun onStructureGrow_blocksProtected_shouldCancelGrowth() {
        val config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(config.getBoolean("offlineBlastProtection.enabled")).thenReturn(true)
        `when`(config.getStringList("offlineBlastProtection.exemptWorlds")).thenReturn(emptyList())
        `when`(config.getBoolean("offlineBlastProtection.allowWhenAnyMemberOnline", true)).thenReturn(true)
        `when`(config.getBoolean("offlineBlastProtection.onlyBlockDamage", false)).thenReturn(false)

        val world = mock(World::class.java)
        val worldId = UUID.randomUUID()
        `when`(world.name).thenReturn("world")
        `when`(world.uid).thenReturn(worldId)

        val chunk = mock(Chunk::class.java)
        `when`(chunk.world).thenReturn(world)
        `when`(chunk.x).thenReturn(0)
        `when`(chunk.z).thenReturn(0)

        val factionId = MfFactionId.generate()
        val claim = MfClaimedChunk(worldId, 0, 0, factionId)
        `when`(claimService.getClaim(chunk)).thenReturn(claim)
        `when`(factionService.hasOnlineMember(factionId)).thenReturn(false)

        val block = mock(Block::class.java)
        `when`(block.world).thenReturn(world)
        `when`(block.chunk).thenReturn(chunk)

        val blockState = mock(BlockState::class.java)
        `when`(blockState.block).thenReturn(block)

        val location = Location(world, 0.0, 70.0, 0.0)
        `when`(world.getChunkAt(location)).thenReturn(chunk)

        val event = mock(StructureGrowEvent::class.java)
        `when`(event.blocks).thenReturn(mutableListOf(blockState))
        `when`(event.location).thenReturn(location)

        uut.onStructureGrow(event)

        verify(event).isCancelled = true
    }

    private fun mockServices() {
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)

        claimService = mock(MfClaimService::class.java)
        `when`(services.claimService).thenReturn(claimService)

        factionService = mock(MfFactionService::class.java)
        `when`(services.factionService).thenReturn(factionService)
    }
}
