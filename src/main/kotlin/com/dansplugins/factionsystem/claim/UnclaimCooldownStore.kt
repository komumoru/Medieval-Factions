package com.dansplugins.factionsystem.claim

import com.dansplugins.factionsystem.RemoFactions
import com.dansplugins.factionsystem.faction.MfFactionId
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.UUID
import java.util.logging.Level

class UnclaimCooldownStore(private val plugin: RemoFactions) {

    private val file = File(plugin.dataFolder, "unclaim-cooldowns.yml")
    private val lastUnclaimByFactionId: MutableMap<String, Instant> = mutableMapOf()

    init {
        load()
    }

    fun getLastUnclaim(factionId: MfFactionId): Instant? {
        return lastUnclaimByFactionId[factionId.value]
    }

    fun setLastUnclaim(factionId: MfFactionId, instant: Instant) {
        lastUnclaimByFactionId[factionId.value] = instant
    }

    fun save() {
        val configuration = YamlConfiguration()
        lastUnclaimByFactionId.forEach { (factionId, instant) ->
            configuration.set("factions.$factionId", instant.toEpochMilli())
        }
        try {
            configuration.save(file)
        } catch (exception: IOException) {
            plugin.logger.log(Level.SEVERE, "Failed to save unclaim cooldowns", exception)
        }
    }

    private fun load() {
        if (!file.exists()) {
            return
        }
        val configuration = YamlConfiguration()
        try {
            configuration.load(file)
        } catch (exception: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to load unclaim cooldowns", exception)
            return
        }
        val section = configuration.getConfigurationSection("factions") ?: return
        for (factionId in section.getKeys(false)) {
            val epochMillis = section.getLong(factionId)
            if (epochMillis <= 0L) {
                continue
            }
            runCatching { UUID.fromString(factionId) }.onFailure {
                plugin.logger.warning("Ignoring unclaim cooldown entry with invalid faction id: $factionId")
            }.onSuccess {
                lastUnclaimByFactionId[factionId] = Instant.ofEpochMilli(epochMillis)
            }
        }
    }
}
