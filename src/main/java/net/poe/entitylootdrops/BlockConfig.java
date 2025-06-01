package net.poe.entitylootdrops;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Configuration class for block drops in the EntityLootDrops mod.
 * Handles loading, saving, and managing custom block drop configurations.
 */
public class BlockConfig {
    private static final Logger LOGGER = LogManager.getLogger();
    
    // Configuration directory paths
    private static final String CONFIG_DIR = "config/EntityLootDrops";
    private static final String BLOCKS_DIR = "Blocks"; // Main blocks directory
    private static final String NORMAL_DROPS_DIR = "Normal Drops";
    private static final String EVENTS_DIR = "Event Drops";
    private static final String BLOCK_TYPES_DIR = "Block Types"; // Directory for block-specific drops
    private static final String GLOBAL_DROPS_FILE = "Global_Block_Drops.json";
    
    // Built-in event types
    private static final String[] EVENT_TYPES = {"Winter", "Summer", "Easter", "Halloween"};
    
    // Storage for loaded drop configurations
    private static Map<String, List<BlockDropEntry>> blockDrops = new HashMap<>();
    private static Map<String, List<CustomBlockDropEntry>> globalDrops = new HashMap<>();
    
    // Tracks which block events are currently active
    private static Set<String> activeBlockEvents = new HashSet<>();
    
    // Special event flags for blocks
    private static boolean blockDropChanceEventActive = false;
    private static boolean blockDoubleDropsActive = false;
    
    // Custom messages for block event notifications
    private static Map<String, String> blockEventEnableMessages = new HashMap<>();
    private static Map<String, String> blockEventDisableMessages = new HashMap<>();
    
    /**
     * Represents a block-specific drop entry.
     */
    public static class BlockDropEntry extends CustomBlockDropEntry {
        private String blockId;  // The Minecraft block ID (e.g., "minecraft:stone")
        
        public String getBlockId() {
            return blockId;
        }
        
        public void setBlockId(String blockId) {
            this.blockId = blockId;
        }
    }
    
    /**
     * Base class for block drop configurations.
     */
    public static class CustomBlockDropEntry {
        private String itemId;              // The item to drop
        private float dropChance;           // Percentage chance to drop
        private int minAmount;              // Minimum amount to drop
        private int maxAmount;              // Maximum amount to drop
        private String nbtData;             // Custom NBT data for the dropped item
        private String requiredTool;        // Required tool to get the drop
        private String requiredToolTier;    // Required tool tier (e.g., "diamond")
        private int requiredToolLevel;      // Required tool level (e.g., mining level)
        private String requiredEnchantment; // Required enchantment on the tool
        private int requiredEnchantLevel;   // Required enchantment level
        private String command;             // Command to execute when block breaks
        private float commandChance;        // Chance to execute the command
        private boolean requirePlayerBreak = true;  // If true, only drops when broken by player
        private boolean allowDefaultDrops = true;   // If true, keeps vanilla drops
        private boolean replaceDefaultDrops = false; // If true, replaces vanilla drops with this item
        private List<String> allowModIDs = new ArrayList<>();
        private String _comment;
        
        // Getters and setters for all fields
        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }
        
        public float getDropChance() { return dropChance; }
        public void setDropChance(float dropChance) { this.dropChance = dropChance; }
        
        public int getMinAmount() { return minAmount; }
        public void setMinAmount(int minAmount) { this.minAmount = minAmount; }
        
        public int getMaxAmount() { return maxAmount; }
        public void setMaxAmount(int maxAmount) { this.maxAmount = maxAmount; }
        
        public String getNbtData() { return nbtData; }
        public void setNbtData(String nbtData) { this.nbtData = nbtData; }
        
        public String getRequiredTool() { return requiredTool; }
        public void setRequiredTool(String requiredTool) { this.requiredTool = requiredTool; }
        
        public String getRequiredToolTier() { return requiredToolTier; }
        public void setRequiredToolTier(String requiredToolTier) { this.requiredToolTier = requiredToolTier; }
        
        public int getRequiredToolLevel() { return requiredToolLevel; }
        public void setRequiredToolLevel(int requiredToolLevel) { this.requiredToolLevel = requiredToolLevel; }
        
        public String getRequiredEnchantment() { return requiredEnchantment; }
        public void setRequiredEnchantment(String requiredEnchantment) { this.requiredEnchantment = requiredEnchantment; }
        
        public int getRequiredEnchantLevel() { return requiredEnchantLevel; }
        public void setRequiredEnchantLevel(int requiredEnchantLevel) { this.requiredEnchantLevel = requiredEnchantLevel; }
        
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        
        public float getCommandChance() { return commandChance; }
        public void setCommandChance(float commandChance) { this.commandChance = commandChance; }
        
        public boolean isRequirePlayerBreak() { return requirePlayerBreak; }
        public void setRequirePlayerBreak(boolean requirePlayerBreak) { this.requirePlayerBreak = requirePlayerBreak; }
        
        public boolean isAllowDefaultDrops() { return allowDefaultDrops; }
        public void setAllowDefaultDrops(boolean allowDefaultDrops) { this.allowDefaultDrops = allowDefaultDrops; }
        
        public boolean isReplaceDefaultDrops() { return replaceDefaultDrops; }
        public void setReplaceDefaultDrops(boolean replaceDefaultDrops) { this.replaceDefaultDrops = replaceDefaultDrops; }
        
        public List<String> getAllowModIDs() { return allowModIDs; }
        public void setAllowModIDs(List<String> allowModIDs) { this.allowModIDs = allowModIDs != null ? allowModIDs : new ArrayList<>(); }
        
        public void setComment(String comment) { this._comment = comment; }
        
        // Helper methods to check if certain fields are set
        public boolean hasNbtData() { return nbtData != null && !nbtData.isEmpty(); }
        public boolean hasCommand() { return command != null && !command.isEmpty(); }
    }
    
    /**
     * Loads all block drop configurations.
     */
    public static void loadConfig() {
        // Store current active events
        Set<String> previousActiveEvents = new HashSet<>(activeBlockEvents);
        boolean previousDropChanceState = blockDropChanceEventActive;
        boolean previousDoubleDropsState = blockDoubleDropsActive;
        
        // Create directories if they don't exist
        createConfigDirectories();
        
        // Load all drop configurations
        loadAllDrops();
        
        // Load custom message configurations
        loadMessages();
        
        // Load active events state
        loadActiveEventsState();
        
        // Restore previous state if no state was loaded
        if (activeBlockEvents.isEmpty() && !previousActiveEvents.isEmpty()) {
            activeBlockEvents.addAll(previousActiveEvents);
            blockDropChanceEventActive = previousDropChanceState;
            blockDoubleDropsActive = previousDoubleDropsState;
            saveActiveEventsState();
        }
        
        LOGGER.info("Loaded block configuration: {} block types, {} global drops, {} active events",
            blockDrops.size(), globalDrops.size(), activeBlockEvents.size());
    }
    
    /**
     * Creates the necessary directory structure for block configurations.
     */
    private static void createConfigDirectories() {
        try {
            // Create main blocks directory
            Path blocksDir = Paths.get(CONFIG_DIR, BLOCKS_DIR);
            Files.createDirectories(blocksDir);
            
            // Create normal drops directory
            Path normalDropsDir = blocksDir.resolve(NORMAL_DROPS_DIR);
            Files.createDirectories(normalDropsDir);
            
            // Create events directory
            Path eventsDir = blocksDir.resolve(EVENTS_DIR);
            Files.createDirectories(eventsDir);
            
            // Check if this is the first time setup (no config files exist)
            boolean isFirstTimeSetup = !Files.exists(normalDropsDir.resolve(GLOBAL_DROPS_FILE));
            
            // Create event type directories
            for (String eventType : EVENT_TYPES) {
                Path eventTypeDir = eventsDir.resolve(eventType);
                Files.createDirectories(eventTypeDir);
                createExampleConfigs(eventTypeDir);
            }
            
            // Create example configurations in normal drops
            createExampleConfigs(normalDropsDir);
            
            // Only create initial example files on first setup
            if (isFirstTimeSetup) {
                createInitialExamples(normalDropsDir);
            }
            
            // Create a custom event example
            Path customEventDir = eventsDir.resolve("Custom_Event_Example");
            if (!Files.exists(customEventDir)) {
                Files.createDirectories(customEventDir);
                createExampleConfigs(customEventDir);
                // Only create initial examples for new custom event
                createInitialExamples(customEventDir);
            }
            
        } catch (IOException e) {
            LOGGER.error("Failed to create block config directories", e);
        }
    }
    
    /**
     * Creates example configurations in the given directory.
     */
    private static void createExampleConfigs(Path directory) throws IOException {
        // Create example global drops file
        Path globalDropsFile = directory.resolve(GLOBAL_DROPS_FILE);
        if (!Files.exists(globalDropsFile)) {
            List<CustomBlockDropEntry> examples = new ArrayList<>();
            
            // Example 1: Simple emerald drop
            CustomBlockDropEntry simple = new CustomBlockDropEntry();
            simple.setItemId("minecraft:emerald");
            simple.setDropChance(5.0f);
            simple.setMinAmount(1);
            simple.setMaxAmount(3);
            simple.setRequirePlayerBreak(true);
            simple.setComment("Simple emerald drop from any block");
            examples.add(simple);
            
            // Example 2: Complex diamond drop with requirements
            CustomBlockDropEntry complex = new CustomBlockDropEntry();
            complex.setItemId("minecraft:diamond");
            complex.setDropChance(1.0f);
            complex.setMinAmount(1);
            complex.setMaxAmount(1);
            complex.setRequiredTool("minecraft:diamond_pickaxe");
            complex.setRequiredToolTier("diamond");
            complex.setRequiredToolLevel(3);
            complex.setRequiredEnchantment("minecraft:fortune");
            complex.setRequiredEnchantLevel(3);
            complex.setCommand("particle minecraft:end_rod {block_x} {block_y} {block_z} 0.5 0.5 0.5 0.1 20");
            complex.setCommandChance(100.0f);
            complex.setComment("Complex diamond drop with tool requirements");
            examples.add(complex);
            
            // Save examples
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.write(globalDropsFile, gson.toJson(examples).getBytes());
        }
        
        // Create example block-specific drops directory
        Path blockTypesDir = directory.resolve(BLOCK_TYPES_DIR);
        Files.createDirectories(blockTypesDir);
    }
    
    /**
     * Creates initial example files only once during first setup.
     */
    private static void createInitialExamples(Path directory) throws IOException {
        Path blockTypesDir = directory.resolve(BLOCK_TYPES_DIR);
        
        // Only create these example files if they don't exist AND it's the first time setup
        Path exampleBlockFile = blockTypesDir.resolve("stone_drops.json");
        if (!Files.exists(exampleBlockFile)) {
            BlockDropEntry example = new BlockDropEntry();
            example.setBlockId("minecraft:stone");
            example.setItemId("minecraft:diamond");
            example.setDropChance(1.0f);
            example.setMinAmount(1);
            example.setMaxAmount(1);
            example.setRequiredTool("minecraft:diamond_pickaxe");
            example.setComment("Example diamond drop from stone");
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.write(exampleBlockFile, gson.toJson(example).getBytes());
        }
        
        // Create example for replacing default drops
        Path replaceDropsFile = blockTypesDir.resolve("replace_drops_example.json");
        if (!Files.exists(replaceDropsFile)) {
            BlockDropEntry example = new BlockDropEntry();
            example.setBlockId("minecraft:coal_ore");
            example.setItemId("minecraft:diamond");
            example.setDropChance(100.0f);
            example.setMinAmount(1);
            example.setMaxAmount(2);
            example.setAllowDefaultDrops(true);
            example.setReplaceDefaultDrops(true);
            example.setComment("Example of replacing coal ore drops with diamonds");
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.write(replaceDropsFile, gson.toJson(example).getBytes());
        }
    }
    
    /**
     * Creates a new custom event directory.
     * 
     * @param eventName The name of the custom event
     * @return true if the event was created successfully, false otherwise
     */
    public static boolean createCustomEvent(String eventName) {
        try {
            // Sanitize event name (remove special characters)
            String sanitizedName = eventName.replaceAll("[^a-zA-Z0-9_]", "_");
            
            // Create the event directory
            Path eventDir = Paths.get(CONFIG_DIR, BLOCKS_DIR, EVENTS_DIR, sanitizedName);
            if (Files.exists(eventDir)) {
                LOGGER.info("Custom event '{}' already exists", sanitizedName);
                return false;
            }
            
            Files.createDirectories(eventDir);
            
            // Create example configurations
            createExampleConfigs(eventDir);
            
            LOGGER.info("Created custom event: {}", sanitizedName);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to create custom event: {}", eventName, e);
            return false;
        }
    }
    
    /**
     * Loads custom message configurations.
     */
    private static void loadMessages() {
        try {
            Path messagesPath = Paths.get(CONFIG_DIR, BLOCKS_DIR, "messages.json");
            if (!Files.exists(messagesPath)) {
                // Create default messages
                Map<String, Object> messages = new HashMap<>();
                Map<String, String> enableMessages = new HashMap<>();
                Map<String, String> disableMessages = new HashMap<>();
                
                for (String eventType : EVENT_TYPES) {
                    enableMessages.put(eventType.toLowerCase(), "§6[Block Events] §a" + eventType + " event has been enabled!");
                    disableMessages.put(eventType.toLowerCase(), "§6[Block Events] §c" + eventType + " event has been disabled!");
                }
                
                messages.put("enableMessages", enableMessages);
                messages.put("disableMessages", disableMessages);
                messages.put("dropChanceEnableMessage", "§6[Block Events] §aDouble Drop Chance event has been enabled! §e(2x drop rates)");
                messages.put("dropChanceDisableMessage", "§6[Block Events] §cDouble Drop Chance event has been disabled!");
                messages.put("doubleDropsEnableMessage", "§6[Block Events] §aDouble Drops event has been enabled! §e(2x amounts)");
                messages.put("doubleDropsDisableMessage", "§6[Block Events] §cDouble Drops event has been disabled!");
                
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Files.write(messagesPath, gson.toJson(messages).getBytes());
            }
            
            // Load messages
            String json = new String(Files.readAllBytes(messagesPath));
            Gson gson = new Gson();
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> messages = gson.fromJson(json, mapType);
            
            if (messages != null) {
                // Load enable messages
                if (messages.containsKey("enableMessages")) {
                    Map<String, String> enableMessages = (Map<String, String>) messages.get("enableMessages");
                    blockEventEnableMessages.putAll(enableMessages);
                }
                
                // Load disable messages
                if (messages.containsKey("disableMessages")) {
                    Map<String, String> disableMessages = (Map<String, String>) messages.get("disableMessages");
                    blockEventDisableMessages.putAll(disableMessages);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load block event messages", e);
        }
    }
    
    /**
     * Loads all drop configurations.
     */
    private static void loadAllDrops() {
        try {
            // Clear existing configurations
            blockDrops.clear();
            globalDrops.clear();
            
            // Load normal drops
            Path normalDropsDir = Paths.get(CONFIG_DIR, BLOCKS_DIR, NORMAL_DROPS_DIR);
            loadDirectoryDrops(normalDropsDir, NORMAL_DROPS_DIR);
            
            // Load event drops
            Path eventsDir = Paths.get(CONFIG_DIR, BLOCKS_DIR, EVENTS_DIR);
            if (Files.exists(eventsDir)) {
                Files.list(eventsDir)
                    .filter(Files::isDirectory)
                    .forEach(eventDir -> {
                        String eventName = eventDir.getFileName().toString();
                        loadDirectoryDrops(eventDir, eventName);
                    });
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load block drops", e);
        }
    }
    
    /**
     * Loads drop configurations from a directory.
     */
    private static void loadDirectoryDrops(Path directory, String dirKey) {
        try {
            // Load global drops file
            Path globalDropsFile = directory.resolve(GLOBAL_DROPS_FILE);
            if (Files.exists(globalDropsFile)) {
                String json = new String(Files.readAllBytes(globalDropsFile));
                Gson gson = new Gson();
                Type listType = new TypeToken<ArrayList<CustomBlockDropEntry>>(){}.getType();
                List<CustomBlockDropEntry> drops = gson.fromJson(json, listType);
                if (drops != null) {
                    globalDrops.put(dirKey, drops);
                }
            }
            
            // Load block-specific drops
            Path blockTypesDir = directory.resolve(BLOCK_TYPES_DIR);
            if (Files.exists(blockTypesDir)) {
                List<BlockDropEntry> dirDrops = new ArrayList<>();
                Files.walk(blockTypesDir)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            String json = new String(Files.readAllBytes(path));
                            Gson gson = new Gson();
                            
                            // Try to parse as a single entry first
                            try {
                                BlockDropEntry drop = gson.fromJson(json, BlockDropEntry.class);
                                if (drop != null && drop.getBlockId() != null) {
                                    dirDrops.add(drop);
                                }
                            } catch (Exception e) {
                                // Try to parse as a list
                                try {
                                    Type listType = new TypeToken<ArrayList<BlockDropEntry>>(){}.getType();
                                    List<BlockDropEntry> drops = gson.fromJson(json, listType);
                                    if (drops != null) {
                                        for (BlockDropEntry drop : drops) {
                                            if (drop.getBlockId() != null) {
                                                dirDrops.add(drop);
                                            }
                                        }
                                    }
                                } catch (Exception e2) {
                                    LOGGER.error("Error parsing block drop file: {}", path, e2);
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error("Error loading block drop file: {}", path, e);
                        }
                    });
                if (!dirDrops.isEmpty()) {
                    blockDrops.put(dirKey, dirDrops);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error loading drops from directory: {}", directory, e);
        }
    }
    
    /**
     * Gets all global block drops.
     */
    public static List<CustomBlockDropEntry> getGlobalBlockDrops() {
        return globalDrops.getOrDefault(NORMAL_DROPS_DIR, Collections.emptyList());
    }
    
    /**
     * Gets block-specific drops for a given block ID.
     */
    public static List<BlockDropEntry> getBlockDrops(String blockId) {
        return blockDrops.getOrDefault(NORMAL_DROPS_DIR, Collections.emptyList()).stream()
            .filter(drop -> drop.getBlockId().equals(blockId))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets event-specific drops for a given event.
     */
    public static List<BlockDropEntry> getEventBlockDrops(String eventName) {
        return blockDrops.getOrDefault(eventName, Collections.emptyList());
    }
    
    /**
     * Gets all available block events.
     */
    public static Set<String> getAvailableBlockEvents() {
        return blockDrops.keySet().stream()
            .filter(key -> !key.equals(NORMAL_DROPS_DIR))
            .collect(Collectors.toSet());
    }
    
    /**
     * Toggles a block event on or off.
     */
    public static void toggleBlockEvent(String eventName, boolean active) {
        // Find the actual case-preserved event name from the map
        String actualEventName = null;
        for (String key : getAvailableBlockEvents()) {
            if (key.equalsIgnoreCase(eventName)) {
                actualEventName = key;
                break;
            }
        }
        
        // If the event doesn't exist in our map, use the provided name
        if (actualEventName == null) {
            actualEventName = eventName;
        }
        
        if (active) {
            activeBlockEvents.add(actualEventName.toLowerCase());
            LOGGER.info("Enabled block event: {}", actualEventName);
            broadcastEventMessage(blockEventEnableMessages.getOrDefault(actualEventName.toLowerCase(),
                "§6[Block Events] §a" + actualEventName + " event has been enabled!"));
        } else {
            activeBlockEvents.remove(actualEventName.toLowerCase());
            LOGGER.info("Disabled block event: {}", actualEventName);
            broadcastEventMessage(blockEventDisableMessages.getOrDefault(actualEventName.toLowerCase(),
                "§6[Block Events] §c" + actualEventName + " event has been disabled!"));
        }
    }
    
    /**
     * Toggles the block drop chance event.
     */
    public static void toggleBlockDropChanceEvent(boolean active) {
        blockDropChanceEventActive = active;
        LOGGER.info("Block drop chance event set to: {}", active);
        broadcastEventMessage(active ? 
            "§6[Block Events] §aDouble Drop Chance event has been enabled! §e(2x drop rates)" :
            "§6[Block Events] §cDouble Drop Chance event has been disabled!");
    }
    
    /**
     * Toggles the block double drops event.
     */
    public static void toggleBlockDoubleDrops(boolean active) {
        blockDoubleDropsActive = active;
        LOGGER.info("Block double drops set to: {}", active);
        broadcastEventMessage(active ?
            "§6[Block Events] §aDouble Drops event has been enabled! §e(2x amounts)" :
            "§6[Block Events] §cDouble Drops event has been disabled!");
    }
    
    /**
     * Broadcasts a message to all players.
     */
    private static void broadcastEventMessage(String message) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
        }
    }
    
    /**
     * Gets all currently active block events.
     */
    public static Set<String> getActiveBlockEvents() {
        return Collections.unmodifiableSet(activeBlockEvents);
    }
    
    /**
     * Checks if the block drop chance event is active.
     */
    public static boolean isBlockDropChanceEventActive() {
        return blockDropChanceEventActive;
    }
    
    /**
     * Checks if the block double drops event is active.
     */
    public static boolean isBlockDoubleDropsActive() {
        return blockDoubleDropsActive;
    }
    
    /**
     * Saves the current state of active events.
     */
    public static void saveActiveEventsState() {
        try {
            Path stateFile = Paths.get(CONFIG_DIR, BLOCKS_DIR, "active_block_events.json");
            Map<String, Object> state = new HashMap<>();
            state.put("activeEvents", new ArrayList<>(activeBlockEvents));
            state.put("dropChanceEventActive", blockDropChanceEventActive);
            state.put("doubleDropsActive", blockDoubleDropsActive);
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(state);
            Files.write(stateFile, json.getBytes());
            
            LOGGER.info("Saved block events state");
        } catch (Exception e) {
            LOGGER.error("Failed to save block events state", e);
        }
    }
    
    /**
     * Loads the state of active events.
     */
    private static void loadActiveEventsState() {
        try {
            Path stateFile = Paths.get(CONFIG_DIR, BLOCKS_DIR, "active_block_events.json");
            if (Files.exists(stateFile)) {
                String json = new String(Files.readAllBytes(stateFile));
                Gson gson = new Gson();
                Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> state = gson.fromJson(json, mapType);
                
                if (state != null) {
                    if (state.containsKey("activeEvents")) {
                        activeBlockEvents.clear();
                        List<String> events = (List<String>) state.get("activeEvents");
                        activeBlockEvents.addAll(events);
                    }
                    
                    if (state.containsKey("dropChanceEventActive")) {
                        blockDropChanceEventActive = (Boolean) state.get("dropChanceEventActive");
                    }
                    
                    if (state.containsKey("doubleDropsActive")) {
                        blockDoubleDropsActive = (Boolean) state.get("doubleDropsActive");
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load block events state", e);
        }
    }
    
    /**
     * Sets a custom message for when a block event is enabled.
     */
    public static void setBlockEventEnableMessage(String eventName, String message) {
        blockEventEnableMessages.put(eventName.toLowerCase(), message);
    }
    
    /**
     * Sets a custom message for when a block event is disabled.
     */
    public static void setBlockEventDisableMessage(String eventName, String message) {
        blockEventDisableMessages.put(eventName.toLowerCase(), message);
    }
    
    /**
     * Gets the message displayed when a block event is enabled.
     */
    public static String getBlockEventEnableMessage(String eventName) {
        return blockEventEnableMessages.getOrDefault(eventName.toLowerCase(),
            "§6[Block Events] §a" + eventName + " event has been enabled!");
    }
    
    /**
     * Gets the message displayed when a block event is disabled.
     */
    public static String getBlockEventDisableMessage(String eventName) {
        return blockEventDisableMessages.getOrDefault(eventName.toLowerCase(),
            "§6[Block Events] §c" + eventName + " event has been disabled!");
    }
}
