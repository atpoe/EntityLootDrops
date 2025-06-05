package net.poe.entitylootdrops.recipes;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.poe.entitylootdrops.recipes.config.RecipeConfigLoader;
import net.poe.entitylootdrops.recipes.config.RecipeConfigManager;
import net.poe.entitylootdrops.recipes.model.RecipeEntry;

/**
 * Main configuration class for custom recipes.
 */
public class RecipeConfig {
    private static final Logger LOGGER = LogManager.getLogger();
    
    private static RecipeConfigManager configManager;
    private static RecipeConfigLoader configLoader;
    
    static {
        configManager = new RecipeConfigManager();
        configLoader = new RecipeConfigLoader(configManager);
    }
    
    /**
     * Loads all recipe configurations.
     */
    public static void loadConfig() {
        configLoader.loadConfig();
        LOGGER.info("Loaded recipe configuration: {} total recipes, {} enabled", 
            configManager.getRecipeCount(), configManager.getEnabledRecipeCount());
    }
    
    /**
     * Gets the config manager instance.
     */
    public static RecipeConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * Gets all enabled recipes.
     */
    public static List<RecipeEntry> getAllRecipes() {
        return configManager.getEnabledRecipes();
    }
    
    /**
     * Gets recipes by type.
     */
    public static List<RecipeEntry> getRecipesByType(String type) {
        return configManager.getRecipesByType(type);
    }
    
    /**
     * Gets recipes that should replace existing ones.
     */
    public static List<RecipeEntry> getReplacementRecipes() {
        return configManager.getReplacementRecipes();
    }
    
    // Convenience methods for specific recipe types
    public static List<RecipeEntry> getCraftingRecipes() {
        return getRecipesByType("crafting");
    }
    
    public static List<RecipeEntry> getFurnaceRecipes() {
        return getRecipesByType("furnace");
    }
    
    public static List<RecipeEntry> getBlastingRecipes() {
        return getRecipesByType("blasting");
    }
    
    public static List<RecipeEntry> getSmokingRecipes() {
        return getRecipesByType("smoking");
    }
    
    public static List<RecipeEntry> getCampfireRecipes() {
        return getRecipesByType("campfire");
    }
    
    public static List<RecipeEntry> getStonecuttingRecipes() {
        return getRecipesByType("stonecutting");
    }
    
    public static List<RecipeEntry> getSmithingRecipes() {
        return getRecipesByType("smithing");
    }
}
