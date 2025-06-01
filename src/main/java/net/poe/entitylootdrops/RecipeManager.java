package net.poe.entitylootdrops;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Manages custom recipes and registers them with Minecraft with improved tag support and reload handling.
 */
@Mod.EventBusSubscriber(modid = EntityLootDrops.MOD_ID)
public class RecipeManager {
    private static final Logger LOGGER = LogManager.getLogger();
    
    // Cache for reflection fields to improve performance
    private static Field recipesField = null;
    private static Field byNameField = null;
    private static boolean fieldsInitialized = false;
    
    // Thread-safe cache for registered recipes to handle reload scenarios
    private static final Map<ResourceLocation, Recipe<?>> registeredRecipeCache = new ConcurrentHashMap<>();
    
    // Flag to track registration state
    private static volatile boolean isRegistering = false;
    
    /**
     * Registers custom recipes when the server starts.
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("Server started, loading and registering custom recipes...");
        
        // Load recipe configuration
        RecipeConfig.loadConfig();
        
        // Register recipes
        boolean success = registerRecipes(event.getServer());
        
        if (success) {
            LOGGER.info("Successfully registered custom recipes on server start");
        } else {
            LOGGER.warn("Some issues occurred during recipe registration on server start");
        }
        
        // Apply recipe replacements after registration
        try {
            RecipeReplacementHandler.applyRecipeReplacements(event.getServer());
        } catch (Exception e) {
            LOGGER.error("Failed to apply recipe replacements on server start", e);
        }
    }
    
    /**
     * Registers all custom recipes with Minecraft with improved tag support and reload handling.
     * This can be called during server runtime to register new recipes without a restart.
     * 
     * @param server The Minecraft server instance
     * @return true if registration was successful, false if there were issues
     */
    public static synchronized boolean registerRecipes(MinecraftServer server) {
        if (server == null) {
            LOGGER.error("Server is null, cannot register recipes");
            return false;
        }
        
        // Check if recipes are currently being reloaded
        if (RecipeConfig.isReloading()) {
            LOGGER.info("Recipes are currently reloading, deferring registration");
            return false;
        }
        
        // Set registration flag
        isRegistering = true;
        
        try {
            // Initialize reflection fields if needed
            if (!fieldsInitialized) {
                initializeReflectionFields(server.getRecipeManager());
            }
            
            int shapedCount = 0;
            int shapelessCount = 0;
            int totalAttempted = 0;
            
            // Clear previous registrations from cache to handle reload scenarios
            clearPreviousRegistrations(server);
            
            // Register shaped recipes with improved tag support
            for (RecipeConfig.CustomRecipe recipeConfig : RecipeConfig.getShapedRecipes()) {
                totalAttempted++;
                try {
                    if (registerShapedRecipe(server, recipeConfig)) {
                        shapedCount++;
                        LOGGER.debug("Successfully registered shaped recipe: {}", recipeConfig.getName());
                    } else {
                        LOGGER.warn("Failed to register shaped recipe: {}", recipeConfig.getName());
                    }
                } catch (Exception e) {
                    LOGGER.error("Exception while registering shaped recipe {}: {}", recipeConfig.getName(), e.getMessage());
                }
            }
            
            // Register shapeless recipes with improved tag support
            for (RecipeConfig.CustomRecipe recipeConfig : RecipeConfig.getShapelessRecipes()) {
                totalAttempted++;
                try {
                    if (registerShapelessRecipe(server, recipeConfig)) {
                        shapelessCount++;
                        LOGGER.debug("Successfully registered shapeless recipe: {}", recipeConfig.getName());
                    } else {
                        LOGGER.warn("Failed to register shapeless recipe: {}", recipeConfig.getName());
                    }
                } catch (Exception e) {
                    LOGGER.error("Exception while registering shapeless recipe {}: {}", recipeConfig.getName(), e.getMessage());
                }
            }
            
            LOGGER.info("Recipe registration complete: {} shaped, {} shapeless out of {} total attempted", 
                shapedCount, shapelessCount, totalAttempted);
            
            // Update the crafting event handler cache
            try {
                CraftingEventHandler.initRecipeCache();
            } catch (Exception e) {
                LOGGER.error("Failed to initialize crafting event handler cache", e);
            }
            
            return shapedCount > 0 || shapelessCount > 0;
            
        } catch (Exception e) {
            LOGGER.error("Failed to register recipes: {}", e.getMessage(), e);
            return false;
        } finally {
            isRegistering = false;
        }
    }
    
    /**
     * Registers a shaped recipe with improved tag support.
     */
    private static boolean registerShapedRecipe(MinecraftServer server, RecipeConfig.CustomRecipe recipeConfig) {
        try {
            // Create recipe ID
            ResourceLocation recipeId = new ResourceLocation(EntityLootDrops.MOD_ID, recipeConfig.getName());
            
            // Check if recipe already exists in cache
            if (registeredRecipeCache.containsKey(recipeId)) {
                LOGGER.debug("Recipe {} already registered, skipping", recipeId);
                return true;
            }
            
            // Create output ItemStack with NBT if specified
            ItemStack output = recipeConfig.createOutputStack();
            if (output.isEmpty()) {
                LOGGER.error("Failed to create output for shaped recipe: {}", recipeConfig.getName());
                return false;
            }
            
            // Validate pattern and key
            if (recipeConfig.getPattern() == null || recipeConfig.getParsedKey() == null) {
                LOGGER.error("Missing pattern or key in shaped recipe: {}", recipeConfig.getName());
                return false;
            }
            
            // Convert pattern to array
            String[] pattern = recipeConfig.getPattern().toArray(new String[0]);
            
            // Create key map with ingredients using improved tag support
            Map<Character, Ingredient> keyMap = new HashMap<>();
            for (Map.Entry<String, RecipeConfig.IngredientEntry> entry : recipeConfig.getParsedKey().entrySet()) {
                if (entry.getKey().length() != 1) {
                    LOGGER.error("Invalid key in shaped recipe {}: {}", recipeConfig.getName(), entry.getKey());
                    continue;
                }
                
                char keyChar = entry.getKey().charAt(0);
                RecipeConfig.IngredientEntry ingredientEntry = entry.getValue();
                
                // Use the improved ingredient creation method that supports tags
                Ingredient ingredient = ingredientEntry.createIngredient();
                
                if (ingredient.isEmpty()) {
                    LOGGER.error("Failed to create ingredient in shaped recipe {}: {}", recipeConfig.getName(), ingredientEntry);
                    continue;
                }
                
                keyMap.put(keyChar, ingredient);
                LOGGER.debug("Added ingredient to shaped recipe {}: {} -> {}", 
                    recipeConfig.getName(), keyChar, 
                    ingredientEntry.isTag() ? "tag:" + ingredientEntry.getTag() : "item:" + ingredientEntry.getItem());
            }
            
            // Convert to NonNullList of Ingredients
            int width = 0;
            int height = pattern.length;
            for (String row : pattern) {
                width = Math.max(width, row.length());
            }
            
            NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
            for (int i = 0; i < pattern.length; i++) {
                String row = pattern[i];
                for (int j = 0; j < row.length(); j++) {
                    char c = row.charAt(j);
                    Ingredient ingredient = keyMap.getOrDefault(c, Ingredient.EMPTY);
                    ingredients.set(i * width + j, ingredient);
                }
            }
            
            // Create the recipe
            ShapedRecipe recipe = new ShapedRecipe(
                recipeId,
                recipeConfig.getGroup() != null ? recipeConfig.getGroup() : "",
                CraftingBookCategory.MISC,
                width,
                height,
                ingredients,
                output
            );
            
            // Register the recipe
            if (registerRecipe(server, recipe)) {
                registeredRecipeCache.put(recipeId, recipe);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            LOGGER.error("Failed to register shaped recipe {}: {}", recipeConfig.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Registers a shapeless recipe with improved tag support.
     */
    private static boolean registerShapelessRecipe(MinecraftServer server, RecipeConfig.CustomRecipe recipeConfig) {
        try {
            // Create recipe ID
            ResourceLocation recipeId = new ResourceLocation(EntityLootDrops.MOD_ID, recipeConfig.getName());
            
            // Check if recipe already exists in cache
            if (registeredRecipeCache.containsKey(recipeId)) {
                LOGGER.debug("Recipe {} already registered, skipping", recipeId);
                return true;
            }
            
            // Create output ItemStack with NBT if specified
            ItemStack output = recipeConfig.createOutputStack();
            if (output.isEmpty()) {
                LOGGER.error("Failed to create output for shapeless recipe: {}", recipeConfig.getName());
                return false;
            }
            
            // Create ingredients list using improved tag support
            if (recipeConfig.getParsedIngredients() == null || recipeConfig.getParsedIngredients().isEmpty()) {
                LOGGER.error("Missing ingredients in shapeless recipe: {}", recipeConfig.getName());
                return false;
            }
            
            // Convert ingredients to NonNullList of Ingredient objects with tag support
            NonNullList<Ingredient> ingredients = NonNullList.create();
            for (RecipeConfig.IngredientEntry ingredientEntry : recipeConfig.getParsedIngredients()) {
                // Use the improved ingredient creation method that supports tags
                Ingredient ingredient = ingredientEntry.createIngredient();
                
                if (ingredient.isEmpty()) {
                    LOGGER.error("Failed to create ingredient in shapeless recipe {}: {}", recipeConfig.getName(), ingredientEntry);
                    continue;
                }
                
                // Add multiple copies if count > 1
                for (int i = 0; i < ingredientEntry.getCount(); i++) {
                    ingredients.add(ingredient);
                }
                
                LOGGER.debug("Added ingredient to shapeless recipe {}: {} (count: {})", 
                    recipeConfig.getName(), 
                    ingredientEntry.isTag() ? "tag:" + ingredientEntry.getTag() : "item:" + ingredientEntry.getItem(),
                    ingredientEntry.getCount());
            }
            
            if (ingredients.isEmpty()) {
                LOGGER.error("No valid ingredients found for shapeless recipe: {}", recipeConfig.getName());
                return false;
            }
            
            // Create the recipe
            ShapelessRecipe recipe = new ShapelessRecipe(
                recipeId,
                recipeConfig.getGroup() != null ? recipeConfig.getGroup() : "",
                CraftingBookCategory.MISC,
                output,
                ingredients
            );
            
            // Register the recipe
            if (registerRecipe(server, recipe)) {
                registeredRecipeCache.put(recipeId, recipe);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            LOGGER.error("Failed to register shapeless recipe {}: {}", recipeConfig.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Helper method to register a recipe with the server's recipe manager using improved reflection.
     */
    private static <C extends Container, T extends Recipe<C>> boolean registerRecipe(MinecraftServer server, T recipe) {
        try {
            // Get the server's recipe manager
            net.minecraft.world.item.crafting.RecipeManager recipeManager = server.getRecipeManager();
            
            if (recipesField == null) {
                LOGGER.error("Recipes field not initialized, cannot register recipe: {}", recipe.getId());
                return false;
            }
            
            recipesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipes = 
                (Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>>) recipesField.get(recipeManager);
            
            // Get the map for this recipe type
            Map<ResourceLocation, Recipe<?>> typeRecipes = recipes.computeIfAbsent(recipe.getType(), k -> new HashMap<>());
            
            // Add the recipe
            typeRecipes.put(recipe.getId(), recipe);
            
            // Also add to the byName map if it exists
            if (byNameField != null) {
                try {
                    byNameField.setAccessible(true);
                    
                    @SuppressWarnings("unchecked")
                    Map<ResourceLocation, Recipe<?>> byName = (Map<ResourceLocation, Recipe<?>>) byNameField.get(recipeManager);
                    byName.put(recipe.getId(), recipe);
                } catch (Exception e) {
                    LOGGER.debug("Could not access byName field (this is normal for some MC versions): {}", e.getMessage());
                }
            }
            
            LOGGER.debug("Successfully registered recipe: {} (type: {})", recipe.getId(), recipe.getType());
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to register recipe {}: {}", recipe.getId(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Initializes reflection fields for recipe manager access.
     */
    private static void initializeReflectionFields(net.minecraft.world.item.crafting.RecipeManager recipeManager) {
        if (fieldsInitialized) {
            return;
        }
        
        // Try to find the recipes field
        recipesField = findRecipesField();
        
        // Try to find the byName field (optional)
        byNameField = findByNameField();
        
        fieldsInitialized = true;
        
        if (recipesField != null) {
            LOGGER.info("Successfully initialized recipe manager reflection fields");
        } else {
            LOGGER.error("Failed to initialize recipe manager reflection fields - recipe registration may not work");
        }
    }
    
    /**
     * Helper method to find the recipes field with better error handling.
     */
    private static Field findRecipesField() {
        Field field = null;
        
        // Try common field names used in different MC versions
        String[] possibleNames = {"recipes", "f_44007_", "field_199522_d"};
        
        for (String fieldName : possibleNames) {
            try {
                field = net.minecraft.world.item.crafting.RecipeManager.class.getDeclaredField(fieldName);
                LOGGER.debug("Found recipes field by name: {}", fieldName);
                break;
            } catch (NoSuchFieldException e) {
                // Continue to next name
            }
        }
        
        // If still not found, search by type
        if (field == null) {
            for (Field f : net.minecraft.world.item.crafting.RecipeManager.class.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(f.getType())) {
                    field = f;
                    LOGGER.debug("Found recipes field by type: {}", f.getName());
                    break;
                }
            }
        }
        
        if (field == null) {
            LOGGER.error("Could not find recipes field in RecipeManager");
        }
        
        return field;
    }
    
    /**
     * Helper method to find the byName field (optional).
     */
    private static Field findByNameField() {
        try {
            Field field = net.minecraft.world.item.crafting.RecipeManager.class.getDeclaredField("byName");
            LOGGER.debug("Found byName field");
            return field;
        } catch (NoSuchFieldException e) {
            LOGGER.debug("byName field not found (this is normal for some MC versions)");
            return null;
        }
    }
    
    /**
     * Clears previous recipe registrations to handle reload scenarios.
     */
    @SuppressWarnings("unchecked")
    private static void clearPreviousRegistrations(MinecraftServer server) {
        if (registeredRecipeCache.isEmpty()) {
            return; // Nothing to clear
        }
        
        try {
            net.minecraft.world.item.crafting.RecipeManager recipeManager = server.getRecipeManager();
            
            if (recipesField == null) {
                LOGGER.warn("Cannot clear previous registrations - recipes field not initialized");
                return;
            }
            
            recipesField.setAccessible(true);
            Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipes = 
                (Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>>) recipesField.get(recipeManager);
            
            int removedCount = 0;
            
            // Remove previously registered recipes
            for (ResourceLocation recipeId : registeredRecipeCache.keySet()) {
                for (Map<ResourceLocation, Recipe<?>> typeRecipes : recipes.values()) {
                    if (typeRecipes.remove(recipeId) != null) {
                        removedCount++;
                        break;
                    }
                }
                
                // Also remove from byName map if it exists
                if (byNameField != null) {
                    try {
                        byNameField.setAccessible(true);
                        Map<ResourceLocation, Recipe<?>> byName = (Map<ResourceLocation, Recipe<?>>) byNameField.get(recipeManager);
                        byName.remove(recipeId);
                    } catch (Exception e) {
                        // Ignore - byName field might not exist
                    }
                }
            }
            
            // Clear the cache
            registeredRecipeCache.clear();
            
            if (removedCount > 0) {
                LOGGER.info("Cleared {} previously registered custom recipes for reload", removedCount);
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to clear previous recipe registrations", e);
            // Clear cache anyway to prevent issues
            registeredRecipeCache.clear();
        }
    }
    
    /**
     * Reloads all recipes by clearing and re-registering them.
     */
    public static boolean reloadRecipes(MinecraftServer server) {
        LOGGER.info("Reloading custom recipes...");
        
        // Reload configuration
        RecipeConfig.reloadConfig();
        
        // Re-register recipes
        boolean success = registerRecipes(server);
        
        if (success) {
            LOGGER.info("Successfully reloaded custom recipes");
            
            // Apply recipe replacements after reload
            try {
                RecipeReplacementHandler.applyRecipeReplacements(server);
            } catch (Exception e) {
                LOGGER.error("Failed to apply recipe replacements after reload", e);
            }
        } else {
            LOGGER.warn("Some issues occurred during recipe reload");
        }
        
        return success;
    }
    
    /**
     * Gets the number of currently registered custom recipes.
     */
    public static int getRegisteredRecipeCount() {
        return registeredRecipeCache.size();
    }
    
    /**
     * Checks if recipes are currently being registered.
     */
    public static boolean isRegistering() {
        return isRegistering;
    }
    
    /**
     * Gets a copy of the registered recipe cache (for debugging).
     */
    public static Map<ResourceLocation, Recipe<?>> getRegisteredRecipes() {
        return new HashMap<>(registeredRecipeCache);
    }
    
    /**
     * Checks if a specific recipe is registered by this manager.
     */
    public static boolean isRecipeRegistered(ResourceLocation recipeId) {
        return registeredRecipeCache.containsKey(recipeId);
    }
}
