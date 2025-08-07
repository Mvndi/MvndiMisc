package net.mvndicraft.mvndimisc

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Description
import org.bukkit.GameMode
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandAlias("gamemodeswitch|gms|gmsp")
class GamemodeSwitchCommand: BaseCommand() {
    @Default
    @CommandCompletion("@nothing")
    @Description("Switches gamemode between survival and spectator")
    @CommandPermission("mvndimisc.gamemode")
    fun onGamemodeSwitch(sender: CommandSender) {
        if (sender is Player)
            sender.gameMode = if (sender.gameMode == GameMode.SPECTATOR) GameMode.SURVIVAL else GameMode.SPECTATOR
    }
}