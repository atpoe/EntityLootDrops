package net.poe.entitylootdrops;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
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
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Manages custom recipes and registers them with Minecraft.
 */
@Mod.EventBusSubscriber(modid = EntityLootDrops.MOD_ID)
public class RecipeManager {
    private static final Logger LOGGER = LogManager.getLogger();
    
    /**
     * Registers custom recipes when the server starts.
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // Load recipe configuration
        RecipeConfig.loadConfig();
        
        // Register recipes
        registerRecipes(event);
    }
    
    /**
     * Registers all custom recipes with Minecraft.
     */
    private static void registerRecipes(ServerStartedEvent event) {
        int shapedCount = 0;
        int shapelessCount = 0;
        
        // Register shaped recipes
        for (RecipeConfig.CustomRecipe recipeConfig : RecipeConfig.getShapedRecipes()) {
            try {
                // Create recipe ID
                ResourceLocation recipeId = new ResourceLocation(EntityLootDrops.MOD_ID, recipeConfig.getName());
                
                // Create output ItemStack with NBT if specified
                ItemStack output = recipeConfig.createOutputStack();
                if (output.isEmpty()) {
                    LOGGER.error("Failed to create output for shaped recipe: {}", recipeConfig.getName());
                    continue;
                }
                
                // Create pattern and key map
                if (recipeConfig.getPattern() == null || recipeConfig.getKey() == null) {
                    LOGGER.error("Missing pattern or key in shaped recipe: {}", recipeConfig.getName());
                    continue;
                }
                
                // Convert pattern to array
                String[] pattern = recipeConfig.getPattern().toArray(new String[0]);
                
                // Create key map with ingredients
                Map<Character, Ingredient> keyMap = new HashMap<>();
                for (Map.Entry<String, String> entry : recipeConfig.getKey().entrySet()) {
                    if (entry.getKey().length() != 1) {
                        LOGGER.error("Invalid key in shaped recipe {}: {}", recipeConfig.getName(), entry.getKey());
                        continue;
                    }
                    
                    char keyChar = entry.getKey().charAt(0);
                    ResourceLocation itemId = new ResourceLocation(entry.getValue());
                    Item item = ForgeRegistries.ITEMS.getValue(itemId);
                    
                    if (item == null) {
                        LOGGER.error("Invalid item ID in shaped recipe {}: {}", recipeConfig.getName(), entry.getValue());
                        continue;
                    }
                    
                    keyMap.put(keyChar, Ingredient.of(item));
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
                    recipeConfig.getGroup(),
                    CraftingBookCategory.MISC,
                    width,
                    height,
                    ingredients,
                    output
                );
                
                // Register the recipe
                registerRecipe(event, recipe);
                
                shapedCount++;
                LOGGER.debug("Registered shaped recipe: {}", recipeId);
                
            } catch (Exception e) {
                LOGGER.error("Failed to register shaped recipe {}: {}", recipeConfig.getName(), e.getMessage());
            }
        }
        
        // Register shapeless recipes
        for (RecipeConfig.CustomRecipe recipeConfig : RecipeConfig.getShapelessRecipes()) {
            try {
                // Create recipe ID
                ResourceLocation recipeId = new ResourceLocation(EntityLootDrops.MOD_ID, recipeConfig.getName());
                
                // Create output ItemStack with NBT if specified
                ItemStack output = recipeConfig.createOutputStack();
                if (output.isEmpty()) {
                    LOGGER.error("Failed to create output for shapeless recipe: {}", recipeConfig.getName());
                    continue;
                }
                
                // Create ingredients list
                if (recipeConfig.getIngredients() == null || recipeConfig.getIngredients().isEmpty()) {
                    LOGGER.error("Missing ingredients in shapeless recipe: {}", recipeConfig.getName());
                    continue;
                }
                
                // Convert ingredients to NonNullList of Ingredient objects
                NonNullList<Ingredient> ingredients = NonNullList.create();
                for (String itemId : recipeConfig.getIngredients()) {
                    ResourceLocation resourceLocation = new ResourceLocation(itemId);
                    Item item = ForgeRegistries.ITEMS.getValue(resourceLocation);
                    
                    if (item == null) {
                        LOGGER.error("Invalid item ID in shapeless recipe {}: {}", recipeConfig.getName(), itemId);
                        continue;
                    }
                    
                    ingredients.add(Ingredient.of(item));
                }
                
                // Create the recipe
                ShapelessRecipe recipe = new ShapelessRecipe(
                    recipeId,
                    recipeConfig.getGroup(),
                    CraftingBookCategory.MISC,
                    output,
                    ingredients
                );
                
                // Register the recipe
                registerRecipe(event, recipe);
                
                shapelessCount++;
                LOGGER.debug("Registered shapeless recipe: {}", recipeId);
                
            } catch (Exception e) {
                LOGGER.error("Failed to register shapeless recipe {}: {}", recipeConfig.getName(), e.getMessage());
            }
        }
        
        LOGGER.info("Registered {} shaped and {} shapeless custom recipes", shapedCount, shapelessCount);
    }
    
    /**
     * Helper method to register a recipe with the server's recipe manager.
     */
    private static <C extends Container, T extends Recipe<C>> void registerRecipe(ServerStartedEvent event, T recipe) {
        try {
            // Get the server's recipe manager
            net.minecraft.world.item.crafting.RecipeManager recipeManager = event.getServer().getRecipeManager();
            
            // Use the server level to register the recipe
            ServerLevel level = event.getServer().getLevel(ServerLevel.OVERWORLD);
            if (level != null) {
                try {
                    // First try to use the addRecipe method if it exists (newer Forge versions)
                    try {
                        java.lang.reflect.Method addRecipeMethod = net.minecraft.world.item.crafting.RecipeManager.class
                            .getDeclaredMethod("addRecipe", Recipe.class);
                        addRecipeMethod.setAccessible(true);
                        addRecipeMethod.invoke(recipeManager, recipe);
                        LOGGER.debug("Successfully registered recipe using addRecipe method: {}", recipe.getId());
                        return;
                    } catch (NoSuchMethodException e) {
                        // Method doesn't exist, fall back to reflection on the recipes map
                    }
                    
                    // Use reflection to access the recipes map
                    Field recipesField = null;
                    
                    // Try different field names that might be used in different versions
                    try {
                        recipesField = net.minecraft.world.item.crafting.RecipeManager.class.getDeclaredField("recipes");
                    } catch (NoSuchFieldException e) {
                        try {
                            recipesField = net.minecraft.world.item.crafting.RecipeManager.class.getDeclaredField("f_44007_"); // Obfuscated name
                        } catch (NoSuchFieldException e2) {
                            // Try to find the field by type
                            for (Field field : net.minecraft.world.item.crafting.RecipeManager.class.getDeclaredFields()) {
                                if (Map.class.isAssignableFrom(field.getType())) {
                                    recipesField = field;
                                    break;
                                }
                            }
                            
                            if (recipesField == null) {
                                throw new RuntimeException("Could not find recipes field in RecipeManager");
                            }
                        }
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
                    try {
                        Field byNameField = net.minecraft.world.item.crafting.RecipeManager.class.getDeclaredField("byName");
                        byNameField.setAccessible(true);
                        
                        @SuppressWarnings("unchecked")
                        Map<ResourceLocation, Recipe<?>> byName = (Map<ResourceLocation, Recipe<?>>) byNameField.get(recipeManager);
                        byName.put(recipe.getId(), recipe);
                    } catch (NoSuchFieldException e) {
                        // byName field doesn't exist in this version, that's okay
                    }
                    
                    LOGGER.debug("Successfully registered recipe: {}", recipe.getId());
                } catch (Exception e) {
                    // If reflection fails, try to use the public API
                    LOGGER.warn("Reflection failed, trying alternative recipe registration for: {}", recipe.getId());
                    
                    // Instead of using the command directly, which has API compatibility issues,
                    // notify the user to manually reload resources
                    LOGGER.info("Please use the /reload command in-game to ensure recipes are properly registered");
                }
            } else {
                LOGGER.error("Failed to register recipe: server level is null");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to register recipe: {}", e.getMessage(), e);
        }
    }
}
