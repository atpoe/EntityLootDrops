package net.poe.entitylootdrops.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.poe.entitylootdrops.EntityLootDrops;
import net.poe.entitylootdrops.adventure.AdventureModeConfigLoader;

@Mod.EventBusSubscriber(modid = EntityLootDrops.MOD_ID)
public class AdventureModeCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("adventuremode")
                .requires(source -> source.hasPermission(2));

        root.then(Commands.literal("reload")
                .executes(context -> {
                    AdventureModeConfigLoader.loadConfig(java.nio.file.Paths.get("config/EntityLootDrops"));
                    context.getSource().sendSuccess(() ->
                            Component.literal("§aReloaded Adventure Mode configuration"), true);
                    return 1;
                })
        );

        dispatcher.register(root);
    }
}