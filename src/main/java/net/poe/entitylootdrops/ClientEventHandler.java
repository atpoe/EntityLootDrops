package net.poe.entitylootdrops;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.poe.entitylootdrops.gui.ConfigScreen;

/**
 * Handles client-side events for the EntityLootDrops mod.
 */
@Mod.EventBusSubscriber(modid = EntityLootDrops.MOD_ID, value = Dist.CLIENT)
public class ClientEventHandler {

    /**
     * Event handler for client chat messages.
     * Used to intercept the config screen command.
     * 
     * @param event The ClientChatEvent
     */
    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {
        if (event.getMessage().equals("/lootdrops_openconfig_client")) {
            // Cancel the command to prevent it from being sent to the server
            event.setCanceled(true);
            
            // Schedule the config screen to open on the next tick
            Minecraft.getInstance().execute(() -> {
                Minecraft.getInstance().setScreen(new ConfigScreen(Minecraft.getInstance().screen));
            });
        }
    }
}
