package com.dansplugins.factionsystem.command.faction.unclaimall

import com.dansplugins.factionsystem.RemoFactions
import org.bukkit.ChatColor.RED
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class MfFactionUnclaimAllCommand(private val plugin: RemoFactions) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mf.unclaimall")) {
            sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimAllNoPermission"]}")
            return true
        }
        if (sender !is Player) {
            sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimAllNotAPlayer"]}")
            return true
        }
        sender.sendMessage("$RED${plugin.language["CommandFactionUnclaimAllDisabled"]}")
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ) = emptyList<String>()
}
