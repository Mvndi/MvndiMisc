package net.mvndicraft.mvndimisc

import net.mvndicraft.mvndicore.events.ReloadConfigEvent
import net.mvndicraft.mvndiequipment.ItemManager
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.ItemFrame
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.plugin.PluginManager
import org.bukkit.plugin.java.JavaPlugin

class MvndiMisc : JavaPlugin(), Listener {

    override fun onEnable() {
        // Plugin startup logic
        Bukkit.getServer().pluginManager.registerEvents(this, this)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    fun onItemFramePlace(event: HangingPlaceEvent) {
        val item = event.itemStack
        val id = ItemManager.getInstance().getId(item)
        val entity = event.entity

        if (id == "invisible_item_frame" && entity is ItemFrame)
            entity.isVisible = false
    }

    @EventHandler
    fun onItemFrameBreak(event: EntityDeathEvent) {
        val entity = event.entity

        if (entity is ItemFrame && !entity.isVisible) {
            event.drops.clear()
            event.drops[0] = ItemManager.getInstance().create("invisible_item_frame", 1)
        }
    }

    @EventHandler
    fun equipmentReload(event: ReloadConfigEvent) {
        this.isEnabled = ItemManager.getInstance().itemExists("invisible_item_frame")
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val p = event.player
        if (p.gameMode != GameMode.CREATIVE && p.location.block.biome.name.lowercase().contains("ocean")) {
            p.sendMessage("No building in ocean")
            event.isCancelled = true
        }
    }
}
