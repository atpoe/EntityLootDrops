package net.poe.entitylootdrops;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class RecipeDebugCommand {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeDebugCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register both command names for compatibility
        dispatcher.register(Commands.literal("findrecipe")
            .requires(source -> source.hasPermission(2)) // OP level 2
            .then(Commands.literal("byitem")
                .then(Commands.argument("item", StringArgumentType.string())
                    .executes(context -> findRecipesByItem(context))))
            .then(Commands.literal("bymod")
                .then(Commands.argument("modid", StringArgumentType.string())
                    .executes(context -> findRecipesByMod(context))))
            .then(Commands.literal("exact")
                .then(Commands.argument("recipeid", StringArgumentType.string())
                    .executes(context -> findExactRecipe(context))))
            .then(Commands.literal("status")
                .executes(context -> showRecipeStatus(context)))
            .then(Commands.literal("reload")
                .executes(context -> reloadRecipes(context)))
        );
        
        // Also register the recipefind alias
        dispatcher.register(Commands.literal("recipefind")
            .requires(source -> source.hasPermission(2))
            .redirect(dispatcher.getRoot().getChild("findrecipe"))
        );
    }
    
    private static int findRecipesByItem(CommandContext<CommandSourceStack> context) {
        String itemId = StringArgumentType.getString(context, "item");
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        
        try {
            Collection<Recipe<?>> allRecipes = RecipeReplacementHandler.getAllRecipes(server.getRecipeManager());
            
            // Filter recipes that output the specified item
            List<Recipe<?>> matchingRecipes = allRecipes.stream()
                .filter(recipe -> {
                    try {
                        ItemStack result = recipe.getResultItem(server.registryAccess());
                        String resultId = ForgeRegistries.ITEMS.getKey(result.getItem()).toString();
                        return resultId.contains(itemId) || resultId.equals(itemId);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
            
            source.sendSuccess(() -> Component.literal("§6=== Recipes that output items matching '" + itemId + "' ==="), false);
            
            if (matchingRecipes.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§cNo recipes found for item: " + itemId), false);
                
                // Suggest similar items
                suggestSimilarItems(source, itemId);
                return 0;
            }
            
            // Group recipes by status
            List<Recipe<?>> activeRecipes = matchingRecipes.stream()
                .filter(recipe -> !RecipeReplacementHandler.isRecipeRemoved(recipe.getId()))
                .collect(Collectors.toList());
            
            List<Recipe<?>> replacedRecipes = matchingRecipes.stream()
                .filter(recipe -> RecipeReplacementHandler.isRecipeRemoved(recipe.getId()))
                .collect(Collectors.toList());
            
            // Show active recipes first
            if (!activeRecipes.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§a=== ACTIVE RECIPES ==="), false);
                for (Recipe<?> recipe : activeRecipes) {
                    displayRecipeInfo(source, recipe, server, "§a[ACTIVE]");
                }
            }
            
            // Show replaced recipes
            if (!replacedRecipes.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§c=== REPLACED RECIPES ==="), false);
                for (Recipe<?> recipe : replacedRecipes) {
                    displayRecipeInfo(source, recipe, server, "§c[REPLACED]");
                }
            }
            
            // Show custom recipes that output this item
            showCustomRecipesForItem(source, itemId);
            
            source.sendSuccess(() -> Component.literal("§6Found " + matchingRecipes.size() + " total recipes (" + 
                activeRecipes.size() + " active, " + replacedRecipes.size() + " replaced)"), false);
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError: " + e.getMessage()));
            LOGGER.error("Error in findRecipesByItem command", e);
        }
        
        return 1;
    }
    
    private static int findRecipesByMod(CommandContext<CommandSourceStack> context) {
        String modId = StringArgumentType.getString(context, "modid");
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        
        try {
            Collection<Recipe<?>> allRecipes = RecipeReplacementHandler.getAllRecipes(server.getRecipeManager());
            
            source.sendSuccess(() -> Component.literal("§6=== Recipes from mod '" + modId + "' ==="), false);
            
            List<Recipe<?>> modRecipes = allRecipes.stream()
                .filter(recipe -> recipe.getId().getNamespace().equals(modId))
                .collect(Collectors.toList());
            
            if (modRecipes.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§cNo recipes found for mod: " + modId), false);
                
                // Suggest similar mod IDs
                suggestSimilarMods(source, allRecipes, modId);
                return 0;
            }
            
            // Group by status
            List<Recipe<?>> activeRecipes = modRecipes.stream()
                .filter(recipe -> !RecipeReplacementHandler.isRecipeRemoved(recipe.getId()))
                .collect(Collectors.toList());
            
            List<Recipe<?>> replacedRecipes = modRecipes.stream()
                .filter(recipe -> RecipeReplacementHandler.isRecipeRemoved(recipe.getId()))
                .collect(Collectors.toList());
            
            // Display active recipes
            if (!activeRecipes.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§a=== ACTIVE RECIPES ==="), false);
                for (Recipe<?> recipe : activeRecipes) {
                    displayRecipeInfo(source, recipe, server, "§a[ACTIVE]");
                }
            }
            
            // Display replaced recipes
            if (!replacedRecipes.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§c=== REPLACED RECIPES ==="), false);
                for (Recipe<?> recipe : replacedRecipes) {
                    displayRecipeInfo(source, recipe, server, "§c[REPLACED]");
                }
            }
            
            source.sendSuccess(() -> Component.literal("§6Found " + modRecipes.size() + " total recipes (" + 
                activeRecipes.size() + " active, " + replacedRecipes.size() + " replaced)"), false);
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError: " + e.getMessage()));
            LOGGER.error("Error in findRecipesByMod command", e);
        }
        
        return 1;
    }
    
    private static int findExactRecipe(CommandContext<CommandSourceStack> context) {
        String recipeId = StringArgumentType.getString(context, "recipeid");
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        
        try {
            ResourceLocation recipeLocation = new ResourceLocation(recipeId);
            Collection<Recipe<?>> allRecipes = RecipeReplacementHandler.getAllRecipes(server.getRecipeManager());
            
            Recipe<?> foundRecipe = allRecipes.stream()
                .filter(recipe -> recipe.getId().equals(recipeLocation))
                .findFirst()
                .orElse(null);
            
            if (foundRecipe == null) {
                source.sendFailure(Component.literal("§cRecipe not found: " + recipeId));
                
                // Suggest similar recipes
                List<Recipe<?>> similarRecipes = allRecipes.stream()
                    .filter(recipe -> recipe.getId().toString().contains(recipeLocation.getPath()) || 
                                    recipe.getId().getNamespace().equals(recipeLocation.getNamespace()))
                    .limit(5)
                    .collect(Collectors.toList());
                
                if (!similarRecipes.isEmpty()) {
                    source.sendSuccess(() -> Component.literal("§6Similar recipes found:"), false);
                    for (Recipe<?> similar : similarRecipes) {
                        source.sendSuccess(() -> Component.literal("§7- " + similar.getId()), false);
                    }
                }
                
                return 0;
            }
            
            // Display detailed recipe information
            source.sendSuccess(() -> Component.literal("§6=== Recipe Details: " + recipeId + " ==="), false);
            source.sendSuccess(() -> Component.literal("§7Type: §f" + foundRecipe.getType()), false);
            
            try {
                ItemStack result = foundRecipe.getResultItem(server.registryAccess());
                String resultId = ForgeRegistries.ITEMS.getKey(result.getItem()).toString();
                source.sendSuccess(() -> Component.literal("§7Output: §f" + resultId + " x" + result.getCount()), false);
            } catch (Exception e) {
                source.sendSuccess(() -> Component.literal("§7Output: §c[Error getting output]"), false);
            }
            
            // Check if this recipe has been replaced
            boolean isReplaced = RecipeReplacementHandler.isRecipeRemoved(foundRecipe.getId());
            String status = isReplaced ? "§c[REPLACED]" : "§a[ACTIVE]";
            source.sendSuccess(() -> Component.literal("§7Status: " + status), false);
            
            // Check if there's a custom recipe that replaces this
            if (isReplaced) {
                showReplacementInfo(source, foundRecipe.getId());
            }
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError: " + e.getMessage()));
            LOGGER.error("Error in findExactRecipe command", e);
        }
        
        return 1;
    }
    
    private static int showRecipeStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        
        try {
            source.sendSuccess(() -> Component.literal("§6=== Recipe System Status ==="), false);
            
            // Show recipe replacement handler status
            boolean isValid = RecipeReplacementHandler.validateState();
            source.sendSuccess(() -> Component.literal("§7Handler Status: " + (isValid ? "§a[OK]" : "§c[ERROR]")), false);
            
            // Show recipe counts
            Collection<Recipe<?>> allRecipes = RecipeReplacementHandler.getAllRecipes(server.getRecipeManager());
            source.sendSuccess(() -> Component.literal("§7Total Recipes: §f" + allRecipes.size()), false);
            
            // Show replacement statistics
            source.sendSuccess(() -> Component.literal("§7" + RecipeReplacementHandler.getReplacementSummary()), false);
            
            // Show config statistics
            var stats = RecipeConfig.getRecipeStatistics();
            source.sendSuccess(() -> Component.literal("§7Custom Recipes: §f" + stats.get("total")), false);
            source.sendSuccess(() -> Component.literal("§7- Shaped: §f" + stats.get("shaped")), false);
            source.sendSuccess(() -> Component.literal("§7- Shapeless: §f" + stats.get("shapeless")), false);
            source.sendSuccess(() -> Component.literal("§7- Brewing: §f" + stats.get("brewing")), false);
            source.sendSuccess(() -> Component.literal("§7- Furnace: §f" + stats.get("furnace")), false);
            source.sendSuccess(() -> Component.literal("§7- Smithing: §f" + stats.get("smithing")), false);
            source.sendSuccess(() -> Component.literal("§7- Replacements: §f" + stats.get("replacements")), false);
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError: " + e.getMessage()));
            LOGGER.error("Error in showRecipeStatus command", e);
        }
        
        return 1;
    }
    
    private static int reloadRecipes(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        
        try {
            source.sendSuccess(() -> Component.literal("§6Reloading recipe configuration..."), false);
            
            // Reload recipe config
            RecipeConfig.reloadConfig();
            
            // Clear and reapply recipe replacements
            RecipeReplacementHandler.clearCaches();
            RecipeReplacementHandler.applyRecipeReplacements(server);
            
            source.sendSuccess(() -> Component.literal("§aRecipe configuration reloaded successfully!"), false);
            source.sendSuccess(() -> Component.literal("§7" + RecipeReplacementHandler.getReplacementSummary()), false);
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError reloading recipes: " + e.getMessage()));
            LOGGER.error("Error in reloadRecipes command", e);
        }
        
        return 1;
    }
    
    // Helper methods
    
    private static void displayRecipeInfo(CommandSourceStack source, Recipe<?> recipe, MinecraftServer server, String status) {
        try {
            ItemStack result = recipe.getResultItem(server.registryAccess());
            String resultId = ForgeRegistries.ITEMS.getKey(result.getItem()).toString();
            source.sendSuccess(() -> Component.literal("§a" + recipe.getId() + " §7-> §f" + resultId + " x" + result.getCount() + " " + status), false);
        } catch (Exception e) {
            source.sendSuccess(() -> Component.literal("§a" + recipe.getId() + " §7-> §c[Error getting output] " + status), false);
        }
    }
    
    private static void suggestSimilarItems(CommandSourceStack source, String itemId) {
        try {
            List<String> similarItems = ForgeRegistries.ITEMS.getKeys().stream()
                .map(ResourceLocation::toString)
                .filter(id -> id.contains(itemId.toLowerCase()) || 
                             id.toLowerCase().contains(itemId.toLowerCase()))
                .limit(5)
                .collect(Collectors.toList());
            
            if (!similarItems.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§6Similar items found:"), false);
                for (String similar : similarItems) {
                    source.sendSuccess(() -> Component.literal("§7- " + similar), false);
                }
            }
        } catch (Exception e) {
            // Ignore errors in suggestions
        }
    }
    
    private static void suggestSimilarMods(CommandSourceStack source, Collection<Recipe<?>> allRecipes, String modId) {
        try {
            List<String> availableMods = allRecipes.stream()
                .map(recipe -> recipe.getId().getNamespace())
                .distinct()
                .filter(namespace -> namespace.contains(modId.toLowerCase()) || 
                                   namespace.toLowerCase().contains(modId.toLowerCase()))
                .limit(5)
                .collect(Collectors.toList());
            
            if (!availableMods.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§6Similar mod IDs found:"), false);
                for (String similar : availableMods) {
                    source.sendSuccess(() -> Component.literal("§7- " + similar), false);
                }
            }
        } catch (Exception e) {
            // Ignore errors in suggestions
        }
    }
    
    private static void showCustomRecipesForItem(CommandSourceStack source, String itemId) {
        try {
            List<RecipeConfig.CustomRecipe> customRecipes = RecipeConfig.getAllCustomRecipes();
            List<RecipeConfig.CustomRecipe> matchingCustom = customRecipes.stream()
                .filter(recipe -> recipe.getOutputItem().contains(itemId))
                .collect(Collectors.toList());
            
            if (!matchingCustom.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§b=== CUSTOM RECIPES ==="), false);
                for (RecipeConfig.CustomRecipe recipe : matchingCustom) {
                    String status = recipe.isEnabled() ? "§b[CUSTOM]" : "§8[DISABLED]";
                    source.sendSuccess(() -> Component.literal("§b" + EntityLootDrops.MOD_ID + ":" + recipe.getName() + 
                        " §7-> §f" + recipe.getOutputItem() + " x" + recipe.getOutputCount() + " " + status), false);
                }
            }
        } catch (Exception e) {
            // Ignore errors in custom recipe display
        }
    }
    
    private static void showReplacementInfo(CommandSourceStack source, ResourceLocation originalRecipeId) {
        try {
            // Find which custom recipe replaces this one
            List<RecipeConfig.CustomRecipe> customRecipes = RecipeConfig.getAllCustomRecipes();
            for (RecipeConfig.CustomRecipe recipe : customRecipes) {
                if (recipe.isReplacement() && recipe.getRecipeToReplace().equals(originalRecipeId.toString())) {
                    source.sendSuccess(() -> Component.literal("§7Replaced by: §b" + EntityLootDrops.MOD_ID + ":" + recipe.getName()), false);
                    break;
                }
            }
        } catch (Exception e) {
            // Ignore errors in replacement info
        }
    }
}
