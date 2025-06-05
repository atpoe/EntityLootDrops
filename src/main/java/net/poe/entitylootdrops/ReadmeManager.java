package net.poe.entitylootdrops;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.poe.entitylootdrops.readme.*;

/**
 * Handles the creation and management of README files for the EntityLootDrops mod.
 * This centralizes all documentation to prevent conflicts between configuration classes.
 */
public class ReadmeManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String CONFIG_DIR = "config/EntityLootDrops";
    
    /**
     * Creates all README files for the mod.
     * This should be called during mod initialization.
     */
    public static void createAllReadmeFiles() {
        try {
            // Create main config directory if it doesn't exist
            Path configDir = Paths.get(CONFIG_DIR);
            Files.createDirectories(configDir);
            
            // Create main README
            MainReadmeCreator.createMainReadme(configDir);
            
            // Create entity loot README
            Path entitiesDir = configDir.resolve("Entities");
            if (Files.exists(entitiesDir)) {
                EntityReadmeCreator.createEntityLootReadme(entitiesDir);
            }
            
            // Create block drops README
            Path blocksDir = configDir.resolve("Blocks");
            if (Files.exists(blocksDir)) {
                BlockReadmeCreator.createBlockDropsReadme(blocksDir);
                BlockReadmeCreator.createNormalDropsReadme(blocksDir.resolve("Normal Drops"));
                BlockReadmeCreator.createEventDropsReadme(blocksDir.resolve("Event Drops"));
                
                // Create README for each event type
                Path eventDropsDir = blocksDir.resolve("Event Drops");
                if (Files.exists(eventDropsDir)) {
                    Files.list(eventDropsDir)
                        .filter(Files::isDirectory)
                        .forEach(eventDir -> {
                            try {
                                String eventName = eventDir.getFileName().toString();
                                BlockReadmeCreator.createEventTypeReadme(eventDir, eventName);
                            } catch (IOException e) {
                                LOGGER.error("Failed to create README for event: {}", eventDir, e);
                            }
                        });
                }
            }
            
            // Create recipes README
            Path recipesDir = configDir.resolve("Recipes");
            if (Files.exists(recipesDir)) {
                RecipeReadmeCreator.createRecipesReadme(recipesDir);
                RecipeReadmeCreator.createShapedRecipesReadme(recipesDir.resolve("Shaped"));
                RecipeReadmeCreator.createShapelessRecipesReadme(recipesDir.resolve("Shapeless"));
                RecipeReadmeCreator.createBrewingRecipesReadme(recipesDir.resolve("Brewing"));
                RecipeReadmeCreator.createFurnaceRecipesReadme(recipesDir.resolve("Furnace"));
                RecipeReadmeCreator.createSmithingRecipesReadme(recipesDir.resolve("Smithing"));
            }
            
            // Create fishing drops README
            Path fishingDir = configDir.resolve("Fishing");
            if (Files.exists(fishingDir)) {
                FishingReadmeCreator.createFishingDropsReadme(fishingDir);
                FishingReadmeCreator.createConditionalFishingReadme(fishingDir.resolve("Conditional Drops"));
                FishingReadmeCreator.createBiomeFishingReadme(fishingDir.resolve("Biome Drops"));
                FishingReadmeCreator.createDimensionFishingReadme(fishingDir.resolve("Dimension Drops"));
            }
            
            LOGGER.info("Created all README files");
        } catch (IOException e) {
            LOGGER.error("Failed to create README files", e);
        }
    }
}
