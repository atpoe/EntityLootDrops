package net.poe.entitylootdrops;

import java.util.Set;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.poe.entitylootdrops.config.ModConfig;

/**
 * Command handler for the EntityLootDrops mod.
 * Registers and processes all commands for controlling events and configuration.
 */
@Mod.EventBusSubscriber(modid = EntityLootDrops.MOD_ID)
public class LootCommands {

    /**
     * Event handler for command registration.
     * This is called when the server is starting and commands are being registered.
     * 
     * @param event The RegisterCommandsEvent containing the command dispatcher
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        // Main command - /lootdrops
        // This is the root command that all other subcommands are attached to
        LiteralArgumentBuilder<CommandSourceStack> rootCommand = Commands.literal("lootdrops")
            .requires(source -> source.hasPermission(2)); // Requires permission level 2 (op)
        
        // Event toggle subcommand - /lootdrops event <eventName> <true|false>
        // Used to enable or disable specific events
        rootCommand.then(Commands.literal("event")
            .then(Commands.argument("eventName", StringArgumentType.string())
                .suggests((context, builder) -> {
                    // Suggest available events when the player is typing
                    // This provides tab-completion for event names
                    for (String eventName : LootConfig.getEventDrops().keySet()) {
                        builder.suggest(eventName);
                    }
                    return builder.buildFuture();
                })
                .then(Commands.argument("active", BoolArgumentType.bool())
                    .executes(context -> {
                        // Extract command arguments
                        String eventName = StringArgumentType.getString(context, "eventName");
                        boolean active = BoolArgumentType.getBool(context, "active");
                        
                        // Validate that the event exists
                        if (!LootConfig.getEventDrops().containsKey(eventName)) {
                            context.getSource().sendFailure(Component.literal("Unknown event: " + eventName));
                            return 0; // Command failed
                        }
                        
                        // Toggle the event state
                        LootConfig.toggleEvent(eventName, active);
                        
                        // Sync changes to Forge config
                        boolean syncSuccess = ModConfig.syncFromLootConfig();
                        if (!syncSuccess && ModConfig.isConfigLoaded()) {
                        
                        // Only show warning if config should be loaded by now
                        context.getSource().sendFailure(Component.literal("Warning: Config sync failed. Changes may not persist after restart."));
                        }

                        return 1; // Command succeeded
                    })
                )
            )
            // Drop chance event subcommand - /lootdrops event dropchance <true|false>
            // Used to enable or disable the double drop chance event
            .then(Commands.literal("dropchance")
                .then(Commands.argument("active", BoolArgumentType.bool())
                    .executes(context -> {
                        // Extract command arguments
                        boolean active = BoolArgumentType.getBool(context, "active");
                        
                        // Toggle the drop chance event
                        LootConfig.toggleDropChanceEvent(active);
                        
                        // Sync changes to Forge config
                        boolean syncSuccess = ModConfig.syncFromLootConfig();
                        if (!syncSuccess) {
                            context.getSource().sendFailure(Component.literal("Warning: Config sync failed. Changes may not persist after restart."));
                        }
                        
                        return 1; // Command succeeded
                    })
                )
            )
            // Double drops event subcommand - /lootdrops event doubledrops <true|false>
            // Used to enable or disable the double drops event
            .then(Commands.literal("doubledrops")
                .then(Commands.argument("active", BoolArgumentType.bool())
                    .executes(context -> {
                        // Extract command arguments
                        boolean active = BoolArgumentType.getBool(context, "active");
                        
                        // Toggle the double drops event
                        LootConfig.toggleDoubleDrops(active);
                        
                        // Sync changes to Forge config
                        boolean syncSuccess = ModConfig.syncFromLootConfig();
                        if (!syncSuccess) {
                            context.getSource().sendFailure(Component.literal("Warning: Config sync failed. Changes may not persist after restart."));
                        }
                        return 1; // Command succeeded
                    })
                )
            )
        );
        
        // Debug logging toggle subcommand - /lootdrops debug <true|false>
        rootCommand.then(Commands.literal("debug")
            .then(Commands.argument("enabled", BoolArgumentType.bool())
                .executes(context -> {
                    // Extract command arguments
                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                    
                    // Toggle debug logging
                    LootConfig.setDebugLogging(enabled);
                    
                    // Sync changes to Forge config
                    boolean syncSuccess = ModConfig.syncFromLootConfig();
                    if (!syncSuccess) {
                        context.getSource().sendFailure(Component.literal("Warning: Config sync failed. Changes may not persist after restart."));
                    }
                    
                    // Send success message to the command sender
                    if (enabled) {
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§aEnabled debug logging - §eDetailed drop information will be logged"), true);
                    } else {
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§cDisabled debug logging - Only errors will be logged"), true);
                    }
                    return 1; // Command succeeded
                })
            )
        );
        
        // List active events subcommand - /lootdrops list
        // Shows all currently active events
        rootCommand.then(Commands.literal("list")
            .executes(context -> {
                // Get the current state of all events
                Set<String> activeEvents = LootConfig.getActiveEvents();
                boolean dropChanceActive = LootConfig.isDropChanceEventActive();
                boolean doubleDropsActive = LootConfig.isDoubleDropsActive();
                boolean debugLoggingActive = LootConfig.isDebugLoggingEnabled();
                
                // If no events are active and debug logging is off, show a message
                if (activeEvents.isEmpty() && !dropChanceActive && !doubleDropsActive && !debugLoggingActive) {
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§cNo active events or features"), false);
                } else {
                    // Build a formatted message with all active events
                    StringBuilder sb = new StringBuilder("§6Active events and features: ");
                    
                    // Add regular events
                    if (!activeEvents.isEmpty()) {
                        sb.append("§a").append(String.join("§6, §a", activeEvents));
                    }
                    
                    // Add drop chance event if active
                    if (dropChanceActive) {
                        if (!activeEvents.isEmpty()) {
                            sb.append("§6, ");
                        }
                        sb.append("§e§ldropchance (2x drop rates)§r");
                    }
                    
                    // Add double drops event if active
                    if (doubleDropsActive) {
                        if (!activeEvents.isEmpty() || dropChanceActive) {
                            sb.append("§6, ");
                        }
                        sb.append("§e§ldoubledrops (2x amounts)§r");
                    }
                    
                    // Add debug logging if active
                    if (debugLoggingActive) {
                        if (!activeEvents.isEmpty() || dropChanceActive || doubleDropsActive) {
                            sb.append("§6, ");
                        }
                        sb.append("§b§ldebug logging§r");
                    }
                    
                    // Send the formatted message to the command sender
                    context.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                }
                return 1; // Command succeeded
            })
        );
        
        // Open config screen subcommand - /lootdrops openconfig
        // Opens the Forge config screen for the mod
        rootCommand.then(Commands.literal("openconfig")
            .executes(context -> {
                // This command can only be executed by a player
                if (!(context.getSource().getEntity() instanceof ServerPlayer)) {
                    context.getSource().sendFailure(Component.literal("This command can only be used by a player"));
                    return 0;
                }
                
                ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
                
                // Send a message to the player
                context.getSource().sendSuccess(() -> 
                    Component.literal("§aOpening configuration screen..."), false);
                
                // Use a direct clickEvent with a command that will be intercepted by our client handler
                player.sendSystemMessage(Component.literal("§a[EntityLootDrops] §eClick here to open config")
                    .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lootdrops_openconfig_client"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                            Component.literal("Click to open configuration screen")))
                        .withUnderlined(true) // Underline the text
                        .withColor(net.minecraft.ChatFormatting.GREEN) // Use built-in ChatFormatting instead
                    )
                );
                
                return 1; // Command succeeded
            })
        );
        
        // Reload configuration subcommand - /lootdrops reload
        // Reloads all configuration files from disk
        rootCommand.then(Commands.literal("reload")
            .executes(context -> {
                // Reload the configuration
                LootConfig.loadConfig();
                
                // Sync changes to Forge config
                boolean syncSuccess = ModConfig.syncFromLootConfig();
                if (!syncSuccess) {
                    context.getSource().sendFailure(Component.literal("Warning: Config sync failed. Changes may not persist after restart."));
                }
                
                // Send success message to the command sender
                context.getSource().sendSuccess(() -> 
                    Component.literal("§aReloaded Entity Loot Drops configuration"), true);
                return 1; // Command succeeded
            })
        );
        
        // Register the command with the dispatcher
        dispatcher.register(rootCommand);
    }
}
