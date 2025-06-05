package net.poe.entitylootdrops;

import java.io.File;

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
import net.poe.entitylootdrops.blockdrops.BlockConfig;
import net.poe.entitylootdrops.blockdrops.events.BlockEventHandler;
import net.poe.entitylootdrops.blockdrops.regeneration.BlockRegenerationManager;
import net.poe.entitylootdrops.config.ModConfig;
import net.poe.entitylootdrops.fishing.config.FishingConfig;
import net.poe.entitylootdrops.fishing.events.FishingEventHandler;
import net.poe.entitylootdrops.gui.ConfigScreen;
import net.poe.entitylootdrops.lootdrops.LootConfig;
import net.poe.entitylootdrops.lootdrops.events.LootEventHandler;
import net.poe.entitylootdrops.recipes.RecipeConfig;
import net.poe.entitylootdrops.recipes.events.RecipeEventHandler;
import net.poe.entitylootdrops.recipes.registration.RecipeRegistrationManager;

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

    /**
     * Recipe registration manager instance
     */
    private static RecipeRegistrationManager recipeRegistrationManager;

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
        
        // Register the block event handler to receive block-related events
        MinecraftForge.EVENT_BUS.register(BlockEventHandler.class);
        
        // Register the recipe event handler to receive crafting events
        MinecraftForge.EVENT_BUS.register(RecipeEventHandler.class);
        
        // Register the fishing event handler to receive fishing-related events
        MinecraftForge.EVENT_BUS.register(FishingEventHandler.class);
        LOGGER.info("Registered FishingEventHandler");
        
        // Register the setup event
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        
        LOGGER.info("Entity Loot Drops mod initializing...");
        
        // Load the initial configuration
        // This creates default files if they don't exist
        LootConfig.loadConfig(); // Uses the refactored LootConfig
        BlockConfig.loadConfig(); // Uses the refactored BlockConfig
        RecipeConfig.loadConfig(); // Uses the new simplified RecipeConfig
        
        // Initialize recipe registration manager
        recipeRegistrationManager = new RecipeRegistrationManager(RecipeConfig.getConfigManager());
        
        // Load fishing config
        try {
            FishingConfig.loadConfig(new File("config"));
            LOGGER.info("Loaded FishingConfig");
        } catch (Exception e) {
            LOGGER.error("Failed to load FishingConfig: {}", e.getMessage());
        }
        
        // Create all README files if ReadmeManager exists
        try {
            Class<?> readmeManagerClass = Class.forName("net.poe.entitylootdrops.ReadmeManager");
            readmeManagerClass.getMethod("createAllReadmeFiles").invoke(null);
            LOGGER.info("Created README files");
        } catch (ClassNotFoundException e) {
            LOGGER.debug("ReadmeManager not found - README files not created");
        } catch (Exception e) {
            LOGGER.warn("Failed to create README files: {}", e.getMessage());
        }
        
        LOGGER.info("Initial config load complete");
        
        // Register config screen on client side only
        ModConfig.setConfigLoaded(true);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                ConfigScreen.register();
                LOGGER.info("Registered config screen");
            } catch (Exception e) {
                LOGGER.warn("ConfigScreen not found or failed to register: {}", e.getMessage());
            }
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
        LOGGER.info("Server starting - reloading configurations...");
        
        // Reload configuration when the server starts
        // This ensures any changes made to the config files are applied
        LootConfig.loadConfig();
        BlockConfig.loadConfig();
        RecipeConfig.loadConfig();
        
        // Reload fishing config
        try {
            FishingConfig.loadConfig(new File("config"));
            LOGGER.info("Reloaded FishingConfig");
        } catch (Exception e) {
            LOGGER.error("Failed to reload FishingConfig: {}", e.getMessage());
        }

        // Load block regeneration data from disk
        try {
            BlockRegenerationManager.loadRegenerationData();
            int regeneratingCount = BlockRegenerationManager.getRegenerationCount();
            if (regeneratingCount > 0) {
                LOGGER.info("Loaded {} regenerating blocks from previous session", regeneratingCount);
            } else {
                LOGGER.info("No regenerating blocks found from previous session");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load block regeneration data: {}", e.getMessage());
        }
        
        // Log debug information about the loaded configuration
        LOGGER.info("Loaded {} normal drops", LootConfig.getNormalDrops().size());
        LOGGER.info("Loaded {} hostile drops", LootConfig.getNormalHostileDrops().size());
        LOGGER.info("Loaded {} event types", LootConfig.getEventDrops().size());
        LOGGER.info("Active events: {}", LootConfig.getActiveEvents());
        LOGGER.info("Loaded {} block drop types", BlockConfig.getAvailableBlockEvents().size());
        LOGGER.info("Active block events: {}", BlockConfig.getActiveBlockEvents());
        LOGGER.info("Loaded {} custom recipes", RecipeConfig.getAllRecipes().size());
        LOGGER.info("Loaded {} enabled custom recipes", RecipeConfig.getConfigManager().getEnabledRecipeCount());
        
        // Log fishing drops
        LOGGER.info("Loaded {} fishing drops", FishingConfig.getFishingDrops().size());
        LOGGER.info("Loaded {} global fishing rewards", FishingConfig.getGlobalFishingRewards().size());
        
        // Log block regeneration status
        int currentRegenerating = BlockRegenerationManager.getRegenerationCount();
        if (currentRegenerating > 0) {
            LOGGER.info("Currently tracking {} regenerating blocks", currentRegenerating);
        }
    }
    
    /**
     * Event handler for server stopping.
     * This is called when a Minecraft server (including integrated server) is stopping.
     * Saves the current event states and regeneration data to ensure they persist through restarts.
     * 
     * @param event The ServerStoppingEvent
     */
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Server stopping - saving configuration states...");
        
        // Save the current event states when the server stops
        // This ensures active events persist through server restarts
        try {
            LootConfig.saveActiveEventsState();
            LOGGER.debug("Saved loot drop event states");
        } catch (Exception e) {
            LOGGER.error("Failed to save loot drop event states: {}", e.getMessage());
        }
        
        try {
            BlockConfig.saveActiveEventsState();
            LOGGER.debug("Saved block drop event states");
        } catch (Exception e) {
            LOGGER.error("Failed to save block drop event states: {}", e.getMessage());
        }
        
        // Save block regeneration data to disk
        try {
            int regeneratingCount = BlockRegenerationManager.getRegenerationCount();
            if (regeneratingCount > 0) {
                LOGGER.info("Saving {} regenerating blocks to disk", regeneratingCount);
                BlockRegenerationManager.saveRegenerationData();
                LOGGER.info("Successfully saved regenerating blocks data");
            } else {
                LOGGER.debug("No regenerating blocks to save");
                // Still save to clear any old data
                BlockRegenerationManager.saveRegenerationData();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save block regeneration data: {}", e.getMessage());
        }
        
        LOGGER.info("Configuration states and regeneration data saved successfully");
    }

    /**
     * Get the recipe registration manager instance
     */
    public static RecipeRegistrationManager getRecipeRegistrationManager() {
        return recipeRegistrationManager;
    }
}
