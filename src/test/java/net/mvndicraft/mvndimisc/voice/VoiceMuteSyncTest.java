package net.mvndicraft.mvndimisc.voice;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import litebans.api.Database;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VoiceMuteSyncTest {
    private static final String SPEAK_PERMISSION = "voicechat.speak";
    private static final String INTERVAL_KEY = "voice-mute-sync-interval-seconds";
    private static final String ADDRESS = "127.0.0.1";

    @Test
    void restoresInheritedPermissionsAfterMuteExpiresAndCleansUpOnDisable() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        FileConfiguration config = mock(FileConfiguration.class);
        AsyncScheduler async = mock(AsyncScheduler.class);
        EntityScheduler entity = mock(EntityScheduler.class);
        ScheduledTask task = mock(ScheduledTask.class);
        Player player = mock(Player.class);
        PermissionAttachment attachment = mock(PermissionAttachment.class);
        Database database = mock(Database.class);
        UUID uuid = UUID.randomUUID();
        AtomicReference<Consumer<ScheduledTask>> refresh = new AtomicReference<>();

        when(plugin.getServer()).thenReturn(server);
        when(plugin.getConfig()).thenReturn(config);
        when(config.getLong(INTERVAL_KEY, 1)).thenReturn(1L);
        when(server.getAsyncScheduler()).thenReturn(async);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getAddress()).thenReturn(new InetSocketAddress(ADDRESS, 25565));
        when(player.getScheduler()).thenReturn(entity);
        when(player.addAttachment(plugin, SPEAK_PERMISSION, false)).thenReturn(attachment);
        when(async.runAtFixedRate(eq(plugin), any(), eq(0L), eq(1L), eq(TimeUnit.SECONDS)))
                .thenAnswer(invocation -> {
                    refresh.set(invocation.getArgument(1));
                    return task;
                });
        when(entity.run(eq(plugin), any(), isNull())).thenAnswer(invocation -> {
            Consumer<ScheduledTask> callback = invocation.getArgument(1);
            callback.accept(task);
            return task;
        });
        when(database.isPlayerMuted(uuid, ADDRESS)).thenReturn(true, true, false, true);

        try (MockedStatic<Database> api = mockStatic(Database.class)) {
            api.when(Database::get).thenReturn(database);
            VoiceMuteSync sync = new VoiceMuteSync(plugin);
            sync.onJoin(new PlayerJoinEvent(player, Component.empty()));
            refresh.get().accept(task);
            refresh.get().accept(task);
            verify(player, times(1)).addAttachment(plugin, SPEAK_PERMISSION, false);

            refresh.get().accept(task);
            verify(attachment).remove();

            refresh.get().accept(task);
            sync.close();
            verify(attachment, times(2)).remove();
            verify(task).cancel();
            refresh.get().accept(task);
            verify(database, times(4)).isPlayerMuted(uuid, ADDRESS);
        }
    }
}
