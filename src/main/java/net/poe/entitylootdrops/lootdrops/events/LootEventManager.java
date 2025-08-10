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
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
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
    private boolean dropCountEnabled = false;

    // Drop count tracking
    private Map<UUID, PlayerDropCount> playerDropCounts = new HashMap<>();

    // Custom messages for event notifications
    private Map<String, String> eventEnableMessages = new HashMap<>();
    private Map<String, String> eventDisableMessages = new HashMap<>();

    // Default messages for special events
    private String dropChanceEnableMessage = "§6[Events] §aDouble Drop Chance §eevent has been enabled! §e(2x drop rates)";
    private String dropChanceDisableMessage = "§6[Events] §cDouble Drop Chance event has been disabled!";
    private String doubleDropsEnableMessage = "§6[Events] §aDouble Drops §eevent has been enabled! §e(2x drop amounts)";
    private String doubleDropsDisableMessage = "§6[Events] §cDouble Drops event has been disabled!";

    /**
     * Player drop count data structure.
     */
    public static class PlayerDropCount {
        private String playerName;
        private int totalDrops = 0;
        private Map<String, Integer> itemCounts = new HashMap<>();
        private long lastUpdated = System.currentTimeMillis();

        public PlayerDropCount() {}

        public PlayerDropCount(String playerName) {
            this.playerName = playerName;
        }

        // Getters and setters
        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }

        public int getTotalDrops() { return totalDrops; }
        public void setTotalDrops(int totalDrops) { this.totalDrops = totalDrops; }

        public Map<String, Integer> getItemCounts() { return itemCounts; }
        public void setItemCounts(Map<String, Integer> itemCounts) { this.itemCounts = itemCounts; }

        public long getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

        /**
         * Adds a drop to this player's count.
         */
        public void addDrop(String itemId, int amount) {
            totalDrops += amount;
            itemCounts.put(itemId, itemCounts.getOrDefault(itemId, 0) + amount);
            lastUpdated = System.currentTimeMillis();
        }
    }

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
     * Enables or disables drop counting.
     */
    public void enableDropCount(boolean enabled) {
        dropCountEnabled = enabled;
        if (enabled) {
            broadcastEventMessage("§6[Drop Count] §aEnabled drop counting! §7Items will be tracked per player.");
            LOGGER.info("Enabled drop counting");
            createDropCountFile();
        } else {
            broadcastEventMessage("§6[Drop Count] §cDisabled drop counting.");
            LOGGER.info("Disabled drop counting");
        }
        saveActiveEventsState();
    }

    /**
     * Records a drop for a player.
     */
    public void recordDrop(Player player, String itemId, int amount) {
        if (!dropCountEnabled || player == null) {
            return;
        }

        UUID playerId = player.getUUID();
        PlayerDropCount dropCount = playerDropCounts.computeIfAbsent(playerId,
                k -> new PlayerDropCount(player.getName().getString()));

        // Update player name in case it changed
        dropCount.setPlayerName(player.getName().getString());
        dropCount.addDrop(itemId, amount);

        LOGGER.debug("Recorded drop for {}: {} x{} (Total: {})",
                player.getName().getString(), itemId, amount, dropCount.getTotalDrops());

        // Save periodically (every 10 drops to avoid excessive I/O)
        if (dropCount.getTotalDrops() % 10 == 0) {
            saveDropCountData();
        }
    }

    /**
     * Gets drop count for a specific player.
     */
    public Optional<PlayerDropCount> getPlayerDropCount(UUID playerId) {
        return Optional.ofNullable(playerDropCounts.get(playerId));
    }

    /**
     * Gets drop count for a specific player by name.
     */
    public Optional<PlayerDropCount> getPlayerDropCount(String playerName) {
        return playerDropCounts.values().stream()
                .filter(dropCount -> playerName.equals(dropCount.getPlayerName()))
                .findFirst();
    }

    /**
     * Gets all player drop counts sorted by total drops (descending).
     */
    public List<Map.Entry<UUID, PlayerDropCount>> getTopDropCounts() {
        return playerDropCounts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().getTotalDrops(), a.getValue().getTotalDrops()))
                .toList();
    }

    /**
     * Resets all drop counts.
     */
    public void resetDropCounts() {
        playerDropCounts.clear();
        broadcastEventMessage("§6[Drop Count] §eAll drop counts have been reset!");
        LOGGER.info("Reset all drop counts");
        saveDropCountData();
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
        if (dropCountEnabled) count++;
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
     * Checks if drop counting is enabled.
     */
    public boolean isDropCountEnabled() {
        return dropCountEnabled;
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
     * Creates the Drop_Count.json file if it doesn't exist.
     */
    public void createDropCountFile() {
        try {
            Path dropCountFile = Paths.get(CONFIG_DIR, "Drop_Count.json");

            if (!Files.exists(dropCountFile)) {
                Files.createDirectories(dropCountFile.getParent());

                Map<String, Object> defaultData = new HashMap<>();
                defaultData.put("enabled", dropCountEnabled);
                defaultData.put("playerDropCounts", new HashMap<String, PlayerDropCount>());
                defaultData.put("lastUpdated", System.currentTimeMillis());
                defaultData.put("comment", "Tracks custom item drops per player for events. Player UUIDs are used as keys.");

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Files.writeString(dropCountFile, gson.toJson(defaultData));
                LOGGER.info("Created Drop_Count.json file: {}", dropCountFile);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create Drop_Count.json file", e);
        }
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
                defaultState.put("dropCountEnabled", false);

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Files.writeString(stateFile, gson.toJson(defaultState));
                LOGGER.info("Created Active_Events.json file: {}", stateFile);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create Active_Events.json file", e);
        }
    }

    /**
     * Saves drop count data to Drop_Count.json.
     */
    public void saveDropCountData() {
        try {
            Path dropCountFile = Paths.get(CONFIG_DIR, "Drop_Count.json");
            Files.createDirectories(dropCountFile.getParent());

            Map<String, Object> data = new HashMap<>();
            data.put("enabled", dropCountEnabled);
            data.put("playerDropCounts", playerDropCounts);
            data.put("lastUpdated", System.currentTimeMillis());
            data.put("comment", "Tracks custom item drops per player for events. Player UUIDs are used as keys.");

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(dropCountFile, gson.toJson(data));
            LOGGER.debug("Saved drop count data to: {}", dropCountFile);

        } catch (Exception e) {
            LOGGER.error("Failed to save drop count data", e);
        }
    }

    /**
     * Loads drop count data from Drop_Count.json.
     */
    public void loadDropCountData() {
        try {
            Path dropCountFile = Paths.get(CONFIG_DIR, "Drop_Count.json");

            if (!Files.exists(dropCountFile)) {
                LOGGER.debug("Drop_Count.json does not exist, using defaults");
                return;
            }

            String json = new String(Files.readAllBytes(dropCountFile));
            if (json.trim().isEmpty()) {
                LOGGER.warn("Drop_Count.json is empty");
                return;
            }

            Gson gson = new Gson();
            java.lang.reflect.Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> data = gson.fromJson(json, mapType);

            if (data.containsKey("enabled")) {
                dropCountEnabled = (Boolean) data.get("enabled");
            }

            if (data.containsKey("playerDropCounts")) {
                java.lang.reflect.Type playerCountsType = new TypeToken<Map<String, PlayerDropCount>>(){}.getType();
                Map<String, PlayerDropCount> counts = gson.fromJson(
                        gson.toJson(data.get("playerDropCounts")), playerCountsType);

                // Convert string UUIDs back to UUID objects
                playerDropCounts.clear();
                if (counts != null) {
                    counts.forEach((uuidStr, dropCount) -> {
                        try {
                            UUID playerId = UUID.fromString(uuidStr);
                            playerDropCounts.put(playerId, dropCount);
                        } catch (IllegalArgumentException e) {
                            LOGGER.warn("Invalid UUID in drop count data: {}", uuidStr);
                        }
                    });
                }
            }

            LOGGER.info("Loaded drop count data: {} players tracked", playerDropCounts.size());

        } catch (Exception e) {
            LOGGER.error("Failed to load drop count data", e);
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
            state.put("dropCountEnabled", dropCountEnabled);

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

                if (state.containsKey("dropCountEnabled")) {
                    dropCountEnabled = (Boolean) state.get("dropCountEnabled");
                    LOGGER.info("Restored drop count enabled state: {}", dropCountEnabled);
                }
            }

            // Load drop count data after loading state
            loadDropCountData();

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