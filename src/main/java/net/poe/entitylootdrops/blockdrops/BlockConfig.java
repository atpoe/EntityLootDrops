package net.poe.entitylootdrops.blockdrops;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.poe.entitylootdrops.blockdrops.config.BlockConfigLoader;
import net.poe.entitylootdrops.blockdrops.config.BlockConfigManager;
import net.poe.entitylootdrops.blockdrops.events.BlockEventManager;
import net.poe.entitylootdrops.blockdrops.model.BlockDropEntry;
import net.poe.entitylootdrops.blockdrops.model.CustomBlockDropEntry;

/**
 * Main configuration class for block drops in the EntityLootDrops mod.
 * Acts as a facade for the block drops system.
 */
public class BlockConfig {
    private static final Logger LOGGER = LogManager.getLogger();
    
    private static BlockConfigManager configManager;
    private static BlockEventManager eventManager;
    private static BlockConfigLoader configLoader;
    
    // Map to store normal (non-event) drops by block ID
    private static final Map<String, List<BlockDropEntry>> normalDrops = new HashMap<>();
    
    // Map to store event drops by event name and block ID
    private static final Map<String, Map<String, List<BlockDropEntry>>> eventDrops = new HashMap<>();
    
    static {
        configManager = new BlockConfigManager();
        eventManager = new BlockEventManager();
        configLoader = new BlockConfigLoader(configManager, eventManager);
    }
    
    /**
     * Loads all block drop configurations.
     */
    public static void loadConfig() {
        configLoader.loadConfig();
        LOGGER.info("Loaded block configuration: {} block types, {} global drops, {} active events",
            configManager.getBlockDropsCount(), configManager.getGlobalDropsCount(), 
            eventManager.getActiveEventsCount());
    }
    
    /**
     * Gets all normal (non-event) drops for a specific block.
     */
    public static List<BlockDropEntry> getNormalDrops(String blockId) {
        return normalDrops.getOrDefault(blockId, new ArrayList<>());
    }
    
    /**
     * Gets all normal drops across all blocks.
     */
    public static List<BlockDropEntry> getNormalDrops() {
        List<BlockDropEntry> allDrops = new ArrayList<>();
        normalDrops.values().forEach(allDrops::addAll);
        return allDrops;
    }
    
    /**
     * Gets event-specific drops for a given block in a specific event.
     */
    public static List<BlockDropEntry> getEventDrops(String eventName, String blockId) {
        Map<String, List<BlockDropEntry>> eventBlockDrops = eventDrops.get(eventName);
        if (eventBlockDrops != null) {
            return eventBlockDrops.getOrDefault(blockId, new ArrayList<>());
        }
        return new ArrayList<>();
    }
    
    /**
     * Gets all drops for a specific event.
     */
    public static List<BlockDropEntry> getEventDrops(String eventName) {
        List<BlockDropEntry> allEventDrops = new ArrayList<>();
        Map<String, List<BlockDropEntry>> eventBlockDrops = eventDrops.get(eventName);
        if (eventBlockDrops != null) {
            eventBlockDrops.values().forEach(allEventDrops::addAll);
        }
        return allEventDrops;
    }
    
    /**
     * Gets all applicable drops for a block, including normal and active event drops.
     */
    public static List<BlockDropEntry> getApplicableDrops(String blockId) {
        List<BlockDropEntry> applicableDrops = new ArrayList<>();
        
        // Add normal drops
        applicableDrops.addAll(getNormalDrops(blockId));
        
        // Add drops from active events
        for (String eventName : getActiveBlockEvents()) {
            applicableDrops.addAll(getEventDrops(eventName, blockId));
        }
        
        return applicableDrops;
    }
    
    /**
     * Gets all global block drops.
     */
    public static List<CustomBlockDropEntry> getGlobalBlockDrops() {
        return configManager.getGlobalBlockDrops();
    }
    
    /**
     * Gets block-specific drops for a given block ID.
     */
    public static List<BlockDropEntry> getBlockDrops(String blockId) {
        return configManager.getBlockDrops(blockId);
    }
    
    /**
     * Gets all available block events.
     */
    public static Set<String> getAvailableBlockEvents() {
        return configManager.getAvailableBlockEvents();
    }
    
    /**
     * Gets all currently active block events.
     */
    public static Set<String> getActiveBlockEvents() {
        return eventManager.getActiveBlockEvents();
    }
    
    /**
     * Toggles a block event on or off.
     */
    public static void toggleBlockEvent(String eventName, boolean active) {
        eventManager.toggleBlockEvent(eventName, active, configManager.getAvailableBlockEvents());
    }
    
    /**
     * Toggles the block drop chance event.
     */
    public static void toggleBlockDropChanceEvent(boolean active) {
        eventManager.toggleBlockDropChanceEvent(active);
    }
    
    /**
     * Toggles the block double drops event.
     */
    public static void toggleBlockDoubleDrops(boolean active) {
        eventManager.toggleBlockDoubleDrops(active);
    }
    
    /**
     * Checks if the block drop chance event is active.
     */
    public static boolean isBlockDropChanceEventActive() {
        return eventManager.isBlockDropChanceEventActive();
    }
    
    /**
     * Checks if the block double drops event is active.
     */
    public static boolean isBlockDoubleDropsActive() {
        return eventManager.isBlockDoubleDropsActive();
    }
    
    /**
     * Creates a new custom event directory.
     */
    public static boolean createCustomEvent(String eventName) {
        return configLoader.createCustomEvent(eventName);
    }
    
    /**
     * Saves the current state of active events.
     */
    public static void saveActiveEventsState() {
        eventManager.saveActiveEventsState();
    }
    
    /**
     * Sets a custom message for when a block event is enabled.
     */
    public static void setBlockEventEnableMessage(String eventName, String message) {
        eventManager.setBlockEventEnableMessage(eventName, message);
    }
    
    /**
     * Sets a custom message for when a block event is disabled.
     */
    public static void setBlockEventDisableMessage(String eventName, String message) {
        eventManager.setBlockEventDisableMessage(eventName, message);
    }
    
    /**
     * Gets the message displayed when a block event is enabled.
     */
    public static String getBlockEventEnableMessage(String eventName) {
        return eventManager.getBlockEventEnableMessage(eventName);
    }
    
    /**
     * Gets the message displayed when a block event is disabled.
     */
    public static String getBlockEventDisableMessage(String eventName) {
        return eventManager.getBlockEventDisableMessage(eventName);
    }
    
    /**
     * Adds a normal drop entry for a specific block.
     */
    public static void addNormalDrop(String blockId, BlockDropEntry entry) {
        normalDrops.computeIfAbsent(blockId, k -> new ArrayList<>()).add(entry);
    }
    
    /**
     * Adds an event drop entry for a specific block and event.
     */
    public static void addEventDrop(String eventName, String blockId, BlockDropEntry entry) {
        eventDrops.computeIfAbsent(eventName, k -> new HashMap<>())
                 .computeIfAbsent(blockId, k -> new ArrayList<>())
                 .add(entry);
    }
    
    /**
     * Clears all loaded drops (useful for reloading).
     */
    public static void clearDrops() {
        normalDrops.clear();
        eventDrops.clear();
    }
}
