package net.poe.entitylootdrops.blockdrops.config;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.poe.entitylootdrops.blockdrops.BlockConfig;
import net.poe.entitylootdrops.blockdrops.events.BlockEventManager;
import net.poe.entitylootdrops.blockdrops.model.BlockDropEntry;
import net.poe.entitylootdrops.blockdrops.model.CustomBlockDropEntry;

/**
 * Handles loading and saving of block drop configurations.
 */
public class BlockConfigLoader {
    private static final Logger LOGGER = LogManager.getLogger();
    
    // Configuration directory paths
    private static final String CONFIG_DIR = "config/EntityLootDrops";
    private static final String BLOCKS_DIR = "Blocks";
    private static final String NORMAL_DROPS_DIR = "Normal Drops";
    private static final String EVENTS_DIR = "Event Drops";
    private static final String BLOCK_TYPES_DIR = "Block Types";
    private static final String GLOBAL_DROPS_FILE = "Global_Block_Drops.json";
    private static final String MESSAGES_FILE = "messages.json";
    
    // Built-in event types
    private static final String[] EVENT_TYPES = {"Winter", "Summer", "Easter", "Halloween"};
    
    private final BlockConfigManager configManager;
    private final BlockEventManager eventManager;
    
    public BlockConfigLoader(BlockConfigManager configManager, BlockEventManager eventManager) {
        this.configManager = configManager;
        this.eventManager = eventManager;
    }
    
    /**
     * Loads all block drop configurations.
     */
    public void loadConfig() {
        // Store current active events
        Set<String> previousActiveEvents = new HashSet<>(eventManager.getActiveBlockEvents());
        boolean previousDropChanceState = eventManager.isBlockDropChanceEventActive();
        boolean previousDoubleDropsState = eventManager.isBlockDoubleDropsActive();
        
        // Create directories if they don't exist
        createConfigDirectories();
        
        // Clear existing drops before loading new ones
        BlockConfig.clearDrops();
        
        // Load all drop configurations
        loadAllDrops();
        
        // Load custom message configurations
        loadMessages();
        
        // Load active events state
        eventManager.loadActiveEventsState();
        
        // Restore previous state if no state was loaded
        eventManager.restorePreviousState(previousActiveEvents, previousDropChanceState, previousDoubleDropsState);
    }
    
    /**
     * Creates the necessary directory structure for block configurations.
     */
    private void createConfigDirectories() {
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
    private void createExampleConfigs(Path directory) throws IOException {
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
            
            // Example 2: Complex diamond drop with requirements and regeneration
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
            complex.setCanRegenerate(true);
            complex.setBrokenBlockReplace("minecraft:bedrock");
            complex.setRespawnTime(30);
            complex.setComment("Complex diamond drop with tool requirements and regeneration");
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
    private void createInitialExamples(Path directory) throws IOException {
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
        
        // Create example for replacing default drops with regeneration
        Path replaceDropsFile = blockTypesDir.resolve("regenerating_ore_example.json");
        if (!Files.exists(replaceDropsFile)) {
            BlockDropEntry example = new BlockDropEntry();
            example.setBlockId("minecraft:coal_ore");
            example.setItemId("minecraft:diamond");
            example.setDropChance(100.0f);
            example.setMinAmount(1);
            example.setMaxAmount(2);
            example.setAllowDefaultDrops(true);
            example.setReplaceDefaultDrops(true);
            example.setCanRegenerate(true);
            example.setBrokenBlockReplace("minecraft:cobblestone");
            example.setRespawnTime(60);
            example.setComment("Example of regenerating coal ore that drops diamonds and regenerates after 60 seconds");
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.write(replaceDropsFile, gson.toJson(example).getBytes());
        }
    }
    
    /**
     * Creates a new custom event directory.
     */
    public boolean createCustomEvent(String eventName) {
        try {
            // Sanitize event name (remove special characters)
            String sanitizedName = eventName.replaceAll("[^a-zA-Z0-9_]", "_");
            
            // Create the event directory
            Path eventsDir = Paths.get(CONFIG_DIR, BLOCKS_DIR, EVENTS_DIR);
            Path customEventDir = eventsDir.resolve(sanitizedName);
            
            if (Files.exists(customEventDir)) {
                LOGGER.warn("Custom event directory already exists: {}", sanitizedName);
                return false;
            }
            
            Files.createDirectories(customEventDir);
            createExampleConfigs(customEventDir);
            createInitialExamples(customEventDir);
            
            LOGGER.info("Created custom event directory: {}", sanitizedName);
            return true;
            
        } catch (IOException e) {
            LOGGER.error("Failed to create custom event directory: {}", eventName, e);
            return false;
        }
    }
    
    /**
     * Loads all drop configurations from all directories.
     */
    private void loadAllDrops() {
        // Clear existing configurations
        configManager.clearConfigurations();
        
        try {
            Path blocksDir = Paths.get(CONFIG_DIR, BLOCKS_DIR);
            
            // Load normal drops
            loadDropsFromDirectory(blocksDir.resolve(NORMAL_DROPS_DIR), NORMAL_DROPS_DIR, false);
            
            // Load event drops
            Path eventsDir = blocksDir.resolve(EVENTS_DIR);
            if (Files.exists(eventsDir)) {
                Files.list(eventsDir)
                    .filter(Files::isDirectory)
                    .forEach(eventDir -> {
                        String eventName = eventDir.getFileName().toString();
                        loadDropsFromDirectory(eventDir, eventName, true);
                    });
            }
            
        } catch (IOException e) {
            LOGGER.error("Failed to load block drop configurations", e);
        }
    }
    
    /**
     * Loads drop configurations from a specific directory.
     */
    private void loadDropsFromDirectory(Path directory, String dirKey, boolean isEventDrop) {
        if (!Files.exists(directory)) {
            return;
        }
        
        try {
            // Load global drops
            Path globalDropsFile = directory.resolve(GLOBAL_DROPS_FILE);
            if (Files.exists(globalDropsFile)) {
                List<CustomBlockDropEntry> globalDrops = loadGlobalDropsFromFile(globalDropsFile);
                configManager.setGlobalDrops(dirKey, globalDrops);
                
                // Convert global drops to BlockDropEntry and add to BlockConfig
                for (CustomBlockDropEntry globalDrop : globalDrops) {
                    BlockDropEntry blockDrop = convertToBlockDropEntry(globalDrop, null);
                    if (isEventDrop) {
                        // Add as event drop for all blocks (null blockId means applies to all)
                        BlockConfig.addEventDrop(dirKey, "global", blockDrop);
                    } else {
                        // Add as normal drop for all blocks
                        BlockConfig.addNormalDrop("global", blockDrop);
                    }
                }
            }
            
            // Load block-specific drops
            Path blockTypesDir = directory.resolve(BLOCK_TYPES_DIR);
            if (Files.exists(blockTypesDir)) {
                List<BlockDropEntry> blockDrops = loadBlockDropsFromDirectory(blockTypesDir);
                configManager.setBlockDrops(dirKey, blockDrops);
                
                // Add block drops to BlockConfig
                for (BlockDropEntry blockDrop : blockDrops) {
                    String blockId = blockDrop.getBlockId() != null ? blockDrop.getBlockId() : "global";
                    if (isEventDrop) {
                        BlockConfig.addEventDrop(dirKey, blockId, blockDrop);
                    } else {
                        BlockConfig.addNormalDrop(blockId, blockDrop);
                    }
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to load drops from directory: {}", directory, e);
        }
    }
    
    /**
     * Converts a CustomBlockDropEntry to a BlockDropEntry.
     */
    private BlockDropEntry convertToBlockDropEntry(CustomBlockDropEntry customEntry, String blockId) {
        BlockDropEntry blockEntry = new BlockDropEntry();
        
        // Copy basic properties
        blockEntry.setBlockId(blockId);
        blockEntry.setItemId(customEntry.getItemId());
        blockEntry.setDropChance(customEntry.getDropChance());
        blockEntry.setMinAmount(customEntry.getMinAmount());
        blockEntry.setMaxAmount(customEntry.getMaxAmount());
        blockEntry.setRequirePlayerBreak(customEntry.isRequirePlayerBreak());
        blockEntry.setAllowDefaultDrops(customEntry.isAllowDefaultDrops());
        blockEntry.setReplaceDefaultDrops(customEntry.isReplaceDefaultDrops());
        
        // Copy tool requirements
        blockEntry.setRequiredTool(customEntry.getRequiredTool());
        blockEntry.setRequiredToolTier(customEntry.getRequiredToolTier());
        blockEntry.setRequiredToolLevel(customEntry.getRequiredToolLevel());
        blockEntry.setRequiredEnchantment(customEntry.getRequiredEnchantment());
        blockEntry.setRequiredEnchantLevel(customEntry.getRequiredEnchantLevel());
        
        // Copy advanced properties
        blockEntry.setNbtData(customEntry.getNbtData());
        blockEntry.setCommand(customEntry.getCommand());
        blockEntry.setCommandChance(customEntry.getCommandChance());
        blockEntry.setAllowModIDs(customEntry.getAllowModIDs());
        blockEntry.setComment(customEntry.getComment());
        
        // Copy regeneration properties
        blockEntry.setCanRegenerate(customEntry.canRegenerate());
        blockEntry.setBrokenBlockReplace(customEntry.getBrokenBlockReplace());
        blockEntry.setRespawnTime(customEntry.getRespawnTime());
        
        return blockEntry;
    }
    
    /**
     * Loads global drops from a JSON file.
     */
    private List<CustomBlockDropEntry> loadGlobalDropsFromFile(Path file) {
        try {
            String json = new String(Files.readAllBytes(file));
            Gson gson = new Gson();
            
            // Try to parse as array first
            Type listType = new TypeToken<List<CustomBlockDropEntry>>(){}.getType();
            List<CustomBlockDropEntry> drops = gson.fromJson(json, listType);
            
            if (drops == null) {
                drops = new ArrayList<>();
            }
            
            LOGGER.debug("Loaded {} global drops from {}", drops.size(), file.getFileName());
            return drops;
            
        } catch (Exception e) {
            LOGGER.error("Failed to load global drops from file: {}", file, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Loads block-specific drops from a directory containing JSON files.
     */
    private List<BlockDropEntry> loadBlockDropsFromDirectory(Path directory) {
        List<BlockDropEntry> allDrops = new ArrayList<>();
        
        try {
            Files.list(directory)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(file -> {
                    try {
                        String json = new String(Files.readAllBytes(file));
                        Gson gson = new Gson();
                        
                        // Try to parse as single object first
                        try {
                            BlockDropEntry singleDrop = gson.fromJson(json, BlockDropEntry.class);
                            if (singleDrop != null && singleDrop.getItemId() != null) {
                                allDrops.add(singleDrop);
                            }
                        } catch (Exception e1) {
                            // Try to parse as array
                            try {
                                Type listType = new TypeToken<List<BlockDropEntry>>(){}.getType();
                                List<BlockDropEntry> drops = gson.fromJson(json, listType);
                                if (drops != null) {
                                    allDrops.addAll(drops);
                                }
                            } catch (Exception e2) {
                                LOGGER.error("Failed to parse block drops file: {}", file, e2);
                            }
                        }
                        
                    } catch (IOException e) {
                        LOGGER.error("Failed to read block drops file: {}", file, e);
                    }
                });
                
        } catch (IOException e) {
            LOGGER.error("Failed to list files in directory: {}", directory, e);
        }
        
        LOGGER.debug("Loaded {} block drops from {}", allDrops.size(), directory);
        return allDrops;
    }
    
    /**
     * Loads custom messages for events.
     */
    private void loadMessages() {
        try {
            Path messagesFile = Paths.get(CONFIG_DIR, BLOCKS_DIR, MESSAGES_FILE);
            
            if (!Files.exists(messagesFile)) {
                createDefaultMessagesFile(messagesFile);
                return;
            }
            
            String json = new String(Files.readAllBytes(messagesFile));
            Gson gson = new Gson();
            
            Type mapType = new TypeToken<Map<String, Map<String, String>>>(){}.getType();
            Map<String, Map<String, String>> messages = gson.fromJson(json, mapType);
            
            if (messages != null) {
                Map<String, String> enableMessages = messages.get("enable");
                Map<String, String> disableMessages = messages.get("disable");
                
                eventManager.setEventMessages(enableMessages, disableMessages);
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to load messages configuration", e);
        }
    }
    
    /**
     * Creates a default messages configuration file.
     */
    private void createDefaultMessagesFile(Path messagesFile) throws IOException {
        Map<String, Map<String, String>> defaultMessages = new HashMap<>();
        
        Map<String, String> enableMessages = new HashMap<>();
        enableMessages.put("winter", "§6[Block Events] §b❄ Winter event has been enabled! §f(Special winter drops)");
        enableMessages.put("summer", "§6[Block Events] §e☀ Summer event has been enabled! §6(Hot summer drops)");
        enableMessages.put("easter", "§6[Block Events] §d🐰 Easter event has been enabled! §a(Easter egg drops)");
        enableMessages.put("halloween", "§6[Block Events] §c🎃 Halloween event has been enabled! §6(Spooky drops)");
        
        Map<String, String> disableMessages = new HashMap<>();
        disableMessages.put("winter", "§6[Block Events] §7❄ Winter event has been disabled!");
        disableMessages.put("summer", "§6[Block Events] §7☀ Summer event has been disabled!");
        disableMessages.put("easter", "§6[Block Events] §7🐰 Easter event has been disabled!");
        disableMessages.put("halloween", "§6[Block Events] §7🎃 Halloween event has been disabled!");
        
        defaultMessages.put("enable", enableMessages);
        defaultMessages.put("disable", disableMessages);
        
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(defaultMessages);
        Files.write(messagesFile, json.getBytes());
        
        LOGGER.info("Created default messages configuration file");
    }
}
