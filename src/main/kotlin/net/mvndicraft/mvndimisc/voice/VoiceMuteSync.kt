package net.mvndicraft.mvndimisc.voice

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import litebans.api.Database
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.permissions.PermissionAttachment
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import java.util.logging.Level
import kotlin.concurrent.withLock

class VoiceMuteSync(private val plugin: JavaPlugin) : Listener, AutoCloseable {
    companion object {
        private const val SPEAK_PERMISSION = "voicechat.speak"
        private const val INTERVAL_KEY = "voice-mute-sync-interval-seconds"
        private const val QUERY_FAILURE = "Could not synchronize LiteBans voice mute"
    }

    private val sessions: MutableMap<UUID, Session> = ConcurrentHashMap()
    private val interval: Long = plugin.config.getLong(INTERVAL_KEY, 1).coerceAtLeast(1)

    fun start() {
        plugin.server.pluginManager.registerEvents(this, plugin)
        plugin.server.onlinePlayers.forEach { player: Player ->
            player.scheduler.run(plugin, { track(player) }, null)
        }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        track(event.player)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        sessions.remove(event.player.uniqueId)?.close()
    }

    private fun track(player: Player) {
        val address: String = player.address?.address?.hostAddress ?: return
        val session: Session = Session(player, address)
        sessions.put(player.uniqueId, session)?.close()
        session.start()
    }

    override fun close() {
        sessions.values.forEach { it.close() }
        sessions.clear()
    }

    private inner class Session(private val player: Player, private val address: String) : AutoCloseable {
        private val querying: AtomicBoolean = AtomicBoolean()
        private val closed: AtomicBoolean = AtomicBoolean()
        private val permissionLock: ReentrantLock = ReentrantLock()
        private var attachment: PermissionAttachment? = null
        private var task: ScheduledTask? = null

        fun start() {
            task = plugin.server.asyncScheduler.runAtFixedRate(
                plugin, { refresh() }, 0, interval, TimeUnit.SECONDS
            )
        }

        private fun refresh() {
            if (closed.get() || !querying.compareAndSet(false, true)) return
            try {
                val muted: Boolean = Database.get().isPlayerMuted(player.uniqueId, address)
                if (closed.get()) return
                player.scheduler.run(plugin, { applyMute(muted) }, null)
            } catch (exception: Exception) {
                if (!closed.get()) plugin.logger.log(Level.WARNING, QUERY_FAILURE, exception)
            } finally {
                querying.set(false)
            }
        }

        private fun applyMute(muted: Boolean): Unit = permissionLock.withLock {
            if (closed.get()) return
            if (muted && attachment == null) {
                attachment = player.addAttachment(plugin, SPEAK_PERMISSION, false)
            } else if (!muted) {
                attachment?.remove()
                attachment = null
            }
        }

        override fun close() {
            closed.set(true)
            task?.cancel()
            permissionLock.withLock {
                attachment?.remove()
                attachment = null
            }
        }
    }
}
