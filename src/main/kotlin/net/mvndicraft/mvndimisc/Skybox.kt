package net.mvndicraft.mvndimisc

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.component.ComponentTypes
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemModel
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes
import com.github.retrooper.packetevents.protocol.world.Location
import com.github.retrooper.packetevents.resources.ResourceLocation
import me.tofaa.entitylib.meta.display.ItemDisplayMeta
import me.tofaa.entitylib.wrapper.WrapperEntity
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import java.util.HashMap
import java.util.UUID

const val UPDATE_TICKS = 5L * 20L
private val playerEntities: MutableMap<UUID, WrapperEntity> = HashMap()
private val skyboxDataKey: NamespacedKey = NamespacedKey.fromString("mvndicraft:skybox") ?: throw RuntimeException()

val skyboxItem: ItemStack = run {
    val builder = ItemStack.builder()
    builder.type(ItemTypes.DIAMOND_HOE)
    builder.amount(1)
    builder.component(ComponentTypes.ITEM_MODEL, ItemModel(ResourceLocation("shader_assets", "skybox")))
    builder.build()
}

internal fun updateSkyboxes() {
    for (player in Bukkit.getOnlinePlayers()) {
        val entity = playerEntities[player.uniqueId] ?: continue
        val targetLoc = Location(player.location.x, player.location.y, player.location.z, 0f, 0f)
        entity.teleport(targetLoc)
    }
}

fun createSkyboxThread(ctx: MvndiMisc) {
    Bukkit.getGlobalRegionScheduler().runAtFixedRate(ctx, {
        updateSkyboxes()
    }, 1, UPDATE_TICKS)
}

fun addSkybox(player: Player) {
    val user = PacketEvents.getAPI().playerManager.getUser(player) ?: return
    val entity = WrapperEntity(EntityTypes.ITEM_DISPLAY)
    val meta = entity.entityMeta as ItemDisplayMeta

    meta.item = skyboxItem.copy()
    entity.spawn(Location(player.location.x, player.location.y, player.location.z, 0f, 0f))
    entity.addViewer(user)

    playerEntities[player.uniqueId] = entity
}

fun removeSkybox(player: Player) {
    val entity = playerEntities.remove(player.uniqueId) ?: return
    entity.remove()
}

fun getSkyboxSetting(player: Player): Boolean {
    return player.persistentDataContainer.get(skyboxDataKey, PersistentDataType.BOOLEAN) ?: true
}

fun setSkyboxSetting(player: Player, setting: Boolean) {
    player.persistentDataContainer.set(skyboxDataKey, PersistentDataType.BOOLEAN, setting)
}