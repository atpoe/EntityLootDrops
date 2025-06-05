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

import net.poe.entitylootdrops.lootdrops.events.LootEventManager;
import net.poe.entitylootdrops.lootdrops.model.CustomDropEntry;
import net.poe.entitylootdrops.lootdrops.model.EntityDropEntry;

/**
 * Handles loading and saving of loot drop configurations.
 * Uses simplified structure: Global drops directly in category folders, entity-specific in Mobs/ subfolders.
 * Fixed to prevent stack overflow errors.
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
        // Store current active events
        Set<String> previousActiveEvents = new HashSet<>(eventManager.getActiveEvents());
        boolean previousDropChanceState = eventManager.isDropChanceEventActive();
        boolean previousDoubleDropsState = eventManager.isDoubleDropsActive();
        boolean previousDebugState = eventManager.isDebugLoggingEnabled();
        
        // Create directories if they don't exist
        createConfigDirectories();
        
        // Load all drop configurations
        loadAllDrops();
        
        // Load custom message configurations
        loadMessages();
        
        // Load active events state
        eventManager.loadActiveEventsState();
        
        // Restore previous state if no state was loaded
        eventManager.restorePreviousState(previousActiveEvents, previousDropChanceState, previousDoubleDropsState, previousDebugState);
    }
    
    /**
     * Creates the necessary directory structure for loot configurations.
     */
    private void createConfigDirectories() {
        try {
            // Create main config directory
            Path configDir = Paths.get(CONFIG_DIR);
            Files.createDirectories(configDir);
            
            // Create "Loot Drops" parent directory
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
                
                if (isFirstTimeSetup) {
                    createExampleGlobalDrops(eventTypeDir, eventType);
                    createExampleMobDrops(eventMobsDir, eventType.toLowerCase());
                }
            }
            
            // Create example configurations for normal drops
            if (isFirstTimeSetup) {
                createExampleGlobalDrops(normalDropsDir, "normal");
                createExampleMobDrops(normalMobsDir, "normal");
            }
            
            // Create custom event example
            if (isFirstTimeSetup) {
                createCustomEventExample(eventDropsDir);
            }
            
        } catch (IOException e) {
            LOGGER.error("Failed to create loot config directories", e);
        }
    }
    
    /**
     * Check if directory has any JSON files (excluding messages.json)
     */
    private boolean hasAnyJsonFiles(Path directory) {
        try {
            return Files.walk(directory, 1) // Only check immediate directory, not recursive
                .anyMatch(path -> path.toString().endsWith(".json") && 
                         !path.getFileName().toString().equals(MESSAGES_FILE));
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Creates example global drop files directly in the category directory.
     */
    private void createExampleGlobalDrops(Path directory, String category) throws IOException {
        Path globalDropsFile = directory.resolve("example_global_drops.json");
        if (!Files.exists(globalDropsFile)) {
            List<CustomDropEntry> examples = new ArrayList<>();
            
            if (category.equals("normal")) {
                // Example 1: Simple coin drop from any hostile mob
                CustomDropEntry coinDrop = new CustomDropEntry();
                coinDrop.setItemId("minecraft:gold_nugget");
                coinDrop.setDropChance(15.0f);
                coinDrop.setMinAmount(1);
                coinDrop.setMaxAmount(3);
                coinDrop.setRequirePlayerKill(true);
                coinDrop.setComment("Example: Gold nuggets from hostile mobs (global drop)");
                examples.add(coinDrop);
                
                // Example 2: Rare drop with requirements
                CustomDropEntry rareDrop = new CustomDropEntry();
                rareDrop.setItemId("minecraft:diamond");
                rareDrop.setDropChance(2.0f);
                rareDrop.setMinAmount(1);
                rareDrop.setMaxAmount(1);
                rareDrop.setRequiredDimension("minecraft:overworld");
                rareDrop.setRequiredAdvancement("minecraft:story/mine_diamond");
                rareDrop.setCommand("tellraw @a {\"text\":\"Someone found a rare diamond drop!\",\"color\":\"aqua\"}");
                rareDrop.setCommandChance(100.0f);
                rareDrop.setComment("Example: Rare diamond drop in overworld with advancement requirement");
                examples.add(rareDrop);
            } else {
                // Event-specific examples
                CustomDropEntry eventDrop = new CustomDropEntry();
                switch (category.toLowerCase()) {
                    case "halloween":
                        eventDrop.setItemId("minecraft:pumpkin");
                        eventDrop.setComment("Example: Halloween pumpkins from hostile mobs");
                        break;
                    case "christmas":
                        eventDrop.setItemId("minecraft:snow_block");
                        eventDrop.setComment("Example: Christmas snow blocks from hostile mobs");
                        break;
                    case "easter":
                        eventDrop.setItemId("minecraft:egg");
                        eventDrop.setComment("Example: Easter eggs from hostile mobs");
                        break;
                    case "summer":
                        eventDrop.setItemId("minecraft:sunflower");
                        eventDrop.setComment("Example: Summer sunflowers from hostile mobs");
                        break;
                    case "winter":
                        eventDrop.setItemId("minecraft:snowball");
                        eventDrop.setComment("Example: Winter snowballs from hostile mobs");
                        break;
                    default:
                        eventDrop.setItemId("minecraft:cake");
                        eventDrop.setComment("Example: " + category + " cake from hostile mobs");
                        break;
                }
                
                eventDrop.setDropChance(20.0f);
                eventDrop.setMinAmount(1);
                eventDrop.setMaxAmount(2);
                eventDrop.setRequirePlayerKill(true);
                examples.add(eventDrop);
            }
            
            // Save examples
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.write(globalDropsFile, gson.toJson(examples).getBytes());
        }
    }
    
    /**
     * Creates example mob drop files in the Mobs directory.
     */
    private void createExampleMobDrops(Path mobsDir, String category) throws IOException {
        // Create zombie example
        Path zombieFile = mobsDir.resolve("zombie_example.json");
        if (!Files.exists(zombieFile)) {
            List<EntityDropEntry> zombieDrops = new ArrayList<>();
            
            EntityDropEntry zombieDrop = new EntityDropEntry();
            zombieDrop.setEntityId("minecraft:zombie");
            zombieDrop.setItemId("minecraft:emerald");
            zombieDrop.setDropChance(category.equals("normal") ? 10.0f : 15.0f);
            zombieDrop.setMinAmount(1);
            zombieDrop.setMaxAmount(3);
            zombieDrop.setRequirePlayerKill(true);
            zombieDrop.setComment("Example: Emerald drop from zombies - " + category + " category");
            zombieDrops.add(zombieDrop);
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.write(zombieFile, gson.toJson(zombieDrops).getBytes());
        }
        
        // Create skeleton example
        Path skeletonFile = mobsDir.resolve("skeleton_example.json");
        if (!Files.exists(skeletonFile)) {
            List<EntityDropEntry> skeletonDrops = new ArrayList<>();
            
            EntityDropEntry skeletonDrop = new EntityDropEntry();
            skeletonDrop.setEntityId("minecraft:skeleton");
            skeletonDrop.setItemId("minecraft:bow");
            skeletonDrop.setDropChance(category.equals("normal") ? 5.0f : 8.0f);
            skeletonDrop.setMinAmount(1);
            skeletonDrop.setMaxAmount(1);
            skeletonDrop.setNbtData("{Enchantments:[{id:\"minecraft:power\",lvl:2}]}");
            skeletonDrop.setCommand("particle minecraft:end_rod ~ ~ ~ 0.5 0.5 0.5 0.1 20");
            skeletonDrop.setCommandChance(100.0f);
            skeletonDrop.setComment("Example: Enchanted bow drop from skeletons - " + category + " category");
            skeletonDrops.add(skeletonDrop);
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.write(skeletonFile, gson.toJson(skeletonDrops).getBytes());
        }
        
        // Create a nested folder example for normal category
        if (category.equals("normal")) {
            Path nestedDir = mobsDir.resolve("custom_category");
            Files.createDirectories(nestedDir);
            
            Path creeperFile = nestedDir.resolve("creeper_example.json");
            if (!Files.exists(creeperFile)) {
                List<EntityDropEntry> creeperDrops = new ArrayList<>();
                
                EntityDropEntry creeperDrop = new EntityDropEntry();
                creeperDrop.setEntityId("minecraft:creeper");
                creeperDrop.setItemId("minecraft:tnt");
                creeperDrop.setDropChance(25.0f);
                creeperDrop.setMinAmount(1);
                creeperDrop.setMaxAmount(2);
                creeperDrop.setComment("Example: TNT drop from creepers - nested in custom_category folder");
                creeperDrops.add(creeperDrop);
                
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Files.write(creeperFile, gson.toJson(creeperDrops).getBytes());
            }
        }
    }
    
    /**
     * Creates an example custom event to show users how to create their own events.
     */
    private void createCustomEventExample(Path eventDropsDir) throws IOException {
        Path customEventDir = eventDropsDir.resolve("MyCustomEvent");
        Files.createDirectories(customEventDir);
        
        // Create Mobs directory
        Path mobsDir = customEventDir.resolve(MOBS_DIR);
        Files.createDirectories(mobsDir);
        
        // Create example global drops for custom event
        createExampleGlobalDrops(customEventDir, "MyCustomEvent");
        
        // Create example mob drops for custom event
        createExampleMobDrops(mobsDir, "mycustomevent");
        
        // Create README for custom events
        Path readmeFile = customEventDir.resolve("README.txt");
        if (!Files.exists(readmeFile)) {
            String readme = "Custom Event: MyCustomEvent\n\n" +
                "This is an example of how to create your own custom events.\n" +
                "You can create any folder name here and it will be treated as an event.\n\n" +
                "Structure:\n" +
                "- [YourEventName]/\n" +
                "  ├── [global_drops].json - Global drops for all mobs during this event\n" +
                "  └── Mobs/\n" +
                "      └── [entity_specific].json - Drops for specific entities during this event\n\n" +
                "To activate this event, use: /entitylootdrops events activate MyCustomEvent\n" +
                "To deactivate: /entitylootdrops events deactivate MyCustomEvent\n\n" +
                "You can create as many custom events as you want by creating new folders!";
            
            Files.write(readmeFile, readme.getBytes());
        }
    }
    
    /**
     * Loads all drop configurations from all directories.
     */
    private void loadAllDrops() {
        // Clear existing configurations
        configManager.clearConfigurations();
        
        try {
            Path lootDropsDir = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR);
            
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
            
        } catch (IOException e) {
            LOGGER.error("Failed to load loot drop configurations", e);
        }
    }
    
    /**
     * Loads drop configurations from a specific directory.
     * Supports both global drops (JSON files directly in directory) and entity-specific drops (in Mobs/ subfolder).
     */
    private void loadDropsFromDirectory(Path directory, String dirKey) {
        if (!Files.exists(directory)) {
            return;
        }
        
        try {
            // Load global drops (JSON files directly in the directory, excluding Mobs folder and messages.json)
            List<CustomDropEntry> globalDrops = loadGlobalDropsFromCategoryDirectory(directory);
            if (!globalDrops.isEmpty()) {
                configManager.setHostileDrops(dirKey, globalDrops);
            }
            
            // Load entity-specific drops from Mobs directory
            Path mobsDir = directory.resolve(MOBS_DIR);
            if (Files.exists(mobsDir)) {
                List<EntityDropEntry> entityDrops = loadEntityDropsFromMobsDirectory(mobsDir);
                if (!entityDrops.isEmpty()) {
                    configManager.setEntityDrops(dirKey, entityDrops);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to load drops from directory: {}", directory, e);
        }
    }
    
    /**
     * Loads global drops from JSON files directly in a category directory (not in Mobs subfolder).
     * FIXED: No longer calls itself recursively.
     */
    private List<CustomDropEntry> loadGlobalDropsFromCategoryDirectory(Path categoryDir) {
        List<CustomDropEntry> allDrops = new ArrayList<>();
        
        try {
            Files.list(categoryDir) // Use list() instead of walk() to avoid recursion
                .filter(path -> path.toString().endsWith(".json"))
                .filter(path -> !path.getFileName().toString().equals(MESSAGES_FILE))
                .filter(Files::isRegularFile) // Only files, not directories
                .forEach(jsonFile -> {
                    try {
                        List<CustomDropEntry> drops = loadGlobalDropsFromFile(jsonFile);
                        allDrops.addAll(drops);
                        LOGGER.debug("Loaded {} global drops from {}", drops.size(), 
                            categoryDir.relativize(jsonFile));
                    } catch (Exception e) {
                        LOGGER.error("Failed to load global drops from file: {}", jsonFile, e);
                    }
                });
                
        } catch (IOException e) {
            LOGGER.error("Failed to list files in category directory: {}", categoryDir, e);
        }
        
        return allDrops;
    }
    
    /**
     * Recursively loads entity drops from the Mobs directory and all subdirectories.
     * FIXED: Now properly calls loadEntityDropsFromFile instead of itself.
     */
    private List<EntityDropEntry> loadEntityDropsFromMobsDirectory(Path mobsDir) {
        List<EntityDropEntry> allDrops = new ArrayList<>();
        
        try {
            Files.walk(mobsDir)
                .filter(path -> path.toString().endsWith(".json"))
                .filter(Files::isRegularFile) // Only files, not directories
                .forEach(jsonFile -> {
                    try {
                        List<EntityDropEntry> drops = loadEntityDropsFromFile(jsonFile); // FIXED: Call the correct method
                        allDrops.addAll(drops);
                        LOGGER.debug("Loaded {} entity drops from {}", drops.size(), 
                            mobsDir.relativize(jsonFile));
                    } catch (Exception e) {
                        LOGGER.error("Failed to load entity drops from file: {}", jsonFile, e);
                    }
                });
                
        } catch (IOException e) {
            LOGGER.error("Failed to walk Mobs directory: {}", mobsDir, e);
        }
        
        return allDrops;
    }
    
    /**
     * Loads entity drops from a JSON file.
     */
    private List<EntityDropEntry> loadEntityDropsFromFile(Path file) {
        try {
            String json = new String(Files.readAllBytes(file));
            Gson gson = new Gson();
            
            // Try to parse as array first
            Type listType = new TypeToken<List<EntityDropEntry>>(){}.getType();
            List<EntityDropEntry> drops = gson.fromJson(json, listType);
            
            if (drops == null) {
                // Try to parse as single object
                EntityDropEntry singleDrop = gson.fromJson(json, EntityDropEntry.class);
                if (singleDrop != null) {
                    drops = new ArrayList<>();
                    drops.add(singleDrop);
                } else {
                    drops = new ArrayList<>();
                }
            }
            
            return drops;
            
        } catch (Exception e) {
            LOGGER.error("Failed to load entity drops from file: {}", file, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Loads global drops from a JSON file.
     */
    private List<CustomDropEntry> loadGlobalDropsFromFile(Path file) {
        try {
            String json = new String(Files.readAllBytes(file));
            Gson gson = new Gson();
            
            Type listType = new TypeToken<List<CustomDropEntry>>(){}.getType();
            List<CustomDropEntry> drops = gson.fromJson(json, listType);
            
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
     * Loads custom messages for events.
     */
    private void loadMessages() {
        try {
            Path messagesFile = Paths.get(CONFIG_DIR, MESSAGES_FILE);
            
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
    enableMessages.put("winter", "§6[Events] §b[WINTER] Winter event has been enabled! §f(Special winter drops)");
    enableMessages.put("summer", "§6[Events] §e[SUMMER] Summer event has been enabled! §6(Hot summer drops)");
    enableMessages.put("easter", "§6[Events] §d[EASTER] Easter event has been enabled! §a(Easter egg drops)");
    enableMessages.put("halloween", "§6[Events] §c[HALLOWEEN] Halloween event has been enabled! §6(Spooky drops)");
    
    Map<String, String> disableMessages = new HashMap<>();
    disableMessages.put("winter", "§6[Events] §7[WINTER] Winter event has been disabled!");
    disableMessages.put("summer", "§6[Events] §7[SUMMER] Summer event has been disabled!");
    disableMessages.put("easter", "§6[Events] §7[EASTER] Easter event has been disabled!");
    disableMessages.put("halloween", "§6[Events] §7[HALLOWEEN] Halloween event has been disabled!");
    
    defaultMessages.put("enable", enableMessages);
    defaultMessages.put("disable", disableMessages);
    
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String json = gson.toJson(defaultMessages);
    Files.write(messagesFile, json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    
    LOGGER.info("Created default messages configuration file");
}
}
