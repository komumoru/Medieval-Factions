package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.RemoFactions
import com.dansplugins.factionsystem.TestUtils
import com.dansplugins.factionsystem.player.MfPlayer
import com.dansplugins.factionsystem.player.MfPlayerId
import com.dansplugins.factionsystem.service.Services
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.block.data.BlockData
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID
import java.util.logging.Logger

class PlayerInteractListenerTest {
    private val testUtils = TestUtils()

    private lateinit var plugin: RemoFactions
    private lateinit var services: Services
    private lateinit var playerService: com.dansplugins.factionsystem.player.MfPlayerService
    private lateinit var claimService: com.dansplugins.factionsystem.claim.MfClaimService
    private lateinit var lockService: com.dansplugins.factionsystem.locks.MfLockService
    private lateinit var interactionService: com.dansplugins.factionsystem.interaction.MfInteractionService
    private lateinit var language: com.dansplugins.factionsystem.lang.Language
    private lateinit var config: FileConfiguration
    private lateinit var player: Player
    private lateinit var mfPlayer: MfPlayer
    private lateinit var uut: PlayerInteractListener

    @BeforeEach
    fun setUp() {
        plugin = mock(RemoFactions::class.java)
        services = mock(Services::class.java)
        playerService = mock(com.dansplugins.factionsystem.player.MfPlayerService::class.java)
        claimService = mock(com.dansplugins.factionsystem.claim.MfClaimService::class.java)
        lockService = mock(com.dansplugins.factionsystem.locks.MfLockService::class.java)
        interactionService = mock(com.dansplugins.factionsystem.interaction.MfInteractionService::class.java)

        `when`(plugin.services).thenReturn(services)
        `when`(services.playerService).thenReturn(playerService)
        `when`(services.claimService).thenReturn(claimService)
        `when`(services.lockService).thenReturn(lockService)
        `when`(services.interactionService).thenReturn(interactionService)

        config = mock(FileConfiguration::class.java)
        `when`(plugin.config).thenReturn(config)
        `when`(config.getBoolean(eq("wilderness.interaction.prevent"), anyBoolean())).thenAnswer { invocation ->
            invocation.getArgument<Boolean>(1)
        }
        `when`(config.getBoolean(eq("wilderness.interaction.alert"), anyBoolean())).thenAnswer { invocation ->
            invocation.getArgument<Boolean>(1)
        }
        `when`(config.getBoolean("factions.nonMembersCanInteractWithDoors")).thenReturn(false)
        `when`(config.getStringList("wilderness.interaction.allowedBlocks")).thenReturn(mutableListOf())
        `when`(config.getStringList("wilderness.interaction.allowedItems")).thenReturn(mutableListOf())

        language = mock(com.dansplugins.factionsystem.lang.Language::class.java)
        `when`(plugin.language).thenReturn(language)
        `when`(language["CannotInteractBlockInWilderness"]).thenReturn("Cannot interact in wilderness")
        `when`(language["CannotUseSpawnEggInWilderness"]).thenReturn("Cannot use spawn eggs in wilderness")

        val logger = mock(Logger::class.java)
        `when`(plugin.logger).thenReturn(logger)

        player = mock(Player::class.java)
        val playerUuid = UUID.randomUUID()
        val playerId = playerUuid.toString()
        `when`(player.uniqueId).thenReturn(playerUuid)
        `when`(player.hasPermission(anyString())).thenReturn(false)
        val mfPlayerId = MfPlayerId(playerId)
        mfPlayer = MfPlayer(mfPlayerId)
        `when`(playerService.getPlayer(player)).thenReturn(mfPlayer)

        uut = PlayerInteractListener(plugin)
    }

    @Test
    fun onPlayerInteract_WildernessSpawnEgg_ShouldCancelAndNotifyPlayer() {
        val block = createBlock(Material.GRASS_BLOCK)
        val item = mock(ItemStack::class.java)
        `when`(item.type).thenReturn(Material.CREEPER_SPAWN_EGG)
        val event = createEvent(block, item)
        `when`(claimService.getClaim(block.chunk)).thenReturn(null)
        `when`(config.getBoolean("wilderness.interaction.alert", true)).thenReturn(true)

        uut.onPlayerInteract(event)

        verify(event).isCancelled = true
        verify(player).sendMessage("${ChatColor.RED}Cannot use spawn eggs in wilderness")
    }

    @Test
    fun onPlayerInteract_WildernessPreventWithoutWhitelist_ShouldCancelAndNotifyPlayer() {
        val block = createBlock(Material.CHEST)
        val event = createEvent(block)
        `when`(claimService.getClaim(block.chunk)).thenReturn(null)
        `when`(config.getBoolean("wilderness.interaction.prevent", false)).thenReturn(true)
        `when`(config.getBoolean("wilderness.interaction.alert", true)).thenReturn(true)

        uut.onPlayerInteract(event)

        verify(event).isCancelled = true
        verify(player).sendMessage("${ChatColor.RED}Cannot interact in wilderness")
    }

    @Test
    fun onPlayerInteract_WildernessBlockInWhitelist_ShouldAllowInteraction() {
        val block = createBlock(Material.CHEST)
        val event = createEvent(block)
        `when`(claimService.getClaim(block.chunk)).thenReturn(null)
        `when`(config.getBoolean("wilderness.interaction.prevent", false)).thenReturn(true)
        `when`(config.getStringList("wilderness.interaction.allowedBlocks")).thenReturn(mutableListOf("CHEST"))

        uut.onPlayerInteract(event)

        verify(event, never()).isCancelled = true
        verify(player, never()).sendMessage(anyString())
    }

    @Test
    fun onPlayerInteract_WildernessItemInWhitelist_ShouldAllowInteraction() {
        val block = createBlock(Material.WATER)
        val item = mock(ItemStack::class.java)
        `when`(item.type).thenReturn(Material.OAK_BOAT)
        val event = createEvent(block, item)
        `when`(claimService.getClaim(block.chunk)).thenReturn(null)
        `when`(config.getBoolean("wilderness.interaction.prevent", false)).thenReturn(true)
        `when`(config.getStringList("wilderness.interaction.allowedItems")).thenReturn(mutableListOf("OAK_BOAT"))

        uut.onPlayerInteract(event)

        verify(event, never()).isCancelled = true
        verify(player, never()).sendMessage(anyString())
    }

    private fun createBlock(material: Material): Block {
        val world = testUtils.createMockWorld()
        val block = testUtils.createMockBlock(world)
        `when`(block.type).thenReturn(material)
        `when`(block.state).thenReturn(mock(BlockState::class.java))
        `when`(block.blockData).thenReturn(mock(BlockData::class.java))
        return block
    }

    private fun createEvent(block: Block, itemStack: ItemStack? = null): PlayerInteractEvent {
        val event = mock(PlayerInteractEvent::class.java)
        `when`(event.action).thenReturn(Action.RIGHT_CLICK_BLOCK)
        `when`(event.hand).thenReturn(EquipmentSlot.HAND)
        `when`(event.clickedBlock).thenReturn(block)
        `when`(event.player).thenReturn(player)
        `when`(event.item).thenReturn(itemStack)
        return event
    }
}
