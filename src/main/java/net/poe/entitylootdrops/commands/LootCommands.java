package net.poe.entitylootdrops.commands;

import java.util.List;
import java.util.Set;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

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
import net.poe.entitylootdrops.config.ModConfig;
import net.poe.entitylootdrops.lootdrops.LootConfig;
import net.poe.entitylootdrops.lootdrops.events.EventDropCountManager;

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
                            for (String eventName : LootConfig.getAllEventNames()) {
                                builder.suggest(eventName);
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("active", BoolArgumentType.bool())
                                .executes(context -> {
                                    // Extract command arguments
                                    String eventName = StringArgumentType.getString(context, "eventName");
                                    boolean active = BoolArgumentType.getBool(context, "active");

                                    // Check against all event names, not just loaded ones
                                    boolean eventExists = false;
                                    for (String availableEvent : LootConfig.getAllEventNames()) {
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
                                    LootConfig.toggleEvent(eventName, active);

                                    // Sync changes to Forge config
                                    boolean syncSuccess = ModConfig.syncFromLootConfig();
                                    if (!syncSuccess && ModConfig.isConfigLoaded()) {
                                        // Only show warning if config should be loaded by now
                                        context.getSource().sendFailure(Component.literal("Warning: Config sync failed. Changes may not persist after restart."));
                                    }

                                    // Send success message
                                    String statusMessage = active ? "§aEnabled" : "§cDisabled";
                                    context.getSource().sendSuccess(() ->
                                            Component.literal(statusMessage + " event: §e" + eventName), true);

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

                                    // Send success message
                                    String statusMessage = active ? "§aEnabled" : "§cDisabled";
                                    context.getSource().sendSuccess(() ->
                                            Component.literal(statusMessage + " drop chance event (2x drop rates)"), true);

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

                                    // Send success message
                                    String statusMessage = active ? "§aEnabled" : "§cDisabled";
                                    context.getSource().sendSuccess(() ->
                                            Component.literal(statusMessage + " double drops event (2x amounts)"), true);

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

        // DROP COUNT COMMANDS - UPDATED FOR PER-EVENT SYSTEM

        // Combined top players across ALL events - /lootdrops alltop [count]
        rootCommand.then(Commands.literal("alltop")
                .executes(context -> {
                    return showAllEventsTopPlayers(context, 5);
                })
                .then(Commands.argument("count", IntegerArgumentType.integer(1, 50))
                        .executes(context -> {
                            int count = IntegerArgumentType.getInteger(context, "count");
                            return showAllEventsTopPlayers(context, count);
                        })
                )
        );

        // Event-specific top players - /lootdrops eventtop <eventName> [count]
        rootCommand.then(Commands.literal("eventtop")
                .then(Commands.argument("eventName", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            // Suggest events that have drop count data
                            for (String eventName : EventDropCountManager.getEventsWithDropCounts()) {
                                builder.suggest(eventName);
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            String eventName = StringArgumentType.getString(context, "eventName");
                            return showEventTopPlayers(context, eventName, 5);
                        })
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 50))
                                .executes(context -> {
                                    String eventName = StringArgumentType.getString(context, "eventName");
                                    int count = IntegerArgumentType.getInteger(context, "count");
                                    return showEventTopPlayers(context, eventName, count);
                                })
                        )
                )
        );

        // Player stats subcommand - /lootdrops playerstats <player>
        rootCommand.then(Commands.literal("playerstats")
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            // Suggest online player names
                            if (context.getSource().getServer() != null) {
                                for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                                    builder.suggest(player.getName().getString());
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            String playerName = StringArgumentType.getString(context, "player");
                            return showPlayerStats(context, playerName);
                        })
                )
        );

        // Reset drop counts subcommand - /lootdrops reset_counts
        rootCommand.then(Commands.literal("reset_counts")
                .executes(context -> {
                    EventDropCountManager.resetAllEventDropCounts();
                    context.getSource().sendSuccess(() ->
                            Component.literal("§aReset all drop counts for all events!"), true);
                    return 1;
                })
        );

        // Debug info command - /lootdrops debuginfo
        rootCommand.then(Commands.literal("debuginfo")
                .executes(context -> {
                    context.getSource().sendSuccess(() ->
                            Component.literal("§6=== Drop Count Debug Info ==="), false);

                    // Check if drop counting is enabled
                    boolean enabled = LootConfig.isDropCountEnabled();
                    context.getSource().sendSuccess(() ->
                            Component.literal("§eGlobal Drop Counting: " + (enabled ? "§aEnabled" : "§cDisabled")), false);

                    // Check active events
                    Set<String> activeEvents = LootConfig.getActiveEvents();
                    context.getSource().sendSuccess(() ->
                            Component.literal("§eActive Events: §f" + String.join(", ", activeEvents)), false);

                    // List events with drop count files
                    List<String> eventsWithCounts = EventDropCountManager.getEventsWithDropCounts();
                    context.getSource().sendSuccess(() ->
                            Component.literal("§eEvents with Drop_Count.json: §f" + String.join(", ", eventsWithCounts)), false);

                    return 1;
                })
        );

        // List active events subcommand - /lootdrops active_events
        rootCommand.then(Commands.literal("active_events")
                .executes(context -> {
                    // Get the current state of all events
                    Set<String> activeEvents = LootConfig.getActiveEvents();
                    boolean dropChanceActive = LootConfig.isDropChanceEventActive();
                    boolean doubleDropsActive = LootConfig.isDoubleDropsActive();
                    boolean debugLoggingActive = LootConfig.isDebugLoggingEnabled();
                    boolean dropCountActive = LootConfig.isDropCountEnabled();

                    // If no events are active and debug logging is off, show a message
                    if (activeEvents.isEmpty() && !dropChanceActive && !doubleDropsActive && !debugLoggingActive && !dropCountActive) {
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

                        // Add drop count if active
                        if (dropCountActive) {
                            if (!activeEvents.isEmpty() || dropChanceActive || doubleDropsActive || debugLoggingActive) {
                                sb.append("§6, ");
                            }
                            sb.append("§d§ldrop counting§r");
                        }

                        // Send the formatted message to the command sender
                        context.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                    }
                    return 1; // Command succeeded
                })
        );

        // List all available events subcommand - /lootdrops listall
        rootCommand.then(Commands.literal("listall")
                .executes(context -> {
                    Set<String> availableEvents = LootConfig.getAllEventNames();

                    if (availableEvents.isEmpty()) {
                        context.getSource().sendSuccess(() ->
                                Component.literal("§cNo events available"), false);
                    } else {
                        // Build a formatted message with all available events
                        StringBuilder sb = new StringBuilder("§6Available events: §a");
                        sb.append(String.join("§6, §a", availableEvents));

                        // Send the formatted message to the command sender
                        context.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                    }
                    return 1; // Command succeeded
                })
        );

        // Open config screen subcommand - /lootdrops openconfig
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
                                    .withUnderlined(true)
                                    .withColor(net.minecraft.ChatFormatting.GREEN)
                            )
                    );

                    return 1; // Command succeeded
                })
        );

        // Reload configuration subcommand - /lootdrops reload
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

    // HELPER METHODS FOR DROP COUNT COMMANDS

    /**
     * Shows combined top players across all events.
     */
    private static int showAllEventsTopPlayers(CommandContext<CommandSourceStack> context, int count) {
        List<EventDropCountManager.CombinedPlayerDropCount> topPlayers =
                EventDropCountManager.getCombinedTopPlayers(count);

        if (topPlayers.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cNo drop count data available across any events"));
            return 0;
        }

        // Header message
        context.getSource().sendSuccess(() ->
                Component.literal("§6=== Top " + Math.min(count, topPlayers.size()) + " Players (All Events) ==="), false);

        int rank = 1;
        for (EventDropCountManager.CombinedPlayerDropCount player : topPlayers) {
            // Create clickable player name for detailed stats
            Component message = Component.literal(String.format("§e%d. ", rank))
                    .append(Component.literal("§a" + player.getPlayerName())
                            .withStyle(style -> style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lootdrops playerstats " + player.getPlayerName()))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                            Component.literal("Click to view " + player.getPlayerName() + "'s detailed stats")))
                                    .withUnderlined(true)
                            )
                    )
                    .append(Component.literal(String.format("§f: §b%d §7total drops", player.getTotalDrops())));

            context.getSource().sendSuccess(() -> message, false);
            rank++;
        }

        // Footer message
        context.getSource().sendSuccess(() ->
                Component.literal("§7§o(Click player names for detailed stats)"), false);

        return 1;
    }

    /**
     * Shows top players for a specific event.
     */
    private static int showEventTopPlayers(CommandContext<CommandSourceStack> context, String eventName, int count) {
        List<EventDropCountManager.EventPlayerDropCount> topPlayers =
                EventDropCountManager.getEventTopPlayers(eventName, count);

        if (topPlayers.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cNo drop count data for event: " + eventName));
            return 0;
        }

        // Header message
        context.getSource().sendSuccess(() ->
                Component.literal("§6=== Top " + Math.min(count, topPlayers.size()) + " Players (" + eventName + ") ==="), false);

        // Display top players
        for (int i = 0; i < topPlayers.size(); i++) {
            var player = topPlayers.get(i);

            // Create clickable player name
            Component message = Component.literal(String.format("§e%d. ", i + 1))
                    .append(Component.literal("§a" + player.getPlayerName())
                            .withStyle(style -> style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lootdrops playerstats " + player.getPlayerName()))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                            Component.literal("Click to view " + player.getPlayerName() + "'s stats for " + eventName)))
                                    .withUnderlined(true)
                            )
                    )
                    .append(Component.literal(String.format("§f: §b%d §7drops", player.getTotalEventDrops())));

            context.getSource().sendSuccess(() -> message, false);
        }

        return 1;
    }

    /**
     * Shows detailed stats for a specific player across all events.
     */
    private static int showPlayerStats(CommandContext<CommandSourceStack> context, String playerName) {
        // Get all events with drop counts
        List<String> events = EventDropCountManager.getEventsWithDropCounts();

        if (events.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cNo events with drop counting found"));
            return 0;
        }

        boolean foundData = false;
        int totalDropsAcrossEvents = 0;

        context.getSource().sendSuccess(() ->
                Component.literal("§6=== " + playerName + " Drop Stats ==="), false);

        // Check each event for this player's data
        for (String eventName : events) {
            EventDropCountManager.EventPlayerDropCount playerData =
                    EventDropCountManager.getPlayerEventDropCount(eventName, playerName);

            if (playerData != null && playerData.getTotalEventDrops() > 0) {
                foundData = true;
                totalDropsAcrossEvents += playerData.getTotalEventDrops();

                context.getSource().sendSuccess(() ->
                        Component.literal("§e" + eventName + "§f: §b" + playerData.getTotalEventDrops() + " §7drops"), false);

                // Show top 3 items for this event
                playerData.getItemCounts().entrySet().stream()
                        .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                        .limit(3)
                        .forEach(entry -> {
                            context.getSource().sendSuccess(() ->
                                    Component.literal("  §7• §a" + entry.getKey() + "§f: §b" + entry.getValue()), false);
                        });
            }
        }

        if (!foundData) {
            context.getSource().sendFailure(Component.literal("§cNo drop count data found for player: " + playerName));
            return 0;
        }

        // Show total across all events
        final int finalTotalDropsAcrossEvents = totalDropsAcrossEvents;
        context.getSource().sendSuccess(() ->
                Component.literal("§6Total Across All Events: §b" + finalTotalDropsAcrossEvents + " §7drops"), false);

        return 1;
    }
}