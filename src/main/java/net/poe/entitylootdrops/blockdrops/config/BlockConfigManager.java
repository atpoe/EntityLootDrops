package net.poe.entitylootdrops.blockdrops.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.poe.entitylootdrops.blockdrops.model.BlockDropEntry;
import net.poe.entitylootdrops.blockdrops.model.CustomBlockDropEntry;

public class BlockConfigManager {
    private static final String NORMAL_DROPS_DIR = "Normal Drops";

    // Storage for loaded drop configurations
    private Map<String, List<BlockDropEntry>> blockDrops = new HashMap<>();
    private Map<String, List<CustomBlockDropEntry>> globalDrops = new HashMap<>();

    // Track events that have Global_Block_Drops.json files
    private Set<String> eventsWithGlobalDrops = new HashSet<>();

    /**
     * Clears all loaded configurations.
     */
    public void clearConfigurations() {
        blockDrops.clear();
        globalDrops.clear();
        eventsWithGlobalDrops.clear();
    }

    /**
     * Sets block drops for a specific directory/event.
     */
    public void setBlockDrops(String dirKey, List<BlockDropEntry> drops) {
        if (drops != null && !drops.isEmpty()) {
            blockDrops.put(dirKey, drops);
        }
    }

    /**
     * Sets global drops for a specific directory/event.
     * Only events with global drops will be considered available.
     */
    public void setGlobalDrops(String dirKey, List<CustomBlockDropEntry> drops) {
        if (drops != null && !drops.isEmpty()) {
            globalDrops.put(dirKey, drops);

            // Track events that have global drops (exclude normal drops)
            if (!dirKey.equals(NORMAL_DROPS_DIR)) {
                eventsWithGlobalDrops.add(dirKey);
            }
        }
    }

    /**
     * Gets all global block drops.
     */
    public List<CustomBlockDropEntry> getGlobalBlockDrops() {
        return globalDrops.getOrDefault(NORMAL_DROPS_DIR, Collections.emptyList());
    }

    /**
     * Gets block-specific drops for a given block ID.
     */
    public List<BlockDropEntry> getBlockDrops(String blockId) {
        return blockDrops.getOrDefault(NORMAL_DROPS_DIR, Collections.emptyList()).stream()
                .filter(drop -> drop.getBlockId().equals(blockId))
                .collect(Collectors.toList());
    }

    /**
     * Gets event-specific drops for a given event.
     */
    public List<BlockDropEntry> getEventBlockDrops(String eventName) {
        return blockDrops.getOrDefault(eventName, Collections.emptyList());
    }

    /**
     * Gets all available block events (only those with Global_Block_Drops.json).
     */
    public Set<String> getAvailableBlockEvents() {
        return new HashSet<>(eventsWithGlobalDrops);
    }

    /**
     * Gets the count of block drop configurations.
     */
    public int getBlockDropsCount() {
        return blockDrops.size();
    }

    /**
     * Gets the count of global drop configurations.
     */
    public int getGlobalDropsCount() {
        return globalDrops.size();
    }
}