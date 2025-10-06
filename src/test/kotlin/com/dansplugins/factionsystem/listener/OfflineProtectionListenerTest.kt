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
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.event.entity.EntityExplodeEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
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
        `when`(config.getBoolean("offlineBlastProtection.onlyBlockDamage", true)).thenReturn(false)

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

    private fun mockServices() {
        val services = mock(Services::class.java)
        `when`(plugin.services).thenReturn(services)

        claimService = mock(MfClaimService::class.java)
        `when`(services.claimService).thenReturn(claimService)

        factionService = mock(MfFactionService::class.java)
        `when`(services.factionService).thenReturn(factionService)
    }
}
