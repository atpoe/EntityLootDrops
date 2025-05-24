package net.poe.entitylootdrops;

import java.util.Set;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Command handler for block-related commands in the EntityLootDrops mod.
 */
@Mod.EventBusSubscriber(modid = EntityLootDrops.MOD_ID)
public class BlockCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        // Main command - /blockdrops
        LiteralArgumentBuilder<CommandSourceStack> rootCommand = Commands.literal("blockdrops")
            .requires(source -> source.hasPermission(2)); // Requires permission level 2 (op)
        
        // Event toggle subcommand - /blockdrops event <eventName> <true|false>
        rootCommand.then(Commands.literal("event")
            .then(Commands.argument("eventName", StringArgumentType.string())
                .suggests((context, builder) -> {
                    // Suggest available events
                    for (String eventName : BlockConfig.getAvailableBlockEvents()) {
                        builder.suggest(eventName);
                    }
                    return builder.buildFuture();
                })
                .then(Commands.argument("active", BoolArgumentType.bool())
                    .executes(context -> {
                        String eventName = StringArgumentType.getString(context, "eventName");
                        boolean active = BoolArgumentType.getBool(context, "active");
                        
                        BlockConfig.toggleBlockEvent(eventName, active);
                        BlockConfig.saveActiveEventsState();
                        
                        return 1;
                    })
                )
            )
            // Drop chance event subcommand - /blockdrops event dropchance <true|false>
            .then(Commands.literal("dropchance")
                .then(Commands.argument("active", BoolArgumentType.bool())
                    .executes(context -> {
                        boolean active = BoolArgumentType.getBool(context, "active");
                        BlockConfig.toggleBlockDropChanceEvent(active);
                        BlockConfig.saveActiveEventsState();
                        return 1;
                    })
                )
            )
            // Double drops event subcommand - /blockdrops event doubledrops <true|false>
            .then(Commands.literal("doubledrops")
                .then(Commands.argument("active", BoolArgumentType.bool())
                    .executes(context -> {
                        boolean active = BoolArgumentType.getBool(context, "active");
                        BlockConfig.toggleBlockDoubleDrops(active);
                        BlockConfig.saveActiveEventsState();
                        return 1;
                    })
                )
            )
        );
        
        // Create custom event subcommand - /blockdrops create <eventName>
        rootCommand.then(Commands.literal("create")
            .then(Commands.argument("eventName", StringArgumentType.word())
                .executes(context -> {
                    String eventName = StringArgumentType.getString(context, "eventName");
                    boolean success = BlockConfig.createCustomEvent(eventName);
                    
                    if (success) {
                        context.getSource().sendSuccess(() -> 
                            Component.literal("§aCreated custom block event: §e" + eventName + 
                                "\n§aReload the configuration with §e/blockdrops reload§a to use it."), true);
                    } else {
                        context.getSource().sendFailure(
                            Component.literal("§cFailed to create custom event. It may already exist."));
                    }
                    
                    return success ? 1 : 0;
                })
            )
        );
        
        // List active events subcommand - /blockdrops active_events
        rootCommand.then(Commands.literal("active_events")
            .executes(context -> {
                // Get current states
                Set<String> activeEvents = BlockConfig.getActiveBlockEvents();
                boolean dropChanceActive = BlockConfig.isBlockDropChanceEventActive();
                boolean doubleDropsActive = BlockConfig.isBlockDoubleDropsActive();
                
                if (activeEvents.isEmpty() && !dropChanceActive && !doubleDropsActive) {
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§cNo active block events"), false);
                } else {
                    StringBuilder sb = new StringBuilder("§6Active block events: ");
                    
                    if (!activeEvents.isEmpty()) {
                        sb.append("§a").append(String.join("§6, §a", activeEvents));
                    }
                    
                    if (dropChanceActive) {
                        if (!activeEvents.isEmpty()) sb.append("§6, ");
                        sb.append("§e§ldropchance (2x drop rates)§r");
                    }
                    
                    if (doubleDropsActive) {
                        if (!activeEvents.isEmpty() || dropChanceActive) sb.append("§6, ");
                        sb.append("§e§ldoubledrops (2x amounts)§r");
                    }
                    
                    context.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                }
                return 1;
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
                    StringBuilder sb = new StringBuilder("§6Available block events: §a");
                    sb.append(String.join("§6, §a", availableEvents));
                    
                    context.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                }
                return 1;
            })
        );
        
        // Reload configuration subcommand - /blockdrops reload
        rootCommand.then(Commands.literal("reload")
            .executes(context -> {
                BlockConfig.loadConfig();
                context.getSource().sendSuccess(() -> 
                    Component.literal("§aReloaded block drops configuration"), true);
                return 1;
            })
        );
        
        // Register the command
        dispatcher.register(rootCommand);
    }
}
