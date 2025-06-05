package net.poe.entitylootdrops.commands;

import java.io.File;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.poe.entitylootdrops.EntityLootDrops;
import net.poe.entitylootdrops.fishing.config.FishingConfig;

/**
 * Command handler for the Fishing Drops system.
 * Registers and processes all commands for controlling fishing configuration.
 */
@Mod.EventBusSubscriber(modid = EntityLootDrops.MOD_ID)
public class FishingCommands {

    /**
     * Event handler for command registration.
     * This is called when the server is starting and commands are being registered.
     * 
     * @param event The RegisterCommandsEvent containing the command dispatcher
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        // Main command - /fishingdrops
        // This is the root command that all other subcommands are attached to
        LiteralArgumentBuilder<CommandSourceStack> rootCommand = Commands.literal("fishingdrops")
            .requires(source -> source.hasPermission(2)); // Requires permission level 2 (op)
        
        // Reload configuration subcommand - /fishingdrops reload
        // Reloads all fishing configuration files from disk
        rootCommand.then(Commands.literal("reload")
            .executes(context -> {
                // Reload the fishing configuration
                try {
                    FishingConfig.reloadConfig(new File("config"));
                    
                    // Send success message to the command sender
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§aReloaded Fishing Drops configuration"), true);
                    
                    // Log the reload
                    EntityLootDrops.LOGGER.info("Fishing configuration reloaded by {}", 
                        context.getSource().getTextName());
                    
                    return 1; // Command succeeded
                } catch (Exception e) {
                    // Handle any errors during reload
                    EntityLootDrops.LOGGER.error("Failed to reload fishing configuration", e);
                    context.getSource().sendFailure(
                        Component.literal("§cFailed to reload fishing configuration: " + e.getMessage()));
                    return 0; // Command failed
                }
            })
        );
        
        // List fishing drops subcommand - /fishingdrops list
        // Shows information about loaded fishing drops
        rootCommand.then(Commands.literal("list")
            .executes(context -> {
                int fishingDropsCount = FishingConfig.getFishingDrops().size();
                int globalRewardsCount = FishingConfig.getGlobalFishingRewards().size();
                
                // Send information about loaded drops
                context.getSource().sendSuccess(() -> 
                    Component.literal("§6Fishing Drops Status:"), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e- Conditional/Biome/Dimension Drops: §a" + fishingDropsCount), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e- Global Fishing Rewards: §a" + globalRewardsCount), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e- Total Fishing Configurations: §a" + (fishingDropsCount + globalRewardsCount)), false);
                
                return 1; // Command succeeded
            })
        );
        
        // Debug information subcommand - /fishingdrops debug
        // Shows detailed debug information about fishing drops
        rootCommand.then(Commands.literal("debug")
            .executes(context -> {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§6Fishing Drops Debug Information:"), false);
                
                // Show conditional drops
                long conditionalDrops = FishingConfig.getFishingDrops().stream()
                    .filter(drop -> drop.getWeather() != null || drop.getTimeOfDay() != null)
                    .count();
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e- Weather/Time Conditional Drops: §a" + conditionalDrops), false);
                
                // Show biome-specific drops
                long biomeDrops = FishingConfig.getFishingDrops().stream()
                    .filter(drop -> drop.getBiome() != null)
                    .count();
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e- Biome-Specific Drops: §a" + biomeDrops), false);
                
                // Show dimension-specific drops
                long dimensionDrops = FishingConfig.getFishingDrops().stream()
                    .filter(drop -> drop.getDimension() != null)
                    .count();
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e- Dimension-Specific Drops: §a" + dimensionDrops), false);
                
                // Show enchantment-required drops
                long enchantmentDrops = FishingConfig.getFishingDrops().stream()
                    .filter(drop -> drop.requiresLure() || drop.requiresLuckOfSea())
                    .count();
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e- Enchantment-Required Drops: §a" + enchantmentDrops), false);
                
                // Show level-gated drops
                long levelGatedDrops = FishingConfig.getFishingDrops().stream()
                    .filter(drop -> drop.getMinFishingLevel() > 0)
                    .count();
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e- Level-Gated Drops: §a" + levelGatedDrops), false);
                
                return 1; // Command succeeded
            })
        );
        
        // Test command subcommand - /fishingdrops test
        // Provides information about testing fishing drops
        rootCommand.then(Commands.literal("test")
            .executes(context -> {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§6Fishing Drops Testing Information:"), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e- Go fishing to test your configured drops"), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e- Check server logs for detailed drop information"), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e- Use different biomes, weather, and times to test conditions"), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e- Try fishing with different enchanted rods"), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e- Test in different dimensions if configured"), false);
                
                return 1; // Command succeeded
            })
        );
        
        // Help subcommand - /fishingdrops help
        // Shows all available commands
        rootCommand.then(Commands.literal("help")
            .executes(context -> {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§6=== Fishing Drops Commands ==="), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e/fishingdrops reload §7- Reload fishing configuration"), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e/fishingdrops list §7- Show loaded fishing drops count"), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e/fishingdrops debug §7- Show detailed debug information"), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e/fishingdrops test §7- Show testing information"), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e/fishingdrops help §7- Show this help message"), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§7All commands require OP permission level 2"), false);
                
                return 1; // Command succeeded
            })
        );
        
        // Register the command with the dispatcher
        dispatcher.register(rootCommand);
    }
}
