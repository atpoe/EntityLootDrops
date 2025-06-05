package net.poe.entitylootdrops.recipes.registration;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.poe.entitylootdrops.EntityLootDrops;
import net.poe.entitylootdrops.recipes.RecipeConfig;
import net.poe.entitylootdrops.recipes.config.RecipeConfigManager;
import net.poe.entitylootdrops.recipes.model.RecipeEntry;

@Mod.EventBusSubscriber(modid = EntityLootDrops.MOD_ID)
public class RecipeRegistrationManager {
    private static final Logger LOGGER = LogManager.getLogger();
    
    private final RecipeConfigManager configManager;
    private static RecipeRegistrationManager instance;
    
    public RecipeRegistrationManager(RecipeConfigManager configManager) {
        this.configManager = configManager;
        instance = this;
    }
    
    /**
     * Registers custom recipes when the server starts.
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // Load recipe configuration
        RecipeConfig.loadConfig();
        
        // Register recipes
        if (instance != null) {
            instance.registerRecipes(event.getServer());
        }
    }
    
    /**
     * Registers all custom recipes with Minecraft.
     * This can be called during server runtime to register new recipes without a restart.
     * 
     * @param server The Minecraft server instance
     * @return true if registration was successful, false if there were issues
     */
    public boolean registerRecipes(MinecraftServer server) {
        if (server == null) {
            LOGGER.error("Server is null, cannot register recipes");
            return false;
        }
        
        try {
            List<RecipeEntry> recipes = configManager.getEnabledRecipes();
            
            if (recipes.isEmpty()) {
                LOGGER.info("No enabled recipes to register");
                return true;
            }
            
            int registeredCount = 0;
            int replacedCount = 0;
            
            // Handle recipe replacements first
            for (RecipeEntry entry : recipes) {
                if (entry.getReplaceRecipe() != null && !entry.getReplaceRecipe().trim().isEmpty()) {
                    try {
                        ResourceLocation replaceId = new ResourceLocation(entry.getReplaceRecipe());
                        if (removeRecipe(server, replaceId)) {
                            replacedCount++;
                            LOGGER.info("Successfully removed original recipe: {}", replaceId);
                        } else {
                            LOGGER.warn("Failed to remove original recipe: {} (recipe may not exist)", replaceId);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to remove recipe for replacement: {}", entry.getReplaceRecipe(), e);
                    }
                }
            }
            
            // Register all custom recipes
            for (RecipeEntry entry : recipes) {
                try {
                    Recipe<?> minecraftRecipe = convertToMinecraftRecipe(entry);
                    if (minecraftRecipe != null) {
                        if (registerRecipe(server, minecraftRecipe)) {
                            registeredCount++;
                            LOGGER.debug("Registered recipe: {} (type: {})", minecraftRecipe.getId(), entry.getType());
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to register recipe {}: {}", entry.getName(), e.getMessage());
                }
            }
            
            LOGGER.info("Registered {} custom recipes, replaced {} existing recipes", registeredCount, replacedCount);
            return registeredCount > 0;
            
        } catch (Exception e) {
            LOGGER.error("Failed to register recipes: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Helper method to register a recipe with the server's recipe manager.
     * Adapted from the old working RecipeManager.
     */
    private static <C extends Container, T extends Recipe<C>> boolean registerRecipe(MinecraftServer server, T recipe) {
        try {
            // Get the server's recipe manager
            net.minecraft.world.item.crafting.RecipeManager recipeManager = server.getRecipeManager();
            
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
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to register recipe: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Helper method to remove a recipe from the server's recipe manager.
     */
    private static boolean removeRecipe(MinecraftServer server, ResourceLocation recipeId) {
        try {
            net.minecraft.world.item.crafting.RecipeManager recipeManager = server.getRecipeManager();
            
            Field recipesField = null;
            
            // Try different field names
            try {
                recipesField = net.minecraft.world.item.crafting.RecipeManager.class.getDeclaredField("recipes");
            } catch (NoSuchFieldException e) {
                try {
                    recipesField = net.minecraft.world.item.crafting.RecipeManager.class.getDeclaredField("f_44007_");
                } catch (NoSuchFieldException e2) {
                    for (Field field : net.minecraft.world.item.crafting.RecipeManager.class.getDeclaredFields()) {
                        if (Map.class.isAssignableFrom(field.getType())) {
                            recipesField = field;
                            break;
                        }
                    }
                }
            }
            
            if (recipesField == null) {
                LOGGER.error("Could not find recipes field in RecipeManager");
                return false;
            }
            
            recipesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipes = 
                (Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>>) recipesField.get(recipeManager);
            
            // Remove from all recipe type maps
            boolean removed = false;
            for (Map<ResourceLocation, Recipe<?>> typeRecipes : recipes.values()) {
                if (typeRecipes.remove(recipeId) != null) {
                    removed = true;
                    LOGGER.debug("Removed recipe {} from type map", recipeId);
                }
            }
            
            // Also remove from byName map if it exists
            try {
                Field byNameField = net.minecraft.world.item.crafting.RecipeManager.class.getDeclaredField("byName");
                byNameField.setAccessible(true);
                
                @SuppressWarnings("unchecked")
                Map<ResourceLocation, Recipe<?>> byName = (Map<ResourceLocation, Recipe<?>>) byNameField.get(recipeManager);
                if (byName.remove(recipeId) != null) {
                    removed = true;
                    LOGGER.debug("Removed recipe {} from byName map", recipeId);
                }
            } catch (NoSuchFieldException e) {
                // byName field doesn't exist in this version, that's okay
                LOGGER.debug("byName field not found, skipping");
            }
            
            if (removed) {
                LOGGER.info("Successfully removed recipe: {}", recipeId);
            } else {
                LOGGER.warn("Recipe not found for removal: {}", recipeId);
            }
            
            return removed;
        } catch (Exception e) {
            LOGGER.error("Failed to remove recipe {}: {}", recipeId, e.getMessage(), e);
            return false;
        }
    }
    
    private Recipe<?> convertToMinecraftRecipe(RecipeEntry entry) {
        if (!entry.isEnabled()) {
            return null;
        }
        
        ResourceLocation recipeId = new ResourceLocation(EntityLootDrops.MOD_ID, entry.getName());
        
        switch (entry.getType().toLowerCase()) {
            case "shaped":
            case "shapeless":
                return createCraftingRecipe(entry, recipeId);
            case "furnace":
                return createSmeltingRecipe(entry, recipeId);
            case "blasting":
                return createBlastingRecipe(entry, recipeId);
            case "smoking":
                return createSmokingRecipe(entry, recipeId);
            case "campfire":
                return createCampfireRecipe(entry, recipeId);
            case "stonecutting":
                return createStonecuttingRecipe(entry, recipeId);
            case "smithing":
                return createSmithingRecipe(entry, recipeId);
            default:
                LOGGER.warn("Unknown recipe type: {} for recipe: {}", entry.getType(), entry.getName());
                return null;
        }
    }
    
    private Recipe<?> createCraftingRecipe(RecipeEntry entry, ResourceLocation recipeId) {
        if (entry.getType().equals("shaped") && entry.getPattern() != null && entry.getKey() != null) {
            return createShapedRecipe(entry, recipeId);
        } else if (entry.getType().equals("shapeless") && entry.getIngredients() != null) {
            return createShapelessRecipe(entry, recipeId);
        }
        return null;
    }
    
    private ShapedRecipe createShapedRecipe(RecipeEntry entry, ResourceLocation recipeId) {
        List<String> pattern = entry.getPattern();
        Map<String, Object> key = entry.getKey(); // Changed to Object to support both formats
        
        // Convert pattern to array (like the old manager)
        String[] patternArray = pattern.toArray(new String[0]);
        
        // Create key map with ingredients - now handles both string and object formats
        Map<Character, Ingredient> keyMap = new HashMap<>();
        for (Map.Entry<String, Object> keyEntry : key.entrySet()) { // Changed to Object
            if (keyEntry.getKey().length() != 1) {
                LOGGER.error("Invalid key in shaped recipe {}: {}", entry.getName(), keyEntry.getKey());
                continue;
            }
            
            char keyChar = keyEntry.getKey().charAt(0);
            Ingredient ingredient = entry.createIngredient(keyEntry.getValue()); // Now uses Object
            
            if (ingredient == Ingredient.EMPTY) {
                LOGGER.error("Invalid ingredient in shaped recipe {}: {}", entry.getName(), keyEntry.getValue());
                continue;
            }
            
            keyMap.put(keyChar, ingredient);
        }
        
        // Convert to NonNullList of Ingredients (like the old manager)
        int width = 0;
        int height = patternArray.length;
        for (String row : patternArray) {
            width = Math.max(width, row.length());
        }
        
        NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
        for (int i = 0; i < patternArray.length; i++) {
            String row = patternArray[i];
            for (int j = 0; j < row.length(); j++) {
                char c = row.charAt(j);
                Ingredient ingredient = keyMap.getOrDefault(c, Ingredient.EMPTY);
                ingredients.set(i * width + j, ingredient);
            }
        }
        
        return new ShapedRecipe(recipeId, entry.getGroup(), CraftingBookCategory.MISC, width, height, ingredients, entry.createOutputStack());
    }
    
    private ShapelessRecipe createShapelessRecipe(RecipeEntry entry, ResourceLocation recipeId) {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (Object ingredientData : entry.getIngredients()) { // Changed to Object to support both formats
            Ingredient ingredient = entry.createIngredient(ingredientData); // Now uses Object
            if (ingredient == Ingredient.EMPTY) {
                LOGGER.error("Invalid ingredient in shapeless recipe {}: {}", entry.getName(), ingredientData);
                continue;
            }
            ingredients.add(ingredient);
        }
        return new ShapelessRecipe(recipeId, entry.getGroup(), CraftingBookCategory.MISC, entry.createOutputStack(), ingredients);
    }
    
    private SmeltingRecipe createSmeltingRecipe(RecipeEntry entry, ResourceLocation recipeId) {
        return new SmeltingRecipe(recipeId, entry.getGroup(), CookingBookCategory.MISC, entry.createIngredient(entry.getInput()), 
            entry.createOutputStack(), entry.getExperience(), entry.getCookingTime());
    }
    
    private BlastingRecipe createBlastingRecipe(RecipeEntry entry, ResourceLocation recipeId) {
        return new BlastingRecipe(recipeId, entry.getGroup(), CookingBookCategory.MISC, entry.createIngredient(entry.getInput()), 
            entry.createOutputStack(), entry.getExperience(), entry.getCookingTime());
    }
    
    private SmokingRecipe createSmokingRecipe(RecipeEntry entry, ResourceLocation recipeId) {
        return new SmokingRecipe(recipeId, entry.getGroup(), CookingBookCategory.MISC, entry.createIngredient(entry.getInput()), 
            entry.createOutputStack(), entry.getExperience(), entry.getCookingTime());
    }
    
    private CampfireCookingRecipe createCampfireRecipe(RecipeEntry entry, ResourceLocation recipeId) {
        return new CampfireCookingRecipe(recipeId, entry.getGroup(), CookingBookCategory.MISC, entry.createIngredient(entry.getInput()), 
            entry.createOutputStack(), entry.getExperience(), entry.getCookingTime());
    }
    
    private StonecutterRecipe createStonecuttingRecipe(RecipeEntry entry, ResourceLocation recipeId) {
        return new StonecutterRecipe(recipeId, entry.getGroup(), entry.createIngredient(entry.getInput()), entry.createOutputStack());
    }
    
    private SmithingTransformRecipe createSmithingRecipe(RecipeEntry entry, ResourceLocation recipeId) {
        return new SmithingTransformRecipe(recipeId, entry.createIngredient(entry.getTemplate()), 
            entry.createIngredient(entry.getBase()), entry.createIngredient(entry.getAddition()), entry.createOutputStack());
    }
    
    public static RecipeRegistrationManager getInstance() {
        return instance;
    }
}
