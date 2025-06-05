package net.poe.entitylootdrops.blockdrops.regeneration;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.poe.entitylootdrops.EntityLootDrops;

/**
 * Handles the periodic processing of block regenerations.
 */
@Mod.EventBusSubscriber(modid = EntityLootDrops.MOD_ID)
public class BlockRegenerationTask {
    private static int tickCounter = 0;
    private static final int PROCESS_INTERVAL = 20; // Process every 20 ticks (1 second)
    
    /**
     * Processes block regenerations every second.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;
            if (tickCounter >= PROCESS_INTERVAL) {
                tickCounter = 0;
                BlockRegenerationManager.processRegenerations();
            }
        }
    }
}
