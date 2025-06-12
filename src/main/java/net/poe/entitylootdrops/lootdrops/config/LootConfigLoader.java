package net.poe.entitylootdrops.lootdrops.config;

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

import net.poe.entitylootdrops.lootdrops.config.EventConfig;
import net.poe.entitylootdrops.lootdrops.events.LootEventManager;
import net.poe.entitylootdrops.lootdrops.model.CustomDropEntry;
import net.poe.entitylootdrops.lootdrops.model.EntityDropEntry;

/**
 * Handles loading of loot drop configurations from files.
 */
public class LootConfigLoader {
    private static final Logger LOGGER = LogManager.getLogger();

    // Configuration directory paths
    private static final String CONFIG_DIR = "config/EntityLootDrops";
    private static final String LOOT_DROPS_DIR = "Loot Drops";
    private static final String NORMAL_DROPS_DIR = "Normal Drops";
    private static final String EVENT_DROPS_DIR = "Event Drops";
    private static final String MOBS_DIR = "Mobs";
    private static final String MESSAGES_FILE = "messages.json";

    // Built-in event types
    private static final String[] EVENT_TYPES = {"Winter", "Summer", "Easter", "Halloween"};

    private final LootConfigManager configManager;
    private final LootEventManager eventManager;

    public LootConfigLoader(LootConfigManager configManager, LootEventManager eventManager) {
        this.configManager = configManager;
        this.eventManager = eventManager;
    }

    /**
     * Loads all loot drop configurations.
     */
    public void loadConfig() {
        LOGGER.info("Starting loot configuration loading...");

        // Store current active events
        Set<String> previousActiveEvents = new HashSet<>(eventManager.getActiveEvents());
        boolean previousDropChanceState = eventManager.isDropChanceEventActive();
        boolean previousDoubleDropsState = eventManager.isDoubleDropsActive();
        boolean previousDebugState = eventManager.isDebugLoggingEnabled();

        // Clear existing configurations
        configManager.clearConfigurations();

        // Create directories if they don't exist
        createConfigDirectories();

        // Load EventConfig (this will create EventConfig.json if it doesn't exist)
        try {
            EventConfig.loadConfig();
            LOGGER.info("EventConfig loaded successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to load EventConfig", e);
        }

        // Create Active_Events.json if it doesn't exist
        try {
            eventManager.createActiveEventsFile();
            LOGGER.info("Active_Events.json created/verified");
        } catch (Exception e) {
            LOGGER.error("Failed to create Active_Events.json", e);
        }

        // Load all drop configurations
        loadAllDrops();

        // Load custom message configurations
        loadMessages();

        // Restore previous state if no state was loaded
        eventManager.restorePreviousState(previousActiveEvents, previousDropChanceState, previousDoubleDropsState, previousDebugState);

        LOGGER.info("Loot configuration loading completed. Entity drops: {}, Hostile drops: {}",
                configManager.getEntityDropsCount(), configManager.getHostileDropsCount());
    }

    /**
     * Creates the necessary directory structure for loot configurations.
     */
    private void createConfigDirectories() {
        LOGGER.info("Creating configuration directories...");

        try {
            // Create main config directory
            Path configDir = Paths.get(CONFIG_DIR);
            Files.createDirectories(configDir);

            // Create "Loot Drops" directory
            Path lootDropsDir = configDir.resolve(LOOT_DROPS_DIR);
            Files.createDirectories(lootDropsDir);

            // Create "Normal Drops" directory
            Path normalDropsDir = lootDropsDir.resolve(NORMAL_DROPS_DIR);
            Files.createDirectories(normalDropsDir);

            // Create "Event Drops" directory
            Path eventDropsDir = lootDropsDir.resolve(EVENT_DROPS_DIR);
            Files.createDirectories(eventDropsDir);

            // Create Mobs directory in Normal Drops
            Path normalMobsDir = normalDropsDir.resolve(MOBS_DIR);
            Files.createDirectories(normalMobsDir);

            // Check if this is the first time setup
            boolean isFirstTimeSetup = !hasAnyJsonFiles(normalDropsDir);

            // Create event type directories with their Mobs folders
            for (String eventType : EVENT_TYPES) {
                Path eventTypeDir = eventDropsDir.resolve(eventType);
                Files.createDirectories(eventTypeDir);

                Path eventMobsDir = eventTypeDir.resolve(MOBS_DIR);
                Files.createDirectories(eventMobsDir);

                // Create examples only on first setup
                if (isFirstTimeSetup) {
                    createExampleHostileDrops(eventTypeDir, eventType);
                    createExampleMobDrops(eventMobsDir, eventType.toLowerCase());
                }
            }

            // Create example configurations for normal drops
            if (isFirstTimeSetup) {
                createExampleHostileDrops(normalDropsDir, "normal");
                createExampleMobDrops(normalMobsDir, "normal");
            }

            // Create messages file
            Path messagesFile = lootDropsDir.resolve(MESSAGES_FILE);
            if (!Files.exists(messagesFile)) {
                createDefaultMessagesFile(messagesFile);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to create config directories", e);
        }
    }

    /**
     * Checks if a directory has any JSON files.
     */
    private boolean hasAnyJsonFiles(Path directory) {
        try {
            if (!Files.exists(directory)) {
                return false;
            }
            return Files.walk(directory)
                    .anyMatch(path -> path.toString().endsWith(".json"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Loads all drop configurations from the file system.
     */
    private void loadAllDrops() {
        try {
            Path lootDropsDir = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR);
            if (!Files.exists(lootDropsDir)) {
                return;
            }

            // Load normal drops
            Path normalDropsDir = lootDropsDir.resolve(NORMAL_DROPS_DIR);
            if (Files.exists(normalDropsDir)) {
                loadDropsFromDirectory(normalDropsDir, NORMAL_DROPS_DIR);
            }

            // Load event drops
            Path eventDropsDir = lootDropsDir.resolve(EVENT_DROPS_DIR);
            if (Files.exists(eventDropsDir)) {
                Files.list(eventDropsDir)
                        .filter(Files::isDirectory)
                        .forEach(eventDir -> {
                            String eventName = eventDir.getFileName().toString();
                            loadDropsFromDirectory(eventDir, eventName);
                        });
            }

        } catch (Exception e) {
            LOGGER.error("Failed to load drop configurations", e);
        }
    }

    /**
     * Loads drops from a specific directory (either normal or event-specific).
     */
    private void loadDropsFromDirectory(Path directory, String dirKey) {
        try {
            // Load global drops (JSON files recursively, excluding Mobs folders and messages.json)
            List<CustomDropEntry> globalDrops = loadGlobalDropsFromCategoryDirectory(directory);
            if (!globalDrops.isEmpty()) {
                configManager.setHostileDrops(dirKey, globalDrops);
                LOGGER.debug("Loaded {} hostile drops for category: {}", globalDrops.size(), dirKey);
            }

            // Load entity-specific drops from Mobs directories
            List<EntityDropEntry> entityDrops = loadEntityDropsFromMobsDirectories(directory);
            if (!entityDrops.isEmpty()) {
                configManager.setEntityDrops(dirKey, entityDrops);
                LOGGER.debug("Loaded {} entity drops for category: {}", entityDrops.size(), dirKey);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to load drops from directory: {}", directory, e);
        }
    }

    /**
     * Loads global drops from JSON files recursively, excluding Mobs folders.
     */
    private List<CustomDropEntry> loadGlobalDropsFromCategoryDirectory(Path categoryDir) {
        List<CustomDropEntry> allDrops = new ArrayList<>();

        try {
            Files.walk(categoryDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .filter(path -> !path.getFileName().toString().equals(MESSAGES_FILE))
                    .filter(path -> !isInMobsDirectory(path))
                    .forEach(jsonFile -> {
                        try {
                            String json = new String(Files.readAllBytes(jsonFile));
                            if (json.trim().isEmpty()) return;

                            Gson gson = new Gson();
                            CustomDropEntry[] drops = gson.fromJson(json, CustomDropEntry[].class);
                            if (drops != null) {
                                for (CustomDropEntry drop : drops) {
                                    if (drop != null && drop.getItemId() != null) {
                                        allDrops.add(drop);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error("Failed to load drops from file: {}", jsonFile, e);
                        }
                    });
        } catch (Exception e) {
            LOGGER.error("Failed to walk directory: {}", categoryDir, e);
        }

        return allDrops;
    }

    /**
     * Loads entity-specific drops from all Mobs directories within a category.
     */
    private List<EntityDropEntry> loadEntityDropsFromMobsDirectories(Path categoryDir) {
        List<EntityDropEntry> allEntityDrops = new ArrayList<>();

        try {
            Files.walk(categoryDir)
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().equals(MOBS_DIR))
                    .forEach(mobsDir -> {
                        try {
                            Files.list(mobsDir)
                                    .filter(Files::isRegularFile)
                                    .filter(path -> path.toString().endsWith(".json"))
                                    .forEach(jsonFile -> {
                                        try {
                                            String json = new String(Files.readAllBytes(jsonFile));
                                            if (json.trim().isEmpty()) return;

                                            Gson gson = new Gson();
                                            EntityDropEntry[] drops = gson.fromJson(json, EntityDropEntry[].class);
                                            if (drops != null) {
                                                for (EntityDropEntry drop : drops) {
                                                    if (drop != null && drop.getEntityId() != null) {
                                                        allEntityDrops.add(drop);
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            LOGGER.error("Failed to load entity drops from file: {}", jsonFile, e);
                                        }
                                    });
                        } catch (Exception e) {
                            LOGGER.error("Failed to list files in Mobs directory: {}", mobsDir, e);
                        }
                    });
        } catch (Exception e) {
            LOGGER.error("Failed to find Mobs directories in: {}", categoryDir, e);
        }

        return allEntityDrops;
    }

    /**
     * Checks if a path is within a Mobs directory.
     */
    private boolean isInMobsDirectory(Path path) {
        return path.toString().contains(MOBS_DIR);
    }

    /**
     * Creates example hostile drop files with themed content based on category.
     */
    private void createExampleHostileDrops(Path directory, String category) throws IOException {
        // For Normal Drops, create both main file and example file
        if ("normal".equals(category)) {
            // Create main Global_Hostile_Drops.json file
            Path mainFile = directory.resolve("Global_Hostile_Drops.json");
            if (!Files.exists(mainFile)) {
                List<CustomDropEntry> basicDrops = createBasicHostileDrops();
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Files.write(mainFile, gson.toJson(basicDrops).getBytes());
            }

            // Create example file
            Path exampleFile = directory.resolve("Global_Hostile_Drops_Example.json");
            if (!Files.exists(exampleFile)) {
                List<CustomDropEntry> examples = createThemedHostileDrops(category);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Files.write(exampleFile, gson.toJson(examples).getBytes());
            }
        } else {
            // For Event Drops, create main file
            Path mainFile = directory.resolve("Global_Hostile_Drops.json");
            if (!Files.exists(mainFile)) {
                List<CustomDropEntry> examples = createThemedHostileDrops(category);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Files.write(mainFile, gson.toJson(examples).getBytes());
            }
        }
    }

    /**
     * Creates basic hostile drops for the main configuration file.
     */
    private List<CustomDropEntry> createBasicHostileDrops() {
        List<CustomDropEntry> basicDrops = new ArrayList<>();

        // Simple gold nugget drop
        CustomDropEntry basicDrop = new CustomDropEntry();
        basicDrop.setComment("Basic example - Gold nuggets from hostile mobs");
        basicDrop.setItemId("minecraft:gold_nugget");
        basicDrop.setDropChance(15.0f);
        basicDrop.setMinAmount(1);
        basicDrop.setMaxAmount(3);
        basicDrop.setRequirePlayerKill(true);
        basicDrop.setCommand("tellraw @a {\"text\":\"A hostile mob was killed!\",\"color\":\"yellow\"}");
        basicDrop.setCommandChance(25.0f);
        basicDrop.setDropCommand("tellraw @a {\"text\":\"Gold nuggets dropped!\",\"color\":\"gold\"}");
        basicDrop.setDropCommandChance(100.0f);
        basicDrop.setExtraDropChance(10.0f);
        basicDrop.setExtraAmountMin(1);
        basicDrop.setExtraAmountMax(2);
        basicDrops.add(basicDrop);

        return basicDrops;
    }

    /**
     * Creates themed hostile drops based on category.
     */
    private List<CustomDropEntry> createThemedHostileDrops(String category) {
        List<CustomDropEntry> examples = new ArrayList<>();

        switch (category.toLowerCase()) {
            case "winter":
                // Ice and snow themed drops
                CustomDropEntry winterDrop = new CustomDropEntry();
                winterDrop.setComment("Winter event - Ice shards from hostile mobs in cold biomes");
                winterDrop.setItemId("minecraft:ice");
                winterDrop.setDropChance(25.0f);
                winterDrop.setMinAmount(1);
                winterDrop.setMaxAmount(2);
                winterDrop.setRequiredBiome("minecraft:snowy_plains");
                winterDrop.setRequiredWeather("clear");
                winterDrop.setDropCommand("tellraw @a {\"text\":\"Ice shards dropped in the winter cold!\",\"color\":\"aqua\"}");
                winterDrop.setDropCommandChance(50.0f);
                examples.add(winterDrop);
                break;

            case "summer":
                // Fire and heat themed drops
                CustomDropEntry summerDrop = new CustomDropEntry();
                summerDrop.setComment("Summer event - Blaze powder from hostile mobs in hot biomes");
                summerDrop.setItemId("minecraft:blaze_powder");
                summerDrop.setDropChance(20.0f);
                summerDrop.setMinAmount(1);
                summerDrop.setMaxAmount(1);
                summerDrop.setRequiredBiome("minecraft:desert");
                summerDrop.setRequiredTime("day");
                summerDrop.setDropCommand("tellraw @a {\"text\":\"The summer heat intensifies the drop!\",\"color\":\"gold\"}");
                summerDrop.setDropCommandChance(30.0f);
                examples.add(summerDrop);
                break;

            case "easter":
                // Colorful and spring themed drops
                CustomDropEntry easterDrop = new CustomDropEntry();
                easterDrop.setComment("Easter event - Colorful eggs from hostile mobs");
                easterDrop.setItemId("minecraft:egg");
                easterDrop.setDropChance(30.0f);
                easterDrop.setMinAmount(2);
                easterDrop.setMaxAmount(4);
                easterDrop.setRequiredTime("dawn");
                easterDrop.setDropCommand("tellraw @a {\"text\":\"Easter eggs have been found!\",\"color\":\"yellow\"}");
                easterDrop.setDropCommandChance(75.0f);
                examples.add(easterDrop);
                break;

            case "halloween":
                // Spooky themed drops
                CustomDropEntry halloweenDrop = new CustomDropEntry();
                halloweenDrop.setComment("Halloween event - Spooky drops from hostile mobs at night");
                halloweenDrop.setItemId("minecraft:pumpkin");
                halloweenDrop.setDropChance(35.0f);
                halloweenDrop.setMinAmount(1);
                halloweenDrop.setMaxAmount(2);
                halloweenDrop.setRequiredTime("night");
                halloweenDrop.setDropCommand("tellraw @a {\"text\":\"A spooky pumpkin appears!\",\"color\":\"dark_purple\"}");
                halloweenDrop.setDropCommandChance(100.0f);
                examples.add(halloweenDrop);
                break;

            case "normal":
            default:
                // Comprehensive example showing all features
                CustomDropEntry comprehensiveDrop = new CustomDropEntry();
                comprehensiveDrop.setComment("Comprehensive example showing ALL available options");
                comprehensiveDrop.setItemId("minecraft:diamond");
                comprehensiveDrop.setDropChance(5.0f);
                comprehensiveDrop.setMinAmount(1);
                comprehensiveDrop.setMaxAmount(1);
                comprehensiveDrop.setExtraDropChance(10.0f);
                comprehensiveDrop.setExtraAmountMin(1);
                comprehensiveDrop.setExtraAmountMax(2);
                comprehensiveDrop.setRequiredAdvancement("minecraft:story/mine_diamond");
                comprehensiveDrop.setRequiredEffect("minecraft:luck");
                comprehensiveDrop.setRequiredBiome("minecraft:desert");
                comprehensiveDrop.setRequiredDimension("minecraft:overworld");
                comprehensiveDrop.setRequiredEquipment("minecraft:bow");
                comprehensiveDrop.setRequiredWeather("clear");
                comprehensiveDrop.setRequiredTime("night");
                comprehensiveDrop.setCommand("tellraw @a {\"text\":\"A mob was killed in the desert at night!\",\"color\":\"yellow\"}");
                comprehensiveDrop.setCommandChance(25.0f);
                comprehensiveDrop.setDropCommand("tellraw @a {\"text\":\"Rare diamond drop!\",\"color\":\"aqua\"}");
                comprehensiveDrop.setDropCommandChance(100.0f);
                examples.add(comprehensiveDrop);
                break;
        }

        return examples;
    }

    /**
     * Creates example mob drop files with themed content.
     */
    private void createExampleMobDrops(Path mobsDir, String category) throws IOException {
        String fileName = category.equals("normal") ? "zombie_example.json" : "zombie_" + category + ".json";
        Path exampleFile = mobsDir.resolve(fileName);

        if (!Files.exists(exampleFile)) {
            List<EntityDropEntry> examples = new ArrayList<>();

            EntityDropEntry zombieDrop = new EntityDropEntry();
            zombieDrop.setComment("Example zombie drops for " + category + " category");
            zombieDrop.setEntityId("minecraft:zombie");
            zombieDrop.setItemId(getThemedItem(category));
            zombieDrop.setDropChance(20.0f);
            zombieDrop.setMinAmount(1);
            zombieDrop.setMaxAmount(2);

            if (!category.equals("normal")) {
                zombieDrop.setRequiredTime(getThemedTime(category));
            }

            examples.add(zombieDrop);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.write(exampleFile, gson.toJson(examples).getBytes());
        }
    }

    /**
     * Gets a themed item based on category.
     */
    private String getThemedItem(String category) {
        switch (category.toLowerCase()) {
            case "winter": return "minecraft:snowball";
            case "summer": return "minecraft:fire_charge";
            case "easter": return "minecraft:carrot";
            case "halloween": return "minecraft:rotten_flesh";
            default: return "minecraft:iron_ingot";
        }
    }

    /**
     * Gets a themed time requirement based on category.
     */
    private String getThemedTime(String category) {
        switch (category.toLowerCase()) {
            case "winter": return "night";
            case "summer": return "day";
            case "easter": return "dawn";
            case "halloween": return "night";
            default: return null;
        }
    }

    /**
     * Creates the default messages.json file.
     */
    private void createDefaultMessagesFile(Path messagesFile) throws IOException {
        Map<String, Map<String, String>> messages = new HashMap<>();

        Map<String, String> enableMessages = new HashMap<>();
        enableMessages.put("Winter", "§b[Events] §fWinter event has begun! Ice-themed drops are now active!");
        enableMessages.put("Summer", "§6[Events] §fSummer event has begun! Fire-themed drops are now active!");
        enableMessages.put("Easter", "§e[Events] §fEaster event has begun! Colorful drops are now active!");
        enableMessages.put("Halloween", "§5[Events] §fHalloween event has begun! Spooky drops are now active!");

        Map<String, String> disableMessages = new HashMap<>();
        disableMessages.put("Winter", "§b[Events] §7Winter event has ended.");
        disableMessages.put("Summer", "§6[Events] §7Summer event has ended.");
        disableMessages.put("Easter", "§e[Events] §7Easter event has ended.");
        disableMessages.put("Halloween", "§5[Events] §7Halloween event has ended.");

        messages.put("enable", enableMessages);
        messages.put("disable", disableMessages);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.write(messagesFile, gson.toJson(messages).getBytes());
    }

    /**
     * Loads custom messages from the messages.json file.
     */
    private void loadMessages() {
        LOGGER.info("Loading custom messages from messages.json...");
        try {
            Path messagesFile = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, MESSAGES_FILE);
            if (Files.exists(messagesFile)) {
                String json = new String(Files.readAllBytes(messagesFile));
                if (!json.trim().isEmpty()) {
                    Gson gson = new Gson();
                    Type mapType = new TypeToken<Map<String, Map<String, String>>>(){}.getType();
                    Map<String, Map<String, String>> messages = gson.fromJson(json, mapType);

                    Map<String, String> enableMessages = messages.get("enable");
                    Map<String, String> disableMessages = messages.get("disable");

                    if (enableMessages != null && disableMessages != null) {
                        eventManager.setEventMessages(enableMessages, disableMessages);
                        LOGGER.debug("Loaded custom messages configuration");
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load messages configuration", e);
        }
    }
}