package dev.ayoangelo.databasemanager;

import dev.ayoangelo.databasemanager.utils.Config;
import org.bukkit.plugin.java.JavaPlugin;

public final class DatabaseManager extends JavaPlugin {

    @Override
    public void onEnable() {
        Config.setup(this);
        getLogger().info("Database manager attivato!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Database manager disattivato!");
    }
}
