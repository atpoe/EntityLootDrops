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
                    Map<String, String> enableMap = new HashMap<>();

                    for (Map.Entry<String, JsonElement> entry : enableMessages.entrySet()) {
                        String eventName = entry.getKey();
                        JsonElement value = entry.getValue();

                        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                            String message = value.getAsString();
                            enableMap.put(eventName, message);
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
                    Map<String, String> disableMap = new HashMap<>();

                    for (Map.Entry<String, JsonElement> entry : disableMessages.entrySet()) {
                        String eventName = entry.getKey();
                        JsonElement value = entry.getValue();

                        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                            String message = value.getAsString();
                            disableMap.put(eventName, message);
                            eventManager.setEventDisableMessage(eventName, message);
                            LOGGER.info("Loaded disable message for {}: {}", eventName, message);
                        } else {
                            LOGGER.warn("Skipping invalid disable message for '{}': expected string but found {}",
                                    eventName, value.getClass().getSimpleName());
                        }
                    }
                }

                // Load other message types if present (drop chance, double drops, etc.)
                loadOtherMessageTypes(rootObject);

                LOGGER.info("Successfully loaded custom event messages");

            } catch (JsonSyntaxException e) {
                LOGGER.error("Invalid JSON syntax in messages file: {}", messagesFile);
                LOGGER.error("JSON syntax error: {}", e.getMessage());
                LOGGER.debug("File content: {}", json);
            }

        } catch (IOException e) {
            LOGGER.error("Failed to read messages file: {}", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unexpected error loading custom messages", e);
        }
    }

    /**
     * Creates a default messages.json file with example messages.
     */
    private void createDefaultMessagesFile(Path messagesFile) throws IOException {
        // Ensure directory exists
        Files.createDirectories(messagesFile.getParent());

        // Create default messages structure
        Map<String, Object> defaultMessages = new HashMap<>();

        // Default enable messages - using proper UTF-8 encoding
        Map<String, String> enableMessages = new HashMap<>();
        enableMessages.put("Winter", "§6[Events] §b[WINTER] Winter event has been enabled!");
        enableMessages.put("Summer", "§6[Events] §e[SUMMER] Summer event has been enabled!");
        enableMessages.put("Easter", "§6[Events] §d[EASTER] Easter event has been enabled!");
        enableMessages.put("Halloween", "§6[Events] §c[HALLOWEEN] Halloween event has been enabled!");

        // Default disable messages
        Map<String, String> disableMessages = new HashMap<>();
        disableMessages.put("Winter", "§6[Events] §7[WINTER] Winter event has been disabled!");
        disableMessages.put("Summer", "§6[Events] §7[SUMMER] Summer event has been disabled!");
        disableMessages.put("Easter", "§6[Events] §7[EASTER] Easter event has been disabled!");
        disableMessages.put("Halloween", "§6[Events] §7[HALLOWEEN] Halloween event has been disabled!");

        defaultMessages.put("enable", enableMessages);
        defaultMessages.put("disable", disableMessages);

        // Add drop chance and double drops messages
        defaultMessages.put("drop_chance_enable", "§6[Events] §aIncreased drop chance event is now active!");
        defaultMessages.put("drop_chance_disable", "§6[Events] §cIncreased drop chance event has been disabled!");
        defaultMessages.put("double_drops_enable", "§6[Events] §aDouble drops event is now active!");
        defaultMessages.put("double_drops_disable", "§6[Events] §cDouble drops event has been disabled!");

        // Write to file with proper UTF-8 encoding
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(defaultMessages);

        Files.writeString(messagesFile, json, StandardCharsets.UTF_8);
        LOGGER.info("Created default messages.json file");
    }

    /**
     * Loads other message types like drop chance and double drops messages.
     */
    private void loadOtherMessageTypes(JsonObject rootObject) {
        // Load drop chance messages
        if (rootObject.has("drop_chance_enable") && rootObject.get("drop_chance_enable").isJsonPrimitive()) {
            String message = rootObject.get("drop_chance_enable").getAsString();
            eventManager.setDropChanceEnableMessage(message);
            LOGGER.info("Loaded drop chance enable message: {}", message);
        }

        if (rootObject.has("drop_chance_disable") && rootObject.get("drop_chance_disable").isJsonPrimitive()) {
            String message = rootObject.get("drop_chance_disable").getAsString();
            eventManager.setDropChanceDisableMessage(message);
            LOGGER.info("Loaded drop chance disable message: {}", message);
        }

        // Load double drops messages
        if (rootObject.has("double_drops_enable") && rootObject.get("double_drops_enable").isJsonPrimitive()) {
            String message = rootObject.get("double_drops_enable").getAsString();
            eventManager.setDoubleDropsEnableMessage(message);
            LOGGER.info("Loaded double drops enable message: {}", message);
        }

        if (rootObject.has("double_drops_disable") && rootObject.get("double_drops_disable").isJsonPrimitive()) {
            String message = rootObject.get("double_drops_disable").getAsString();
            eventManager.setDoubleDropsDisableMessage(message);
            LOGGER.info("Loaded double drops disable message: {}", message);
        }
    }
}