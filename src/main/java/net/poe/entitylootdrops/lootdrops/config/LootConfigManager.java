package net.poe.entitylootdrops.lootdrops.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.poe.entitylootdrops.lootdrops.model.CustomDropEntry;
import net.poe.entitylootdrops.lootdrops.model.EntityDropEntry;

/**
 * Manages the storage and retrieval of loot drop configurations.
 */
public class LootConfigManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String CONFIG_DIR = "config/EntityLootDrops";
    private static final String LOOT_DROPS_DIR = "Loot Drops";
    private static final String NORMAL_DROPS_DIR = "Normal Drops";
    private static final String EVENT_DROPS_DIR = "Event Drops";

    // Storage for loaded drop configurations
    private Map<String, List<EntityDropEntry>> entityDrops = new HashMap<>();
    private Map<String, List<CustomDropEntry>> hostileDrops = new HashMap<>();
    private Map<String, String> customMessages = new HashMap<>();

    /**
     * Clears all loaded configurations.
     */
    public void clearConfigurations() {
        entityDrops.clear();
        hostileDrops.clear();
        customMessages.clear();
    }

    /**
     * Sets entity drops for a specific directory/event.
     */
    public void setEntityDrops(String dirKey, List<EntityDropEntry> drops) {
        if (drops != null && !drops.isEmpty()) {
            entityDrops.put(dirKey, drops);
        }
    }

    /**
     * Sets hostile drops for a specific directory/event.
     */
    public void setHostileDrops(String dirKey, List<CustomDropEntry> drops) {
        if (drops != null && !drops.isEmpty()) {
            hostileDrops.put(dirKey, drops);
        }
    }

    /**
     * Sets custom messages for the configuration.
     */
    public void setCustomMessages(Map<String, String> messages) {
        if (messages != null) {
            customMessages.clear();
            customMessages.putAll(messages);
        }
    }

    /**
     * Gets all custom messages.
     */
    public Map<String, String> getCustomMessages() {
        return new HashMap<>(customMessages);
    }

    /**
     * Gets a specific custom message by key.
     */
    public String getCustomMessage(String key) {
        return customMessages.get(key);
    }

    /**
     * Gets the normal (always active) entity-specific drops.
     */
    public List<EntityDropEntry> getNormalDrops() {
        return entityDrops.getOrDefault(NORMAL_DROPS_DIR, Collections.emptyList());
    }

    /**
     * Gets the normal (always active) hostile mob drops.
     */
    public List<CustomDropEntry> getNormalHostileDrops() {
        return hostileDrops.getOrDefault(NORMAL_DROPS_DIR, Collections.emptyList());
    }

    /**
     * Gets all event-specific entity drops.
     */
    public Map<String, List<EntityDropEntry>> getEventDrops() {
        return entityDrops.entrySet().stream()
                .filter(e -> !e.getKey().equals(NORMAL_DROPS_DIR))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Gets the hostile mob drops for a specific event.
     */
    public List<CustomDropEntry> getEventHostileDrops(String eventName) {
        return hostileDrops.getOrDefault(eventName, Collections.emptyList());
    }

    /**
     * Gets entity drops for a specific event.
     */
    public List<EntityDropEntry> getEventEntityDrops(String eventName) {
        return entityDrops.getOrDefault(eventName, Collections.emptyList());
    }

    /**
     * Gets all available event names.
     */
    public Set<String> getAllEventNames() {
        Set<String> eventNames = new HashSet<>();

        // Add events from loaded configurations
        eventNames.addAll(getEventDrops().keySet());

        // Add events from directories (safely)
        try {
            Path eventsDir = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, EVENT_DROPS_DIR);
            if (Files.exists(eventsDir) && Files.isDirectory(eventsDir)) {
                try {
                    Files.list(eventsDir)
                            .filter(Files::isDirectory)
                            .forEach(eventDir -> eventNames.add(eventDir.getFileName().toString()));
                } catch (IOException e) {
                    LOGGER.warn("Failed to list event directories: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get event names from directories", e);
        }

        return eventNames;
    }

    /**
     * Gets all available event names (alias for getAllEventNames).
     * This method is used by the event manager for compatibility.
     */
    public Set<String> getAvailableEvents() {
        return getAllEventNames();
    }

    /**
     * Gets the count of entity drops across all configurations.
     */
    public int getEntityDropsCount() {
        return entityDrops.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    /**
     * Gets the count of hostile drops across all configurations.
     */
    public int getHostileDropsCount() {
        return hostileDrops.values().stream()
                .mapToInt(List::size)
                .sum();
    }
}