package net.poe.entitylootdrops.lootdrops.events;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Manages loot events and their states.
 */
public class LootEventManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String CONFIG_DIR = "config/EntityLootDrops";
    private static final String EVENTS_DIR = "Event Drops";
    
    // Tracks which events are currently active
    private Set<String> activeEvents = new HashSet<>();
    
    // Special event flags
    private boolean dropChanceEventActive = false;
    private boolean doubleDropsActive = false;
    private boolean debugLoggingEnabled = false;
    
    // Custom messages for event notifications
    private Map<String, String> eventEnableMessages = new HashMap<>();
    private Map<String, String> eventDisableMessages = new HashMap<>();
    
    // Default messages for special events
    private String dropChanceEnableMessage = "§6[Events] §aDouble Drop Chance §eevent has been enabled! §e(2x drop rates)";
    private String dropChanceDisableMessage = "§6[Events] §cDouble Drop Chance event has been disabled!";
    private String doubleDropsEnableMessage = "§6[Events] §aDouble Drops §eevent has been enabled! §e(2x drop amounts)";
    private String doubleDropsDisableMessage = "§6[Events] §cDouble Drops event has been disabled!";
    
    /**
     * Checks if an event is currently active.
     */
    public boolean isEventActive(String eventName) {
        return activeEvents.contains(eventName.toLowerCase());
    }
    
    /**
     * Enables or disables an event.
     */
    public void toggleEvent(String eventName, boolean active, Set<String> availableEvents) {
        // Find the actual case-preserved event name
        String actualEventName = null;
        
        // First check in the available events
        for (String key : availableEvents) {
            if (key.equalsIgnoreCase(eventName)) {
                actualEventName = key;
                break;
            }
        }
        
        // If not found, check the directory structure directly
        if (actualEventName == null) {
            Path eventsDir = Paths.get(CONFIG_DIR, EVENTS_DIR);
            if (Files.exists(eventsDir)) {
                try {
                    Optional<Path> matchingDir = Files.list(eventsDir)
                        .filter(Files::isDirectory)
                        .filter(dir -> dir.getFileName().toString().equalsIgnoreCase(eventName))
                        .findFirst();
                    
                    if (matchingDir.isPresent()) {
                        actualEventName = matchingDir.get().getFileName().toString();
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to check event directories", e);
                }
            }
        }
        
        // If the event doesn't exist anywhere, use the provided name
        if (actualEventName == null) {
            actualEventName = eventName;
            LOGGER.warn("Event '{}' not found in configuration, using name as-is", eventName);
        }
        
        if (active) {
            activeEvents.add(actualEventName.toLowerCase());
            LOGGER.info("Enabled event: {}", actualEventName);
            String message = eventEnableMessages.getOrDefault(actualEventName.toLowerCase(), 
                "§6[Events] §a" + actualEventName + " event has been enabled!");
            broadcastEventMessage(message);
        } else {
            activeEvents.remove(actualEventName.toLowerCase());
            LOGGER.info("Disabled event: {}", actualEventName);
            String message = eventDisableMessages.getOrDefault(actualEventName.toLowerCase(), 
                "§6[Events] §c" + actualEventName + " event has been disabled!");
            broadcastEventMessage(message);
        }
        
        // Save the updated state
        saveActiveEventsState();
    }
    
    /**
     * Enables or disables the drop chance event.
     */
    public void toggleDropChanceEvent(boolean active) {
        dropChanceEventActive = active;
        LOGGER.info("Drop chance event set to: {}", active);
        if (active) {
            broadcastEventMessage(dropChanceEnableMessage);
        } else {
            broadcastEventMessage(dropChanceDisableMessage);
        }
        saveActiveEventsState();
    }
    
    /**
     * Enables or disables the double drops event.
     */
    public void toggleDoubleDrops(boolean active) {
        doubleDropsActive = active;
        LOGGER.info("Double drops set to: {}", active);
        if (active) {
            broadcastEventMessage(doubleDropsEnableMessage);
        } else {
            broadcastEventMessage(doubleDropsDisableMessage);
        }
        saveActiveEventsState();
    }
    
    /**
     * Clears all active events.
     */
    public void clearActiveEvents() {
        activeEvents.clear();
        saveActiveEventsState();
    }
    
    /**
     * Gets all currently active events.
     */
    public Set<String> getActiveEvents() {
        return Collections.unmodifiableSet(activeEvents);
    }
    
    /**
     * Gets the count of active events.
     */
    public int getActiveEventsCount() {
        return activeEvents.size();
    }
    
    /**
     * Checks if the drop chance event is active.
     */
    public boolean isDropChanceEventActive() {
        return dropChanceEventActive;
    }
    
    /**
     * Checks if the double drops event is active.
     */
    public boolean isDoubleDropsActive() {
        return doubleDropsActive;
    }
    
    /**
     * Checks if debug logging is enabled.
     */
    public boolean isDebugLoggingEnabled() {
        return debugLoggingEnabled;
    }
    
    /**
     * Enables or disables debug logging.
     */
    public void setDebugLogging(boolean enabled) {
        debugLoggingEnabled = enabled;
        LootEventHandler.setDebugLogging(enabled);
        LOGGER.info("Debug logging has been {}", enabled ? "enabled" : "disabled");
        saveActiveEventsState();
    }
    
    /**
     * Saves the current state of active events.
     */
    public void saveActiveEventsState() {
        try {
            Path configDir = Paths.get(CONFIG_DIR);
            Path stateFile = configDir.resolve("active_events.json");

            Map<String, Object> state = new HashMap<>();
            state.put("activeEvents", activeEvents);
            state.put("dropChanceEventActive", dropChanceEventActive);
            state.put("doubleDropsActive", doubleDropsActive);
            state.put("debugLoggingEnabled", debugLoggingEnabled);
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(state);
            Files.write(stateFile, json.getBytes());
            
            LOGGER.info("Saved active events state");
        } catch (Exception e) {
            LOGGER.error("Failed to save active events state", e);
        }
    }
    
    /**
     * Loads the active events state from file.
     */
    public void loadActiveEventsState() {
        try {
            Path configDir = Paths.get(CONFIG_DIR);
            Path stateFile = configDir.resolve("active_events.json");
            
            if (Files.exists(stateFile)) {
                String json = new String(Files.readAllBytes(stateFile));
                Gson gson = new Gson();
                java.lang.reflect.Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> state = gson.fromJson(json, mapType);
                
                if (state != null) {
                    if (state.containsKey("activeEvents")) {
                        activeEvents.clear();
                        @SuppressWarnings("unchecked")
                        java.util.List<String> events = (java.util.List<String>) state.get("activeEvents");
                        activeEvents.addAll(events);
                        LOGGER.info("Restored {} active events from state file", events.size());
                    }
                    
                    if (state.containsKey("dropChanceEventActive")) {
                        dropChanceEventActive = (Boolean) state.get("dropChanceEventActive");
                        LOGGER.info("Restored drop chance event state: {}", dropChanceEventActive);
                    }
                    
                    if (state.containsKey("doubleDropsActive")) {
                        doubleDropsActive = (Boolean) state.get("doubleDropsActive");
                        LOGGER.info("Restored double drops event state: {}", doubleDropsActive);
                    }

                    if (state.containsKey("debugLoggingEnabled")) {
                        debugLoggingEnabled = (Boolean) state.get("debugLoggingEnabled");
                        LootEventHandler.setDebugLogging(debugLoggingEnabled);
                        LOGGER.info("Restored debug logging state: {}", debugLoggingEnabled);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load active events state", e);
        }
    }
    
    /**
     * Restores previous event state if needed.
     */
    public void restorePreviousState(Set<String> previousActiveEvents, boolean previousDropChanceState, boolean previousDoubleDropsState, boolean previousDebugState) {
        if (activeEvents.isEmpty() && !previousActiveEvents.isEmpty()) {
            activeEvents.addAll(previousActiveEvents);
            dropChanceEventActive = previousDropChanceState;
            doubleDropsActive = previousDoubleDropsState;
            debugLoggingEnabled = previousDebugState;
            saveActiveEventsState();
        }
    }
    
    /**
     * Broadcasts a message to all players on the server.
     */
    private void broadcastEventMessage(String message) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
        }
    }
    
    /**
     * Sets event messages.
     */
    public void setEventMessages(Map<String, String> enableMessages, Map<String, String> disableMessages) {
        if (enableMessages != null) {
            eventEnableMessages.putAll(enableMessages);
        }
        if (disableMessages != null) {
            eventDisableMessages.putAll(disableMessages);
        }
    }
    
    // Message getters and setters
    public void setEventEnableMessage(String eventName, String message) {
        eventEnableMessages.put(eventName.toLowerCase(), message);
    }
    
    public void setEventDisableMessage(String eventName, String message) {
        eventDisableMessages.put(eventName.toLowerCase(), message);
    }
    
    public String getEventEnableMessage(String eventName) {
        return eventEnableMessages.getOrDefault(eventName.toLowerCase(), 
            "§6[Events] §a" + eventName + " event has been enabled!");
    }
    
    public String getEventDisableMessage(String eventName) {
        return eventDisableMessages.getOrDefault(eventName.toLowerCase(), 
            "§6[Events] §c" + eventName + " event has been disabled!");
    }
    
    public void setDropChanceEnableMessage(String message) {
        dropChanceEnableMessage = message;
    }
    
    public void setDropChanceDisableMessage(String message) {
        dropChanceDisableMessage = message;
    }
    
    public String getDropChanceEnableMessage() {
        return dropChanceEnableMessage;
    }
    
    public String getDropChanceDisableMessage() {
        return dropChanceDisableMessage;
    }
    
    public void setDoubleDropsEnableMessage(String message) {
        doubleDropsEnableMessage = message;
    }
    
    public void setDoubleDropsDisableMessage(String message) {
        doubleDropsDisableMessage = message;
    }
    
    public String getDoubleDropsEnableMessage() {
        return doubleDropsEnableMessage;
    }
    
    public String getDoubleDropsDisableMessage() {
        return doubleDropsDisableMessage;
    }
}
