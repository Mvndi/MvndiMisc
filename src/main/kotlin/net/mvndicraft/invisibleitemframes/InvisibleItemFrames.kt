package net.mvndicraft.invisibleitemframes

import net.mvndicraft.mvndicore.events.ReloadConfigEvent
import net.mvndicraft.mvndiequipment.ItemManager
import org.bukkit.entity.ItemFrame
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.plugin.java.JavaPlugin

class InvisibleItemFrames : JavaPlugin(), Listener {

    override fun onEnable() {
        // Plugin startup logic
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    fun onItemFramePlace(event: HangingPlaceEvent) {
        val item = event.itemStack;
        val id = ItemManager.getInstance().getId(item);
        var entity = event.entity;

        if (id == "invisible_item_frame" && entity is ItemFrame) {
            entity.isVisible = false
        }
    }

    @EventHandler
    fun onItemFrameBreak(event: EntityDeathEvent) {
        var entity = event.entity;

        if (entity is ItemFrame) {
            if (!entity.isVisible) {
                event.drops.clear()
                event.drops[0] = ItemManager.getInstance().create("invisible_item_frame", 1)
            }
        }
    }

    @EventHandler
    fun equipmentReload(event: ReloadConfigEvent) {
        if (!ItemManager.getInstance().itemExists("invisible_item_frame")) {
            this.isEnabled = false
        } else
            this.isEnabled = true
    }
}
