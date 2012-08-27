package edgruberman.bukkit.timeservice;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import edgruberman.bukkit.timeservice.commands.Reload;
import edgruberman.bukkit.timeservice.commands.TimeZone;
import edgruberman.bukkit.timeservice.messaging.ConfigurationCourier;
import edgruberman.bukkit.timeservice.messaging.Courier;
import edgruberman.bukkit.timeservice.util.CustomPlugin;

public final class Main extends CustomPlugin {

    public static Courier courier = null;
    public static Horologist horologist = null;

    @Override
    public void onLoad() { this.putConfigMinimum("config.yml", "1.0.0"); }

    @Override
    public void onEnable() {
        this.reloadConfig();
        Main.courier = ConfigurationCourier.Factory.create(this).setBase("messages").build();

        Main.horologist = new Horologist(this, new File(this.getDataFolder(), "zones.yml"));

        final List<Pattern> exclude = new ArrayList<Pattern>();
        for (final String pattern : this.getConfig().getStringList("exclude"))
            try {
                exclude.add(Pattern.compile(pattern));
            } catch (final PatternSyntaxException e) {
                this.getLogger().warning("Unable to parse regular expression pattern for exclude: " + pattern + "; " + e);
            }
        for (final Pattern pattern : exclude)
            this.getLogger().log(Level.CONFIG, "Excluding time zone results that match: {0}", pattern.pattern());

        this.getCommand("timeservice:timezone").setExecutor(new TimeZone(exclude));
        this.getCommand("timeservice:reload").setExecutor(new Reload(this));
    }

    @Override
    public void onDisable() {
        Main.courier = null;
        Main.horologist = null;
    }

}
