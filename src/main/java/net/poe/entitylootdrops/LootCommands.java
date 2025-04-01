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

@Mod.EventBusSubscriber(modid = EntityLootDrops.MOD_ID)
public class LootCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        // Main command
        LiteralArgumentBuilder<CommandSourceStack> rootCommand = Commands.literal("lootdrops")
            .requires(source -> source.hasPermission(2)); // Requires permission level 2 (op)
        
        // Event toggle subcommand
        rootCommand.then(Commands.literal("event")
            .then(Commands.argument("eventName", StringArgumentType.string())
                .suggests((context, builder) -> {
                    // Suggest available events
                    for (String eventName : LootConfig.getEventDrops().keySet()) {
                        builder.suggest(eventName);
                    }
                    return builder.buildFuture();
                })
                .then(Commands.argument("active", BoolArgumentType.bool())
                    .executes(context -> {
                        String eventName = StringArgumentType.getString(context, "eventName");
                        boolean active = BoolArgumentType.getBool(context, "active");
                        
                        if (!LootConfig.getEventDrops().containsKey(eventName)) {
                            context.getSource().sendFailure(Component.literal("Unknown event: " + eventName));
                            return 0;
                        }
                        
                        LootConfig.toggleEvent(eventName, active);
                        
                        if (active) {
                            context.getSource().sendSuccess(() -> 
                                Component.literal("§aEnabled §6" + eventName + "§a event drops"), true);
                        } else {
                            context.getSource().sendSuccess(() -> 
                                Component.literal("§cDisabled §6" + eventName + "§c event drops"), true);
                        }
                        
                        return 1;
                    })
                )
            )
            // Add dropchance under event command
            .then(Commands.literal("dropchance")
                .then(Commands.argument("active", BoolArgumentType.bool())
                    .executes(context -> {
                        boolean active = BoolArgumentType.getBool(context, "active");
                        LootConfig.toggleDropChanceEvent(active);
                        
                        if (active) {
                            context.getSource().sendSuccess(() -> 
                                Component.literal("§aEnabled drop chance bonus event - §eAll drop chances are now DOUBLED!"), true);
                        } else {
                            context.getSource().sendSuccess(() -> 
                                Component.literal("§cDisabled drop chance bonus event - Drop chances returned to normal"), true);
                        }
                        
                        return 1;
                    })
                )
            )
            // Add doubledrops under event command
            .then(Commands.literal("doubledrops")
                .then(Commands.argument("active", BoolArgumentType.bool())
                    .executes(context -> {
                        boolean active = BoolArgumentType.getBool(context, "active");
                        LootConfig.toggleDoubleDrops(active);
                        
                        if (active) {
                            context.getSource().sendSuccess(() -> 
                                Component.literal("§aEnabled double drops event - §eAll mob drops are now DOUBLED!"), true);
                        } else {
                            context.getSource().sendSuccess(() -> 
                                Component.literal("§cDisabled double drops event - Drop amounts returned to normal"), true);
                        }
                        
                        return 1;
                    })
                )
            )
        );
        
        // List active events subcommand
        rootCommand.then(Commands.literal("list")
            .executes(context -> {
                Set<String> activeEvents = LootConfig.getActiveEvents();
                boolean dropChanceActive = LootConfig.isDropChanceEventActive();
                boolean doubleDropsActive = LootConfig.isDoubleDropsActive();
                
                if (activeEvents.isEmpty() && !dropChanceActive && !doubleDropsActive) {
                    context.getSource().sendSuccess(() -> 
                        Component.literal("§cNo active events"), false);
                } else {
                    StringBuilder sb = new StringBuilder("§6Active events: ");
                    if (!activeEvents.isEmpty()) {
                        sb.append("§a").append(String.join("§6, §a", activeEvents));
                    }
                    
                    if (dropChanceActive) {
                        if (!activeEvents.isEmpty()) {
                            sb.append("§6, ");
                        }
                        sb.append("§e§ldropchance (2x drop rates)§r");
                    }
                    
                    if (doubleDropsActive) {
                        if (!activeEvents.isEmpty() || dropChanceActive) {
                            sb.append("§6, ");
                        }
                        sb.append("§e§ldoubledrops (2x amounts)§r");
                    }
                    
                    context.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                }
                
                return 1;
            })
        );
        
        // Reload configuration subcommand
        rootCommand.then(Commands.literal("reload")
            .executes(context -> {
                LootConfig.loadConfig();
                context.getSource().sendSuccess(() -> 
                    Component.literal("§aReloaded Entity Loot Drops configuration"), true);
                return 1;
            })
        );
        
        dispatcher.register(rootCommand);
    }
}

