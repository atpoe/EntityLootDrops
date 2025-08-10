package net.poe.entitylootdrops.lootdrops.events;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.minecraft.world.entity.player.Player;

/**
 * Manages per-event drop counting for specific items.
 */
public class EventDropCountManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String CONFIG_DIR = "config/EntityLootDrops";
    private static final String LOOT_DROPS_DIR = "Loot Drops";
    private static final String EVENT_DROPS_DIR = "Event Drops";
    private static final String DROP_COUNT_FILE = "Drop_Count.json";

    /**
     * Player drop count data for a specific event.
     */
    public static class EventPlayerDropCount {
        private String playerName;
        private Map<String, Integer> itemCounts = new HashMap<>();
        private int totalEventDrops = 0;
        private long lastUpdated = System.currentTimeMillis();

        public EventPlayerDropCount() {}

        public EventPlayerDropCount(String playerName) {
            this.playerName = playerName;
        }

        public void addDrop(String itemId, int amount) {
            itemCounts.put(itemId, itemCounts.getOrDefault(itemId, 0) + amount);
            totalEventDrops += amount;
            lastUpdated = System.currentTimeMillis();
        }

        // Getters and setters
        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }
        public Map<String, Integer> getItemCounts() { return itemCounts; }
        public void setItemCounts(Map<String, Integer> itemCounts) { this.itemCounts = itemCounts; }
        public int getTotalEventDrops() { return totalEventDrops; }
        public void setTotalEventDrops(int totalEventDrops) { this.totalEventDrops = totalEventDrops; }
        public long getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    /**
     * Combined player drop count data across multiple events.
     */
    public static class CombinedPlayerDropCount {
        private String playerName;
        private int totalDrops = 0;
        private Map<String, Integer> eventTotals = new HashMap<>();

        public CombinedPlayerDropCount(String playerName) {
            this.playerName = playerName;
        }

        public void addEventDrops(String eventName, int drops) {
            eventTotals.put(eventName, drops);
            totalDrops += drops;
        }

        // Getters
        public String getPlayerName() { return playerName; }
        public int getTotalDrops() { return totalDrops; }
        public Map<String, Integer> getEventTotals() { return eventTotals; }
    }

    /**
     * Records a drop for a specific event and item.
     */
    public static void recordEventDrop(String eventName, Player player, String itemId, int amount) {
        if (player == null || eventName == null || itemId == null) {
            return;
        }

        try {
            Path eventDir = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, EVENT_DROPS_DIR, eventName);
            Path dropCountFile = eventDir.resolve(DROP_COUNT_FILE);

            // Load existing data
            Map<String, EventPlayerDropCount> playerData = loadEventDropData(dropCountFile);

            // Get or create player entry
            String playerId = player.getUUID().toString();
            EventPlayerDropCount playerDropCount = playerData.computeIfAbsent(playerId,
                    k -> new EventPlayerDropCount(player.getName().getString()));

            // Update player name in case it changed
            playerDropCount.setPlayerName(player.getName().getString());

            // Record the drop
            playerDropCount.addDrop(itemId, amount);

            // Save back to file
            saveEventDropData(dropCountFile, playerData);

            LOGGER.debug("Recorded event drop for {}: {} x{} in event {}",
                    player.getName().getString(), itemId, amount, eventName);

        } catch (Exception e) {
            LOGGER.error("Failed to record event drop for event: " + eventName, e);
        }
    }

    /**
     * Gets top players for a specific event.
     */
    public static List<EventPlayerDropCount> getEventTopPlayers(String eventName, int count) {
        Path dropCountFile = getEventDropCountFile(eventName);
        Map<String, EventPlayerDropCount> playerData = loadEventDropData(dropCountFile);

        return playerData.values().stream()
                .sorted((a, b) -> Integer.compare(b.getTotalEventDrops(), a.getTotalEventDrops()))
                .limit(count)
                .collect(Collectors.toList());
    }

    /**
     * Gets combined top players across all events with drop counts.
     */
    public static List<CombinedPlayerDropCount> getCombinedTopPlayers(int count) {
        Map<String, CombinedPlayerDropCount> combinedTotals = new HashMap<>();

        try {
            Path eventsDir = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, EVENT_DROPS_DIR);
            if (!Files.exists(eventsDir)) {
                return new ArrayList<>();
            }

            // Iterate through all event directories
            Files.list(eventsDir)
                    .filter(Files::isDirectory)
                    .forEach(eventDir -> {
                        String eventName = eventDir.getFileName().toString();
                        Path dropCountFile = eventDir.resolve(DROP_COUNT_FILE);

                        if (Files.exists(dropCountFile)) {
                            Map<String, EventPlayerDropCount> eventData = loadEventDropData(dropCountFile);
                            eventData.values().forEach(playerCount -> {
                                String playerName = playerCount.getPlayerName();
                                CombinedPlayerDropCount combined = combinedTotals.computeIfAbsent(
                                        playerName, CombinedPlayerDropCount::new);
                                combined.addEventDrops(eventName, playerCount.getTotalEventDrops());
                            });
                        }
                    });

        } catch (Exception e) {
            LOGGER.error("Failed to get combined top players", e);
        }

        return combinedTotals.values().stream()
                .sorted((a, b) -> Integer.compare(b.getTotalDrops(), a.getTotalDrops()))
                .limit(count)
                .collect(Collectors.toList());
    }

    /**
     * Gets drop count data for a specific player in a specific event.
     */
    public static EventPlayerDropCount getPlayerEventDropCount(String eventName, String playerName) {
        Path dropCountFile = getEventDropCountFile(eventName);
        Map<String, EventPlayerDropCount> playerData = loadEventDropData(dropCountFile);

        return playerData.values().stream()
                .filter(count -> count.getPlayerName().equals(playerName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets all events that have drop count data.
     */
    public static List<String> getEventsWithDropCounts() {
        List<String> events = new ArrayList<>();

        try {
            Path eventsDir = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, EVENT_DROPS_DIR);
            if (!Files.exists(eventsDir)) {
                return events;
            }

            Files.list(eventsDir)
                    .filter(Files::isDirectory)
                    .forEach(eventDir -> {
                        Path dropCountFile = eventDir.resolve(DROP_COUNT_FILE);
                        if (Files.exists(dropCountFile)) {
                            events.add(eventDir.getFileName().toString());
                        }
                    });

        } catch (Exception e) {
            LOGGER.error("Failed to get events with drop counts", e);
        }

        return events;
    }

    /**
     * Gets the drop count file path for a specific event.
     */
    private static Path getEventDropCountFile(String eventName) {
        Path eventDir = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, EVENT_DROPS_DIR, eventName);
        return eventDir.resolve(DROP_COUNT_FILE);
    }

    /**
     * Loads drop count data from an event's Drop_Count.json file.
     */
    private static Map<String, EventPlayerDropCount> loadEventDropData(Path dropCountFile) {
        Map<String, EventPlayerDropCount> playerData = new HashMap<>();

        try {
            if (!Files.exists(dropCountFile)) {
                return playerData; // Return empty map if file doesn't exist
            }

            String json = Files.readString(dropCountFile);
            if (json.trim().isEmpty()) {
                return playerData;
            }

            Gson gson = new Gson();
            java.lang.reflect.Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> data = gson.fromJson(json, mapType);

            if (data.containsKey("playerDropCounts")) {
                java.lang.reflect.Type playerCountsType = new TypeToken<Map<String, EventPlayerDropCount>>(){}.getType();
                Map<String, EventPlayerDropCount> counts = gson.fromJson(
                        gson.toJson(data.get("playerDropCounts")), playerCountsType);

                if (counts != null) {
                    playerData.putAll(counts);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed to load event drop data from: " + dropCountFile, e);
        }

        return playerData;
    }

    /**
     * Saves drop count data to an event's Drop_Count.json file.
     */
    private static void saveEventDropData(Path dropCountFile, Map<String, EventPlayerDropCount> playerData) {
        try {
            // Ensure directory exists
            Files.createDirectories(dropCountFile.getParent());

            // Prepare data structure
            Map<String, Object> data = new HashMap<>();
            data.put("eventName", dropCountFile.getParent().getFileName().toString());
            data.put("playerDropCounts", playerData);
            data.put("lastUpdated", System.currentTimeMillis());
            data.put("comment", "Tracks custom item drops per player for this specific event. Only items with enableDropCount=true are tracked.");

            // Calculate totals for summary
            Map<String, Integer> itemTotals = new HashMap<>();
            int totalPlayers = playerData.size();
            int totalDrops = 0;

            for (EventPlayerDropCount playerCount : playerData.values()) {
                totalDrops += playerCount.getTotalEventDrops();
                for (Map.Entry<String, Integer> itemEntry : playerCount.getItemCounts().entrySet()) {
                    itemTotals.put(itemEntry.getKey(),
                            itemTotals.getOrDefault(itemEntry.getKey(), 0) + itemEntry.getValue());
                }
            }

            data.put("summary", Map.of(
                    "totalPlayers", totalPlayers,
                    "totalDrops", totalDrops,
                    "itemTotals", itemTotals
            ));

            // Save to file
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(dropCountFile, gson.toJson(data));

        } catch (Exception e) {
            LOGGER.error("Failed to save event drop data to: " + dropCountFile, e);
        }
    }

    /**
     * Resets drop count data for a specific event.
     */
    public static void resetEventDropCounts(String eventName) {
        try {
            Path eventDir = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, EVENT_DROPS_DIR, eventName);
            Path dropCountFile = eventDir.resolve(DROP_COUNT_FILE);

            if (Files.exists(dropCountFile)) {
                Files.delete(dropCountFile);
                LOGGER.info("Reset drop counts for event: {}", eventName);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to reset drop counts for event: " + eventName, e);
        }
    }

    /**
     * Resets drop count data for all events.
     */
    public static void resetAllEventDropCounts() {
        try {
            Path eventsDir = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, EVENT_DROPS_DIR);
            if (!Files.exists(eventsDir)) {
                return;
            }

            Files.list(eventsDir)
                    .filter(Files::isDirectory)
                    .forEach(eventDir -> {
                        Path dropCountFile = eventDir.resolve(DROP_COUNT_FILE);
                        if (Files.exists(dropCountFile)) {
                            try {
                                Files.delete(dropCountFile);
                                LOGGER.info("Reset drop counts for event: {}", eventDir.getFileName());
                            } catch (Exception e) {
                                LOGGER.error("Failed to reset drop counts for event: " + eventDir.getFileName(), e);
                            }
                        }
                    });

        } catch (Exception e) {
            LOGGER.error("Failed to reset all event drop counts", e);
        }
    }
}