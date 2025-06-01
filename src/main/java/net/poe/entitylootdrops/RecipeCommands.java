package net.poe.entitylootdrops;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles commands related to custom recipes.
 */
@Mod.EventBusSubscriber(modid = EntityLootDrops.MOD_ID)
public class RecipeCommands {
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("entitylootdrops")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("recipes")
                .then(Commands.literal("reload")
                    .executes(RecipeCommands::reloadRecipes))
                .then(Commands.literal("list")
                    .executes(RecipeCommands::listRecipes))
                .then(Commands.literal("info")
                    .then(Commands.argument("recipeName", StringArgumentType.string())
                        .executes(RecipeCommands::getRecipeInfo)))
                .then(Commands.literal("debug")
                    .executes(RecipeCommands::debugRecipes))
                .then(Commands.literal("cache")
                    .then(Commands.literal("clear")
                        .executes(RecipeCommands::clearCache))
                    .then(Commands.literal("rebuild")
                        .executes(RecipeCommands::rebuildCache))
                    .then(Commands.literal("info")
                        .executes(RecipeCommands::cacheInfo)))
                .then(Commands.literal("examples")
                    .then(Commands.literal("enable")
                        .executes(RecipeCommands::enableExamples))
                    .then(Commands.literal("disable")
                        .executes(RecipeCommands::disableExamples)))
                .then(Commands.literal("replacements")
                    .then(Commands.literal("apply")
                        .executes(RecipeCommands::applyReplacements))
                    .then(Commands.literal("list")
                        .executes(RecipeCommands::listReplacements))))
        );
    }
    
    /**
     * Reloads all recipe configurations.
     */
    private static int reloadRecipes(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        try {
            source.sendSuccess(() -> Component.literal("Reloading recipe configurations..."), false);
            
            // Reload the configuration
            RecipeConfig.reloadConfig();
            
            // Apply recipe replacements if server is available
            MinecraftServer server = source.getServer();
            if (server != null) {
                RecipeReplacementHandler.applyRecipeReplacements(server);
            }
            
            int totalRecipes = RecipeConfig.getTotalRecipeCount();
            int replacementRecipes = RecipeConfig.getReplacementRecipeCount();
            
            source.sendSuccess(() -> Component.literal(
                String.format("Successfully reloaded %d recipes (%d replacements)", 
                    totalRecipes, replacementRecipes)), true);
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to reload recipes: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Lists all loaded recipes.
     */
    private static int listRecipes(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        int shapedCount = RecipeConfig.getShapedRecipes().size();
        int shapelessCount = RecipeConfig.getShapelessRecipes().size();
        int brewingCount = RecipeConfig.getBrewingRecipes().size();
        int furnaceCount = RecipeConfig.getFurnaceRecipes().size();
        int smithingCount = RecipeConfig.getSmithingRecipes().size();
        int totalCount = RecipeConfig.getTotalRecipeCount();
        int replacementCount = RecipeConfig.getReplacementRecipeCount();
        
        source.sendSuccess(() -> Component.literal("=== Recipe Summary ==="), false);
        source.sendSuccess(() -> Component.literal("Shaped recipes: " + shapedCount), false);
        source.sendSuccess(() -> Component.literal("Shapeless recipes: " + shapelessCount), false);
        source.sendSuccess(() -> Component.literal("Brewing recipes: " + brewingCount), false);
        source.sendSuccess(() -> Component.literal("Furnace recipes: " + furnaceCount), false);
        source.sendSuccess(() -> Component.literal("Smithing recipes: " + smithingCount), false);
        source.sendSuccess(() -> Component.literal("Total recipes: " + totalCount), false);
        source.sendSuccess(() -> Component.literal("Replacement recipes: " + replacementCount), false);
        
        return 1;
    }
    
    /**
     * Gets detailed information about a specific recipe.
     */
    private static int getRecipeInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String recipeName = StringArgumentType.getString(context, "recipeName");
        
        // Search through all recipe types
        RecipeConfig.BaseRecipe foundRecipe = null;
        String recipeType = "Unknown";
        
        // Check shaped recipes
        for (RecipeConfig.CustomRecipe recipe : RecipeConfig.getShapedRecipes()) {
            if (recipe.getName().equals(recipeName)) {
                foundRecipe = recipe;
                recipeType = "Shaped";
                break;
            }
        }
        
        // Check shapeless recipes
        if (foundRecipe == null) {
            for (RecipeConfig.CustomRecipe recipe : RecipeConfig.getShapelessRecipes()) {
                if (recipe.getName().equals(recipeName)) {
                    foundRecipe = recipe;
                    recipeType = "Shapeless";
                    break;
                }
            }
        }
        
        // Check brewing recipes
        if (foundRecipe == null) {
            for (RecipeConfig.BrewingRecipe recipe : RecipeConfig.getBrewingRecipes()) {
                if (recipe.getName().equals(recipeName)) {
                    foundRecipe = recipe;
                    recipeType = "Brewing";
                    break;
                }
            }
        }
        
        // Check furnace recipes
        if (foundRecipe == null) {
            for (RecipeConfig.FurnaceRecipe recipe : RecipeConfig.getFurnaceRecipes()) {
                if (recipe.getName().equals(recipeName)) {
                    foundRecipe = recipe;
                    recipeType = "Furnace";
                    break;
                }
            }
        }
        
        // Check smithing recipes
        if (foundRecipe == null) {
            for (RecipeConfig.SmithingRecipe recipe : RecipeConfig.getSmithingRecipes()) {
                if (recipe.getName().equals(recipeName)) {
                    foundRecipe = recipe;
                    recipeType = "Smithing";
                    break;
                }
            }
        }
        
        if (foundRecipe == null) {
            source.sendFailure(Component.literal("Recipe not found: " + recipeName));
            return 0;
        }
        
        // Make variables final for lambda expressions
        final RecipeConfig.BaseRecipe finalFoundRecipe = foundRecipe;
        final String finalRecipeType = recipeType;
        
        source.sendSuccess(() -> Component.literal("=== Recipe Info: " + recipeName + " ==="), false);
        source.sendSuccess(() -> Component.literal("Type: " + finalRecipeType), false);
        source.sendSuccess(() -> Component.literal("Enabled: " + finalFoundRecipe.isEnabled()), false);
        source.sendSuccess(() -> Component.literal("Has Commands: " + finalFoundRecipe.hasCommands()), false);
        source.sendSuccess(() -> Component.literal("Is Replacement: " + finalFoundRecipe.isReplacement()), false);
        
        if (finalFoundRecipe.isReplacement()) {
            source.sendSuccess(() -> Component.literal("Replaces: " + finalFoundRecipe.getRecipeToReplace()), false);
            source.sendSuccess(() -> Component.literal("Remove Original: " + finalFoundRecipe.isRemoveOriginal()), false);
        }
        
        if (finalFoundRecipe.hasCommands()) {
            source.sendSuccess(() -> Component.literal("Commands (" + finalFoundRecipe.getCraftCommands().size() + "):"), false);
            for (int i = 0; i < finalFoundRecipe.getCraftCommands().size(); i++) {
                final int index = i;
                source.sendSuccess(() -> Component.literal("  " + (index + 1) + ". " + finalFoundRecipe.getCraftCommands().get(index)), false);
            }
        }
        
        return 1;
    }
    
    /**
     * Enables debug mode for recipes.
     */
    private static int debugRecipes(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("=== Recipe Debug Info ==="), false);
        
        // Debug cache contents
        CraftingEventHandler.debugCacheContents();
        
        source.sendSuccess(() -> Component.literal("Debug information printed to server log"), false);
        return 1;
    }
    
    /**
     * Clears the recipe cache.
     */
    private static int clearCache(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        CraftingEventHandler.initRecipeCache();
        source.sendSuccess(() -> Component.literal("Recipe cache cleared"), false);
        
        return 1;
    }
    
    /**
     * Rebuilds the recipe cache.
     */
    private static int rebuildCache(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        CraftingEventHandler.initRecipeCache();
        source.sendSuccess(() -> Component.literal("Recipe cache rebuilt"), false);
        
        return 1;
    }
    
    /**
     * Shows cache information.
     */
    private static int cacheInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("=== Cache Information ==="), false);
        CraftingEventHandler.debugCacheContents();
        source.sendSuccess(() -> Component.literal("Cache information printed to server log"), false);
        
        return 1;
    }
    
    /**
     * Enables example recipe generation.
     */
    private static int enableExamples(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        RecipeConfig.enableExampleRecipes();
        source.sendSuccess(() -> Component.literal("Example recipe generation enabled"), false);
        
        return 1;
    }
    
    /**
     * Disables example recipe generation.
     */
    private static int disableExamples(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        RecipeConfig.disableExampleRecipes();
        source.sendSuccess(() -> Component.literal("Example recipe generation disabled"), false);
        
        return 1;
    }
    
    /**
     * Applies recipe replacements.
     */
    private static int applyReplacements(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        try {
            MinecraftServer server = source.getServer();
            if (server == null) {
                source.sendFailure(Component.literal("Server not available"));
                return 0;
            }
            
            RecipeReplacementHandler.applyRecipeReplacements(server);
            source.sendSuccess(() -> Component.literal("Recipe replacements applied"), false);
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to apply replacements: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Lists all replacement recipes.
     */
    private static int listReplacements(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        int replacementCount = RecipeConfig.getReplacementRecipeCount();
        
        source.sendSuccess(() -> Component.literal("=== Replacement Recipes (" + replacementCount + ") ==="), false);
        
        // List shaped replacements
        for (RecipeConfig.CustomRecipe recipe : RecipeConfig.getShapedRecipes()) {
            if (recipe.isReplacement()) {
                source.sendSuccess(() -> Component.literal("Shaped: " + recipe.getName() + " -> " + recipe.getRecipeToReplace()), false);
            }
        }
        
        // List shapeless replacements
        for (RecipeConfig.CustomRecipe recipe : RecipeConfig.getShapelessRecipes()) {
            if (recipe.isReplacement()) {
                source.sendSuccess(() -> Component.literal("Shapeless: " + recipe.getName() + " -> " + recipe.getRecipeToReplace()), false);
            }
        }
        
        // List brewing replacements
        for (RecipeConfig.BrewingRecipe recipe : RecipeConfig.getBrewingRecipes()) {
            if (recipe.isReplacement()) {
                source.sendSuccess(() -> Component.literal("Brewing: " + recipe.getName() + " -> " + recipe.getRecipeToReplace()), false);
            }
        }
        
        // List furnace replacements
        for (RecipeConfig.FurnaceRecipe recipe : RecipeConfig.getFurnaceRecipes()) {
            if (recipe.isReplacement()) {
                source.sendSuccess(() -> Component.literal("Furnace: " + recipe.getName() + " -> " + recipe.getRecipeToReplace()), false);
            }
        }
        
        // List smithing replacements
        for (RecipeConfig.SmithingRecipe recipe : RecipeConfig.getSmithingRecipes()) {
            if (recipe.isReplacement()) {
                source.sendSuccess(() -> Component.literal("Smithing: " + recipe.getName() + " -> " + recipe.getRecipeToReplace()), false);
            }
        }
        
        return 1;
    }
}
