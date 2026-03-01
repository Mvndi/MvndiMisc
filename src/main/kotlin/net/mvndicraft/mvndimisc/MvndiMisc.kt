package net.mvndicraft.mvndimisc

import co.aikar.commands.PaperCommandManager
import com.github.retrooper.packetevents.PacketEvents
import me.tofaa.entitylib.APIConfig
import me.tofaa.entitylib.EntityLib
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform
import net.mvndicraft.mvndiequipment.Armor
import net.mvndicraft.mvndiequipment.Item
import net.mvndicraft.mvndiequipment.ItemManager
import net.mvndicraft.mvndimisc.command.GamemodeSwitchCommand
import net.mvndicraft.mvndimisc.command.SkyboxCommand
import net.mvndicraft.mvndimmo.executors.blacksmith.AnvilExecutor
import net.mvndicraft.mvndiplayers.PlayerManager
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.Block
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.AbstractHorse
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.*
import org.bukkit.event.world.PortalCreateEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class MvndiMisc : JavaPlugin(), Listener {

    override fun onEnable() {
        // Plugin startup logic
        Bukkit.getServer().pluginManager.registerEvents(this, this)
        val cmdManager = PaperCommandManager(this)
        cmdManager.registerCommand(GamemodeSwitchCommand())
        cmdManager.registerCommand(SkyboxCommand())

        val platform = SpigotEntityLibPlatform(this)
        val settings = APIConfig(PacketEvents.getAPI()).tickTickables().tickTickables()
//            .trackPlatformEntities(true)
            .usePlatformLogger()

        EntityLib.init(platform, settings)
        createSkyboxThread(this)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    fun onItemFramePlace(event: HangingPlaceEvent) {
        val item = event.itemStack ?: return
        val id = ItemManager.getInstance().getId(item)
        val entity = event.entity

        if (id == "invisible_item_frame" && entity is ItemFrame) entity.isVisible = false
    }

    @EventHandler
    fun onItemFrameBreak(event: HangingBreakByEntityEvent) {
        val entity = event.entity

        if (Bukkit.getPluginManager()
                .isPluginEnabled("MvndiMMO") && entity.persistentDataContainer.has(AnvilExecutor.MINIGAME_KEY)
        ) return

        if (entity is ItemFrame && !entity.isVisible) {
            entity.remove()
            val heldItem = entity.item
            entity.world.dropItem(entity.location, heldItem)

            if (!ItemManager.getInstance().itemExists("invisible_item_frame")) return

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
    fun onBlockPlace(event: BlockPlaceEvent) {
        val p = event.player

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
    fun onWalkOnLilypad(e: PlayerMoveEvent) {
        val p = e.player

        if (p.gameMode == GameMode.CREATIVE || p.gameMode == GameMode.SPECTATOR) return

        val horse = p.vehicle?.let { it.type in listOf(EntityType.HORSE, EntityType.DONKEY, EntityType.MULE) } ?: false
        val b = p.location.block

        if (b.type != Material.LILY_PAD) return

        b.breakNaturally()

        if (horse) breakUnderHorse(b)
    }

    @EventHandler
    fun noSpawnPvP(e: EntityDamageByEntityEvent) {
        val damager = e.damager
        val victim = e.entity

        if (damager !is Player || victim !is Player) return

        val world = damager.location.world ?: return
        val spawn = world.spawnLocation

        val spawnRadius = Bukkit.getSpawnRadius().toDouble()

        val dx = damager.location.x - spawn.x
        val dz = damager.location.z - spawn.z
        val distanceSquared = dx * dx + dz * dz

        if (distanceSquared <= spawnRadius * spawnRadius) {
            e.isCancelled = true
             damager.sendMessage("You cannot fight in spawn protection!")
        }
    }

    @EventHandler
    fun onWalkOnIce(e: PlayerMoveEvent) {
        val p = e.player

        if (p.gameMode == GameMode.CREATIVE || p.gameMode == GameMode.SPECTATOR) return

        val horse = p.vehicle?.let { it.type in listOf(EntityType.HORSE, EntityType.DONKEY, EntityType.MULE) } ?: false
        val b = p.location.subtract(0.0, 1.0, 0.0).block

        if (!b.type.toString().lowercase().contains("ice")) return

        val mPlayer = PlayerManager.getInstance().getPlayer(p.uniqueId) ?: return
        val stats = mPlayer.stats

        if (horse && mPlayer.equipLoad / stats.equipLoad > 0.5 && Math.random() <= 0.5) breakUnderHorse(b)
        else if (mPlayer.equipLoad / stats.equipLoad > 0.75 && Math.random() <= 0.5) {
            b.breakNaturally()
            b.location.world?.playSound(b.location, Material.ICE.createBlockData().soundGroup.breakSound, 2f, 1f)
        }
    }


    private fun breakUnderHorse(block: Block) {
        if (block.type == Material.LILY_PAD || block.type.toString().lowercase().contains("ice")) block.breakNaturally()

        for (x in -3..3) for (z in -3..3) {
            val targetBlock = block.location.clone().add(x.toDouble(), 0.0, z.toDouble()).block
            if (targetBlock.type == Material.LILY_PAD || targetBlock.type.toString().lowercase()
                    .contains("ice")
            ) targetBlock.breakNaturally()
            if (targetBlock.type.toString().lowercase().contains("ice")) targetBlock.world.playSound(
                targetBlock.location, Material.ICE.createBlockData().soundGroup.breakSound, 2f, 1f
            )
        }

        if (block.type.toString().lowercase().contains("ice")) block.world.playSound(
            block.location, Material.ICE.createBlockData().soundGroup.breakSound, 2f, 1f
        )
    }

    @EventHandler
    fun preventInfest(e: EntityChangeBlockEvent) {
        if (e.entity.type == EntityType.SILVERFISH) e.isCancelled = true
    }

    @EventHandler
    fun preventHoeWithWeapon(e: PlayerInteractEvent) {
        val item = e.item
        if (item == null || !item.hasItemMeta() || e.clickedBlock?.type != Material.GRASS_BLOCK || ItemManager.getInstance()
                .getId(item) == null
        ) return

        val id = ItemManager.getInstance().getId(item) ?: return
        val mvndiItem = ItemManager.getInstance().getItem(id) ?: return
        if (mvndiItem.type == Item.Type.WEAPON) e.isCancelled = true
    }

    @EventHandler
    fun armorStands(event: PlayerInteractAtEntityEvent) {
        val entity = event.rightClicked
        if (entity.type != EntityType.ARMOR_STAND) return

        val armorStand = entity as ArmorStand
        if (!armorStand.isInvisible) {
            armorStand.setArms(true)
            var item = event.player.equipment.itemInMainHand
            val empty = item.isEmpty
            val offItem = event.player.equipment.itemInOffHand
            if (empty) item = offItem

            if (item.isEmpty) return

            if (item.type == Material.PLAYER_HEAD) {
                val armorStandHead = armorStand.equipment.getItem(EquipmentSlot.HEAD)
                if (!armorStandHead.isEmpty) armorStand.world.dropItemNaturally(armorStand.location, armorStandHead)

                armorStand.equipment.setItem(EquipmentSlot.HEAD, item)
                item.amount = 0
                return
            }

            val mvndiId = ItemManager.getInstance().getId(item) ?: return
            val mvndiItem = ItemManager.getInstance().getItem(mvndiId) ?: return

            val slot =
                if (mvndiItem.type == Item.Type.WEAPON || mvndiItem.type == Item.Type.ITEM) if (empty) EquipmentSlot.OFF_HAND else EquipmentSlot.HAND else (mvndiItem as Armor).slot
            val armorStandItem = armorStand.equipment.getItem(slot)

            if (!armorStandItem.isEmpty) armorStand.world.dropItemNaturally(armorStand.location, armorStandItem)

            armorStand.equipment.setItem(slot, item)
            item.amount = 0
        }
    }

    @EventHandler
    fun nether(e: PortalCreateEvent) {
        e.isCancelled = true
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun respawn(e: PlayerRespawnEvent) {
        // we have patch on Mvndi/molia to add respawn event but listeners that set the respawnLocation just dont actually make the player teleport there for some reason? but we need that patch anyway since without it this event wont fire at all and this listener wouldnt schedule the teleport (folia disables the paper event)
        e.player.scheduler.runDelayed(this, {
            e.player.teleportAsync(e.respawnLocation)
        }, null, 2L)
    }

//    @EventHandler
//    fun dropArmorStandEquipment(e: EntityDeathEvent) {
//        val entity = e.entity
//        if (entity.type != EntityType.ARMOR_STAND || entity.isInvisible) return
//
//        val armorStand = entity as ArmorStand
//        EquipmentSlot.entries.forEach { equipmentSlot ->
//            try {
//                armorStand.world.dropItemNaturally(
//                    armorStand.location, armorStand.getItem(equipmentSlot)
//                )
//            } catch (ignore: Exception) {
//            }
//        }
//    }

    @EventHandler
    fun anvilCost(e: PrepareAnvilEvent) {
        e.view.repairCost = 0
    }

    private val noDamageMod =
        AttributeModifier(NamespacedKey(this, "attack_damage_mod"), -1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1)

    private fun removeDamage(item: ItemStack?): ItemStack? {
        if (item == null || item.isEmpty) return item

        val meta = item.itemMeta
        if (meta.hasAttributeModifiers() && meta.attributeModifiers?.get(Attribute.ATTACK_DAMAGE)
                ?.contains(noDamageMod) === true
        ) return item

        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, noDamageMod)
        item.itemMeta = meta
        return item
    }

    private fun isTool(i: ItemStack?): Boolean {
        if (i == null) return false
        val lowercase = i.type.toString().lowercase()
        return lowercase.contains("axe") || lowercase.contains("shovel")
    }

    private fun isArmor(i: ItemStack?): Boolean {
        if (i == null) return false
        val lowercase = i.type.toString().lowercase()
        return lowercase.contains("leggings") || lowercase.contains("chestplate") || lowercase.contains("boots") || lowercase.contains(
            "helmet"
        )
    }

    @EventHandler
    fun removeToolDamageCraft(e: PrepareItemCraftEvent) {
        if (!isTool(e.inventory.getItem(0))) return
        val item = removeDamage(e.inventory.getItem(0)) ?: return
        e.inventory.setItem(0, item)
    }

    @EventHandler
    fun removeToolDamageClickRemoveVanilaArmor(e: InventoryClickEvent) {
        if (e.clickedInventory == null || e.whoClicked.gameMode == GameMode.CREATIVE || e.whoClicked.gameMode == GameMode.SPECTATOR) return
        if (isTool(e.currentItem)) {
            e.currentItem = removeDamage(e.currentItem)
        }
        if (!ItemManager.getInstance().isItem(e.currentItem) && isArmor(e.currentItem)) {
            e.currentItem = null
        }
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (getSkyboxSetting(player)) {
            addSkybox(event.player)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        removeSkybox(event.player)
    }
}
