package com.dansplugins.factionsystem.command.faction.unclaim

import com.dansplugins.factionsystem.RemoFactions
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.faction.MfFaction
import com.dansplugins.factionsystem.player.MfPlayer
import dev.forkhandles.result4k.onFailure
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.ChatColor.YELLOW
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level.SEVERE

class MfFactionUnclaimCommand(private val plugin: RemoFactions) : CommandExecutor, TabCompleter {

    private val pendingUnclaims = ConcurrentHashMap<UUID, PendingUnclaim>()
    private val confirmationTimeout: Duration = Duration.ofMinutes(1)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.unclaim")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimNotAPlayer"]}")
            return true
        }
        val confirmRequested = args.firstOrNull()?.equals("confirm", ignoreCase = true) == true
        val radiusArgIndex = if (confirmRequested) 1 else 0
        val requestedRadius = args.getOrNull(radiusArgIndex)?.toIntOrNull()
        plugin.server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                val playerService = plugin.services.playerService
                val mfPlayer = playerService.getPlayer(sender)
                    ?: playerService.save(MfPlayer(plugin, sender)).onFailure {
                        sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimFailedToSavePlayer"]}")
                        plugin.logger.log(SEVERE, "Failed to save player: ${it.reason.message}", it.reason.cause)
                        return@Runnable
                    }
                val factionService = plugin.services.factionService
                val hasBypass = mfPlayer.isBypassEnabled && sender.hasPermission("mf.bypass")
                var faction: MfFaction? = null
                if (!hasBypass) {
                    faction = factionService.getFaction(mfPlayer.id)
                    if (faction == null) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimMustBeInAFaction"]}")
                        return@Runnable
                    }
                    val role = faction.getRole(mfPlayer.id)
                    if (role == null || !role.hasPermission(faction, plugin.factionPermissions.unclaim)) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimNoFactionPermission"]}")
                        return@Runnable
                    }
                }
                val maxClaimRadius = plugin.config.getInt("factions.maxClaimRadius")
                val cooldownHours = plugin.config.getLong("factions.unclaimCooldownHours", 24L).coerceAtLeast(0L)
                val cooldownDuration = if (cooldownHours <= 0L) Duration.ZERO else Duration.ofHours(cooldownHours)
                var radiusToUse = requestedRadius
                val now = Instant.now()
                if (!hasBypass) {
                    val cooldownStore = plugin.unclaimCooldownStore
                    val factionId = faction!!.id
                    val playerId = sender.uniqueId
                    if (!confirmRequested) {
                        if (radiusToUse != null && (radiusToUse < 0 || radiusToUse > maxClaimRadius)) {
                            sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimMaxClaimRadius", maxClaimRadius.toString()]}")
                            return@Runnable
                        }
                        if (!cooldownDuration.isZero) {
                            val lastUnclaim = cooldownStore.getLastUnclaim(factionId)
                            if (lastUnclaim != null) {
                                val elapsed = Duration.between(lastUnclaim, now)
                                if (elapsed < cooldownDuration) {
                                    val remaining = cooldownDuration.minus(elapsed)
                                    sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimOnCooldown", formatDuration(remaining)]}")
                                    return@Runnable
                                }
                            }
                        }
                        pendingUnclaims[playerId] = PendingUnclaim(radiusToUse, now)
                        sender.sendMessage("$YELLOW${plugin.language["CommandFactionUnclaimConfirmationPrompt"]}")
                        return@Runnable
                    } else {
                        val pending = pendingUnclaims.remove(playerId)
                        if (pending == null) {
                            sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimConfirmationMissing"]}")
                            return@Runnable
                        }
                        if (Duration.between(pending.createdAt, now) > confirmationTimeout) {
                            sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimConfirmationExpired"]}")
                            return@Runnable
                        }
                        radiusToUse = pending.radius
                        if (radiusToUse != null && (radiusToUse < 0 || radiusToUse > maxClaimRadius)) {
                            sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimMaxClaimRadius", maxClaimRadius.toString()]}")
                            return@Runnable
                        }
                        if (!cooldownDuration.isZero) {
                            val lastUnclaim = cooldownStore.getLastUnclaim(factionId)
                            if (lastUnclaim != null) {
                                val elapsed = Duration.between(lastUnclaim, now)
                                if (elapsed < cooldownDuration) {
                                    val remaining = cooldownDuration.minus(elapsed)
                                    sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimOnCooldown", formatDuration(remaining)]}")
                                    return@Runnable
                                }
                            }
                        }
                    }
                } else {
                    if (radiusToUse != null && (radiusToUse < 0 || radiusToUse > maxClaimRadius)) {
                        sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimMaxClaimRadius", maxClaimRadius.toString()]}")
                        return@Runnable
                    }
                }
                val claimService = plugin.services.claimService
                val senderChunk = sender.location.chunk
                val senderChunkX = senderChunk.x
                val senderChunkZ = senderChunk.z
                val effectiveRadius = radiusToUse
                plugin.server.scheduler.runTask(
                    plugin,
                    Runnable {
                        val chunks = if (effectiveRadius == null) {
                            listOf(senderChunk)
                        } else {
                            (senderChunkX - effectiveRadius..senderChunkX + effectiveRadius).flatMap { x ->
                                (senderChunkZ - effectiveRadius..senderChunkZ + effectiveRadius).filter { z ->
                                    val a = x - senderChunkX
                                    val b = z - senderChunkZ
                                    (a * a) + (b * b) <= effectiveRadius * effectiveRadius
                                }.map { z -> sender.world.getChunkAt(x, z) }
                            }
                        }
                        plugin.server.scheduler.runTaskAsynchronously(
                            plugin,
                            Runnable saveChunks@{
                                val claims: List<MfClaimedChunk> = if (!hasBypass) {
                                    chunks.mapNotNull { chunk ->
                                        claimService.getClaim(chunk)
                                    }.filter { claim ->
                                        faction != null && claim.factionId.value == faction.id.value
                                    }
                                } else {
                                    chunks.mapNotNull { chunk ->
                                        claimService.getClaim(chunk)
                                    }
                                }
                                if (claims.isEmpty()) {
                                    sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimNoUnclaimableChunks"]}")
                                    return@saveChunks
                                }
                                claims.forEach { claim ->
                                    claimService.delete(claim)
                                        .onFailure {
                                            sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimFailedToDeleteClaim"]}")
                                            plugin.logger.log(SEVERE, "Failed to delete claimed chunk: ${it.reason.message}", it.reason.cause)
                                            return@saveChunks
                                        }
                                }
                                if (!hasBypass && faction != null) {
                                    plugin.unclaimCooldownStore.setLastUnclaim(faction.id, Instant.now())
                                    plugin.unclaimCooldownStore.save()
                                }
                                sender.sendMessage("$GREEN${plugin.language["CommandFactionUnclaimSuccess", claims.size.toString()]}")
                            }
                        )
                    }
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

    private fun formatDuration(duration: Duration): String {
        var remaining = duration
        if (remaining.isNegative || remaining.isZero) {
            return "0s"
        }
        val parts = mutableListOf<String>()
        val hours = remaining.toHours()
        if (hours > 0) {
            parts += "${hours}h"
            remaining = remaining.minusHours(hours)
        }
        val minutes = remaining.toMinutes()
        if (minutes > 0) {
            parts += "${minutes}m"
            remaining = remaining.minusMinutes(minutes)
        }
        val seconds = remaining.seconds
        if (seconds > 0 || parts.isEmpty()) {
            parts += "${seconds}s"
        }
        return parts.joinToString(" ")
    }

    private data class PendingUnclaim(val radius: Int?, val createdAt: Instant)
}
