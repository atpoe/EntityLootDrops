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

                // Only create initial example files on first setup
                if (isFirstTimeSetup) {
                    createInitialExamples(eventTypeDir);
                }
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

            // Single example: Complex diamond drop with all features
            CustomBlockDropEntry example = new CustomBlockDropEntry();
            example.setItemId("minecraft:diamond");
            example.setDropChance(5.0f);
            example.setMinAmount(1);
            example.setMaxAmount(1);
            example.setNbtData("{display:{Name:'{\"text\":\"Magical Diamond\",\"color\":\"aqua\",\"italic\":false}',Lore:['{\"text\":\"A rare diamond with magical properties\",\"color\":\"gray\",\"italic\":false}']},Enchantments:[{id:\"minecraft:unbreaking\",lvl:3}]}");
            example.setRequirePlayerBreak(true);
            example.setRequiredTool("minecraft:diamond_pickaxe");
            example.setRequiredEnchantment("minecraft:fortune");
            example.setRequiredEnchantLevel(2);
            example.setCommand("say %player% found a magical diamond!");
            example.setCommandChance(50.0f);
            example.setCanRegenerate(true);
            example.setBrokenBlockReplace("minecraft:bedrock");
            example.setRespawnTime(30);
            example.setComment("Example global drop with NBT, enchantments, commands, and regeneration");

            examples.add(example);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.write(globalDropsFile, gson.toJson(examples).getBytes());
        }

        // Create Block Types directory
        Path blockTypesDir = directory.resolve(BLOCK_TYPES_DIR);
        Files.createDirectories(blockTypesDir);
    }

    /**
     * Creates initial example files only once during first setup.
     */
    private void createInitialExamples(Path directory) throws IOException {
        Path blockTypesDir = directory.resolve(BLOCK_TYPES_DIR);

        // Single example block-specific drop
        Path exampleBlockFile = blockTypesDir.resolve("stone_drops.json");
        if (!Files.exists(exampleBlockFile)) {
            BlockDropEntry example = new BlockDropEntry();
            example.setBlockId("minecraft:stone");
            example.setItemId("minecraft:emerald");
            example.setDropChance(5.0f);
            example.setMinAmount(1);
            example.setMaxAmount(2);
            example.setNbtData("{display:{Name:'{\"text\":\"Stone Emerald\",\"color\":\"green\",\"italic\":false}',Lore:['{\"text\":\"Found in stone blocks\",\"color\":\"gray\",\"italic\":false}']}}");
            example.setRequirePlayerBreak(true);
            example.setRequiredTool("minecraft:diamond_pickaxe");
            example.setCanRegenerate(true);
            example.setBrokenBlockReplace("minecraft:cobblestone");
            example.setRespawnTime(60);
            example.setComment("Example stone drop with NBT data and regeneration");

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.write(exampleBlockFile, gson.toJson(example).getBytes());
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
     * Loads global drops from a JSON file.
     */
    private List<CustomBlockDropEntry> loadGlobalDropsFromFile(Path file) {
        try {
            String json = new String(Files.readAllBytes(file));
            Gson gson = new Gson();

            Type listType = new TypeToken<List<CustomBlockDropEntry>>(){}.getType();
            List<CustomBlockDropEntry> drops = gson.fromJson(json, listType);

            return drops != null ? drops : new ArrayList<>();

        } catch (Exception e) {
            LOGGER.error("Failed to load global drops from file: {}", file, e);
            return new ArrayList<>();
        }
    }

    /**
     * Loads block drops from a directory containing JSON files.
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

                            // Try to parse as single entry first
                            try {
                                BlockDropEntry singleEntry = gson.fromJson(json, BlockDropEntry.class);
                                if (singleEntry != null && singleEntry.getItemId() != null) {
                                    allDrops.add(singleEntry);
                                    return;
                                }
                            } catch (Exception ignored) {}

                            // Try to parse as array
                            Type listType = new TypeToken<List<BlockDropEntry>>(){}.getType();
                            List<BlockDropEntry> entries = gson.fromJson(json, listType);
                            if (entries != null) {
                                allDrops.addAll(entries);
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
        blockEntry.setRequiredEnchantment(customEntry.getRequiredEnchantment());
        blockEntry.setRequiredEnchantLevel(customEntry.getRequiredEnchantLevel());

        // Copy advanced properties
        blockEntry.setNbtData(customEntry.getNbtData());
        blockEntry.setCommand(customEntry.getCommand());
        blockEntry.setCommandChance(customEntry.getCommandChance());

        // Copy regeneration properties
        blockEntry.setCanRegenerate(customEntry.canRegenerate());
        blockEntry.setBrokenBlockReplace(customEntry.getBrokenBlockReplace());
        blockEntry.setRespawnTime(customEntry.getRespawnTime());

        // Copy comment
        blockEntry.setComment(customEntry.getComment());

        return blockEntry;
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
        enableMessages.put("dropchance", "§6[Block Events] §aDouble drop chance event has been enabled! §7(2x drop rates)");
        enableMessages.put("doubledrops", "§6[Block Events] §aDouble drops event has been enabled! §7(2x drop amounts)");
        enableMessages.put("winter", "§6[Block Events] §bWinter event has been enabled!");
        enableMessages.put("summer", "§6[Block Events] §eSummer event has been enabled!");
        enableMessages.put("easter", "§6[Block Events] §dEaster event has been enabled!");
        enableMessages.put("halloween", "§6[Block Events] §cHalloween event has been enabled!");

        Map<String, String> disableMessages = new HashMap<>();
        disableMessages.put("dropchance", "§6[Block Events] §7Double drop chance event has been disabled!");
        disableMessages.put("doubledrops", "§6[Block Events] §7Double drops event has been disabled!");
        disableMessages.put("winter", "§6[Block Events] §7Winter event has been disabled!");
        disableMessages.put("summer", "§6[Block Events] §7Summer event has been disabled!");
        disableMessages.put("easter", "§6[Block Events] §7Easter event has been disabled!");
        disableMessages.put("halloween", "§6[Block Events] §7alloween event has been disabled!");

        defaultMessages.put("enable", enableMessages);
        defaultMessages.put("disable", disableMessages);

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        Files.write(messagesFile, gson.toJson(defaultMessages).getBytes());
    }
}