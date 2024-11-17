package net.mvndicraft.mvndimisc

import net.mvndicraft.mvndicore.events.ReloadConfigEvent
import net.mvndicraft.mvndiequipment.ItemManager
import net.mvndicraft.mvndiseasons.biomes.NMSBiomeUtils
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.player.PlayerAttemptPickupItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.java.JavaPlugin

class MvndiMisc : JavaPlugin(), Listener {

    private val playersInChests = HashSet<Player>()

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
    fun onItemFrameBreak(event: HangingBreakByEntityEvent) {
        val entity = event.entity

        if (entity is ItemFrame && !entity.isVisible) {
            entity.remove()
            val heldItem = entity.item
            entity.world.dropItem(entity.location, heldItem)

            val toDrop = ItemManager.getInstance().create("invisible_item_frame", 1)
            if (toDrop != null) {
                entity.world.dropItem(entity.location, toDrop)
            }
            event.isCancelled = false
        }
    }

    @EventHandler
    fun equipmentReload(event: ReloadConfigEvent) {
        this.isEnabled = ItemManager.getInstance().itemExists("invisible_item_frame")
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {

        val p = event.player
        val biomeName = p.location.block.biome.name.lowercase()

        if (p.gameMode != GameMode.CREATIVE && (biomeName.contains("ocean") || NMSBiomeUtils.getBiomeKeyString(p.location).contains("ocean"))) {
            p.sendMessage("No building in ocean")
            event.isCancelled = true
        }

        if (p.location.y <= -60) {
            p.sendMessage("No building this far underground")
            event.isCancelled = true
        }

        if (p.location.y >= 256) {
            p.sendMessage("No building this far up")
            event.isCancelled = true
        }
    }

    @EventHandler
    fun noShulkerInChestHopper(event: InventoryMoveItemEvent) {
        val typeLC = event.destination.type.toString().lowercase()
        if (!typeLC.contains("chest") && !typeLC.contains("shulker"))
            return

        if (event.item.toString().lowercase().contains("shulker"))
            event.isCancelled = true
    }

    @EventHandler
    fun noShulkerInChest(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK)
            return

        val block = event.clickedBlock
        if (block == null || !block.type.toString().lowercase().contains("chest"))
            return

        if (event.player.inventory.storageContents.any{ itemStack -> itemStack.toString().lowercase().contains("shulker") }) {
            event.player.sendMessage("Cannot open storage blocks with shulker in inventory")
            event.isCancelled = true
        } else {
            playersInChests.add(event.player)
        }
    }

    @EventHandler
    fun removePlayerFromPlayersInChests(event: InventoryCloseEvent) {
        playersInChests.remove(event.player)
    }

    @EventHandler
    fun noPickupShulkerIfNearChest(event: PlayerAttemptPickupItemEvent) {
        if (event.item.itemStack.type.toString().lowercase().contains("shulker") && playersInChests.contains(event.player))
            event.isCancelled = true
    }

    @EventHandler
    fun dropShulkerInvOnBreak() {

    }
}
