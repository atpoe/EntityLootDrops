package net.poe.entitylootdrops;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecipeReplacementHandler {
   private static final Logger LOGGER = LoggerFactory.getLogger(RecipeReplacementHandler.class);
   private static Field recipesField = null;
   private static Field byNameField = null;
   private static boolean fieldsInitialized = false;
   private static final Set<ResourceLocation> removedRecipeCache = ConcurrentHashMap.newKeySet();
   private static final Set<ResourceLocation> addedRecipeCache = ConcurrentHashMap.newKeySet();

   public static void applyRecipeReplacements(MinecraftServer server) {
      try {
         net.minecraft.world.item.crafting.RecipeManager recipeManager = server.getRecipeManager();
         if (RecipeConfig.getTotalRecipeCount() == 0) {
            LOGGER.warn("No recipes loaded, skipping recipe replacements");
            return;
         }

         if (RecipeConfig.isReloading()) {
            LOGGER.info("Recipes are currently reloading, deferring recipe replacements");
            return;
         }

         clearCaches();
         processRecipeReplacements(recipeManager);
      } catch (Exception var2) {
         LOGGER.error("Failed to apply recipe replacements", var2);
      }

   }

   public static void processRecipeReplacements(net.minecraft.world.item.crafting.RecipeManager recipeManager) {
      Set<ResourceLocation> removedRecipes = new HashSet();
      Set<ResourceLocation> addedRecipes = new HashSet();
      if (!fieldsInitialized) {
         initializeReflectionFields(recipeManager);
      }

      if (recipesField == null) {
         LOGGER.error("Cannot process recipe replacements - reflection fields not initialized");
      } else {
         List<RecipeConfig.CustomRecipe> customRecipes = new ArrayList();
         customRecipes.addAll(RecipeConfig.getShapedRecipes());
         customRecipes.addAll(RecipeConfig.getShapelessRecipes());
         if (!customRecipes.isEmpty()) {
            LOGGER.info("Processing {} custom recipe replacements", customRecipes.size());
            Iterator var4 = customRecipes.iterator();

            while(var4.hasNext()) {
               RecipeConfig.CustomRecipe replacement = (RecipeConfig.CustomRecipe)var4.next();
               processCustomRecipeReplacement(recipeManager, replacement, removedRecipes, addedRecipes);
            }
         }

         List<RecipeConfig.BrewingRecipe> brewingRecipes = RecipeConfig.getBrewingRecipes();
         if (!brewingRecipes.isEmpty()) {
            LOGGER.info("Processing {} brewing recipe replacements", brewingRecipes.size());
            Iterator var10 = brewingRecipes.iterator();

            while(var10.hasNext()) {
               RecipeConfig.BrewingRecipe replacement = (RecipeConfig.BrewingRecipe)var10.next();
               processBrewingRecipeReplacement(recipeManager, replacement, removedRecipes, addedRecipes);
            }
         }

         List<RecipeConfig.FurnaceRecipe> furnaceRecipes = RecipeConfig.getFurnaceRecipes();
         if (!furnaceRecipes.isEmpty()) {
            LOGGER.info("Processing {} furnace recipe replacements", furnaceRecipes.size());
            Iterator var12 = furnaceRecipes.iterator();

            while(var12.hasNext()) {
               RecipeConfig.FurnaceRecipe replacement = (RecipeConfig.FurnaceRecipe)var12.next();
               processFurnaceRecipeReplacement(recipeManager, replacement, removedRecipes, addedRecipes);
            }
         }

         List<RecipeConfig.SmithingRecipe> smithingRecipes = RecipeConfig.getSmithingRecipes();
         if (!smithingRecipes.isEmpty()) {
            LOGGER.info("Processing {} smithing recipe replacements", smithingRecipes.size());
            Iterator var14 = smithingRecipes.iterator();

            while(var14.hasNext()) {
               RecipeConfig.SmithingRecipe replacement = (RecipeConfig.SmithingRecipe)var14.next();
               processSmithingRecipeReplacement(recipeManager, replacement, removedRecipes, addedRecipes);
            }
         }

         removedRecipeCache.addAll(removedRecipes);
         addedRecipeCache.addAll(addedRecipes);
         LOGGER.info("Recipe replacement complete. Removed: {}, Added: {}", removedRecipes.size(), addedRecipes.size());
         if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Removed recipes: {}", removedRecipes);
            LOGGER.debug("Added recipes: {}", addedRecipes);
         }

      }
   }

   private static void processCustomRecipeReplacement(net.minecraft.world.item.crafting.RecipeManager recipeManager, RecipeConfig.CustomRecipe replacement, Set<ResourceLocation> removedRecipes, Set<ResourceLocation> addedRecipes) {
      try {
         if (!replacement.isEnabled()) {
            LOGGER.debug("Skipping disabled replacement recipe: {}", replacement.getName());
            return;
         }

         ResourceLocation recipeToReplace = new ResourceLocation(replacement.getRecipeToReplace());
         Collection<Recipe<?>> allRecipes = getAllRecipes(recipeManager);
         Recipe<?> existingRecipe = null;
         Iterator var7 = allRecipes.iterator();

         while(var7.hasNext()) {
            Recipe<?> recipe = (Recipe)var7.next();
            if (recipe.getId().equals(recipeToReplace)) {
               existingRecipe = recipe;
               break;
            }
         }

         if (existingRecipe == null && replacement.isReplacement()) {
            LOGGER.warn("Recipe to replace not found: {}. Skipping replacement.", recipeToReplace);
            allRecipes.stream().filter((r) -> {
               return r.getId().getPath().equals(recipeToReplace.getPath());
            }).limit(5L).forEach((r) -> {
               LOGGER.debug("Available recipe in namespace: {}", r.getId());
            });
            return;
         }

         if (replacement.isRemoveOriginal() && existingRecipe != null) {
            if (removeRecipeUsingReflection(recipeManager, recipeToReplace)) {
               LOGGER.info("Successfully removed original recipe: {}", recipeToReplace);
               removedRecipes.add(recipeToReplace);
            } else {
               LOGGER.warn("Failed to remove original recipe: {}", recipeToReplace);
            }
         }

         if (registerCustomReplacementRecipe(recipeManager, replacement)) {
            ResourceLocation newRecipeId = new ResourceLocation("entitylootdrops", replacement.getName());
            addedRecipes.add(newRecipeId);
            LOGGER.info("Successfully registered replacement recipe: {} -> {}", replacement.getRecipeToReplace(), replacement.getName());
         } else {
            LOGGER.error("Failed to register replacement recipe: {}", replacement.getName());
         }
      } catch (Exception var9) {
         LOGGER.error("Failed to process replacement recipe: {}", replacement.getName(), var9);
      }

   }

   private static void processBrewingRecipeReplacement(net.minecraft.world.item.crafting.RecipeManager recipeManager, RecipeConfig.BrewingRecipe replacement, Set<ResourceLocation> removedRecipes, Set<ResourceLocation> addedRecipes) {
      try {
         if (!replacement.isEnabled()) {
            LOGGER.debug("Skipping disabled brewing replacement recipe: {}", replacement.getName());
            return;
         }

         ResourceLocation recipeToReplace = new ResourceLocation(replacement.getRecipeToReplace());
         if (replacement.isRemoveOriginal()) {
            if (removeRecipeUsingReflection(recipeManager, recipeToReplace)) {
               LOGGER.info("Successfully removed brewing recipe: {}", recipeToReplace);
               removedRecipes.add(recipeToReplace);
            } else {
               LOGGER.warn("Failed to remove brewing recipe: {}", recipeToReplace);
            }
         }

         LOGGER.info("Brewing recipe replacement configured: {} -> {}", replacement.getRecipeToReplace(), replacement.getName());
      } catch (Exception var5) {
         LOGGER.error("Failed to process brewing replacement recipe: {}", replacement.getName(), var5);
      }

   }

   private static void processFurnaceRecipeReplacement(net.minecraft.world.item.crafting.RecipeManager recipeManager, RecipeConfig.FurnaceRecipe replacement, Set<ResourceLocation> removedRecipes, Set<ResourceLocation> addedRecipes) {
      try {
         if (!replacement.isEnabled()) {
            LOGGER.debug("Skipping disabled furnace replacement recipe: {}", replacement.getName());
            return;
         }

         ResourceLocation recipeToReplace = new ResourceLocation(replacement.getRecipeToReplace());
         if (replacement.isRemoveOriginal()) {
            if (removeRecipeUsingReflection(recipeManager, recipeToReplace)) {
               LOGGER.info("Successfully removed furnace recipe: {}", recipeToReplace);
               removedRecipes.add(recipeToReplace);
            } else {
               LOGGER.warn("Failed to remove furnace recipe: {}", recipeToReplace);
            }
         }

         LOGGER.info("Furnace recipe replacement configured: {} -> {}", replacement.getRecipeToReplace(), replacement.getName());
      } catch (Exception var5) {
         LOGGER.error("Failed to process furnace replacement recipe: {}", replacement.getName(), var5);
      }

   }

   private static void processSmithingRecipeReplacement(net.minecraft.world.item.crafting.RecipeManager recipeManager, RecipeConfig.SmithingRecipe replacement, Set<ResourceLocation> removedRecipes, Set<ResourceLocation> addedRecipes) {
      try {
         if (!replacement.isEnabled()) {
            LOGGER.debug("Skipping disabled smithing replacement recipe: {}", replacement.getName());
            return;
         }

         ResourceLocation recipeToReplace = new ResourceLocation(replacement.getRecipeToReplace());
         if (replacement.isRemoveOriginal()) {
            if (removeRecipeUsingReflection(recipeManager, recipeToReplace)) {
               LOGGER.info("Successfully removed smithing recipe: {}", recipeToReplace);
               removedRecipes.add(recipeToReplace);
            } else {
               LOGGER.warn("Failed to remove smithing recipe: {}", recipeToReplace);
            }
         }

         LOGGER.info("Smithing recipe replacement configured: {} -> {}", replacement.getRecipeToReplace(), replacement.getName());
      } catch (Exception var5) {
         LOGGER.error("Failed to process smithing replacement recipe: {}", replacement.getName(), var5);
      }

   }

   public static Collection<Recipe<?>> getAllRecipes(net.minecraft.world.item.crafting.RecipeManager recipeManager) {
      try {
         if (recipesField == null) {
            LOGGER.warn("Recipes field not initialized, returning empty collection");
            return Collections.emptyList();
         } else {
            recipesField.setAccessible(true);
            Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipes = (Map)recipesField.get(recipeManager);
            if (recipes == null) {
               LOGGER.warn("Recipes map is null, returning empty collection");
               return Collections.emptyList();
            } else {
               List<Recipe<?>> allRecipes = new ArrayList();
               Iterator var3 = recipes.values().iterator();

               while(var3.hasNext()) {
                  Map<ResourceLocation, Recipe<?>> typeRecipes = (Map)var3.next();
                  if (typeRecipes != null) {
                     allRecipes.addAll(typeRecipes.values());
                  }
               }

               LOGGER.debug("Retrieved {} total recipes from recipe manager", allRecipes.size());
               return allRecipes;
            }
         }
      } catch (Exception var5) {
         LOGGER.error("Failed to get all recipes using reflection", var5);
         return Collections.emptyList();
      }
   }

   private static boolean registerCustomReplacementRecipe(net.minecraft.world.item.crafting.RecipeManager recipeManager, RecipeConfig.CustomRecipe replacement) {
      try {
         ResourceLocation recipeId = new ResourceLocation("entitylootdrops", replacement.getName());
         Collection<Recipe<?>> existingRecipes = getAllRecipes(recipeManager);
         boolean recipeExists = existingRecipes.stream().anyMatch((recipex) -> {
            return recipex.getId().equals(recipeId);
         });
         if (recipeExists) {
            LOGGER.warn("Recipe {} already exists, skipping registration", recipeId);
            return false;
         } else {
            ItemStack output = replacement.createOutputStack();
            if (output.isEmpty()) {
               LOGGER.error("Failed to create output for replacement recipe: {}", replacement.getName());
               return false;
            } else {
               Recipe<?> recipe = null;
               if (replacement.isShaped()) {
                  recipe = createShapedRecipe(replacement, recipeId, output);
               } else if (replacement.isShapeless()) {
                  recipe = createShapelessRecipe(replacement, recipeId, output);
               }

               if (recipe == null) {
                  LOGGER.error("Failed to create replacement recipe: {}", replacement.getName());
                  return false;
               } else {
                  boolean success = addRecipeUsingReflection(recipeManager, recipe);
                  if (success) {
                     LOGGER.debug("Successfully created and registered recipe: {} (Type: {})", recipeId, recipe.getClass().getSimpleName());
                  }

                  return success;
               }
            }
         }
      } catch (Exception var8) {
         LOGGER.error("Failed to register replacement recipe {}: {}", new Object[]{replacement.getName(), var8.getMessage(), var8});
         return false;
      }
   }

   private static Recipe<?> createShapedRecipe(RecipeConfig.CustomRecipe replacement, ResourceLocation recipeId, ItemStack output) {
      List<String> pattern = replacement.getPattern();
      Map<String, RecipeConfig.IngredientEntry> parsedKey = replacement.getParsedKey();
      if (pattern != null && !pattern.isEmpty()) {
         if (parsedKey != null && !parsedKey.isEmpty()) {
            String[] patternArray = (String[])pattern.toArray(new String[0]);
            Map<Character, Ingredient> keyMap = new HashMap();
            Iterator var7 = parsedKey.entrySet().iterator();

            while(true) {
               while(var7.hasNext()) {
                  Entry<String, RecipeConfig.IngredientEntry> entry = (Entry)var7.next();
                  if (((String)entry.getKey()).length() != 1) {
                     LOGGER.error("Invalid key in shaped replacement recipe {}: {}", replacement.getName(), entry.getKey());
                  } else {
                     char keyChar = ((String)entry.getKey()).charAt(0);
                     Ingredient ingredient = ((RecipeConfig.IngredientEntry)entry.getValue()).createIngredient();
                     if (ingredient != null && !ingredient.isEmpty()) {
                        keyMap.put(keyChar, ingredient);
                     } else {
                        LOGGER.error("Failed to create ingredient in shaped replacement recipe {}: {}", replacement.getName(), entry.getValue());
                     }
                  }
               }

               if (keyMap.isEmpty()) {
                  LOGGER.error("No valid ingredients found for shaped replacement recipe: {}", replacement.getName());
                  return null;
               }

               int width = 0;
               int height = patternArray.length;
               String[] var17 = patternArray;
               int i = patternArray.length;

               for(int var11 = 0; var11 < i; ++var11) {
                  String row = var17[var11];
                  width = Math.max(width, row.length());
               }

               if (width != 0 && height != 0) {
                  NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);

                  for(i = 0; i < patternArray.length; ++i) {
                     String row = patternArray[i];

                     for(int j = 0; j < row.length(); ++j) {
                        char c = row.charAt(j);
                        if (c != ' ') {
                           Ingredient ingredient = (Ingredient)keyMap.getOrDefault(c, Ingredient.EMPTY);
                           ingredients.set(i * width + j, ingredient);
                        }
                     }
                  }

                  return new ShapedRecipe(recipeId, replacement.getGroup() != null ? replacement.getGroup() : "", CraftingBookCategory.MISC, width, height, ingredients, output);
               }

               LOGGER.error("Invalid pattern dimensions for shaped replacement recipe: {} ({}x{})", new Object[]{replacement.getName(), width, height});
               return null;
            }
         } else {
            LOGGER.error("Missing or empty key in shaped replacement recipe: {}", replacement.getName());
            return null;
         }
      } else {
         LOGGER.error("Missing or empty pattern in shaped replacement recipe: {}", replacement.getName());
         return null;
      }
   }

   private static Recipe<?> createShapelessRecipe(RecipeConfig.CustomRecipe replacement, ResourceLocation recipeId, ItemStack output) {
      List<RecipeConfig.IngredientEntry> parsedIngredients = replacement.getParsedIngredients();
      if (parsedIngredients != null && !parsedIngredients.isEmpty()) {
         NonNullList<Ingredient> ingredients = NonNullList.create();
         Iterator var5 = parsedIngredients.iterator();

         while(true) {
            while(var5.hasNext()) {
               RecipeConfig.IngredientEntry ingredientEntry = (RecipeConfig.IngredientEntry)var5.next();
               Ingredient ingredient = ingredientEntry.createIngredient();
               if (ingredient != null && !ingredient.isEmpty()) {
                  int count = Math.max(1, ingredientEntry.getCount());

                  for(int i = 0; i < count; ++i) {
                     ingredients.add(ingredient);
                  }
               } else {
                  LOGGER.error("Failed to create ingredient in shapeless replacement recipe {}: {}", replacement.getName(), ingredientEntry);
               }
            }

            if (ingredients.isEmpty()) {
               LOGGER.error("No valid ingredients found for shapeless replacement recipe: {}", replacement.getName());
               return null;
            }

            return new ShapelessRecipe(recipeId, replacement.getGroup() != null ? replacement.getGroup() : "", CraftingBookCategory.MISC, output, ingredients);
         }
      } else {
         LOGGER.error("Missing ingredients in shapeless replacement recipe: {}", replacement.getName());
         return null;
      }
   }

   private static boolean removeRecipeUsingReflection(net.minecraft.world.item.crafting.RecipeManager recipeManager, ResourceLocation recipeId) {
      try {
         if (recipesField == null) {
            LOGGER.error("Recipes field not initialized for recipe removal");
            return false;
         } else {
            recipesField.setAccessible(true);
            Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipes = (Map)recipesField.get(recipeManager);
            if (recipes == null) {
               LOGGER.error("Recipes map is null, cannot remove recipe: {}", recipeId);
               return false;
            } else {
               boolean removed = false;
               Recipe<?> removedRecipe = null;
               LOGGER.debug("Checking {} recipe types for recipe: {}", recipes.size(), recipeId);
               Iterator var5 = recipes.entrySet().iterator();

               Entry entry;
               Map typeRecipes;
               while(var5.hasNext()) {
                  entry = (Entry)var5.next();
                  typeRecipes = (Map)entry.getValue();
                  if (typeRecipes != null) {
                     if (typeRecipes.containsKey(recipeId)) {
                        LOGGER.debug("Found recipe {} in type {}", recipeId, entry.getKey());
                        removedRecipe = (Recipe)typeRecipes.remove(recipeId);
                        removed = true;
                        LOGGER.debug("Removed recipe {} from type {} map", recipeId, entry.getKey());
                     } else {
                        LOGGER.debug("Recipe {} not found in type {}", recipeId, entry.getKey());
                     }
                  }
               }

               if (byNameField != null) {
                  try {
                     byNameField.setAccessible(true);
                     Map<ResourceLocation, Recipe<?>> byName = (Map)byNameField.get(recipeManager);
                     if (byName != null && byName.containsKey(recipeId)) {
                        byName.remove(recipeId);
                        LOGGER.debug("Removed recipe {} from byName map", recipeId);
                     }
                  } catch (Exception var10) {
                     LOGGER.debug("Could not access byName field: {}", var10.getMessage());
                  }
               }

               if (removed && removedRecipe != null) {
                  LOGGER.info("Successfully removed recipe: {} (Type: {})", recipeId, removedRecipe.getClass().getSimpleName());
                  return removed;
               } else {
                  LOGGER.warn("Recipe {} was not found in any recipe maps", recipeId);
                  LOGGER.debug("Available recipes:");
                  var5 = recipes.entrySet().iterator();

                  while(true) {
                     do {
                        if (!var5.hasNext()) {
                           return removed;
                        }

                        entry = (Entry)var5.next();
                        typeRecipes = (Map)entry.getValue();
                     } while(typeRecipes == null);

                     Iterator var8 = typeRecipes.keySet().iterator();

                     while(var8.hasNext()) {
                        ResourceLocation id = (ResourceLocation)var8.next();
                        if (id.getPath().equals(recipeId.getPath())) {
                           LOGGER.debug("  - {} (type: {})", id, entry.getKey());
                        }
                     }
                  }
               }
            }
         }
      } catch (Exception var11) {
         LOGGER.error("Failed to remove recipe using reflection: {}", var11.getMessage(), var11);
         return false;
      }
   }

   private static boolean addRecipeUsingReflection(net.minecraft.world.item.crafting.RecipeManager recipeManager, Recipe<?> recipe) {
      try {
         if (recipesField == null) {
            LOGGER.error("Recipes field not initialized for recipe addition");
            return false;
         } else {
            recipesField.setAccessible(true);
            Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipes = (Map)recipesField.get(recipeManager);
            if (recipes == null) {
               LOGGER.error("Recipes map is null, cannot add recipe: {}", recipe.getId());
               return false;
            } else {
               RecipeType<?> recipeType = recipe.getType();
               Map<ResourceLocation, Recipe<?>> typeRecipes = (Map)recipes.computeIfAbsent(recipeType, (k) -> {
                  return new HashMap();
               });
               if (typeRecipes.containsKey(recipe.getId())) {
                  LOGGER.warn("Recipe {} already exists in type {} map, replacing", recipe.getId(), recipeType);
               }

               typeRecipes.put(recipe.getId(), recipe);
               if (byNameField != null) {
                  try {
                     byNameField.setAccessible(true);
                     Map<ResourceLocation, Recipe<?>> byName = (Map)byNameField.get(recipeManager);
                     if (byName != null) {
                        byName.put(recipe.getId(), recipe);
                     }
                  } catch (Exception var6) {
                     LOGGER.debug("Could not access byName field: {}", var6.getMessage());
                  }
               }

               LOGGER.debug("Added recipe {} to type {} map", recipe.getId(), recipeType);
               return true;
            }
         }
      } catch (Exception var7) {
         LOGGER.error("Failed to add recipe using reflection: {}", var7.getMessage(), var7);
         return false;
      }
   }

   private static void initializeReflectionFields(net.minecraft.world.item.crafting.RecipeManager recipeManager) {
      if (!fieldsInitialized) {
         LOGGER.info("Initializing recipe reflection fields...");
         recipesField = findRecipesField(recipeManager);
         byNameField = findByNameField(recipeManager);
         fieldsInitialized = true;
         if (recipesField != null) {
            LOGGER.info("Successfully initialized recipe reflection fields");
         } else {
            LOGGER.error("Failed to initialize recipe reflection fields - recipe replacement may not work");
         }

      }
   }

   private static Field findRecipesField(net.minecraft.world.item.crafting.RecipeManager recipeManager) {
      Field field = null;
      String[] possibleNames = new String[]{"recipes", "f_44007_", "field_199522_d", "f_135_"};
      String[] var3 = possibleNames;
      int var4 = possibleNames.length;
      int var5 = 0;

      while(var5 < var4) {
         String fieldName = var3[var5];

         try {
            field = net.minecraft.world.item.crafting.RecipeManager.class.getDeclaredField(fieldName);
            LOGGER.debug("Found recipes field by name: {}", fieldName);
            break;
         } catch (NoSuchFieldException var9) {
            ++var5;
         }
      }

      Field[] var10;
      Field f;
      if (field == null) {
         LOGGER.debug("Searching for recipes field by type...");
         var10 = net.minecraft.world.item.crafting.RecipeManager.class.getDeclaredFields();
         var4 = var10.length;

         for(var5 = 0; var5 < var4; ++var5) {
            f = var10[var5];
            if (Map.class.isAssignableFrom(f.getType())) {
               try {
                  f.setAccessible(true);
                  Object fieldValue = f.get(recipeManager);
                  if (fieldValue instanceof Map) {
                     field = f;
                     LOGGER.debug("Found recipes field by type: {}", f.getName());
                     break;
                  }
               } catch (Exception var8) {
               }
            }
         }
      }

      if (field == null) {
         LOGGER.error("Could not find recipes field in RecipeManager");
         LOGGER.debug("Available fields:");
         var10 = net.minecraft.world.item.crafting.RecipeManager.class.getDeclaredFields();
         var4 = var10.length;

         for(var5 = 0; var5 < var4; ++var5) {
            f = var10[var5];
            LOGGER.debug("  - {} ({})", f.getName(), f.getType().getSimpleName());
         }
      }

      return field;
   }

   private static Field findByNameField(net.minecraft.world.item.crafting.RecipeManager recipeManager) {
      String[] possibleNames = new String[]{"byName", "f_44008_", "field_199523_e"};
      String[] var2 = possibleNames;
      int var3 = possibleNames.length;
      int var4 = 0;

      while(var4 < var3) {
         String fieldName = var2[var4];

         try {
            Field field = net.minecraft.world.item.crafting.RecipeManager.class.getDeclaredField(fieldName);
            LOGGER.debug("Found byName field: {}", fieldName);
            return field;
         } catch (NoSuchFieldException var7) {
            ++var4;
         }
      }

      LOGGER.debug("byName field not found (this is normal for some MC versions)");
      return null;
   }

   public static void clearCaches() {
      removedRecipeCache.clear();
      addedRecipeCache.clear();
      LOGGER.debug("Cleared recipe caches");
   }

   public static void clearRemovedRecipeCache() {
      removedRecipeCache.clear();
      LOGGER.debug("Cleared removed recipe cache");
   }

   public static Set<ResourceLocation> getRemovedRecipes() {
      return new HashSet(removedRecipeCache);
   }

   public static Set<ResourceLocation> getAddedRecipes() {
      return new HashSet(addedRecipeCache);
   }

   public static boolean isRecipeRemoved(ResourceLocation recipeId) {
      return removedRecipeCache.contains(recipeId);
   }

   public static boolean isRecipeAdded(ResourceLocation recipeId) {
      return addedRecipeCache.contains(recipeId);
   }

   public static void logDebugInfo() {
      LOGGER.info("=== Recipe Replacement Handler Debug Info ===");
      LOGGER.info("Fields initialized: {}", fieldsInitialized);
      LOGGER.info("Recipes field available: {}", recipesField != null);
      LOGGER.info("ByName field available: {}", byNameField != null);
      LOGGER.info("Removed recipes count: {}", removedRecipeCache.size());
      LOGGER.info("Added recipes count: {}", addedRecipeCache.size());
      if (!removedRecipeCache.isEmpty()) {
         LOGGER.info("Removed recipes: {}", removedRecipeCache);
      }

      if (!addedRecipeCache.isEmpty()) {
         LOGGER.info("Added recipes: {}", addedRecipeCache);
      }

      if (recipesField != null) {
         LOGGER.info("Recipes field name: {}", recipesField.getName());
         LOGGER.info("Recipes field type: {}", recipesField.getType().getSimpleName());
      }

      if (byNameField != null) {
         LOGGER.info("ByName field name: {}", byNameField.getName());
         LOGGER.info("ByName field type: {}", byNameField.getType().getSimpleName());
      }

      LOGGER.info("=== End Debug Info ===");
   }

   public static String getReplacementSummary() {
      return String.format("Recipe Replacements - Removed: %d, Added: %d, Fields Init: %s", removedRecipeCache.size(), addedRecipeCache.size(), fieldsInitialized);
   }

   public static boolean validateState() {
    boolean isValid = true;
    if (!fieldsInitialized) {
        LOGGER.warn("Recipe replacement fields not initialized");
        isValid = false;
    }

    if (recipesField == null) {
        LOGGER.warn("Recipes field is null");
        isValid = false;
    }

    try {
        if (recipesField != null) {
            recipesField.setAccessible(true);
            LOGGER.debug("Recipes field is accessible: {}", recipesField.isAccessible());
        }
    } catch (Exception e) {
        LOGGER.warn("Exception checking recipes field: {}", e.getMessage());
        isValid = false;
    }

    LOGGER.info("Recipe replacement state validation: {}", isValid ? "VALID" : "INVALID");
    return isValid;
}
}