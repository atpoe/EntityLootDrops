package net.poe.entitylootdrops;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Command handler for recipe-related commands.
 */
@Mod.EventBusSubscriber(modid = EntityLootDrops.MOD_ID)
public class RecipeCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        // Main command - /recipes
        LiteralArgumentBuilder<CommandSourceStack> rootCommand = Commands.literal("recipes")
            .requires(source -> source.hasPermission(2)); // Requires permission level 2 (op)
        
        // Reload subcommand - /recipes reload
        rootCommand.then(Commands.literal("reload")
            .executes(context -> {
                // Reload recipe configuration
                RecipeConfig.loadConfig();
                
                // Send success message
                context.getSource().sendSuccess(() -> 
                    Component.literal("§aReloaded recipe configuration. §eServer restart required to apply changes."), true);
                
                return 1;
            })
        );
        
        // List subcommand - /recipes list
        rootCommand.then(Commands.literal("list")
            .executes(context -> {
                int shapedCount = RecipeConfig.getShapedRecipes().size();
                int shapelessCount = RecipeConfig.getShapelessRecipes().size();
                
                context.getSource().sendSuccess(() -> 
                    Component.literal("§6Custom Recipes: §a" + shapedCount + " shaped, §a" + shapelessCount + " shapeless"), false);
                
                return 1;
            })
        );
        
        // Register the command
        dispatcher.register(rootCommand);
    }
}
