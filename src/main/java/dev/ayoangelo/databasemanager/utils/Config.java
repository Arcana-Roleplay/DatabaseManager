package dev.ayoangelo.databasemanager.utils;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.util.TimeZone;
import java.time.format.DateTimeFormatter;

import java.io.File;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Config {

    private static JavaPlugin plugin;
    private static File configFile;
    private static FileConfiguration config;

    public static void setup(JavaPlugin main) {
        plugin = main;
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            try (InputStream in = plugin.getResource("config.yml");
                 OutputStream out = Files.newOutputStream(configFile.toPath())) {
                if (in != null) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        reload();
    }


    public static void reload() {
        if (configFile == null) return;

        config = YamlConfiguration.loadConfiguration(configFile);
        InputStream defConfigStream = plugin.getResource("config.yml");
        if (defConfigStream != null) {
            config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream)));
        }
    }

    public static <T> T getData(Class<T> clazz, String key, T defaultValue) {
        if (config == null) return defaultValue;

        try {
            Object value = config.get(key);
            if (value == null) return defaultValue;

            if (clazz.isInstance(value)) {
                return clazz.cast(value);
            }

            // Tipi primitivi/supportati nativamente
            if (clazz == Integer.class) return clazz.cast(config.getInt(key));
            if (clazz == Double.class) return clazz.cast(config.getDouble(key));
            if (clazz == Boolean.class) return clazz.cast(config.getBoolean(key));
            if (clazz == String.class) return clazz.cast(config.getString(key));
            if (clazz == List.class) return clazz.cast(config.getList(key));

            // Tipi Bukkit speciali
            if (clazz == Material.class) {
                String mat = config.getString(key);
                return clazz.cast(Material.valueOf(mat.toUpperCase()));
            }

            if (clazz == Sound.class) {
                String sound = config.getString(key);
                return clazz.cast(Sound.valueOf(sound.toUpperCase()));
            }

            if (clazz == PotionEffectType.class) {
                String potion = config.getString(key);
                return clazz.cast(PotionEffectType.getByName(potion.toUpperCase()));
            }

            if (clazz == Enchantment.class) {
                String enchant = config.getString(key);
                return clazz.cast(Enchantment.getByName(enchant.toUpperCase()));
            }

            if (clazz == TimeZone.class) {
                String zone = config.getString(key);
                TimeZone tz = TimeZone.getTimeZone(zone);
                if (tz.getID().equals("GMT") && !zone.equalsIgnoreCase("GMT")) return defaultValue;
                return clazz.cast(tz);
            }

            if (clazz == DateTimeFormatter.class) {
                String pattern = config.getString(key);
                try {
                    return clazz.cast(DateTimeFormatter.ofPattern(pattern));
                } catch (IllegalArgumentException e) {
                    return defaultValue;
                }
            }

        } catch (Exception e) {
            return defaultValue;
        }

        return defaultValue;
    }


    public static boolean isType(Class<?> clazz, String key) {
        if (config == null) return false;

        try {
            Object value = config.get(key);
            if (value == null) return false;

            if (clazz.isInstance(value)) return true;

            // Tipi primitivi/supportati nativamente
            if (clazz == Integer.class) {
                config.getInt(key);
            } else if (clazz == Double.class) {
                config.getDouble(key);
            } else if (clazz == Boolean.class) {
                config.getBoolean(key);
            } else if (clazz == String.class) {
                config.getString(key);
            } else if (clazz == List.class) {
                config.getList(key);
            }

            // Tipi Bukkit speciali
            else if (clazz == Material.class) {
                String mat = config.getString(key);
                Material.valueOf(mat.toUpperCase());
            } else if (clazz == Sound.class) {
                String sound = config.getString(key);
                Sound.valueOf(sound.toUpperCase());
            } else if (clazz == PotionEffectType.class) {
                String potion = config.getString(key);
                if (PotionEffectType.getByName(potion.toUpperCase()) == null) return false;
            } else if (clazz == Enchantment.class) {
                String enchant = config.getString(key);
                if (Enchantment.getByName(enchant.toUpperCase()) == null) return false;
            } else if (clazz == TimeZone.class) {
                String zone = config.getString(key);
                TimeZone tz = TimeZone.getTimeZone(zone);
                return !(tz.getID().equals("GMT") && !zone.equalsIgnoreCase("GMT"));
            }
            else if (clazz == DateTimeFormatter.class) {
                String pattern = config.getString(key);
                try {
                    DateTimeFormatter.ofPattern(pattern);
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            } else {
                return false;
            }



            return true;

        } catch (Exception e) {
            return false;
        }
    }

    public static List<String> getKeys(String key) {
        if (config == null) return new ArrayList<>();

        ConfigurationSection section = config.getConfigurationSection(key);
        if (section == null) return new ArrayList<>();

        return new ArrayList<>(section.getKeys(false));
    }
}
