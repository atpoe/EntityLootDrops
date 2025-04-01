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
        MinecraftForge.EVENT_BUS.register(LootEventHandler.class);
        LOGGER.info("Entity Loot Drops mod initializing...");
        
        // Initial config load
        LootConfig.loadConfig();
        LOGGER.info("Initial config load complete");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Server starting - reloading loot config...");
        LootConfig.loadConfig();
        
        // Debug info
        LOGGER.info("Loaded {} normal drops", LootConfig.getNormalDrops().size());
        LOGGER.info("Loaded {} hostile drops", LootConfig.getNormalHostileDrops().size());
        LOGGER.info("Loaded {} event types", LootConfig.getEventDrops().size());
        LOGGER.info("Active events: {}", LootConfig.getActiveEvents());
    }
}
