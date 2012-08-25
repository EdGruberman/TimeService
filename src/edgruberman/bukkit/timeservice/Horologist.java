package edgruberman.bukkit.timeservice;

import java.io.File;
import java.util.List;
import java.util.TimeZone;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

import edgruberman.bukkit.timeservice.util.BufferedYamlConfiguration;

public class Horologist implements Listener {

    private static TimeZone DEFAULT_TIME_ZONE = TimeZone.getDefault();

    public static TimeZone getTimeZone(final Player player) {
        final List<MetadataValue> values = player.getMetadata("TimeZone");
        if (values.size() == 0) return Horologist.DEFAULT_TIME_ZONE;
        return (TimeZone) values.get(0).value();
    }



    private final Plugin plugin;
    private final BufferedYamlConfiguration zones;

    Horologist(final Plugin plugin, final File zones) {
        this.plugin = plugin;
        try {
            this.zones = new BufferedYamlConfiguration(plugin, zones, 5000);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
        this.setDefault(this.getDefault());
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void set(final String player, final TimeZone zone) {
        this.zones.set("players." + player, zone.getID());
        this.zones.queueSave();

        final Player online = Bukkit.getPlayer(player);
        if (online == null) return;

        this.refresh(online);
    }

    public void refresh(final Player player) {
        TimeZone zone = Horologist.DEFAULT_TIME_ZONE;
        if (this.zones.isString("players." + player.getName()))
            zone = TimeZone.getTimeZone(this.zones.getString("players." + player.getName()));

        player.setMetadata("TimeZone", new FixedMetadataValue(this.plugin, zone));
    }

    public TimeZone getDefault() {
        if (!this.zones.isString("default"))
            return Horologist.DEFAULT_TIME_ZONE;

        return TimeZone.getTimeZone(this.zones.getString("default"));
    }

    public void setDefault(final TimeZone zone) {
        this.zones.set("default", zone.getID());
        Horologist.DEFAULT_TIME_ZONE = zone;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent joined) {
        this.refresh(joined.getPlayer());
    }

}
