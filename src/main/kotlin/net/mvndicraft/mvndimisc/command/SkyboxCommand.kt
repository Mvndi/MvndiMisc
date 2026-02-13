package net.mvndicraft.mvndimisc.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Description
import net.mvndicraft.mvndimisc.addSkybox
import net.mvndicraft.mvndimisc.getSkyboxSetting
import net.mvndicraft.mvndimisc.removeSkybox
import net.mvndicraft.mvndimisc.setSkyboxSetting
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandAlias("skybox")
class SkyboxCommand: BaseCommand() {

    @Default
    @CommandCompletion("@nothing")
    @Description("Turns the skybox on/off")
    @CommandPermission("mvndimisc.skybox")
    fun onSkyboxToggle(sender: CommandSender) {
        if (sender is Player) {
            val currentState = getSkyboxSetting(sender)
            val descriptor = let {
                if (currentState) {
                    "<red>OFF"
                } else {
                    "<green>ON"
                }
            }

            if (!currentState) {
                addSkybox(sender)
            } else {
                removeSkybox(sender)
            }

            setSkyboxSetting(sender, !currentState)
            sender.sendRichMessage("The skybox is now $descriptor")
        } else {
            sender.sendMessage("This command can only be run as a player")
        }
    }
}
