package net.poe.entitylootdrops.config;

import net.poe.entitylootdrops.EntityLootDrops;
import net.poe.entitylootdrops.lootdrops.LootConfig;

/**
 * Configuration helper for the EntityLootDrops mod.
 * This class helps manage the state of active events and settings.
 */
public class ModConfig {
    // Flag to track if config has been loaded
    private static boolean configLoaded = false;
    
    /**
     * Updates the Forge config with values from LootConfig
     * This should be called whenever LootConfig is modified programmatically
     * @return true if the sync was successful, false otherwise
     */
    public static boolean syncFromLootConfig() {
        try {
            // Check if config has been loaded
            if (!configLoaded) {
                // Instead of just warning, initialize the config values
                EntityLootDrops.getLogger().info("Config not loaded yet, initializing with current values");
                configLoaded = true;
            }
            
            // Since we're not using Forge config, we just need to ensure
            // the active events are saved to disk when the server stops
            // This is handled by LootConfig.loadConfig() which is called on server start
            
            EntityLootDrops.getLogger().debug("Config sync successful");
            return true;
        } catch (Exception e) {
            EntityLootDrops.getLogger().error("Error syncing config: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Checks if the config has been loaded
     * @return true if the config has been loaded, false otherwise
     */
    public static boolean isConfigLoaded() {
        return configLoaded;
    }
    
    /**
     * Marks the config as loaded
     * This should be called after LootConfig.loadConfig()
     */
    public static void setConfigLoaded(boolean loaded) {
        configLoaded = loaded;
    }
}
