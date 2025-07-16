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
                Files.list(eventDropsDir)
                        .filter(Files::isDirectory)
                        .forEach(eventDir -> {
                            String eventName = eventDir.getFileName().toString();
                            LOGGER.info("Loading event drops for event: {}", eventName);
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
            LOGGER.info("Loading drops from directory: {} (key: {})", directory, dirKey);

            // Load global drops (JSON files recursively, excluding Mobs folders and messages.json)
            List<CustomDropEntry> globalDrops = loadGlobalDropsFromCategoryDirectory(directory);
            if (!globalDrops.isEmpty()) {
                configManager.setHostileDrops(dirKey, globalDrops);
                LOGGER.info("Loaded {} hostile drops for category: {}", globalDrops.size(), dirKey);
            }

            // Load entity-specific drops from Mobs directories
            List<EntityDropEntry> entityDrops = loadEntityDropsFromMobsDirectories(directory);
            if (!entityDrops.isEmpty()) {
                configManager.setEntityDrops(dirKey, entityDrops);
                LOGGER.info("Loaded {} entity drops for category: {}", entityDrops.size(), dirKey);
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
     * Loads custom message configurations.
     */
    private void loadMessages() {
        try {
            Path messagesFile = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, MESSAGES_FILE);
            if (Files.exists(messagesFile)) {
                String json = new String(Files.readAllBytes(messagesFile));
                if (!json.trim().isEmpty()) {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    Type type = new TypeToken<Map<String, String>>(){}.getType();
                    Map<String, String> messages = gson.fromJson(json, type);
                    if (messages != null) {
                        configManager.setCustomMessages(messages);
                        LOGGER.info("Loaded {} custom messages", messages.size());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load custom messages", e);
        }
    }
}