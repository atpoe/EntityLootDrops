package net.poe.entitylootdrops.commands;

import java.util.Map;
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
import net.poe.entitylootdrops.EntityLootDrops;
import net.poe.entitylootdrops.blockdrops.BlockConfig;
import net.poe.entitylootdrops.blockdrops.regeneration.BlockRegenerationManager;

/**
 * Command handler for the EntityLootDrops mod block events.
 * Registers and processes all commands for controlling block events, configuration, and regeneration.
 */
@Mod.EventBusSubscriber(modid = EntityLootDrops.MOD_ID)
public class BlockEventCommands {

    /**
     * Event handler for command registration.
     * This is called when the server is starting and commands are being registered.
     * 
     * @param event The RegisterCommandsEvent containing the command dispatcher
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        // Main command - /blockdrops
        // This is the root command that all other subcommands are attached to
        LiteralArgumentBuilder<CommandSourceStack> rootCommand = Commands.literal("blockdrops")
            .requires(source -> source.hasPermission(2)); // Requires permission level 2 (op)
        
        // Event toggle subcommand - /blockdrops event <eventName> <true|false>
        // Used to enable or disable specific events
        rootCommand.then(Commands.literal("event")
            .then(Commands.argument("eventName", StringArgumentType.string())
                .suggests((context, builder) -> {
                    // Suggest available events when the player is typing
                    // This provides tab-completion for event names
                    for (String eventName : BlockConfig.getAvailableBlockEvents()) {
                        builder.suggest(eventName);
                    }
                    return builder.buildFuture();
                })
                .then(Commands.argument("active", BoolArgumentType.bool())
                    .executes(context -> {
                        // Extract command arguments
                        String eventName = StringArgumentType.getString(context, "eventName");
                        boolean active = BoolArgumentType.getBool(context, "active");
                        
                        // Check against all event names
                        boolean eventExists = false;
                        for (String availableEvent : BlockConfig.getAvailableBlockEvents()) {
                            if (availableEvent.equalsIgnoreCase(eventName)) {
                                eventExists = true;
                                break;
                            }
                        }
                        
                        // Validate that the event exists
                        if (!eventExists) {
                            context.getSource().sendFailure(Component.literal("Unknown event: " + eventName));
                            return 0; // Command failed
                        }
                        
                        // Toggle the event state
                        BlockConfig.toggleBlockEvent(eventName, active);

                        // Send success message
                        String statusMessage = active ? "§aEnabled" : "§cDisabled";
                        context.getSource().sendSuccess(() -> 
                            Component.literal(statusMessage + " block event: §e" + eventName), true);

                        return 1; // Command succeeded
                    })
                )
            )
            // Drop chance event subcommand - /blockdrops dropchance <true|false>
            // Used to enable or disable the double drop chance event
            .then(Commands.literal("dropchance")
                .then(Commands.argument("active", BoolArgumentType.bool())
                    .executes(context -> {
                        // Extract command arguments
                        boolean active = BoolArgumentType.getBool(context, "active");
                        
                        // Toggle the drop chance event
                        BlockConfig.toggleBlockDropChanceEvent(active);
                        
                        // Send success message
                        String statusMessage = active ? "§aEnabled" : "§cDisabled";
                        context.getSource().sendSuccess(() -> 
                            Component.literal(statusMessage + " block drop chance event (2x drop rates)"), true);
                        
                        return 1; // Command succeeded
                    })
                )
            )
            // Double drops event subcommand - /blockdrops doubledrops <true|false>
            // Used to enable or disable the double drops event
            .then(Commands.literal("doubledrops")
                .then(Commands.argument("active", BoolArgumentType.bool())
                    .executes(context -> {
                        // Extract command arguments
                        boolean active = BoolArgumentType.getBool(context, "active");
                        
                        // Toggle the double drops event
                        BlockConfig.toggleBlockDoubleDrops(active);
                        
                        // Send success message
                        String statusMessage = active ? "§aEnabled" : "§cDisabled";
                        context.getSource().sendSuccess(() -> 
                            Component.literal(statusMessage + " block double drops event (2x amounts)"), true);
                        
                        return 1; // Command succeeded
                    })
                )
            )
        );
        
        // List active events subcommand - /blockdrops active_events
        // Shows all currently active events
        rootCommand.then(Commands.literal("active_events")
            .executes(context -> {
                // Get the current state of all events
                Set<String> activeEvents = BlockConfig.getActiveBlockEvents();
                boolean dropChanceActive = BlockConfig.isBlockDropChanceEventActive();
                boolean doubleDropsActive = BlockConfig.isBlockDoubleDropsActive();
                int regeneratingBlocks = BlockRegenerationManager.getRegenerationCount();
                
                // If no events are active, show a message
                if (activeEvents.isEmpty() && !dropChanceActive && !doubleDropsActive) {
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§cNo active block events or features"), false);
                } else {
                    // Build a formatted message with all active events
                    StringBuilder sb = new StringBuilder("§6Active block events and features: ");
                    
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
                    
                    // Send the formatted message to the command sender
                    context.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                }
                
                // Show regeneration info
                if (regeneratingBlocks > 0) {
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§6Currently regenerating: §e" + regeneratingBlocks + " §6blocks"), false);
                }
                
                return 1; // Command succeeded
            })
        );
        
        // List all available events subcommand - /blockdrops listall
        rootCommand.then(Commands.literal("listall")
            .executes(context -> {
                Set<String> availableEvents = BlockConfig.getAvailableBlockEvents();
                
                if (availableEvents.isEmpty()) {
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§cNo block events available"), false);
                } else {
                    // Build a formatted message with all available events
                    StringBuilder sb = new StringBuilder("§6Available block events: §a");
                    sb.append(String.join("§6, §a", availableEvents));
                    
                    // Send the formatted message to the command sender
                    context.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                }
                return 1; // Command succeeded
            })
        );

        // Regeneration management subcommands - /blockdrops regeneration
        rootCommand.then(Commands.literal("regeneration")
            .then(Commands.literal("list")
                .executes(context -> {
                    int count = BlockRegenerationManager.getRegenerationCount();
                    if (count == 0) {
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6No blocks are currently regenerating"), false);
                    } else {
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6Currently tracking §e" + count + " §6regenerating blocks"), false);
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§7These blocks are protected and will regenerate automatically"), false);
                    }
                    return 1;
                })
            )
            .then(Commands.literal("clear")
                .executes(context -> {
                    int count = BlockRegenerationManager.getRegenerationCount();
                    if (count == 0) {
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6No regenerating blocks to clear"), false);
                    } else {
                        BlockRegenerationManager.clearAll();
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§6Cleared §e" + count + " §6regenerating blocks"), true);
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§7All replacement blocks will remain as-is"), false);
                    }
                    return 1;
                })
            )
            .then(Commands.literal("info")
                .executes(context -> {
                    int count = BlockRegenerationManager.getRegenerationCount();
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§6=== Block Regeneration Info ==="), false);
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§e- Currently regenerating: §a" + count + " blocks"), false);
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§e- Protection: §aReplacement blocks are unbreakable"), false);
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§e- Creative bypass: §aCreative players can break and cancel"), false);
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§e- Persistence: §aRegenerations survive server restarts"), false);
                    return 1;
                })
            )
        );

        // Open config screen subcommand - /blockdrops openconfig
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
                    Component.literal("§aOpening block configuration screen..."), false);
                
                // Use a direct clickEvent with a command that will be intercepted by our client handler
                player.sendSystemMessage(Component.literal("§a[EntityLootDrops] §eClick here to open block config")
                    .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/blockdrops_openconfig_client"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                            Component.literal("Click to open block configuration screen")))
                        .withUnderlined(true)
                        .withColor(net.minecraft.ChatFormatting.GREEN)
                    )
                );
                
                return 1; // Command succeeded
            })
        );
        
        // Reload configuration subcommand - /blockdrops reload
        // Reloads all configuration files from disk
        rootCommand.then(Commands.literal("reload")
            .executes(context -> {
                // Reload the configuration
                BlockConfig.loadConfig();
                
                // Get updated stats
                int regeneratingCount = BlockRegenerationManager.getRegenerationCount();
                
                // Send success message to the command sender
                context.getSource().sendSuccess(() -> 
                    Component.literal("§aReloaded Entity Loot Drops block configuration"), true);
                
                if (regeneratingCount > 0) {
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§6Note: " + regeneratingCount + " blocks are still regenerating"), false);
                }
                
                return 1; // Command succeeded
            })
        );

        // Debug information subcommand - /blockdrops debug
        rootCommand.then(Commands.literal("debug")
            .executes(context -> {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§6=== Block Drops Debug Information ==="), false);
                
                // Show event information
                Set<String> activeEvents = BlockConfig.getActiveBlockEvents();
                Set<String> availableEvents = BlockConfig.getAvailableBlockEvents();
                
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e- Available Events: §a" + availableEvents.size()), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e- Active Events: §a" + activeEvents.size()), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e- Drop Chance Event: §a" + 
                                    (BlockConfig.isBlockDropChanceEventActive() ? "Active" : "Inactive")), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e- Double Drops Event: §a" + 
                                    (BlockConfig.isBlockDoubleDropsActive() ? "Active" : "Inactive")), false);
                
                // Show regeneration information
                int regeneratingCount = BlockRegenerationManager.getRegenerationCount();
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e- Regenerating Blocks: §a" + regeneratingCount), false);
                
                // Show configuration counts
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e- Normal Drops: §a" + BlockConfig.getNormalDrops().size()), false);
                
                return 1; // Command succeeded
            })
        );
        
        // Help subcommand - /blockdrops help
        // Shows all available commands
        rootCommand.then(Commands.literal("help")
            .executes(context -> {
                context.getSource().sendSuccess(() -> 
                    Component.literal("§6=== Block Drops Commands ==="), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e/blockdrops event <name> <true|false> §7- Toggle block event"), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e/blockdrops dropchance <true|false> §7- Toggle 2x drop rates"), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e/blockdrops doubledrops <true|false> §7- Toggle 2x drop amounts"), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e/blockdrops active_events §7- Show active events"), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e/blockdrops listall §7- Show all available events"), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e/blockdrops regeneration list §7- Show regenerating blocks"), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e/blockdrops regeneration clear §7- Clear all regenerations"), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e/blockdrops regeneration info §7- Show regeneration info"), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e/blockdrops reload §7- Reload configuration"), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e/blockdrops debug §7- Show debug information"), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§e/blockdrops help §7- Show this help message"), false);
                context.getSource().sendSuccess(() -> 
                    Component.literal("§7All commands require OP permission level 2"), false);
                
                return 1; // Command succeeded
            })
        );
        
        // Register the command with the dispatcher
        dispatcher.register(rootCommand);
    }
}
