package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.RemoFactions
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.LingeringPotionSplashEvent

class LingeringPotionSplashListener(private val plugin: RemoFactions) : Listener {

    @EventHandler
    fun onLingeringPotionSplash(event: LingeringPotionSplashEvent) {
        val thrower = event.entity.shooter as? Player
        if (thrower != null) {
            plugin.services.potionService.addLingeringEffectThrower(event.areaEffectCloud, thrower)
        }
    }
}
