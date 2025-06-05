package net.poe.entitylootdrops.fishing.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages the creation and organization of fishing configuration directories.
 */
public class FishingDirectoryManager {
    private static final Logger LOGGER = LogManager.getLogger();
    
    // Configuration directory paths
    private static final String CONFIG_DIR = "config/EntityLootDrops";
    private static final String FISHING_DIR = "Fishing";
    private static final String CONDITIONAL_DIR = "Conditional Drops";
    private static final String BIOME_DIR = "Biome Drops";
    private static final String DIMENSION_DIR = "Dimension Drops";
    
    /**
     * Creates the directory structure for fishing configuration.
     */
    public static void createConfigDirectories() {
        try {
            // Create main config directory
            Path configDir = Paths.get(CONFIG_DIR);
            Files.createDirectories(configDir);
            
            // Create fishing directory
            Path fishingDir = Paths.get(CONFIG_DIR, FISHING_DIR);
            Files.createDirectories(fishingDir);
            
            // Create subdirectories
            Path conditionalDir = Paths.get(CONFIG_DIR, FISHING_DIR, CONDITIONAL_DIR);
            Path biomeDir = Paths.get(CONFIG_DIR, FISHING_DIR, BIOME_DIR);
            Path dimensionDir = Paths.get(CONFIG_DIR, FISHING_DIR, DIMENSION_DIR);
            
            Files.createDirectories(conditionalDir);
            Files.createDirectories(biomeDir);
            Files.createDirectories(dimensionDir);
            
            // Create default files if they don't exist
            FishingDefaultFileCreator.createDefaultFiles(fishingDir, conditionalDir, biomeDir, dimensionDir);
            
        } catch (IOException e) {
            LOGGER.error("Failed to create fishing config directories", e);
        }
    }
}
