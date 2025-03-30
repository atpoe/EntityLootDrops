package net.poe.entitylootdrops;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("entitylootdrops")
public class EntityLootDrops {
    private static final Logger LOGGER = LogManager.getLogger();
    
    public EntityLootDrops() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        
        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new LootEventHandler());
    }
    
    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("Entity Loot Drops mod initializing");
        // Load config during setup
        event.enqueueWork(() -> {
            LootConfig.loadConfig();
        });
    }
    
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LootCommand.register(event.getDispatcher());
        LOGGER.info("Entity Loot Drops commands registered");
    }
}
