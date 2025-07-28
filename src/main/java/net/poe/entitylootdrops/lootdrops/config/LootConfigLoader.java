package net.poe.entitylootdrops.lootdrops.config;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
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

        // Create example files on first load
        createExampleFiles();

        // Check and regenerate Global_Hostile_Drops.json if needed
        checkAndRegenerateGlobalHostileDrops();

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

        // Load active events from Active_Events.json
        boolean activeEventsLoaded = false;
        try {
            eventManager.loadActiveEventsState();
            activeEventsLoaded = true;
            LOGGER.info("Active events loaded from Active_Events.json");
        } catch (Exception e) {
            LOGGER.error("Failed to load active events from file", e);
        }

        // Load all drop configurations
        loadAllDrops();

        // Load custom message configurations
        loadMessages();

        // Restore previous state only if no active events were loaded from file
        if (!activeEventsLoaded) {
            eventManager.restorePreviousState(previousActiveEvents, previousDropChanceState, previousDoubleDropsState, previousDebugState);
            LOGGER.info("Used previous state as fallback since no active events file was loaded");
        }
    }

    /**
     * Creates the necessary directory structure for loot configurations.
     */
    private void createConfigDirectories() {
        LOGGER.info("Creating configuration directories...");

        try {
            // Create main loot drops directory
            Path lootDropsDir = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR);
            Files.createDirectories(lootDropsDir);

            // Create normal drops directory and mobs subdirectory
            Path normalDropsDir = lootDropsDir.resolve(NORMAL_DROPS_DIR);
            Files.createDirectories(normalDropsDir);
            Files.createDirectories(normalDropsDir.resolve(MOBS_DIR));

            // Create event drops directory
            Path eventDropsDir = lootDropsDir.resolve(EVENT_DROPS_DIR);
            Files.createDirectories(eventDropsDir);

            // Create built-in event directories
            for (String eventType : EVENT_TYPES) {
                Path eventDir = eventDropsDir.resolve(eventType);
                Files.createDirectories(eventDir);
                Files.createDirectories(eventDir.resolve(MOBS_DIR));
            }

            LOGGER.info("Configuration directories created successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to create configuration directories", e);
        }
    }

    /**
     * Creates example files if directories are empty of JSON files.
     */
    private void createExampleFiles() {
        LOGGER.info("Checking and creating example files...");

        try {
            // Check Normal Drops directory
            Path normalDropsDir = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, NORMAL_DROPS_DIR);
            if (isDirectoryEmptyOfJsonFiles(normalDropsDir)) {
                createNormalDropsExamples();
            }

            // Check Event Drops directories
            Path eventDropsDir = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, EVENT_DROPS_DIR);
            for (String eventType : EVENT_TYPES) {
                Path eventDir = eventDropsDir.resolve(eventType);
                boolean eventDirWasEmpty = isDirectoryEmptyOfJsonFiles(eventDir);

                if (eventDirWasEmpty) {
                    createEventDropsExample(eventType);
                }

                // Only create mobs examples if the parent event directory was also empty (first time setup)
                Path eventMobsDir = eventDir.resolve(MOBS_DIR);
                if (eventDirWasEmpty && isDirectoryEmptyOfAnyFiles(eventMobsDir)) {
                    createEventMobsExample(eventType);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed to create example files", e);
        }
    }

    /**
     * Checks if Global_Hostile_Drops.json exists and recreates it if deleted.
     */
    private void checkAndRegenerateGlobalHostileDrops() {
        try {
            Path globalHostileDropsFile = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, NORMAL_DROPS_DIR, "Global_Hostile_Drops.json");

            if (!Files.exists(globalHostileDropsFile)) {
                LOGGER.info("Global_Hostile_Drops.json not found, regenerating...");
                createGlobalHostileDropsFile();
            }

        } catch (Exception e) {
            LOGGER.error("Failed to check/regenerate Global_Hostile_Drops.json", e);
        }
    }

    /**
     * Checks if a directory is empty of JSON files.
     */
    private boolean isDirectoryEmptyOfJsonFiles(Path directory) {
        try {
            if (!Files.exists(directory)) {
                return true;
            }

            return Files.walk(directory, 1)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .findFirst()
                    .isEmpty();

        } catch (Exception e) {
            LOGGER.error("Failed to check if directory is empty of JSON files: {}", directory, e);
            return false;
        }
    }

    /**
     * Checks if a directory is empty of any files (not just JSON files).
     */
    private boolean isDirectoryEmptyOfAnyFiles(Path directory) {
        try {
            if (!Files.exists(directory)) {
                return true;
            }

            return Files.walk(directory, 1)
                    .filter(Files::isRegularFile)
                    .findFirst()
                    .isEmpty();

        } catch (Exception e) {
            LOGGER.error("Failed to check if directory is empty of any files: {}", directory, e);
            return false;
        }
    }

    /**
     * Creates example files for normal drops.
     */
    private void createNormalDropsExamples() {
        LOGGER.info("Creating normal drops examples...");
        createGlobalHostileDropsFile();
    }

    /**
     * Creates the Global_Hostile_Drops.json file.
     */
    private void createGlobalHostileDropsFile() {
        try {
            Path globalFile = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, NORMAL_DROPS_DIR, "Global_Hostile_Drops.json");

            String exampleJson = "[\n" +
                    "  {\n" +
                    "    \"requiredDimension\": \"minecraft:overworld\",\n" +
                    "    \"itemId\": \"minecraft:diamond\",\n" +
                    "    \"dropChance\": 5.0,\n" +
                    "    \"minAmount\": 1,\n" +
                    "    \"maxAmount\": 1,\n" +
                    "    \"command\": \"effect give {player} minecraft:speed 10 1\",\n" +
                    "    \"commandChance\": 100.0,\n" +
                    "    \"commandCoolDown\": 300,\n" +
                    "    \"_comment\": \"Speed effect on diamond drop\",\n" +
                    "    \"requirePlayerKill\": true,\n" +
                    "    \"extraDropChance\": 10.0,\n" +
                    "    \"extraAmountMin\": 1,\n" +
                    "    \"extraAmountMax\": 3\n" +
                    "  }\n" +
                    "]";

            Files.writeString(globalFile, exampleJson, StandardCharsets.UTF_8);
            LOGGER.info("Created Global_Hostile_Drops.json");

            // Also create the comprehensive example file
            createGlobalHostileDropsExampleFile();

        } catch (Exception e) {
            LOGGER.error("Failed to create Global_Hostile_Drops.json", e);
        }
    }

    /**
     * Creates the comprehensive Global_Hostile_Drops.example file with all possible fields.
     */
    private void createGlobalHostileDropsExampleFile() {
        try {
            Path exampleFile = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, NORMAL_DROPS_DIR, "Global_Hostile_Drops.example");

            String exampleJson = "[\n" +
                    "  {\n" +
                    "    \"itemId\": \"minecraft:golden_apple\",\n" +
                    "    \"dropChance\": 0.5,\n" +
                    "    \"minAmount\": 1,\n" +
                    "    \"maxAmount\": 1,\n" +
                    "    \"nbtData\": \"{Enchantments:[{id:\\\"minecraft:sharpness\\\",lvl:5s}]}\",\n" +
                    "    \"requiredAdvancement\": \"minecraft:story/mine_diamond\",\n" +
                    "    \"requiredEffect\": \"minecraft:strength\",\n" +
                    "    \"requiredEquipment\": \"minecraft:diamond_sword\",\n" +
                    "    \"requiredWeather\": \"rain\",\n" +
                    "    \"requiredTime\": \"night\",\n" +
                    "    \"requiredDimension\": \"minecraft:overworld\",\n" +
                    "    \"requiredBiome\": \"minecraft:forest\",\n" +
                    "    \"command\": \"effect give {player} minecraft:regeneration 30 1\",\n" +
                    "    \"commandChance\": 50.0,\n" +
                    "    \"commandCoolDown\": 300,\n" +
                    "    \"dropCommand\": \"playsound minecraft:entity.player.levelup player {player}\",\n" +
                    "    \"dropCommandChance\": 100.0,\n" +
                    "    \"_comment\": \"Comprehensive example with all possible fields\",\n" +
                    "    \"requirePlayerKill\": true,\n" +
                    "    \"allowDefaultDrops\": true,\n" +
                    "    \"allowModIDs\": [\"examplemod:special_item\"],\n" +
                    "    \"extraDropChance\": 10.0,\n" +
                    "    \"extraAmountMin\": 1,\n" +
                    "    \"extraAmountMax\": 3\n" +
                    "  }\n" +
                    "]";

            Files.writeString(exampleFile, exampleJson, StandardCharsets.UTF_8);
            LOGGER.info("Created Global_Hostile_Drops.example");

        } catch (Exception e) {
            LOGGER.error("Failed to create Global_Hostile_Drops.example", e);
        }
    }

    /**
     * Creates example files for normal mobs.
     */
    private void createNormalMobsExamples() {
        LOGGER.info("Creating normal mobs examples...");

        try {
            Path mobsDir = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, NORMAL_DROPS_DIR, MOBS_DIR);

            // Create Zombie example
            Path zombieFile = mobsDir.resolve("Zombie_Example.json");
            String zombieJson = "[\n" +
                    "  {\n" +
                    "    \"entityId\": \"minecraft:zombie\",\n" +
                    "    \"itemId\": \"minecraft:iron_ingot\",\n" +
                    "    \"dropChance\": 10.0,\n" +
                    "    \"minAmount\": 1,\n" +
                    "    \"maxAmount\": 2,\n" +
                    "    \"_comment\": \"Iron ingot from zombies\",\n" +
                    "    \"requirePlayerKill\": true\n" +
                    "  }\n" +
                    "]";

            Files.writeString(zombieFile, zombieJson, StandardCharsets.UTF_8);
            LOGGER.info("Created Zombie_Example.json");

            // Create Skeleton example
            Path skeletonFile = mobsDir.resolve("Skeleton_Example.json");
            String skeletonJson = "[\n" +
                    "  {\n" +
                    "    \"entityId\": \"minecraft:skeleton\",\n" +
                    "    \"itemId\": \"minecraft:flint\",\n" +
                    "    \"dropChance\": 15.0,\n" +
                    "    \"minAmount\": 1,\n" +
                    "    \"maxAmount\": 3,\n" +
                    "    \"_comment\": \"Flint from skeletons\",\n" +
                    "    \"requirePlayerKill\": false\n" +
                    "  }\n" +
                    "]";

            Files.writeString(skeletonFile, skeletonJson, StandardCharsets.UTF_8);
            LOGGER.info("Created Skeleton_Example.json");

        } catch (Exception e) {
            LOGGER.error("Failed to create normal mobs examples", e);
        }
    }

    /**
     * Creates example files for event drops.
     */
    private void createEventDropsExample(String eventType) {
        LOGGER.info("Creating event drops example for: {}", eventType);

        try {
            Path eventDir = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, EVENT_DROPS_DIR, eventType);
            Path exampleFile = eventDir.resolve(eventType + "_Event_Drops_Example.json");

            String exampleJson = "";
            switch (eventType) {
                case "Winter":
                    exampleJson = "[\n" +
                            "  {\n" +
                            "    \"itemId\": \"minecraft:snowball\",\n" +
                            "    \"dropChance\": 20.0,\n" +
                            "    \"minAmount\": 2,\n" +
                            "    \"maxAmount\": 5,\n" +
                            "    \"_comment\": \"Winter event snowballs\",\n" +
                            "    \"requirePlayerKill\": true\n" +
                            "  }\n" +
                            "]";
                    break;
                case "Summer":
                    exampleJson = "[\n" +
                            "  {\n" +
                            "    \"itemId\": \"minecraft:sunflower\",\n" +
                            "    \"dropChance\": 15.0,\n" +
                            "    \"minAmount\": 1,\n" +
                            "    \"maxAmount\": 2,\n" +
                            "    \"_comment\": \"Summer event sunflowers\",\n" +
                            "    \"requirePlayerKill\": true\n" +
                            "  }\n" +
                            "]";
                    break;
                case "Easter":
                    exampleJson = "[\n" +
                            "  {\n" +
                            "    \"itemId\": \"minecraft:egg\",\n" +
                            "    \"dropChance\": 25.0,\n" +
                            "    \"minAmount\": 1,\n" +
                            "    \"maxAmount\": 3,\n" +
                            "    \"_comment\": \"Easter event eggs\",\n" +
                            "    \"requirePlayerKill\": true\n" +
                            "  }\n" +
                            "]";
                    break;
                case "Halloween":
                    exampleJson = "[\n" +
                            "  {\n" +
                            "    \"itemId\": \"minecraft:pumpkin\",\n" +
                            "    \"dropChance\": 30.0,\n" +
                            "    \"minAmount\": 1,\n" +
                            "    \"maxAmount\": 2,\n" +
                            "    \"_comment\": \"Halloween event pumpkins\",\n" +
                            "    \"requirePlayerKill\": true\n" +
                            "  }\n" +
                            "]";
                    break;
            }

            Files.writeString(exampleFile, exampleJson, StandardCharsets.UTF_8);
            LOGGER.info("Created {}_Event_Drops_Example.json", eventType);

        } catch (Exception e) {
            LOGGER.error("Failed to create event drops example for {}", eventType, e);
        }
    }

    /**
     * Creates example files for event mobs.
     */
    private void createEventMobsExample(String eventType) {
        LOGGER.info("Creating event mobs example for: {}", eventType);

        try {
            Path eventMobsDir = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, EVENT_DROPS_DIR, eventType, MOBS_DIR);
            Path exampleFile = eventMobsDir.resolve(eventType + "_Mob_Drops_Example.json");

            String exampleJson = "";
            switch (eventType) {
                case "Winter":
                    exampleJson = "[\n" +
                            "  {\n" +
                            "    \"entityId\": \"minecraft:zombie\",\n" +
                            "    \"itemId\": \"minecraft:ice\",\n" +
                            "    \"dropChance\": 12.0,\n" +
                            "    \"minAmount\": 1,\n" +
                            "    \"maxAmount\": 2,\n" +
                            "    \"_comment\": \"Winter event ice from zombie\",\n" +
                            "    \"requirePlayerKill\": true\n" +
                            "  }\n" +
                            "]";
                    break;
                case "Summer":
                    exampleJson = "[\n" +
                            "  {\n" +
                            "    \"entityId\": \"minecraft:skeleton\",\n" +
                            "    \"itemId\": \"minecraft:melon_slice\",\n" +
                            "    \"dropChance\": 18.0,\n" +
                            "    \"minAmount\": 2,\n" +
                            "    \"maxAmount\": 4,\n" +
                            "    \"_comment\": \"Summer event melon from skeleton\",\n" +
                            "    \"requirePlayerKill\": true\n" +
                            "  }\n" +
                            "]";
                    break;
                case "Easter":
                    exampleJson = "[\n" +
                            "  {\n" +
                            "    \"entityId\": \"minecraft:chicken\",\n" +
                            "    \"itemId\": \"minecraft:golden_egg\",\n" +
                            "    \"dropChance\": 5.0,\n" +
                            "    \"minAmount\": 1,\n" +
                            "    \"maxAmount\": 1,\n" +
                            "    \"_comment\": \"Easter event golden egg from chicken\",\n" +
                            "    \"requirePlayerKill\": false\n" +
                            "  }\n" +
                            "]";
                    break;
                case "Halloween":
                    exampleJson = "[\n" +
                            "  {\n" +
                            "    \"entityId\": \"minecraft:zombie\",\n" +
                            "    \"itemId\": \"minecraft:jack_o_lantern\",\n" +
                            "    \"dropChance\": 8.0,\n" +
                            "    \"minAmount\": 1,\n" +
                            "    \"maxAmount\": 1,\n" +
                            "    \"_comment\": \"Halloween event jack o lantern from zombie\",\n" +
                            "    \"requirePlayerKill\": true\n" +
                            "  }\n" +
                            "]";
                    break;
            }

            Files.writeString(exampleFile, exampleJson, StandardCharsets.UTF_8);
            LOGGER.info("Created {}_Mob_Drops_Example.json", eventType);

        } catch (Exception e) {
            LOGGER.error("Failed to create event mobs example for {}", eventType, e);
        }
    }

    /**
     * Loads all drop configurations from the loot drops directory.
     */
    private void loadAllDrops() {
        try {
            Path lootDropsDir = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR);
            if (!Files.exists(lootDropsDir)) {
                LOGGER.warn("Loot drops directory does not exist: {}", lootDropsDir);
                return;
            }

            // Load normal drops
            Path normalDropsDir = lootDropsDir.resolve(NORMAL_DROPS_DIR);
            if (Files.exists(normalDropsDir)) {
                LOGGER.info("Loading normal drops from: {}", normalDropsDir);
                loadDropsFromDirectory(normalDropsDir, NORMAL_DROPS_DIR);
            }

            // Load event drops
            Path eventDropsDir = lootDropsDir.resolve(EVENT_DROPS_DIR);
            if (Files.exists(eventDropsDir)) {
                LOGGER.info("Loading event drops from: {}", eventDropsDir);
                Files.walk(eventDropsDir, 1)
                        .filter(Files::isDirectory)
                        .filter(path -> !path.equals(eventDropsDir))
                        .forEach(eventDir -> {
                            String eventName = eventDir.getFileName().toString();
                            LOGGER.info("Loading event drops for: {}", eventName);
                            loadDropsFromDirectory(eventDir, eventName);
                        });
            }

        } catch (Exception e) {
            LOGGER.error("Failed to load drop configurations", e);
        }
    }

    /**
     * Loads drops from a specific directory.
     */
    private void loadDropsFromDirectory(Path directory, String dirKey) {
        try {
            // Load general drops (not in Mobs subdirectory)
            List<CustomDropEntry> generalDrops = loadGeneralDropsFromDirectory(directory);
            if (!generalDrops.isEmpty()) {
                configManager.setHostileDrops(dirKey, generalDrops);
                LOGGER.info("Loaded {} general drops for {}", generalDrops.size(), dirKey);
            }

            // Load entity-specific drops from Mobs directories
            List<EntityDropEntry> entityDrops = loadEntityDropsFromMobsDirectories(directory);
            if (!entityDrops.isEmpty()) {
                configManager.setEntityDrops(dirKey, entityDrops);
                LOGGER.info("Loaded {} entity drops for {}", entityDrops.size(), dirKey);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to load drops from directory: {}", directory, e);
        }
    }

    /**
     * Loads general (non-entity-specific) drops from a directory.
     */
    private List<CustomDropEntry> loadGeneralDropsFromDirectory(Path directory) {
        List<CustomDropEntry> allDrops = new ArrayList<>();

        try {
            Files.walk(directory, 1)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .filter(path -> !isInMobsDirectory(path))
                    .forEach(jsonFile -> {
                        try {
                            String json = new String(Files.readAllBytes(jsonFile));
                            if (json.trim().isEmpty()) {
                                LOGGER.warn("Empty JSON file: {}", jsonFile);
                                return;
                            }

                            Gson gson = new Gson();
                            CustomDropEntry[] drops = gson.fromJson(json, CustomDropEntry[].class);
                            if (drops != null) {
                                for (CustomDropEntry drop : drops) {
                                    if (drop != null) {
                                        allDrops.add(drop);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error("Failed to load general drops from file: {}", jsonFile, e);
                        }
                    });
        } catch (Exception e) {
            LOGGER.error("Failed to walk directory for general drops: {}", directory, e);
        }

        return allDrops;
    }

    /**
     * Loads entity-specific drops from all Mobs directories within a category.
     * Now supports nested folders within Mobs directories.
     */
    private List<EntityDropEntry> loadEntityDropsFromMobsDirectories(Path categoryDir) {
        List<EntityDropEntry> allEntityDrops = new ArrayList<>();

        try {
            LOGGER.info("Looking for Mobs directories in: {}", categoryDir);

            Files.walk(categoryDir)
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().equals(MOBS_DIR))
                    .forEach(mobsDir -> {
                        LOGGER.info("Found Mobs directory: {}", mobsDir);
                        try {
                            // Use Files.walk() to support nested folders within Mobs directory
                            Files.walk(mobsDir)
                                    .filter(Files::isRegularFile)
                                    .filter(path -> path.toString().endsWith(".json"))
                                    .forEach(jsonFile -> {
                                        LOGGER.info("Processing entity drops file: {}", jsonFile);
                                        try {
                                            String json = new String(Files.readAllBytes(jsonFile));
                                            if (json.trim().isEmpty()) {
                                                LOGGER.warn("Empty JSON file: {}", jsonFile);
                                                return;
                                            }

                                            Gson gson = new Gson();
                                            EntityDropEntry[] drops = gson.fromJson(json, EntityDropEntry[].class);
                                            if (drops != null) {
                                                for (EntityDropEntry drop : drops) {
                                                    if (drop != null && drop.getEntityId() != null) {
                                                        allEntityDrops.add(drop);
                                                        LOGGER.info("Loaded entity drop for {} from file: {}",
                                                                drop.getEntityId(), jsonFile);
                                                    }
                                                }
                                            } else {
                                                LOGGER.warn("Failed to parse JSON or got null from file: {}", jsonFile);
                                            }
                                        } catch (Exception e) {
                                            LOGGER.error("Failed to load entity drops from file: {}", jsonFile, e);
                                        }
                                    });
                        } catch (Exception e) {
                            LOGGER.error("Failed to walk Mobs directory: {}", mobsDir, e);
                        }
                    });
        } catch (Exception e) {
            LOGGER.error("Failed to find Mobs directories in: {}", categoryDir, e);
        }

        LOGGER.info("Total entity drops loaded: {}", allEntityDrops.size());
        return allEntityDrops;
    }

    /**
     * Checks if a path is within a Mobs directory.
     */
    private boolean isInMobsDirectory(Path path) {
        return path.toString().contains(MOBS_DIR);
    }

    /**
     * Loads custom message configurations with support for nested structure.
     */
    private void loadMessages() {
        try {
            Path messagesFile = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, MESSAGES_FILE);
            if (!Files.exists(messagesFile)) {
                LOGGER.info("Messages file does not exist, creating default: {}", messagesFile);
                createDefaultMessagesFile(messagesFile);
            }

            String json = Files.readString(messagesFile, StandardCharsets.UTF_8);
            if (json.trim().isEmpty()) {
                LOGGER.warn("Messages file is empty: {}", messagesFile);
                return;
            }

            LOGGER.debug("Loading messages from: {}", messagesFile);
            LOGGER.debug("JSON content: {}", json);

            try {
                JsonElement jsonElement = JsonParser.parseString(json);
                if (!jsonElement.isJsonObject()) {
                    LOGGER.error("Messages file must contain a JSON object, but found: {}", jsonElement.getClass().getSimpleName());
                    return;
                }

                JsonObject rootObject = jsonElement.getAsJsonObject();

                // Load enable messages
                if (rootObject.has("enable") && rootObject.get("enable").isJsonObject()) {
                    JsonObject enableMessages = rootObject.getAsJsonObject("enable");

                    for (Map.Entry<String, JsonElement> entry : enableMessages.entrySet()) {
                        String eventName = entry.getKey();
                        JsonElement value = entry.getValue();

                        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                            String message = value.getAsString();
                            eventManager.setEventEnableMessage(eventName, message);
                            LOGGER.info("Loaded enable message for {}: {}", eventName, message);
                        } else {
                            LOGGER.warn("Skipping invalid enable message for '{}': expected string but found {}",
                                    eventName, value.getClass().getSimpleName());
                        }
                    }
                }

                // Load disable messages
                if (rootObject.has("disable") && rootObject.get("disable").isJsonObject()) {
                    JsonObject disableMessages = rootObject.getAsJsonObject("disable");

                    for (Map.Entry<String, JsonElement> entry : disableMessages.entrySet()) {
                        String eventName = entry.getKey();
                        JsonElement value = entry.getValue();

                        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                            String message = value.getAsString();
                            eventManager.setEventDisableMessage(eventName, message);
                            LOGGER.info("Loaded disable message for {}: {}", eventName, message);
                        } else {
                            LOGGER.warn("Skipping invalid disable message for '{}': expected string but found {}",
                                    eventName, value.getClass().getSimpleName());
                        }
                    }
                }

                // Load drop chance enable message
                if (rootObject.has("drop_chance_enable") && rootObject.get("drop_chance_enable").isJsonPrimitive()) {
                    String message = rootObject.get("drop_chance_enable").getAsString();
                    eventManager.setDropChanceEnableMessage(message);
                    LOGGER.info("Loaded drop chance enable message: {}", message);
                }

                // Load drop chance disable message
                if (rootObject.has("drop_chance_disable") && rootObject.get("drop_chance_disable").isJsonPrimitive()) {
                    String message = rootObject.get("drop_chance_disable").getAsString();
                    eventManager.setDropChanceDisableMessage(message);
                    LOGGER.info("Loaded drop chance disable message: {}", message);
                }

                // Load double drops enable message
                if (rootObject.has("double_drops_enable") && rootObject.get("double_drops_enable").isJsonPrimitive()) {
                    String message = rootObject.get("double_drops_enable").getAsString();
                    eventManager.setDoubleDropsEnableMessage(message);
                    LOGGER.info("Loaded double drops enable message: {}", message);
                }

                // Load double drops disable message
                if (rootObject.has("double_drops_disable") && rootObject.get("double_drops_disable").isJsonPrimitive()) {
                    String message = rootObject.get("double_drops_disable").getAsString();
                    eventManager.setDoubleDropsDisableMessage(message);
                    LOGGER.info("Loaded double drops disable message: {}", message);
                }

            } catch (JsonSyntaxException e) {
                LOGGER.error("Invalid JSON syntax in messages file: {}", messagesFile, e);
            } catch (Exception e) {
                LOGGER.error("Failed to parse messages from file: {}", messagesFile, e);
            }

        } catch (IOException e) {
            LOGGER.error("Failed to read messages file: {}", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error while loading messages", e);
        }
    }

    /**
     * Creates a default messages file with example structure.
     */
    private void createDefaultMessagesFile(Path messagesFile) {
        try {
            String defaultJson = """
                    {
                        "enable": {
                            "Easter": "§6[Events] §d[EASTER] Easter event has been enabled!",
                            "Winter": "§6[Events] §b[WINTER] Winter event has been enabled!",
                            "Summer": "§6[Events] §e[SUMMER] Summer event has been enabled!",
                            "Halloween": "§6[Events] §c[HALLOWEEN] Halloween event has been enabled!"
                        },
                        "disable": {
                            "Easter": "§6[Events] §d[EASTER] Easter event has been disabled!",
                            "Winter": "§6[Events] §b[WINTER] Winter event has been disabled!",
                            "Summer": "§6[Events] §e[SUMMER] Summer event has been disabled!",
                            "Halloween": "§6[Events] §c[HALLOWEEN] Halloween event has been disabled!"
                        },
                        "drop_chance_enable": "§6[Events] §aDouble Drop Chance §eevent has been enabled! §e(2x drop rates)",
                        "drop_chance_disable": "§6[Events] §cDouble Drop Chance event has been disabled!",
                        "double_drops_enable": "§6[Events] §aDouble Drops §eevent has been enabled! §e(2x drop amounts)",
                        "double_drops_disable": "§6[Events] §cDouble Drops event has been disabled!"
                    }
                    """;

            Files.writeString(messagesFile, defaultJson, StandardCharsets.UTF_8);
            LOGGER.info("Created default messages file: {}", messagesFile);
        } catch (IOException e) {
            LOGGER.error("Failed to create default messages file: {}", messagesFile, e);
        }
    }
}