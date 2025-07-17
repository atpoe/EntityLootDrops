package net.poe.entitylootdrops.lootdrops.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Manages event-specific configuration settings.
 */
public class EventConfig {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String CONFIG_DIR = "config/EntityLootDrops";
    private static final String EVENT_CONFIG_FILE = "EventConfig.json";

    private static EventConfigData config = new EventConfigData();

    /**
     * Configuration data structure for events.
     */
    public static class EventConfigData {
        private boolean enableDropChanceEvent = true;
        private boolean enableDoubleDropsEvent = true;
        private boolean affectVanillaDrops = true;
        private boolean affectModdedDrops = true;
        private List<String> allowedMods = new ArrayList<>();
        private List<String> blockedMods = new ArrayList<>();
        private double dropChanceMultiplier = 0.5; // 50% chance for extra drops
        private double doubleDropChanceMultiplier = 2.0; // 2x multiplier for double drops
        private String comment = "Event configuration for EntityLootDrops mod";

        public EventConfigData() {
            // Default allowed mods - popular RPG/loot mods
            allowedMods.add("minecraft");
            allowedMods.add("mmorpg");
            allowedMods.add("age_of_exile");
            allowedMods.add("mine_and_slash");
            allowedMods.add("dungeons_gear");
            allowedMods.add("reliquary");

            // Example blocked mods (performance-sensitive mods)
            blockedMods.add("create");
            blockedMods.add("immersiveengineering");
        }

        // Getters and setters
        public boolean isEnableDropChanceEvent() { return enableDropChanceEvent; }
        public void setEnableDropChanceEvent(boolean enableDropChanceEvent) { this.enableDropChanceEvent = enableDropChanceEvent; }

        public boolean isEnableDoubleDropsEvent() { return enableDoubleDropsEvent; }
        public void setEnableDoubleDropsEvent(boolean enableDoubleDropsEvent) { this.enableDoubleDropsEvent = enableDoubleDropsEvent; }

        public boolean isAffectVanillaDrops() { return affectVanillaDrops; }
        public void setAffectVanillaDrops(boolean affectVanillaDrops) { this.affectVanillaDrops = affectVanillaDrops; }

        public boolean isAffectModdedDrops() { return affectModdedDrops; }
        public void setAffectModdedDrops(boolean affectModdedDrops) { this.affectModdedDrops = affectModdedDrops; }

        public List<String> getAllowedMods() { return allowedMods; }
        public void setAllowedMods(List<String> allowedMods) { this.allowedMods = allowedMods; }

        public List<String> getBlockedMods() { return blockedMods; }
        public void setBlockedMods(List<String> blockedMods) { this.blockedMods = blockedMods; }

        public double getDropChanceMultiplier() { return dropChanceMultiplier; }
        public void setDropChanceMultiplier(double dropChanceMultiplier) { this.dropChanceMultiplier = dropChanceMultiplier; }

        public double getDoubleDropChanceMultiplier() { return doubleDropChanceMultiplier; }
        public void setDoubleDropChanceMultiplier(double doubleDropChanceMultiplier) { this.doubleDropChanceMultiplier = doubleDropChanceMultiplier; }

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }

    /**
     * Loads the event configuration from file.
     */
    public static void loadConfig() {
        Path configDir = Paths.get(CONFIG_DIR);
        Path configFile = configDir.resolve(EVENT_CONFIG_FILE);

        try {
            // Ensure config directory exists
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
                LOGGER.info("Created config directory: {}", configDir);
            }

            if (!Files.exists(configFile)) {
                createDefaultConfig(configFile);
                LOGGER.info("Created default event configuration: {}", configFile);
            } else {
                String json = Files.readString(configFile);
                Gson gson = new Gson();
                config = gson.fromJson(json, EventConfigData.class);

                // Validate loaded config
                if (config == null) {
                    LOGGER.warn("Failed to parse event configuration, creating new default");
                    config = new EventConfigData();
                    createDefaultConfig(configFile);
                } else {
                    LOGGER.info("Loaded event configuration from: {}", configFile);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load event configuration, using defaults", e);
            config = new EventConfigData();

            // Try to create default config as fallback
            try {
                createDefaultConfig(configFile);
            } catch (IOException ioException) {
                LOGGER.error("Failed to create default event configuration", ioException);
            }
        }
    }

    /**
     * Creates a default configuration file with examples.
     */
    private static void createDefaultConfig(Path configFile) throws IOException {
        // Ensure directory exists
        Files.createDirectories(configFile.getParent());

        // Create enhanced JSON with detailed comments
        String enhancedJson = """
            {
              "comment": "Event configuration for EntityLootDrops mod",
              "enableDropChanceEvent": true,
              "enableDoubleDropsEvent": true,
              "affectVanillaDrops": true,
              "affectModdedDrops": true,
              "dropChanceMultiplier": 0.5,
              "doubleDropChanceMultiplier": 2.0,
              "allowedMods": [
                "minecraft",
                "mmorpg",
                "age_of_exile",
                "mine_and_slash",
                "dungeons_gear",
                "reliquary"
              ],
              "blockedMods": [
                "create",
                "immersiveengineering"
              ]
            }
            """;

        Files.writeString(configFile, enhancedJson);
        LOGGER.info("Created EventConfig.json with comprehensive documentation");
    }

    /**
     * Checks if drop chance events are enabled in config.
     */
    public static boolean isDropChanceEventEnabled() {
        return config.isEnableDropChanceEvent();
    }

    /**
     * Checks if double drops events are enabled in config.
     */
    public static boolean isDoubleDropsEventEnabled() {
        return config.isEnableDoubleDropsEvent();
    }

    /**
     * Checks if a mod should be affected by events.
     */
    public static boolean shouldAffectMod(String modId) {
        // Check if vanilla drops should be affected
        if ("minecraft".equals(modId) && !config.isAffectVanillaDrops()) {
            return false;
        }

        // Check if modded drops should be affected
        if (!"minecraft".equals(modId) && !config.isAffectModdedDrops()) {
            return false;
        }

        // Check blocked mods list
        if (config.getBlockedMods().contains(modId)) {
            return false;
        }

        // If allowed mods list is empty, allow all (except blocked)
        if (config.getAllowedMods().isEmpty()) {
            return true;
        }

        // Check if mod is in allowed list
        return config.getAllowedMods().contains(modId);
    }

    /**
     * Gets the drop chance multiplier.
     */
    public static double getDropChanceMultiplier() {
        return config.getDropChanceMultiplier();
    }

    /**
     * Gets the double drops multiplier.
     */
    public static double getDoubleDropChanceMultiplier() {
        return config.getDoubleDropChanceMultiplier();
    }

    /**
     * Gets the current configuration data.
     */
    public static EventConfigData getConfig() {
        return config;
    }

    /**
     * Checks if a mod is allowed for drop events (drop chance and double drops).
     */
    public static boolean isModAllowedForDropEvents(String modId) {
        return shouldAffectMod(modId);
    }
}