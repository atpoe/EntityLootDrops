package net.poe.entitylootdrops.lootdrops;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.poe.entitylootdrops.lootdrops.config.LootConfigLoader;
import net.poe.entitylootdrops.lootdrops.config.LootConfigManager;
import net.poe.entitylootdrops.lootdrops.events.LootEventManager;
import net.poe.entitylootdrops.lootdrops.model.CustomDropEntry;
import net.poe.entitylootdrops.lootdrops.model.EntityDropEntry;

/**
 * Main configuration class for the EntityLootDrops mod.
 * Acts as a facade for the loot drops system.
 */
public class LootConfig {
    private static final Logger LOGGER = LogManager.getLogger();
    
    private static LootConfigManager configManager;
    private static LootEventManager eventManager;
    private static LootConfigLoader configLoader;
    
    static {
        configManager = new LootConfigManager();
        eventManager = new LootEventManager();
        configLoader = new LootConfigLoader(configManager, eventManager);
    }
    
    /**
     * Main method to load all configuration files.
     */
    public static void loadConfig() {
        configLoader.loadConfig();
        LOGGER.info("Reloaded configuration: {} entity drop types, {} hostile drop types, {} active events", 
            configManager.getEntityDropsCount(), configManager.getHostileDropsCount(), 
            eventManager.getActiveEventsCount());
    }
    
    /**
     * Gets the normal (always active) entity-specific drops.
     */
    public static List<EntityDropEntry> getNormalDrops() {
        return configManager.getNormalDrops();
    }
    
    /**
     * Gets the normal (always active) hostile mob drops.
     */
    public static List<CustomDropEntry> getNormalHostileDrops() {
        return configManager.getNormalHostileDrops();
    }
    
    /**
     * Gets all event-specific entity drops.
     */
    public static Map<String, List<EntityDropEntry>> getEventDrops() {
        return configManager.getEventDrops();
    }
    
    /**
     * Gets the hostile mob drops for a specific event.
     */
    public static List<CustomDropEntry> getEventHostileDrops(String eventName) {
        return configManager.getEventHostileDrops(eventName);
    }
    
    /**
     * Checks if an event is currently active.
     */
    public static boolean isEventActive(String eventName) {
        return eventManager.isEventActive(eventName);
    }
    
    /**
     * Enables or disables an event.
     */
    public static void toggleEvent(String eventName, boolean active) {
        eventManager.toggleEvent(eventName, active, configManager.getAvailableEvents());
    }
    
    /**
     * Enables or disables the drop chance event.
     */
    public static void toggleDropChanceEvent(boolean active) {
        eventManager.toggleDropChanceEvent(active);
    }
    
    /**
     * Enables or disables the double drops event.
     */
    public static void toggleDoubleDrops(boolean active) {
        eventManager.toggleDoubleDrops(active);
    }
    
    /**
     * Clears all active events.
     */
    public static void clearActiveEvents() {
        eventManager.clearActiveEvents();
    }
    
    /**
     * Gets all currently active events.
     */
    public static Set<String> getActiveEvents() {
        return eventManager.getActiveEvents();
    }
    
    /**
     * Gets all available event names.
     */
    public static Set<String> getAllEventNames() {
        return configManager.getAllEventNames();
    }
    
    /**
     * Checks if the drop chance event is active.
     */
    public static boolean isDropChanceEventActive() {
        return eventManager.isDropChanceEventActive();
    }
    
    /**
     * Checks if the double drops event is active.
     */
    public static boolean isDoubleDropsActive() {
        return eventManager.isDoubleDropsActive();
    }
    
    /**
     * Checks if debug logging is enabled.
     */
    public static boolean isDebugLoggingEnabled() {
        return eventManager.isDebugLoggingEnabled();
    }
    
    /**
     * Enables or disables debug logging.
     */
    public static void setDebugLogging(boolean enabled) {
        eventManager.setDebugLogging(enabled);
    }
    
    /**
     * Saves the current state of active events.
     */
    public static void saveActiveEventsState() {
        eventManager.saveActiveEventsState();
    }
    
    // Message management methods
    public static void setEventEnableMessage(String eventName, String message) {
        eventManager.setEventEnableMessage(eventName, message);
    }
    
    public static void setEventDisableMessage(String eventName, String message) {
        eventManager.setEventDisableMessage(eventName, message);
    }
    
    public static String getEventEnableMessage(String eventName) {
        return eventManager.getEventEnableMessage(eventName);
    }
    
    public static String getEventDisableMessage(String eventName) {
        return eventManager.getEventDisableMessage(eventName);
    }
    
    public static void setDropChanceEnableMessage(String message) {
        eventManager.setDropChanceEnableMessage(message);
    }
    
    public static void setDropChanceDisableMessage(String message) {
        eventManager.setDropChanceDisableMessage(message);
    }
    
    public static String getDropChanceEnableMessage() {
        return eventManager.getDropChanceEnableMessage();
    }
    
    public static String getDropChanceDisableMessage() {
        return eventManager.getDropChanceDisableMessage();
    }
    
    public static void setDoubleDropsEnableMessage(String message) {
        eventManager.setDoubleDropsEnableMessage(message);
    }
    
    public static void setDoubleDropsDisableMessage(String message) {
        eventManager.setDoubleDropsDisableMessage(message);
    }
    
    public static String getDoubleDropsEnableMessage() {
        return eventManager.getDoubleDropsEnableMessage();
    }
    
    public static String getDoubleDropsDisableMessage() {
        return eventManager.getDoubleDropsDisableMessage();
    }
}
