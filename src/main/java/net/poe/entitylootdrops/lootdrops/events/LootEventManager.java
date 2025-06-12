package net.poe.entitylootdrops.lootdrops.events;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    private static final String LOOT_DROPS_DIR = "Loot Drops";
    private static final String EVENT_DROPS_DIR = "Event Drops";

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
            Path eventsDir = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, EVENT_DROPS_DIR);
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

        // Toggle the event
        if (active) {
            activeEvents.add(actualEventName.toLowerCase());
            broadcastEventMessage(getEventEnableMessage(actualEventName));
            LOGGER.info("Enabled event: {}", actualEventName);
        } else {
            activeEvents.remove(actualEventName.toLowerCase());
            broadcastEventMessage(getEventDisableMessage(actualEventName));
            LOGGER.info("Disabled event: {}", actualEventName);
        }

        // Save state after change
        saveActiveEventsState();
    }

    /**
     * Enables or disables the drop chance event.
     */
    public void toggleDropChanceEvent(boolean active) {
        dropChanceEventActive = active;
        if (active) {
            broadcastEventMessage(dropChanceEnableMessage);
            LOGGER.info("Enabled drop chance event");
        } else {
            broadcastEventMessage(dropChanceDisableMessage);
            LOGGER.info("Disabled drop chance event");
        }
        saveActiveEventsState();
    }

    /**
     * Enables or disables the double drops event.
     */
    public void toggleDoubleDrops(boolean active) {
        doubleDropsActive = active;
        if (active) {
            broadcastEventMessage(doubleDropsEnableMessage);
            LOGGER.info("Enabled double drops event");
        } else {
            broadcastEventMessage(doubleDropsDisableMessage);
            LOGGER.info("Disabled double drops event");
        }
        saveActiveEventsState();
    }

    /**
     * Clears all active events.
     */
    public void clearActiveEvents() {
        activeEvents.clear();
        dropChanceEventActive = false;
        doubleDropsActive = false;
        broadcastEventMessage("§6[Events] §cAll events have been disabled!");
        LOGGER.info("Cleared all active events");
        saveActiveEventsState();
    }

    /**
     * Gets all currently active events.
     */
    public Set<String> getActiveEvents() {
        return new HashSet<>(activeEvents);
    }

    /**
     * Gets the count of active events.
     */
    public int getActiveEventsCount() {
        int count = activeEvents.size();
        if (dropChanceEventActive) count++;
        if (doubleDropsActive) count++;
        return count;
    }

    /**
     * Checks if drop chance event is active.
     */
    public boolean isDropChanceEventActive() {
        return dropChanceEventActive;
    }

    /**
     * Checks if double drops event is active.
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
        LOGGER.info("Debug logging {}", enabled ? "enabled" : "disabled");
        saveActiveEventsState();
    }

    /**
     * Creates the Active_Events.json file with default values if it doesn't exist.
     */
    public void createActiveEventsFile() {
        try {
            Path stateFile = Paths.get(CONFIG_DIR, "Active_Events.json");

            if (!Files.exists(stateFile)) {
                Files.createDirectories(stateFile.getParent());

                // Create default state
                Map<String, Object> defaultState = new HashMap<>();
                defaultState.put("activeEvents", new ArrayList<String>());
                defaultState.put("dropChanceEventActive", false);
                defaultState.put("doubleDropsActive", false);
                defaultState.put("debugLoggingEnabled", false);

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Files.writeString(stateFile, gson.toJson(defaultState));
                LOGGER.info("Created Active_Events.json file: {}", stateFile);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create Active_Events.json file", e);
        }
    }

    /**
     * Saves the current active events state to file.
     */
    public void saveActiveEventsState() {
        try {
            Path stateFile = Paths.get(CONFIG_DIR, "Active_Events.json");
            Files.createDirectories(stateFile.getParent());

            Map<String, Object> state = new HashMap<>();
            state.put("activeEvents", new ArrayList<>(activeEvents));
            state.put("dropChanceEventActive", dropChanceEventActive);
            state.put("doubleDropsActive", doubleDropsActive);
            state.put("debugLoggingEnabled", debugLoggingEnabled);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(stateFile, gson.toJson(state));
            LOGGER.debug("Saved active events state to: {}", stateFile);

        } catch (Exception e) {
            LOGGER.error("Failed to save active events state", e);
        }
    }

    /**
     * Loads the active events state from file.
     */
    public void loadActiveEventsState() {
        try {
            Path stateFile = Paths.get(CONFIG_DIR, "Active_Events.json");

            // Create file if it doesn't exist
            if (!Files.exists(stateFile)) {
                createActiveEventsFile();
                return;
            }

            LOGGER.info("Loading active events state from: {}", stateFile);
            String json = new String(Files.readAllBytes(stateFile));

            if (json.trim().isEmpty()) {
                LOGGER.warn("Active_Events.json is empty, creating default state");
                createActiveEventsFile();
                return;
            }

            Gson gson = new Gson();
            java.lang.reflect.Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> state = gson.fromJson(json, mapType);

            if (state != null) {
                if (state.containsKey("activeEvents")) {
                    activeEvents.clear();
                    @SuppressWarnings("unchecked")
                    List<String> events = (List<String>) state.get("activeEvents");
                    if (events != null) {
                        activeEvents.addAll(events);
                        LOGGER.info("Restored {} active events from state file", events.size());
                    }
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
        } catch (Exception e) {
            LOGGER.error("Failed to load active events state", e);
            // Create default file on error
            createActiveEventsFile();
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