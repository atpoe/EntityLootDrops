package net.poe.entitylootdrops;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.poe.entitylootdrops.config.ModConfig;
import net.poe.entitylootdrops.gui.ConfigScreen;

/**
 * Main mod class for the EntityLootDrops mod.
 * This is the entry point for the mod and handles initialization.
 */
@Mod(EntityLootDrops.MOD_ID)
public class EntityLootDrops {
    /**
     * The mod ID used for registration with Forge.
     * This should match the modid in the mods.toml file.
     */
    public static final String MOD_ID = "entitylootdrops";
    
    /**
     * Logger for this class.
     * Used to output information, warnings, and errors to the console and log file.
     */
    public static final Logger LOGGER = LogManager.getLogger();

    public static Logger getLogger() {
    return LOGGER;
}

    /**
     * Constructor for the mod class.
     * Called by Forge during mod initialization.
     * Registers event handlers and loads initial configuration.
     */
    public EntityLootDrops() {
        // Register this class to receive Forge events
        MinecraftForge.EVENT_BUS.register(this);
        
        // Register the loot event handler to receive entity drop events
        MinecraftForge.EVENT_BUS.register(LootEventHandler.class);
        
        // Register the setup event
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        
        LOGGER.info("Entity Loot Drops mod initializing...");
        
        // Load the initial configuration
        // This creates default files if they don't exist
        LootConfig.loadConfig();
        
        LOGGER.info("Initial config load complete");
        
        // Register config screen on client side only
        ModConfig.setConfigLoaded(true);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ConfigScreen.register();
        }
    }
    
    /**
     * Setup method called during mod initialization.
     * This is called after configs are loaded but before the game starts.
     * 
     * @param event The FMLCommonSetupEvent
     */
    private void setup(final FMLCommonSetupEvent event) {
        // Any setup that needs to happen after mod initialization
        LOGGER.info("Entity Loot Drops setup complete");
    }

    /**
     * Event handler for server starting.
     * This is called when a Minecraft server (including integrated server) is starting.
     * Reloads the configuration to ensure it's up to date.
     * 
     * @param event The ServerStartingEvent
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Server starting - reloading loot config...");
        
        // Reload configuration when the server starts
        // This ensures any changes made to the config files are applied
        LootConfig.loadConfig();
        
        // Log debug information about the loaded configuration
        LOGGER.info("Loaded {} normal drops", LootConfig.getNormalDrops().size());
        LOGGER.info("Loaded {} hostile drops", LootConfig.getNormalHostileDrops().size());
        LOGGER.info("Loaded {} event types", LootConfig.getEventDrops().size());
        LOGGER.info("Active events: {}", LootConfig.getActiveEvents());
    }
    
    /**
     * Event handler for server stopping.
     * This is called when a Minecraft server (including integrated server) is stopping.
     * Saves the current event states to ensure they persist through restarts.
     * 
     * @param event The ServerStoppingEvent
     */
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Server stopping - saving loot config state...");
        
        // Save the current event states when the server stops
        // This ensures active events persist through server restarts
        LootConfig.saveActiveEventsState();
    }
}
