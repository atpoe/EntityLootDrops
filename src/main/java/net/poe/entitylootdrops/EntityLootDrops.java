package net.poe.entitylootdrops;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(EntityLootDrops.MOD_ID)
public class EntityLootDrops {
    public static final String MOD_ID = "entitylootdrops";
    private static final Logger LOGGER = LogManager.getLogger();

    public EntityLootDrops() {
        MinecraftForge.EVENT_BUS.register(this);
        LootConfig.loadConfig();
        LOGGER.info("Entity Loot Drops mod initialized");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LootConfig.loadConfig();
        LOGGER.info("Entity Loot Drops configuration loaded");
    }
}
