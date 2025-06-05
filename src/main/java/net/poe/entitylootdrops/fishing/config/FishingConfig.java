package net.poe.entitylootdrops.fishing.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.poe.entitylootdrops.fishing.FishingDrop;
import net.poe.entitylootdrops.fishing.FishingReward;

/**
 * Configuration class for managing fishing drop rewards and commands.
 */
public class FishingConfig {
    private static final Logger LOGGER = LogManager.getLogger();
    
    private static List<FishingDrop> fishingDrops = new ArrayList<>();
    private static List<FishingReward> globalFishingRewards = new ArrayList<>();
    
    /**
     * Loads the fishing configuration from files.
     */
    public static void loadConfig(File configDir) {
        try {
            // Create directories first
            FishingDirectoryManager.createConfigDirectories();
            
            // Load all fishing data
            fishingDrops.clear();
            globalFishingRewards.clear();
            
            // Load global fishing rewards
            File globalFile = new File(configDir, "EntityLootDrops/Fishing/Global_Fishing_Rewards.json");
            FishingConfigLoader.loadGlobalFishingRewards(globalFile, globalFishingRewards);
            
            // Load conditional drops
            File conditionalDir = new File(configDir, "EntityLootDrops/Fishing/Conditional Drops");
            FishingConfigLoader.loadDropsFromDirectory(conditionalDir, fishingDrops);
            
            // Load biome drops
            File biomeDir = new File(configDir, "EntityLootDrops/Fishing/Biome Drops");
            FishingConfigLoader.loadDropsFromDirectory(biomeDir, fishingDrops);
            
            // Load dimension drops
            File dimensionDir = new File(configDir, "EntityLootDrops/Fishing/Dimension Drops");
            FishingConfigLoader.loadDropsFromDirectory(dimensionDir, fishingDrops);
            
            LOGGER.info("Loaded {} fishing drops and {} global fishing rewards", 
                       fishingDrops.size(), globalFishingRewards.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load fishing configuration", e);
        }
    }
    
    /**
     * Reloads the fishing configuration.
     */
    public static void reloadConfig(File configDir) {
        LOGGER.info("Reloading fishing configuration...");
        loadConfig(configDir);
    }
    
    // Getters for the loaded data
    public static List<FishingDrop> getFishingDrops() {
        return new ArrayList<>(fishingDrops);
    }
    
    public static List<FishingReward> getGlobalFishingRewards() {
        return new ArrayList<>(globalFishingRewards);
    }
}
