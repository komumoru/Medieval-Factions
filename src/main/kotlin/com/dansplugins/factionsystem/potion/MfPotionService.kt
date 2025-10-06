package com.dansplugins.factionsystem.potion

import com.dansplugins.factionsystem.RemoFactions
import org.bukkit.OfflinePlayer
import org.bukkit.entity.AreaEffectCloud
import org.bukkit.entity.Player
import java.util.*

class MfPotionService(private val plugin: RemoFactions) {

    private val lingeringEffectThrowers = mutableMapOf<Int, UUID>()

    fun addLingeringEffectThrower(effect: AreaEffectCloud, thrower: Player) {
        lingeringEffectThrowers[effect.entityId] = thrower.uniqueId
        plugin.server.scheduler.runTaskLater(
            plugin,
            Runnable {
                removeLingeringPotionEffectThrower(effect)
            },
            effect.duration.toLong()
        )
    }

    fun removeLingeringPotionEffectThrower(effect: AreaEffectCloud) {
        lingeringEffectThrowers.remove(effect.entityId)
    }

    fun getLingeringPotionEffectThrower(effect: AreaEffectCloud): OfflinePlayer? {
        return lingeringEffectThrowers[effect.entityId]?.let(plugin.server::getOfflinePlayer)
    }
}
