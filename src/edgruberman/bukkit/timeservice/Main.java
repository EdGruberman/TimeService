package edgruberman.bukkit.timeservice;

import java.io.File;

import edgruberman.bukkit.timeservice.commands.Reload;
import edgruberman.bukkit.timeservice.commands.TimeZone;
import edgruberman.bukkit.timeservice.messaging.ConfigurationCourier;
import edgruberman.bukkit.timeservice.messaging.Courier;
import edgruberman.bukkit.timeservice.util.CustomPlugin;

public final class Main extends CustomPlugin {

    public static Courier courier = null;
    public static Horologist horologist = null;

    @Override
    public void onLoad() { this.putConfigMinimum("config.yml", "0.0.0a0"); }

    @Override
    public void onEnable() {
        this.reloadConfig();
        Main.courier = ConfigurationCourier.Factory.create(this).setBase("messages").build();

        Main.horologist = new Horologist(this, new File(this.getDataFolder(), "zones.yml"));

        this.getCommand("timeservice:timezone").setExecutor(new TimeZone());
        this.getCommand("timeservice:reload").setExecutor(new Reload(this));
    }

    @Override
    public void onDisable() {
        Main.courier = null;
        Main.horologist = null;
    }

}
