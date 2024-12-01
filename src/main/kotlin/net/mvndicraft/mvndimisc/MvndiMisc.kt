package net.mvndicraft.mvndimisc

import net.mvndicraft.mvndicore.events.ReloadConfigEvent
import net.mvndicraft.mvndiequipment.Armor
import net.mvndicraft.mvndiequipment.Item
import net.mvndicraft.mvndiequipment.ItemManager
import net.mvndicraft.mvndiplayers.MvndiPlayer
import net.mvndicraft.mvndiplayers.PlayerManager
import net.mvndicraft.mvndiseasons.biomes.NMSBiomeUtils
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemFrame
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

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

        if (id == "invisible_item_frame" && entity is ItemFrame) entity.isVisible = false
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
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        val blockstate = block.state
        val loc = block.location
        if (blockstate is ShulkerBox) {
            for (item in blockstate.inventory.contents) {
                if (item != null) {
                    loc.world.dropItem(loc, item)
                }
            }
            loc.world.dropItem(loc, ItemStack.of(block.type))
            event.isDropItems = false
        }
    }

    @EventHandler
    fun equipmentReload(event: ReloadConfigEvent) {
        this.isEnabled = ItemManager.getInstance().itemExists("invisible_item_frame")
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val p = event.player
        val biomeKey = NMSBiomeUtils.getBiomeKeyString(p.location)
        if (p.gameMode != GameMode.CREATIVE && (NMSBiomeUtils.matchTag(
                biomeKey, "minecraft:is_ocean"
            ) || NMSBiomeUtils.matchTag(biomeKey, "mvndi:is_deep_ocean"))
        ) {
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
    fun onWalkOnIce(e: PlayerMoveEvent) {
        val p = e.player
        val b = if (p.vehicle != null) p.location.subtract(0.0, 2.0, 0.0).block else p.location.subtract(
            0.0, 1.0, 0.0
        ).block
        if (!b.type.toString().lowercase().contains("ice")) return

        val mPlayer = Objects.requireNonNull<MvndiPlayer>(PlayerManager.getInstance().getPlayer(p.uniqueId))
        val stats = mPlayer.stats
        if (mPlayer.equipLoad / stats.equipLoad > 0.75 && Random().nextFloat() <= 0.5f) {
            p.playSound(p, Material.ICE.createBlockData().soundGroup.breakSound, 0.05f, 1f)
            b.breakNaturally()
            p.playSound(p, Material.ICE.createBlockData().soundGroup.breakSound, 2f, 1f)
        }
    }

    @EventHandler
    fun preventInfest(e: EntityChangeBlockEvent) {
        if (e.entity.type == EntityType.SILVERFISH)
            e.isCancelled = true
    }

    @EventHandler
    fun preventHoeWithWeapon(e: PlayerInteractEvent) {
        val item = e.item
        if (item == null || !item.hasItemMeta() || e.clickedBlock?.type != Material.GRASS_BLOCK || ItemManager.getInstance()
                .getId(item) == null
        )
            return

        val id = ItemManager.getInstance().getId(item) ?: return
        val mvndiItem = ItemManager.getInstance().getItem(id) ?: return
        if (mvndiItem.type == Item.Type.WEAPON)
            e.isCancelled = true
    }

    @EventHandler
    fun preventBonemeal(e: PlayerInteractEvent) {
        val b = e.clickedBlock
        val item = e.item
        if (b == null || e.action != Action.RIGHT_CLICK_BLOCK || e.hand == EquipmentSlot.OFF_HAND || item == null) return
        if (item.type == Material.BONE_MEAL && !b.type.toString().lowercase().contains("grass")) e.isCancelled = true
    }

    @EventHandler
    fun armorStands(event: PlayerInteractAtEntityEvent) {
        val entity = event.rightClicked
        if (entity.type != EntityType.ARMOR_STAND)
            return

        val armorStand = entity as ArmorStand
        if (!armorStand.isInvisible) {
            armorStand.setArms(true)
            var item = event.player.equipment.itemInMainHand
            val empty = item.isEmpty
            val offItem = event.player.equipment.itemInOffHand
            if (empty)
                item = offItem

            if (item.isEmpty)
                return

            if (item.type == Material.PLAYER_HEAD) {
                armorStand.equipment.setItem(EquipmentSlot.HEAD, item)
                item.subtract()
                return
            }

            val mvndiId = ItemManager.getInstance().getId(item) ?: return
            val mvndiItem = ItemManager.getInstance().getItem(mvndiId) ?: return
            armorStand.equipment.setItem(
                if (mvndiItem.type == Item.Type.WEAPON) if (empty) EquipmentSlot.OFF_HAND else EquipmentSlot.HAND else (mvndiItem as Armor).slot,
                item
            )
            item.subtract()
        }
    }

    @EventHandler
    fun dropArmorStandEquipment(e: EntityDeathEvent) {
        val entity = e.entity
        if (entity.type != EntityType.ARMOR_STAND || entity.isInvisible)
            return

        val armorStand = entity as ArmorStand
        EquipmentSlot.entries.forEach { equipmentSlot ->
            try {
                armorStand.world.dropItemNaturally(
                    armorStand.location,
                    armorStand.getItem(equipmentSlot)
                )
            } catch (ignore: Exception) {
            }
        }
    }

    @EventHandler
    fun anvilCost(e: PrepareAnvilEvent) {
        e.view.repairCost = 0
    }
}
