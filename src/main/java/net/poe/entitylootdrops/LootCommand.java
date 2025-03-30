package net.poe.entitylootdrops;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class LootCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("entitylootdrops")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("reload")
                .executes(context -> {
                    LootConfig.loadConfig();
                    context.getSource().sendSuccess(() -> Component.literal("Configuration reloaded"), true);
                    return 1;
                })
            )
            .then(Commands.literal("dropchance")
                .then(Commands.literal("enable")
                    .executes(context -> {
                        context.getSource().sendSuccess(() -> LootConfig.toggleDropchance(true), true);
                        return 1;
                    })
                )
                .then(Commands.literal("disable")
                    .executes(context -> {
                        context.getSource().sendSuccess(() -> LootConfig.toggleDropchance(false), true);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("winter")
                .then(Commands.literal("enable")
                    .executes(context -> {
                        context.getSource().sendSuccess(() -> LootConfig.toggleWinterEvent(true), true);
                        return 1;
                    })
                )
                .then(Commands.literal("disable")
                    .executes(context -> {
                        context.getSource().sendSuccess(() -> LootConfig.toggleWinterEvent(false), true);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("status")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> LootConfig.getEventStatus(), false);
                    return 1;
                })
            );

        dispatcher.register(command);
    }
}
