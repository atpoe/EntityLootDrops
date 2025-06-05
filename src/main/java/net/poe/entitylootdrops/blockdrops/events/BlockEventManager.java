package net.poe.entitylootdrops.blockdrops.events;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.poe.entitylootdrops.blockdrops.regeneration.BlockRegenerationManager;

/**
 * Manages block events and their states, including regeneration tracking.
 */
public class BlockEventManager {
    private static final Logger LOGGER = LogManager.getLogger();
    
    // File paths for persistence
    private static final String CONFIG_DIR = "config/EntityLootDrops";
    private static final String ACTIVE_EVENTS_FILE = CONFIG_DIR + "/active_block_events.json";
    private static final String MESSAGES_FILE = CONFIG_DIR + "/Blocks/messages.json";
    
    // Event states
    private final Set<String> activeBlockEvents = new HashSet<>();
    private boolean blockDropChanceEventActive = false;
    private boolean blockDoubleDropsActive = false;
    
    // Custom messages for events
    private final Map<String, String> eventEnableMessages = new HashMap<>();
    private final Map<String, String> eventDisableMessages = new HashMap<>();
    
    /**
     * Gets all currently active block events.
     */
    public Set<String> getActiveBlockEvents() {
        return new HashSet<>(activeBlockEvents);
    }
    
    /**
     * Gets the count of active events.
     */
    public int getActiveEventsCount() {
        int count = activeBlockEvents.size();
        if (blockDropChanceEventActive) count++;
        if (blockDoubleDropsActive) count++;
        return count;
    }
    
    /**
     * Toggles a block event on or off.
     */
    public void toggleBlockEvent(String eventName, boolean active, Set<String> availableEvents) {
        if (!availableEvents.contains(eventName)) {
            LOGGER.warn("Attempted to toggle unknown block event: {}", eventName);
            return;
        }
        
        if (active) {
            activeBlockEvents.add(eventName);
            LOGGER.info("Enabled block event: {}", eventName);
        } else {
            activeBlockEvents.remove(eventName);
            LOGGER.info("Disabled block event: {}", eventName);
        }
        
        // Save state immediately
        saveActiveEventsState();
    }
    
    /**
     * Toggles the block drop chance event.
     */
    public void toggleBlockDropChanceEvent(boolean active) {
        blockDropChanceEventActive = active;
        LOGGER.info("{} block drop chance event (2x drop rates)", active ? "Enabled" : "Disabled");
        saveActiveEventsState();
    }
    
    /**
     * Toggles the block double drops event.
     */
    public void toggleBlockDoubleDrops(boolean active) {
        blockDoubleDropsActive = active;
        LOGGER.info("{} block double drops event (2x amounts)", active ? "Enabled" : "Disabled");
        saveActiveEventsState();
    }
    
    /**
     * Checks if the block drop chance event is active.
     */
    public boolean isBlockDropChanceEventActive() {
        return blockDropChanceEventActive;
    }
    
    /**
     * Checks if the block double drops event is active.
     */
    public boolean isBlockDoubleDropsActive() {
        return blockDoubleDropsActive;
    }
    
    /**
     * Sets custom messages for events.
     */
    public void setEventMessages(Map<String, String> enableMessages, Map<String, String> disableMessages) {
        if (enableMessages != null) {
            this.eventEnableMessages.clear();
            this.eventEnableMessages.putAll(enableMessages);
        }
        if (disableMessages != null) {
            this.eventDisableMessages.clear();
            this.eventDisableMessages.putAll(disableMessages);
        }
    }
    
    /**
     * Sets a custom message for when a block event is enabled.
     */
    public void setBlockEventEnableMessage(String eventName, String message) {
        eventEnableMessages.put(eventName.toLowerCase(), message);
    }
    
    /**
     * Sets a custom message for when a block event is disabled.
     */
    public void setBlockEventDisableMessage(String eventName, String message) {
        eventDisableMessages.put(eventName.toLowerCase(), message);
    }
    
    /**
     * Gets the message displayed when a block event is enabled.
     */
    public String getBlockEventEnableMessage(String eventName) {
        return eventEnableMessages.getOrDefault(eventName.toLowerCase(), 
            "§6[Block Events] §aEnabled event: §e" + eventName);
    }
    
    /**
     * Gets the message displayed when a block event is disabled.
     */
    public String getBlockEventDisableMessage(String eventName) {
        return eventDisableMessages.getOrDefault(eventName.toLowerCase(), 
            "§6[Block Events] §cDisabled event: §e" + eventName);
    }
    
    /**
     * Saves the current state of active events to disk.
     */
    public void saveActiveEventsState() {
        try {
            File file = new File(ACTIVE_EVENTS_FILE);
            file.getParentFile().mkdirs();
            
            Map<String, Object> state = new HashMap<>();
            state.put("activeEvents", activeBlockEvents);
            state.put("dropChanceActive", blockDropChanceEventActive);
            state.put("doubleDropsActive", blockDoubleDropsActive);
            
            // Include regeneration statistics
            int regeneratingCount = BlockRegenerationManager.getRegenerationCount();
            state.put("regeneratingBlocks", regeneratingCount);
            
            try (FileWriter writer = new FileWriter(file)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(state, writer);
            }
            
            LOGGER.debug("Saved block events state: {} active events, {} regenerating blocks", 
                        getActiveEventsCount(), regeneratingCount);
            
        } catch (IOException e) {
            LOGGER.error("Failed to save block events state", e);
        }
    }
    
    /**
     * Loads the active events state from disk.
     */
    public void loadActiveEventsState() {
        File file = new File(ACTIVE_EVENTS_FILE);
        if (!file.exists()) {
            LOGGER.debug("No saved block events state found");
            return;
        }
        
        try (FileReader reader = new FileReader(file)) {
            Gson gson = new Gson();
            java.lang.reflect.Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> state = gson.fromJson(reader, type);
            
            if (state != null) {
                // Load active events
                Object activeEventsObj = state.get("activeEvents");
                if (activeEventsObj instanceof java.util.List) {
                    activeBlockEvents.clear();
                    @SuppressWarnings("unchecked")
                    java.util.List<String> eventsList = (java.util.List<String>) activeEventsObj;
                    activeBlockEvents.addAll(eventsList);
                }
                
                // Load special event states
                Object dropChanceObj = state.get("dropChanceActive");
                if (dropChanceObj instanceof Boolean) {
                    blockDropChanceEventActive = (Boolean) dropChanceObj;
                }
                
                Object doubleDropsObj = state.get("doubleDropsActive");
                if (doubleDropsObj instanceof Boolean) {
                    blockDoubleDropsActive = (Boolean) doubleDropsObj;
                }
                
                // Log regeneration info if available
                Object regeneratingObj = state.get("regeneratingBlocks");
                if (regeneratingObj instanceof Number) {
                    int savedRegeneratingCount = ((Number) regeneratingObj).intValue();
                    int currentRegeneratingCount = BlockRegenerationManager.getRegenerationCount();
                    LOGGER.info("Loaded block events state: {} active events, {} regenerating blocks (was {})", 
                               getActiveEventsCount(), currentRegeneratingCount, savedRegeneratingCount);
                } else {
                    LOGGER.info("Loaded block events state: {} active events", getActiveEventsCount());
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to load block events state", e);
        }
    }
    
    /**
     * Restores previous state if no state was loaded.
     */
    public void restorePreviousState(Set<String> previousActiveEvents, 
                                   boolean previousDropChanceState, 
                                   boolean previousDoubleDropsState) {
        if (activeBlockEvents.isEmpty() && !blockDropChanceEventActive && !blockDoubleDropsActive) {
            activeBlockEvents.addAll(previousActiveEvents);
            blockDropChanceEventActive = previousDropChanceState;
            blockDoubleDropsActive = previousDoubleDropsState;
            
            if (!previousActiveEvents.isEmpty() || previousDropChanceState || previousDoubleDropsState) {
                LOGGER.info("Restored previous block events state: {} events", getActiveEventsCount());
                saveActiveEventsState();
            }
        }
    }
    
    /**
     * Gets regeneration statistics for reporting.
     */
    public Map<String, Object> getRegenerationStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("regeneratingBlocks", BlockRegenerationManager.getRegenerationCount());
        stats.put("activeEvents", getActiveEventsCount());
        stats.put("dropChanceActive", blockDropChanceEventActive);
        stats.put("doubleDropsActive", blockDoubleDropsActive);
        return stats;
    }
    
    /**
     * Clears all active events and regeneration data.
     */
    public void clearAllEvents() {
        activeBlockEvents.clear();
        blockDropChanceEventActive = false;
        blockDoubleDropsActive = false;
        
        // Also clear regeneration data
        BlockRegenerationManager.clearAll();
        
        saveActiveEventsState();
        LOGGER.info("Cleared all block events and regeneration data");
    }
}
