package net.poe.entitylootdrops.commands;

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

@Mod.EventBusSubscriber(modid = EntityLootDrops.MOD_ID)
public class BlockEventCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        LiteralArgumentBuilder<CommandSourceStack> rootCommand = Commands.literal("blockdrops")
                .requires(source -> source.hasPermission(2));

        // /blockdrops event <eventName> <true|false>
        rootCommand.then(Commands.literal("event")
                .then(Commands.argument("eventName", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            for (String eventName : BlockConfig.getAvailableBlockEvents()) {
                                builder.suggest(eventName);
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("active", BoolArgumentType.bool())
                                .executes(context -> {
                                    String eventName = StringArgumentType.getString(context, "eventName");
                                    boolean active = BoolArgumentType.getBool(context, "active");
                                    boolean eventExists = false;
                                    for (String availableEvent : BlockConfig.getAvailableBlockEvents()) {
                                        if (availableEvent.equalsIgnoreCase(eventName)) {
                                            eventExists = true;
                                            break;
                                        }
                                    }
                                    if (!eventExists) {
                                        context.getSource().sendFailure(Component.literal("Unknown event: " + eventName));
                                        return 0;
                                    }
                                    BlockConfig.toggleBlockEvent(eventName, active);

                                    // Get custom message from messages.json
                                    String customMessage = BlockConfig.getToggleMessage(eventName, active);
                                    context.getSource().sendSuccess(() ->
                                            Component.literal(customMessage), true);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("dropchance")
                        .then(Commands.argument("active", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean active = BoolArgumentType.getBool(context, "active");
                                    BlockConfig.toggleBlockDropChanceEvent(active);

                                    // Get custom message from messages.json
                                    String customMessage = BlockConfig.getToggleMessage("dropchance", active);
                                    context.getSource().sendSuccess(() ->
                                            Component.literal(customMessage), true);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("doubledrops")
                        .then(Commands.argument("active", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean active = BoolArgumentType.getBool(context, "active");
                                    BlockConfig.toggleBlockDoubleDrops(active);

                                    // Get custom message from messages.json
                                    String customMessage = BlockConfig.getToggleMessage("doubledrops", active);
                                    context.getSource().sendSuccess(() ->
                                            Component.literal(customMessage), true);
                                    return 1;
                                })
                        )
                )
        );

        // /blockdrops active_events
        rootCommand.then(Commands.literal("active_events")
                .executes(context -> {
                    Set<String> activeEvents = BlockConfig.getActiveBlockEvents();
                    boolean dropChanceActive = BlockConfig.isBlockDropChanceEventActive();
                    boolean doubleDropsActive = BlockConfig.isBlockDoubleDropsActive();
                    int regeneratingBlocks = BlockRegenerationManager.getRegenerationCount();

                    if (activeEvents.isEmpty() && !dropChanceActive && !doubleDropsActive) {
                        context.getSource().sendSuccess(() ->
                                Component.literal("§cNo active block events or features"), false);
                    } else {
                        StringBuilder sb = new StringBuilder("§6Active block events and features: ");
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
                    if (regeneratingBlocks > 0) {
                        context.getSource().sendSuccess(() ->
                                Component.literal("§6Currently regenerating: §e" + regeneratingBlocks + " §6blocks"), false);
                    }
                    return 1;
                })
        );

        // /blockdrops listall
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

        // /blockdrops regeneration <list|clear|info|force>
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
                .then(Commands.literal("force")
                        .executes(context -> {
                            if (net.poe.entitylootdrops.blockdrops.regeneration.ForceRegenerationTask.isRunning()) {
                                context.getSource().sendFailure(Component.literal("§cA force regeneration is already in progress!"));
                                return 0;
                            }
                            int count = BlockRegenerationManager.getRegenerationCount();
                            if (count == 0) {
                                context.getSource().sendSuccess(() ->
                                        Component.literal("§6No regenerating blocks to force"), false);
                                return 1;
                            }
                            net.poe.entitylootdrops.blockdrops.regeneration.ForceRegenerationTask.start();
                            context.getSource().sendSuccess(() ->
                                    Component.literal("§aForce regeneration started for §e" + count + " §ablocks (1 per tick)"), true);
                            return 1;
                        })
                )
        );

        // /blockdrops openconfig
        rootCommand.then(Commands.literal("openconfig")
                .executes(context -> {
                    if (!(context.getSource().getEntity() instanceof ServerPlayer)) {
                        context.getSource().sendFailure(Component.literal("This command can only be used by a player"));
                        return 0;
                    }
                    ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
                    context.getSource().sendSuccess(() ->
                            Component.literal("§aOpening block configuration screen..."), false);
                    player.sendSystemMessage(Component.literal("§a[EntityLootDrops] §eClick here to open block config")
                            .withStyle(style -> style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/blockdrops_openconfig_client"))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                            Component.literal("Click to open block configuration screen")))
                                    .withUnderlined(true)
                                    .withColor(net.minecraft.ChatFormatting.GREEN)
                            )
                    );
                    return 1;
                })
        );

        // /blockdrops reload
        rootCommand.then(Commands.literal("reload")
                .executes(context -> {
                    BlockConfig.loadConfig();
                    int regeneratingCount = BlockRegenerationManager.getRegenerationCount();
                    context.getSource().sendSuccess(() ->
                            Component.literal("§aReloaded Entity Loot Drops block configuration"), true);
                    if (regeneratingCount > 0) {
                        context.getSource().sendSuccess(() ->
                                Component.literal("§6Note: " + regeneratingCount + " blocks are still regenerating"), false);
                    }
                    return 1;
                })
        );

        // /blockdrops debug
        rootCommand.then(Commands.literal("debug")
                .executes(context -> {
                    context.getSource().sendSuccess(() ->
                            Component.literal("§6=== Block Drops Debug Information ==="), false);
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
                    int regeneratingCount = BlockRegenerationManager.getRegenerationCount();
                    context.getSource().sendSuccess(() ->
                            Component.literal("§e- Regenerating Blocks: §a" + regeneratingCount), false);
                    context.getSource().sendSuccess(() ->
                            Component.literal("§e- Normal Drops: §a" + BlockConfig.getNormalDrops().size()), false);
                    return 1;
                })
        );

        // /blockdrops help
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
                            Component.literal("§e/blockdrops regeneration force §7- Force all regenerations (1 per tick)"), false);
                    context.getSource().sendSuccess(() ->
                            Component.literal("§e/blockdrops reload §7- Reload configuration"), false);
                    context.getSource().sendSuccess(() ->
                            Component.literal("§e/blockdrops debug §7- Show debug information"), false);
                    context.getSource().sendSuccess(() ->
                            Component.literal("§e/blockdrops help §7- Show this help message"), false);
                    context.getSource().sendSuccess(() ->
                            Component.literal("§7All commands require OP permission level 2"), false);
                    return 1;
                })
        );

        dispatcher.register(rootCommand);
    }
}