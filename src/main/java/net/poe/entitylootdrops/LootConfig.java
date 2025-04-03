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
 * Main configuration class for the EntityLootDrops mod.
 * Handles loading, saving, and managing custom drop configurations.
 */
public class LootConfig {
    // Logger for this class
    private static final Logger LOGGER = LogManager.getLogger();
    
    // Configuration directory paths
    private static final String CONFIG_DIR = "config/EntityLootDrops";
    private static final String NORMAL_DROPS_DIR = "Normal Drops";  // Directory for regular drops (always active)
    private static final String EVENTS_DIR = "Event Drops";        // Directory for event-specific drops
    private static final String MOBS_DIR = "Mobs";           // Subdirectory for entity-specific drops
    private static final String HOSTILE_DROPS_FILE = "Global_Hostile_Drops.json";  // File for general hostile mob drops
    
    // Built-in event types
    private static final String[] EVENT_TYPES = {"Winter", "Summer", "Easter", "Halloween"};
    
    // Storage for loaded drop configurations
    private static Map<String, List<EntityDropEntry>> entityDrops = new HashMap<>(); // Maps directory name -> entity-specific drops
    private static Map<String, List<CustomDropEntry>> hostileDrops = new HashMap<>(); // Maps directory name -> general hostile drops
    
    // Tracks which events are currently active
    private static Set<String> activeEvents = new HashSet<>();
    
    // Special event flags
    private static boolean dropChanceEventActive = false;  // When true, doubles all drop chances
    private static boolean doubleDropsActive = false;      // When true, doubles all drop amounts
    private static boolean debugLoggingEnabled = false; // When true, enables debug logging for drop events
    // Add these getter and setter methods
/**
 * Checks if debug logging is enabled.
 * 
 * @return True if debug logging is enabled
 */
public static boolean isDebugLoggingEnabled() {
    return debugLoggingEnabled;
}

/**
 * Enables or disables debug logging.
 * 
 * @param enabled True to enable logging, false to disable
 */
public static void setDebugLogging(boolean enabled) {
    debugLoggingEnabled = enabled;
    LootEventHandler.setDebugLogging(enabled);
    LOGGER.info("Debug logging has been {}", enabled ? "enabled" : "disabled");
}

    // Custom messages for event notifications
    private static Map<String, String> eventEnableMessages = new HashMap<>();
    private static Map<String, String> eventDisableMessages = new HashMap<>();
    
    // Default messages for special events
    private static String dropChanceEnableMessage = "§6[Events] §aDouble Drop Chance event has been enabled! §e(2x drop rates)";
    private static String dropChanceDisableMessage = "§6[Events] §cDouble Drop Chance event has been disabled!";
    private static String doubleDropsEnableMessage = "§6[Events] §aDouble Drops event has been enabled! §e(2x drop amounts)";
    private static String doubleDropsDisableMessage = "§6[Events] §cDouble Drops event has been disabled!";

    /**
     * Main method to load all configuration files.
     * Called when the mod starts and when the reload command is used.
     */
    public static void loadConfig() {
        // Store current active events and event states to restore them after reload
        Set<String> previousActiveEvents = new HashSet<>(activeEvents);
        boolean previousDropChanceState = dropChanceEventActive;
        boolean previousDoubleDropsState = doubleDropsActive;
        
        // Create directories and default files if they don't exist
        createConfigDirectories();
        
        // Load all drop configurations from files
        loadAllDrops();
        
        // Load custom message configurations
        loadMessages();
        
        // Restore active events (but only if they still exist in the config)
        activeEvents.clear();
        for (String event : previousActiveEvents) {
            if (entityDrops.containsKey(event) || hostileDrops.containsKey(event)) {
                activeEvents.add(event);
            }
        }
        
        // Restore special event states
        dropChanceEventActive = previousDropChanceState;
        doubleDropsActive = previousDoubleDropsState;
        
        // Log information about the loaded configuration
        LOGGER.info("Reloaded configuration: {} entity drop types, {} hostile drop types, {} active events", 
            entityDrops.size(), hostileDrops.size(), activeEvents.size());
        
        if (dropChanceEventActive) {
            LOGGER.info("Drop chance bonus event is active (2x drop rates)");
        }
        
        if (doubleDropsActive) {
            LOGGER.info("Double drops event is active (2x drop amounts)");
        }
    }
    
    /**
     * Loads all drop configurations from files.
     * This includes normal drops and event-specific drops.
     */
    private static void loadAllDrops() {
        // Clear existing drops to start fresh
        entityDrops.clear();
        hostileDrops.clear();
        
        // Load normal drops (always active)
        Path normalDropsDir = Paths.get(CONFIG_DIR, NORMAL_DROPS_DIR);
        loadDirectoryDrops(normalDropsDir, NORMAL_DROPS_DIR);
        
        // Load event drops (can be toggled on/off)
        Path eventsDir = Paths.get(CONFIG_DIR, EVENTS_DIR);
        if (Files.exists(eventsDir)) {
            try {
                // Load each event directory
                Files.list(eventsDir)
                    .filter(Files::isDirectory)
                    .forEach(eventDir -> {
                        String eventName = eventDir.getFileName().toString();
                        loadDirectoryDrops(eventDir, eventName);
                    });
            } catch (IOException e) {
                LOGGER.error("Failed to load event directories", e);
            }
        }
    }

    // Path to the messages configuration file
    private static final String MESSAGES_FILE = "messages.json";

    /**
     * Class to store message configurations.
     * Used for JSON serialization/deserialization.
     */
    public static class MessageConfig {
        private Map<String, String> eventEnableMessages = new HashMap<>();
        private Map<String, String> eventDisableMessages = new HashMap<>();
        private String dropChanceEnableMessage = "§6[Events] §aDouble Drop Chance event has been enabled! §e(2x drop rates)";
        private String dropChanceDisableMessage = "§6[Events] §cDouble Drop Chance event has been disabled!";
        private String doubleDropsEnableMessage = "§6[Events] §aDouble Drops event has been enabled! §e(2x drop amounts)";
        private String doubleDropsDisableMessage = "§6[Events] §cDouble Drops event has been disabled!";
        private String _comment = "Configure broadcast messages for events";
    }

    /**
     * Loads custom message configurations from the messages.json file.
     * Creates default messages if the file doesn't exist.
     */
    private static void loadMessages() {
        Path messagesPath = Paths.get(CONFIG_DIR, MESSAGES_FILE);
        
        // Create default messages file if it doesn't exist
        if (!Files.exists(messagesPath)) {
            try {
                MessageConfig defaultConfig = new MessageConfig();
                
                // Add default messages for built-in events
                for (String eventType : EVENT_TYPES) {
                    defaultConfig.eventEnableMessages.put(eventType, 
                        "§6[Events] §a" + eventType + " event has been enabled!");
                    defaultConfig.eventDisableMessages.put(eventType, 
                        "§6[Events] §c" + eventType + " event has been disabled!");
                }
                
                // Write the default configuration to file
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(defaultConfig);
                Files.write(messagesPath, json.getBytes());
                LOGGER.info("Created default messages configuration");
            } catch (IOException e) {
                LOGGER.error("Failed to create default messages file", e);
            }
        }
        
        // Load messages from file
        try {
            if (Files.exists(messagesPath)) {
                String json = new String(Files.readAllBytes(messagesPath));
                Gson gson = new Gson();
                MessageConfig config = gson.fromJson(json, MessageConfig.class);
                
                if (config != null) {
                    // Apply loaded configuration to the static fields
                    eventEnableMessages.clear();
                    eventEnableMessages.putAll(config.eventEnableMessages);
                    
                    eventDisableMessages.clear();
                    eventDisableMessages.putAll(config.eventDisableMessages);
                    
                    dropChanceEnableMessage = config.dropChanceEnableMessage;
                    dropChanceDisableMessage = config.dropChanceDisableMessage;
                    doubleDropsEnableMessage = config.doubleDropsEnableMessage;
                    doubleDropsDisableMessage = config.doubleDropsDisableMessage;
                    
                    LOGGER.info("Loaded message configurations");
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load messages configuration", e);
        }
    }

    /**
     * Creates the directory structure for the mod.
     * This includes the main config directory, normal drops directory, and event directories.
     */
    private static void createConfigDirectories() {
        try {
            // Create main config directory
            Path configDir = Paths.get(CONFIG_DIR);
            Files.createDirectories(configDir);
            
            // Create README.txt if it doesn't exist
            createReadmeFile(configDir);
            
            // Create and initialize normal drops directory
            Path normalDropsDir = Paths.get(CONFIG_DIR, NORMAL_DROPS_DIR);
            Files.createDirectories(normalDropsDir);
            createDefaultDrops(normalDropsDir, null);
            
            // Create and initialize event directories
            Path eventsDir = Paths.get(CONFIG_DIR, EVENTS_DIR);
            Files.createDirectories(eventsDir);
            
            // Create a directory for each event type
            for (String eventType : EVENT_TYPES) {
                Path eventTypeDir = Paths.get(CONFIG_DIR, EVENTS_DIR, eventType);
                Files.createDirectories(eventTypeDir);
                createDefaultDrops(eventTypeDir, eventType);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create config directories", e);
        }
    }
    
    /**
     * Creates a README.txt file with documentation.
     * This file explains how to configure the mod.
     */
    private static void createReadmeFile(Path configDir) throws IOException {
        Path readmePath = configDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Entity Loot Drops - Configuration Guide\n");
            readme.append("=====================================\n\n");
            
            readme.append("This mod allows you to customize what items drop from mobs, with powerful features like conditional drops, commands, and special effects.\n\n");
            
            readme.append("Quick Start:\n");
            readme.append("-----------\n");
            readme.append("1. All configuration is done through JSON files\n");
            readme.append("2. Files are organized in folders for normal drops and event drops\n");
            readme.append("3. Use /lootdrops commands to control events in-game\n\n");

            readme.append("1. Directory Structure\n");
            readme.append("----------------------\n");
            readme.append("config/entitylootdrops/\n");
            readme.append("|-- Normal/                  # Regular drops (always active)\n");
            readme.append("|   |-- Global_Hostile_Drops.json   # Drops for all hostile mobs\n");
            readme.append("|   `-- Mobs/                # Entity-specific drops\n");
            readme.append("|       |-- zombie_drops.json\n");
            readme.append("|       |-- skeleton_drops.json\n");
            readme.append("|       `-- ...\n");
            readme.append("|-- Event Drops/                  # Event-specific drops\n");
            readme.append("    |-- Winter/              # Winter event drops\n");
            readme.append("    |   |-- hostile_drops.json\n");
            readme.append("    |   `-- Mobs/\n");
            readme.append("    |       |-- skeleton_ice.json\n");
            readme.append("    |       `-- ...\n");
            readme.append("    |-- Summer/              # Summer event drops\n");
            readme.append("    |-- Easter/              # Easter event drops\n");
            readme.append("    |-- Halloween/           # Halloween event drops\n");
            readme.append("    `-- [custom event]/      # Custom event drops (create your own folder)\n\n");

            readme.append("2. Drop Configuration Format\n");
            readme.append("-------------------------\n");
            readme.append("All drop configurations use JSON format. Here's a detailed breakdown:\n\n");
            
            readme.append("Basic Properties:\n");
            readme.append("- \"_comment\": \"Comment for this drop configuration (e.g., \"zombie drops diamonds\")\n");
            readme.append("- itemId: The Minecraft item ID (e.g., \"minecraft:diamond\")\n");
            readme.append("- dropChance: Percentage chance to drop (0-100)\n");
            readme.append("- minAmount: Minimum number of items to drop\n");
            readme.append("- maxAmount: Maximum number of items to drop\n\n");
            
            readme.append("Advanced Properties:\n");
            readme.append("- nbtData: Custom NBT data for the item\n");
            readme.append("- command: Command to execute when the mob dies\n");
            readme.append("- commandChance: Chance for the command to execute (0-100)\n");
            readme.append("- requiredAdvancement: Player must have this advancement\n");
            readme.append("- requiredEffect: Player must have this potion effect\n");
            readme.append("- requiredEquipment: Player must have this item equipped\n");
            readme.append("- requiredDimension: Player must be in this dimension (e.g., \"minecraft:overworld\")\n\n");
            
            readme.append("3. Example Configurations\n");
            readme.append("----------------------\n\n");
            
            readme.append("A. Basic Hostile Drop:\n");
            readme.append("```json\n");
            readme.append("{\n");
            readme.append("    \"_comment\": \"Simple diamond drop with 5% chance\",\n");
            readme.append("    \"itemId\": \"minecraft:diamond\",\n");
            readme.append("    \"dropChance\": 5.0,\n");
            readme.append("    \"minAmount\": 1,\n");
            readme.append("    \"maxAmount\": 2\n");
            readme.append("}\n");
            readme.append("```\n\n");
            
            readme.append("B. Advanced Item Drop with NBT:\n");
            readme.append("```json\n");
            readme.append("{\n");
            readme.append("    \"_comment\": \"Named sword with enchantments\",\n");
            readme.append("    \"itemId\": \"minecraft:diamond_sword\",\n");
            readme.append("    \"dropChance\": 10.0,\n");
            readme.append("    \"minAmount\": 1,\n");
            readme.append("    \"maxAmount\": 1,\n");
            readme.append("    \"nbtData\": \"{display:{Name:'{\\\"text\\\":\\\"Legendary Blade\\\",\\\"color\\\":\\\"gold\\\"}',Lore:['{\\\"text\\\":\\\"Ancient power flows through this weapon\\\",\\\"color\\\":\\\"purple\\\"}']},Enchantments:[{id:\\\"minecraft:sharpness\\\",lvl:5},{id:\\\"minecraft:looting\\\",lvl:3}]}\"\n");
            readme.append("}\n");
            readme.append("```\n\n");
            
            readme.append("C. Conditional Drop with Command:\n");
            readme.append("```json\n");
            readme.append("{\n");
            readme.append("    \"_comment\": \"Special drop for advanced players\",\n");
            readme.append("    \"itemId\": \"minecraft:netherite_ingot\",\n");
            readme.append("    \"dropChance\": 15.0,\n");
            readme.append("    \"minAmount\": 1,\n");
            readme.append("    \"maxAmount\": 1,\n");
            readme.append("    \"requiredAdvancement\": \"minecraft:story/enter_the_nether\",\n");
            readme.append("    \"requiredEquipment\": \"minecraft:diamond_sword\",\n");
            readme.append("    \"command\": \"title {player} title {\\\"text\\\":\\\"Legendary Find!\\\",\\\"color\\\":\\\"gold\\\"}\",\n");
            readme.append("    \"commandChance\": 100.0\n");
            readme.append("}\n");
            readme.append("```\n\n");
            
            readme.append("Complete example showing all available options:\n");
            readme.append("```json\n");
            readme.append("{\n");
            readme.append("    \"_comment\": \"Example configuration showing all available options\",\n");
            readme.append("    \"entityId\": \"minecraft:zombie\",\n");
            readme.append("    \"itemId\": \"minecraft:diamond_sword\",\n");
            readme.append("    \"dropChance\": 10.0,\n");
            readme.append("    \"minAmount\": 1,\n");
            readme.append("    \"maxAmount\": 3,\n");
            readme.append("    \"nbtData\": \"{display:{Name:'{\\\"text\\\":\\\"Ultimate Sword\\\",\\\"color\\\":\\\"gold\\\"}',Lore:['{\\\"text\\\":\\\"A powerful weapon\\\",\\\"color\\\":\\\"purple\\\"}']},Enchantments:[{id:\\\"minecraft:sharpness\\\",lvl:5},{id:\\\"minecraft:looting\\\",lvl:3}]}\",\n");
            readme.append("    \"requiredAdvancement\": \"minecraft:story/enter_the_nether\",\n");
            readme.append("    \"requiredEffect\": \"minecraft:strength\",\n");
            readme.append("    \"requiredEquipment\": \"minecraft:diamond_pickaxe\",\n");
            readme.append("    \"requiredDimension\": \"minecraft:overworld\",\n");
            readme.append("    \"command\": \"title {player} title {\\\"text\\\":\\\"Epic Kill!\\\",\\\"color\\\":\\\"gold\\\"}\",\n");
            readme.append("    \"commandChance\": 100.0\n");
            readme.append("}\n");
            readme.append("```\n\n");

            readme.append("4. Command Placeholders\n");
            readme.append("--------------------\n");
            readme.append("Use these in your commands to reference specific values:\n\n");
            
            readme.append("Player Related:\n");
            readme.append("- {player}    : Player's name\n");
            readme.append("- {player_x}  : Player's X coordinate\n");
            readme.append("- {player_y}  : Player's Y coordinate\n");
            readme.append("- {player_z}  : Player's Z coordinate\n\n");
            
            readme.append("Entity Related:\n");
            readme.append("- {entity}    : Killed entity's name\n");
            readme.append("- {entity_id} : Entity's Minecraft ID\n");
            readme.append("- {entity_x}  : Entity's X coordinate\n");
            readme.append("- {entity_y}  : Entity's Y coordinate\n");
            readme.append("- {entity_z}  : Entity's Z coordinate\n\n");
            
            readme.append("5. Requirements System\n");
            readme.append("-------------------\n\n");
            
            readme.append("A. Advancements:\n");
            readme.append("Use \"requiredAdvancement\" to check if a player has completed specific advancements:\n");
            readme.append("```json\n");
            readme.append("\"requiredAdvancement\": \"minecraft:story/mine_diamond\"  // Must have mined diamonds\n");
            readme.append("\"requiredAdvancement\": \"minecraft:nether/create_beacon\"  // Must have created a beacon\n");
            readme.append("```\n\n");
            
            readme.append("B. Potion Effects:\n");
            readme.append("Use \"requiredEffect\" to check if a player has active effects:\n");
            readme.append("```json\n");
            readme.append("\"requiredEffect\": \"minecraft:strength\"  // Must have Strength effect\n");
            readme.append("\"requiredEffect\": \"minecraft:invisibility\"  // Must be invisible\n");
            readme.append("```\n\n");
            
            readme.append("C. Equipment:\n");
            readme.append("Use \"requiredEquipment\" to check what the player is holding:\n");
            readme.append("```json\n");
            readme.append("\"requiredEquipment\": \"minecraft:diamond_sword\"  // Must hold diamond sword\n");
            readme.append("\"requiredEquipment\": \"minecraft:bow\"  // Must hold a bow\n");
            readme.append("```\n\n");
            
            readme.append("D. Dimensions:\n");
            readme.append("Use \"requiredDimension\" to check which dimension the player is in:\n");
            readme.append("```json\n");
            readme.append("\"requiredDimension\": \"minecraft:overworld\"  // Must be in the overworld\n");
            readme.append("\"requiredDimension\": \"minecraft:the_nether\"  // Must be in the nether\n");
            readme.append("\"requiredDimension\": \"minecraft:the_end\"  // Must be in the end\n");
            readme.append("\"requiredDimension\": \"mymod:custom_dimension\"  // Must be in a custom dimension\n");
            readme.append("```\n\n");

            readme.append("6. Command Examples\n");
            readme.append("----------------\n\n");
            
            readme.append("A. Effects and Sounds:\n");
            readme.append("```json\n");
            readme.append("\"command\": \"effect give {player} minecraft:regeneration 10 1\"  // Give regeneration\n");
            readme.append("\"command\": \"playsound minecraft:entity.experience_orb.pickup master {player}\"  // Play sound\n");
            readme.append("```\n\n");
            
            readme.append("B. Visual Effects:\n");
            readme.append("```json\n");
            readme.append("\"command\": \"particle minecraft:heart {entity_x} {entity_y} {entity_z} 1 1 1 0.1 20\"  // Heart particles\n");
            readme.append("\"command\": \"title {player} actionbar {\\\"text\\\":\\\"Epic kill!\\\",\\\"color\\\":\\\"gold\\\"}\"  // Show message\n");
            readme.append("```\n\n");
            
            readme.append("C. Complex Commands:\n");
            readme.append("```json\n");
            readme.append("\"command\": \"summon minecraft:lightning_bolt {entity_x} {entity_y} {entity_z}\"  // Strike lightning\n");
            readme.append("\"command\": \"give {player} minecraft:diamond{display:{Name:'{\\\"text\\\":\\\"Reward\\\"}'}}\"  // Give named item\n");
            readme.append("```\n\n");
            
            readme.append("7. Player Commands\n");
            readme.append("----------------\n");
            readme.append("The mod provides several commands to control events and settings:\n\n");
            
            readme.append("A. Managing Events:\n");
            readme.append("- /lootdrops event <eventName> true    - Enable an event\n");
            readme.append("- /lootdrops event <eventName> false   - Disable an event\n");
            readme.append("- /lootdrops event dropchance true     - Enable 2x drop chance bonus\n");
            readme.append("- /lootdrops event dropchance false    - Disable drop chance bonus\n\n");
            readme.append("- /lootdrops event doubledrops true    - Enable 2x item drops bonus\n");
            readme.append("- /lootdrops event doubledrops false   - Disable item drop bonus\n\n");
            
            readme.append("B. Information Commands:\n");
            readme.append("- /lootdrops active_events             - List all active events\n");
            readme.append("- /lootdrops listall                   - List all available events\n");
            readme.append("- /lootdrops openconfig                - Opens custom JSON editor menu\n");
            readme.append("- /lootdrops reload                    - Reload all configuration files\n");
            readme.append("- /lootdrops help                      - Show command help\n\n");
            
            readme.append("C. Examples:\n");
            readme.append("- /lootdrops event winter true         - Enable winter event drops\n");
            readme.append("- /lootdrops event halloween false     - Disable halloween event drops\n");
            readme.append("- /lootdrops event mycustomevent true  - Enable a custom event\n\n");
            
            readme.append("D. Permissions:\n");
            readme.append("- entitylootdrops.command.event        - Permission to enable/disable events\n");
            readme.append("- entitylootdrops.command.list         - Permission to list active events\n");
            readme.append("- entitylootdrops.command.reload       - Permission to reload configurations\n");
            readme.append("- entitylootdrops.command.admin        - Permission for all commands\n\n");
            
            readme.append("8. Events System\n");
            readme.append("-------------\n");
            readme.append("- Events are special drop configurations that can be toggled on/off\n");
            readme.append("- Multiple events can be active simultaneously\n");
            readme.append("- Event drops stack with normal drops\n");
            readme.append("- Custom events can be created by adding new folders in the events directory\n");
            readme.append("- The drop chance event doubles all drop chances when active\n");
            readme.append("- The double drops event doubles all item drops when active\n\n");
            
            readme.append("9. Customizing Event Messages\n");
            readme.append("----------------------------\n");
            readme.append("- You can customize the broadcast messages that appear when events are enabled or disabled\n");
            readme.append("- 1. Edit the `messages.json` file in the config/entitylootdrops directory\n");
            readme.append("- 2. Customize messages for built-in events or add messages for your custom events\n");
            readme.append("- 3. Use color codes with § symbol (e.g., §a for green, §c for red, §6 for gold)\n");
            readme.append("- 4. Changes take effect after reloading the configuration (/lootdrops reload)\n\n");

            readme.append("10. Tips and Best Practices\n");
            readme.append("-----------------------\n");
            readme.append("1. Start with small drop chances (1-5%) for valuable items\n");
            readme.append("2. Use commands sparingly to avoid spam\n");
            readme.append("3. Test configurations on a test server first\n");
            readme.append("4. Back up your config files before making major changes\n");
            readme.append("5. Use meaningful comments in your JSON files\n");
            readme.append("6. Keep drop chances balanced for game progression\n\n");
            readme.append("7. You can delete most of the default example generated files, but keep the hostile_drops.json file\n\n");
            
            readme.append("11. Common Issues\n");
            readme.append("-------------\n");
            readme.append("1. Invalid JSON: Check your syntax, especially with NBT data\n");
            readme.append("2. Missing Quotes: All strings need double quotes\n");
            readme.append("3. Wrong IDs: Verify Minecraft IDs are correct (include \"minecraft:\" prefix)\n");
            readme.append("4. Command Errors: Test commands in-game first\n");
            readme.append("5. Case Sensitivity: IDs and commands are case-sensitive\n");
            readme.append("6. Custom Events: Inside your custom event folder, you MUST have at least one of these:\n");
            readme.append("-  A Global_Hostile_Drops.json file for drops that apply to all hostile mobs\n");
            readme.append("-  A Mobs folder with entity-specific drop files\n\n");

            Files.write(readmePath, readme.toString().getBytes());
            LOGGER.info("Created README.txt in config directory");
        }
    }
    
    /**
     * Creates default drop configuration files in a directory.
     * This includes the hostile_drops.json file and an example mob file.
     * 
     * @param directory The directory to create files in
     * @param eventType The event type (null for normal drops)
     */
    private static void createDefaultDrops(Path directory, String eventType) throws IOException {
        // First create the mobs directory for entity-specific drops
        Path mobsDir = directory.resolve(MOBS_DIR);
        Files.createDirectories(mobsDir);
        LOGGER.info("Created mobs directory at: {}", mobsDir);
    
        // Create a hostile drops file if it doesn't exist
        Path hostileDropsPath = directory.resolve(HOSTILE_DROPS_FILE);
        boolean isNewHostileDropsFile = !Files.exists(hostileDropsPath);
        
        if (isNewHostileDropsFile) {
            List<CustomDropEntry> defaultHostileDrops = new ArrayList<>();
            
            // Create a comprehensive example drop for all hostile mobs
            CustomDropEntry exampleDrop = new CustomDropEntry();
            exampleDrop.setItemId("minecraft:diamond_sword");
            exampleDrop.setDropChance(25.0f);
            exampleDrop.setMinAmount(1);
            exampleDrop.setMaxAmount(3);
            exampleDrop.setNbtData("{display:{Name:'{\"text\":\"Ultimate Weapon\",\"color\":\"gold\"}',Lore:['{\"text\":\"Legendary drop example\",\"color\":\"purple\"}','{\"text\":\"Shows all features\",\"color\":\"gray\"}']},Enchantments:[{id:\"minecraft:sharpness\",lvl:5},{id:\"minecraft:smite\",lvl:3},{id:\"minecraft:looting\",lvl:3}]}");
            exampleDrop.setRequiredAdvancement("minecraft:story/enter_the_nether");
            exampleDrop.setRequiredEffect("minecraft:strength");
            exampleDrop.setRequiredEquipment("minecraft:diamond_sword");
            exampleDrop.setRequiredDimension("minecraft:overworld");
            exampleDrop.setCommand("title {player} title {\"text\":\"Epic Drop!\",\"color\":\"gold\"}\nparticle minecraft:heart {entity_x} {entity_y} {entity_z} 1 1 1 0.1 20\neffect give {player} minecraft:regeneration 10 1");
            exampleDrop.setCommandChance(100.0f);
            exampleDrop.setComment("Complete example showing all available options");
            defaultHostileDrops.add(exampleDrop);
    
            // Add event-specific example if this is an event directory
            if (eventType != null) {
                CustomDropEntry eventDrop = new CustomDropEntry();
                // Configure the drop based on the event type
                switch (eventType) {
                    case "Winter":
                        eventDrop.setItemId("minecraft:blue_ice");
                        eventDrop.setNbtData("{display:{Name:'{\"text\":\"Frozen Treasure\",\"color\":\"aqua\"}'},Enchantments:[{id:\"minecraft:frost_walker\",lvl:2}]}");
                        eventDrop.setCommand("particle minecraft:snowflake {entity_x} {entity_y} {entity_z} 1 1 1 0.1 50");
                        break;
                    case "Summer":
                        eventDrop.setItemId("minecraft:magma_block");
                        eventDrop.setNbtData("{display:{Name:'{\"text\":\"Summer Heat\",\"color\":\"red\"}'},Enchantments:[{id:\"minecraft:fire_aspect\",lvl:2}]}");
                        eventDrop.setCommand("particle minecraft:flame {entity_x} {entity_y} {entity_z} 1 1 1 0.1 50");
                        break;
                    case "Halloween":
                        eventDrop.setItemId("minecraft:jack_o_lantern");
                        eventDrop.setNbtData("{display:{Name:'{\"text\":\"Spooky Treasure\",\"color\":\"dark_purple\"}'},Enchantments:[{id:\"minecraft:soul_speed\",lvl:3}]}");
                        eventDrop.setCommand("particle minecraft:soul {entity_x} {entity_y} {entity_z} 1 1 1 0.1 50");
                        break;
                    case "Easter":
                        eventDrop.setItemId("minecraft:egg");
                        eventDrop.setNbtData("{display:{Name:'{\"text\":\"Festive Surprise\",\"color\":\"light_purple\"}'},Enchantments:[{id:\"minecraft:luck_of_the_sea\",lvl:3}]}");
                        eventDrop.setCommand("particle minecraft:end_rod {entity_x} {entity_y} {entity_z} 1 1 1 0.1 50");
                        break;
                }
                if (eventDrop.getItemId() != null) {
                    eventDrop.setDropChance(50.0f);
                    eventDrop.setMinAmount(1);
                    eventDrop.setMaxAmount(3);
                    eventDrop.setRequiredDimension("minecraft:overworld");
                    eventDrop.setCommandChance(100.0f);
                    eventDrop.setComment(eventType + " event example drop");
                    defaultHostileDrops.add(eventDrop);
                }
            }
            
            // Write the drops to the file
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(defaultHostileDrops);
            Files.write(hostileDropsPath, json.getBytes());
            LOGGER.info("Created test hostile drops at: {}", hostileDropsPath);
            
            // Only create example mob file if we just created the hostile_drops.json file
            // This ensures the example is only generated once
            createExampleMobFile(mobsDir, eventType);
        }
    
        // Log the contents of the directory for debugging
        LOGGER.info("Directory contents for {}", directory);
        try {
            Files.list(directory).forEach(path -> 
                LOGGER.info("- {}", path.getFileName()));
            
            if (Files.exists(mobsDir)) {
                LOGGER.info("Mobs directory contents:");
                Files.list(mobsDir).forEach(path -> 
                    LOGGER.info("- {}", path.getFileName()));
            }
        } catch (IOException e) {
            LOGGER.error("Error listing directory contents", e);
        }
    }
    
    /**
     * Creates an example mob drop file.
     * This is only called when the hostile_drops.json file is first created.
     * 
     * @param mobsDir The mobs directory to create the file in
     * @param eventType The event type (null for normal drops)
     */
    private static void createExampleMobFile(Path mobsDir, String eventType) throws IOException {
        // Create an example mob file
        String mobFileName = "zombie_example.json";
        Path exampleMobPath = mobsDir.resolve(mobFileName);
        
        // Only create the file if it doesn't already exist
        if (!Files.exists(exampleMobPath)) {
            EntityDropEntry exampleMob = new EntityDropEntry();
            exampleMob.setEntityId("minecraft:zombie");
            exampleMob.setItemId("minecraft:emerald");
            exampleMob.setDropChance(30.0f);
            exampleMob.setMinAmount(1);
            exampleMob.setMaxAmount(3);
            
            // Add event-specific properties if this is for an event
            if (eventType != null) {
                exampleMob.setComment("Example " + eventType + " event drop for zombies");
                // Configure the drop based on the event type
                switch (eventType) {
                    case "Winter":
                        exampleMob.setItemId("minecraft:snowball");
                        exampleMob.setNbtData("{display:{Name:'{\"text\":\"Frozen Zombie Heart\",\"color\":\"aqua\"}'}}");
                        break;
                    case "Summer":
                        exampleMob.setItemId("minecraft:fire_charge");
                        exampleMob.setNbtData("{display:{Name:'{\"text\":\"Zombie Ember\",\"color\":\"red\"}'}}");
                        break;
                    case "Halloween":
                        exampleMob.setItemId("minecraft:bone");
                        exampleMob.setNbtData("{display:{Name:'{\"text\":\"Cursed Zombie Bone\",\"color\":\"dark_purple\"}'}}");
                        break;
                    case "Easter":
                        exampleMob.setItemId("minecraft:rabbit_foot");
                        exampleMob.setNbtData("{display:{Name:'{\"text\":\"Lucky Zombie Foot\",\"color\":\"light_purple\"}'}}");
                        break;
                    default:
                        exampleMob.setComment("Example zombie drop configuration");
                }
            } else {
                exampleMob.setComment("Example zombie drop configuration");
            }
            
            // Write the example to the file
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(exampleMob);
            Files.write(exampleMobPath, json.getBytes());
            LOGGER.info("Created example mob file at: {}", exampleMobPath);
        }
    } // End of createExampleMobFile method

    /**
     * Broadcasts a message to all players on the server.
     * Used when events are enabled or disabled.
     * 
     * @param message The message to broadcast
     */
    private static void broadcastEventMessage(String message) {
        // Get the server instance
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            // Send message to all players
            server.getPlayerList().broadcastSystemMessage(
                Component.literal(message), false);
        }
    }

    /**
     * Loads drop configurations from a directory.
     * This includes hostile_drops.json and entity-specific drops in the mobs directory.
     * 
     * @param directory The directory to load from
     * @param dirKey The key to use in the maps (directory name)
     */
    private static void loadDirectoryDrops(Path directory, String dirKey) {
        if (!Files.exists(directory)) {
            return;
        }
        
        try {
            // Load all JSON files from the directory root as hostile drops
            List<CustomDropEntry> directoryHostileDrops = new ArrayList<>();
            
            // Recursively get all JSON files in the directory and subdirectories
            Files.walk(directory)
                .filter(path -> path.toString().toLowerCase().endsWith(".json"))
                .filter(path -> !path.toString().contains(MOBS_DIR)) // Exclude files in Mobs directory
                .forEach(path -> {
                    try {
                        String json = new String(Files.readAllBytes(path));
                        Gson gson = new Gson();
                        
                        // Try to parse as a list of CustomDropEntry first
                        try {
                            Type listType = new TypeToken<ArrayList<CustomDropEntry>>(){}.getType();
                            List<CustomDropEntry> drops = gson.fromJson(json, listType);
                            
                            if (drops != null && !drops.isEmpty()) {
                                directoryHostileDrops.addAll(drops);
                                LOGGER.debug("Loaded {} hostile drops from {}", drops.size(), path.getFileName());
                            }
                        } catch (Exception e) {
                            // If it's not a list, try to parse as a single CustomDropEntry
                            try {
                                CustomDropEntry entry = gson.fromJson(json, CustomDropEntry.class);
                                if (entry != null && entry.getItemId() != null) {
                                    directoryHostileDrops.add(entry);
                                    LOGGER.debug("Loaded single hostile drop from {}", path.getFileName());
                                }
                            } catch (Exception e2) {
                                LOGGER.error("Error parsing JSON file {}: {}", path, e2.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error reading file {}: {}", path, e.getMessage());
                    }
                });
            
            // Add all loaded hostile drops to the map
            if (!directoryHostileDrops.isEmpty()) {
                hostileDrops.put(dirKey, directoryHostileDrops);
                LOGGER.debug("Loaded a total of {} hostile drops from directory {}", directoryHostileDrops.size(), dirKey);
            }
            
            // Load entity-specific drops from the mobs subdirectory and its subdirectories
            Path mobsDir = directory.resolve(MOBS_DIR);
            if (Files.exists(mobsDir)) {
                List<EntityDropEntry> dirDrops = new ArrayList<>();
                Files.walk(mobsDir)
                    .filter(path -> path.toString().toLowerCase().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            String json = new String(Files.readAllBytes(path));
                            Gson gson = new Gson();
                            
                            // Try to parse as a list of EntityDropEntry first
                            try {
                                Type listType = new TypeToken<ArrayList<EntityDropEntry>>(){}.getType();
                                List<EntityDropEntry> entries = gson.fromJson(json, listType);
                                
                                if (entries != null && !entries.isEmpty()) {
                                    for (EntityDropEntry entry : entries) {
                                        if (entry.getEntityId() != null && entry.getItemId() != null) {
                                            dirDrops.add(entry);
                                            LOGGER.debug("Loaded entity drop: {} -> {} from {}", 
                                                entry.getEntityId(), entry.getItemId(), path.getFileName());
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                // If it's not a list, try to parse as a single EntityDropEntry
                                try {
                                    EntityDropEntry entry = gson.fromJson(json, EntityDropEntry.class);
                                    if (entry != null && entry.getEntityId() != null && entry.getItemId() != null) {
                                        dirDrops.add(entry);
                                        LOGGER.debug("Loaded entity drop: {} -> {} from {}", 
                                            entry.getEntityId(), entry.getItemId(), path.getFileName());
                                    }
                                } catch (Exception e2) {
                                    LOGGER.error("Error parsing JSON file {}: {}", path, e2.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error("Error loading drop file {}: {}", path, e.getMessage());
                        }
                    });
                
                if (!dirDrops.isEmpty()) {
                    entityDrops.put(dirKey, dirDrops);
                    LOGGER.debug("Loaded {} entity drops from {}", dirDrops.size(), dirKey);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load drops from directory: {}", directory, e);
        }
    }
    
/**
 * Loads the active events state from file.
 * Called during loadConfig() to restore event states.
 */
private static void loadActiveEventsState() {
    try {
        Path configDir = Paths.get(CONFIG_DIR);
        Path stateFile = configDir.resolve("active_events.json");
        
        if (Files.exists(stateFile)) {
            String json = new String(Files.readAllBytes(stateFile));
            Gson gson = new Gson();
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> state = gson.fromJson(json, mapType);
            
            if (state != null) {
                // Restore active events
                if (state.containsKey("activeEvents")) {
                    activeEvents.clear();
                    List<String> events = (List<String>) state.get("activeEvents");
                    activeEvents.addAll(events);
                    LOGGER.info("Restored {} active events from state file", events.size());
                }
                
                // Restore special event states
                if (state.containsKey("dropChanceEventActive")) {
                    dropChanceEventActive = (Boolean) state.get("dropChanceEventActive");
                    LOGGER.info("Restored drop chance event state: {}", dropChanceEventActive);
                }
                
                if (state.containsKey("doubleDropsActive")) {
                    doubleDropsActive = (Boolean) state.get("doubleDropsActive");
                    LOGGER.info("Restored double drops event state: {}", doubleDropsActive);
                }

                if (state.containsKey("debugLoggingEnabled")) {
                debugLoggingEnabled = (Boolean) state.get("debugLoggingEnabled");
                LootEventHandler.setDebugLogging(debugLoggingEnabled);
                LOGGER.info("Restored debug logging state: {}", debugLoggingEnabled);
                }
            }
        }
    } catch (Exception e) {
        LOGGER.error("Failed to load active events state", e);
    }
}

    /**
     * Gets the normal (always active) entity-specific drops.
     * 
     * @return A list of entity drop entries
     */
    public static List<EntityDropEntry> getNormalDrops() {
        return entityDrops.getOrDefault(NORMAL_DROPS_DIR, Collections.emptyList());
    }
    
    /**
     * Gets the normal (always active) hostile mob drops.
     * 
     * @return A list of custom drop entries
     */
    public static List<CustomDropEntry> getNormalHostileDrops() {
        return hostileDrops.getOrDefault(NORMAL_DROPS_DIR, Collections.emptyList());
    }
    
    /**
     * Gets all event-specific entity drops.
     * 
     * @return A map of event name -> entity drop entries
     */
    public static Map<String, List<EntityDropEntry>> getEventDrops() {
        return entityDrops.entrySet().stream()
            .filter(e -> !e.getKey().equals(NORMAL_DROPS_DIR))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    /**
     * Gets the hostile mob drops for a specific event.
     * 
     * @param eventName The name of the event
     * @return A list of custom drop entries
     */
    public static List<CustomDropEntry> getEventHostileDrops(String eventName) {
        return hostileDrops.getOrDefault(eventName, Collections.emptyList());
    }
    
    /**
     * Checks if an event is currently active.
     * 
     * @param eventName The name of the event
     * @return True if the event is active, false otherwise
     */
    public static boolean isEventActive(String eventName) {
        return activeEvents.contains(eventName.toLowerCase());
    }
    
    /**
     * Enables or disables an event.
     * Broadcasts a message to all players.
     * 
     * @param eventName The name of the event
     * @param active True to enable, false to disable
     */
    public static void toggleEvent(String eventName, boolean active) {
        // Find the actual case-preserved event name from the map
        String actualEventName = null;
        for (String key : getEventDrops().keySet()) {
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
            activeEvents.add(actualEventName.toLowerCase());
            LOGGER.info("Enabled event: {}", actualEventName);
            String message = eventEnableMessages.getOrDefault(actualEventName.toLowerCase(), 
                "§6[Events] §a" + actualEventName + " event has been enabled!");
            broadcastEventMessage(message);
        } else {
            activeEvents.remove(actualEventName.toLowerCase());
            LOGGER.info("Disabled event: {}", actualEventName);
            String message = eventDisableMessages.getOrDefault(actualEventName.toLowerCase(), 
                "§6[Events] §c" + actualEventName + " event has been disabled!");
            broadcastEventMessage(message);
        }
    }
    
    /**
     * Enables or disables the drop chance event.
     * When active, this doubles all drop chances.
     * 
     * @param active True to enable, false to disable
     */
    public static void toggleDropChanceEvent(boolean active) {
        dropChanceEventActive = active;
        LOGGER.info("Drop chance event set to: {}", active);
        if (active) {
            broadcastEventMessage(dropChanceEnableMessage);
        } else {
            broadcastEventMessage(dropChanceDisableMessage);
        }
    }
    
    /**
     * Enables or disables the double drops event.
     * When active, this doubles all drop amounts.
     * 
     * @param active True to enable, false to disable
     */
    public static void toggleDoubleDrops(boolean active) {
        doubleDropsActive = active;
        LOGGER.info("Double drops set to: {}", active);
        if (active) {
            broadcastEventMessage(doubleDropsEnableMessage);
        } else {
            broadcastEventMessage(doubleDropsDisableMessage);
        }
    }

/**
 * Toggles an event on or off.
 * 
 * @param eventName The name of the event to toggle
 * @param active Whether the event should be active
 */

/**
 * Clears all active events.
 */
public static void clearActiveEvents() {
    activeEvents.clear();
}


    /**
     * Sets the message to display when an event is enabled.
     * 
     * @param eventName The name of the event
     * @param message The message to display
     */
    public static void setEventEnableMessage(String eventName, String message) {
        eventEnableMessages.put(eventName.toLowerCase(), message);
    }

    /**
     * Sets the message to display when an event is disabled.
     * 
     * @param eventName The name of the event
     * @param message The message to display
     */
    public static void setEventDisableMessage(String eventName, String message) {
        eventDisableMessages.put(eventName.toLowerCase(), message);
    }

    /**
     * Sets the message to display when the drop chance event is enabled.
     * 
     * @param message The message to display
     */
    public static void setDropChanceEnableMessage(String message) {
        dropChanceEnableMessage = message;
    }

    /**
     * Sets the message to display when the drop chance event is disabled.
     * 
     * @param message The message to display
     */
    public static void setDropChanceDisableMessage(String message) {
        dropChanceDisableMessage = message;
    }

    /**
     * Sets the message to display when the double drops event is enabled.
     * 
     * @param message The message to display
     */
    public static void setDoubleDropsEnableMessage(String message) {
        doubleDropsEnableMessage = message;
    }

    /**
     * Sets the message to display when the double drops event is disabled.
     * 
     * @param message The message to display
     */
    public static void setDoubleDropsDisableMessage(String message) {
        doubleDropsDisableMessage = message;
    }

    /**
     * Gets the message to display when an event is enabled.
     * 
     * @param eventName The name of the event
     * @return The message to display
     */
    public static String getEventEnableMessage(String eventName) {
        return eventEnableMessages.getOrDefault(eventName.toLowerCase(), 
            "§6[Events] §a" + eventName + " event has been enabled!");
    }

    /**
     * Gets the message to display when an event is disabled.
     * 
     * @param eventName The name of the event
     * @return The message to display
     */
    public static String getEventDisableMessage(String eventName) {
        return eventDisableMessages.getOrDefault(eventName.toLowerCase(), 
            "§6[Events] §c" + eventName + " event has been disabled!");
    }

    /**
     * Gets the message to display when the drop chance event is enabled.
     * 
     * @return The message to display
     */
    public static String getDropChanceEnableMessage() {
        return dropChanceEnableMessage;
    }

    /**
     * Gets the message to display when the drop chance event is disabled.
     * 
     * @return The message to display
     */
    public static String getDropChanceDisableMessage() {
        return dropChanceDisableMessage;
    }

    /**
     * Gets the message to display when the double drops event is enabled.
     * 
     * @return The message to display
     */
    public static String getDoubleDropsEnableMessage() {
        return doubleDropsEnableMessage;
    }

    /**
     * Gets the message to display when the double drops event is disabled.
     * 
     * @return The message to display
     */
    public static String getDoubleDropsDisableMessage() {
        return doubleDropsDisableMessage;
    }

    /**
     * Gets all currently active events.
     * 
     * @return A set of active event names
     */
    public static Set<String> getActiveEvents() {
        return Collections.unmodifiableSet(activeEvents);
    }

    /**
     * Checks if the drop chance event is active.
     * 
     * @return True if active, false otherwise
     */
    public static boolean isDropChanceEventActive() {
        return dropChanceEventActive;
    }

    /**
     * Checks if the double drops event is active.
     * 
     * @return True if active, false otherwise
     */
    public static boolean isDoubleDropsActive() {
        return doubleDropsActive;
    }
    public static void saveActiveEventsState() {
        try {
            Path configDir = Paths.get(CONFIG_DIR);
            Path stateFile = configDir.resolve("active_events.json");

            // Create a simple object to store the state
        Map<String, Object> state = new HashMap<>();
        state.put("activeEvents", new ArrayList<>(activeEvents));
        state.put("dropChanceEventActive", dropChanceEventActive);
        state.put("doubleDropsActive", doubleDropsActive);
        state.put("debugLoggingEnabled", debugLoggingEnabled);
        
        // Write to file
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(state);
        Files.write(stateFile, json.getBytes());
        
        LOGGER.info("Saved active events state");
    } catch (Exception e) {
        LOGGER.error("Failed to save active events state", e);
    }
}
/**
 * Represents a custom drop entry for hostile mobs.
 * This is the base class for all drop configurations.
 */
public static class CustomDropEntry {
    private String itemId;              // The Minecraft item ID (e.g., "minecraft:diamond")
    private float dropChance;           // Percentage chance to drop (0-100)
    private int minAmount;              // Minimum number of items to drop
    private int maxAmount;              // Maximum number of items to drop
    private String nbtData;             // Custom NBT data for the item (for enchantments, names, etc.)
    private String requiredAdvancement; // Player must have this advancement to get the drop
    private String requiredEffect;      // Player must have this potion effect to get the drop
    private String requiredEquipment;   // Player must have this item equipped to get the drop
    private String requiredDimension;   // Player must be in this dimension to get the drop
    private String command;             // Command to execute when the drop occurs
    private float commandChance;        // Percentage chance to execute the command (0-100)
    private String _comment;            // Comment for documentation in the JSON file
    
    /**
     * Default constructor for Gson deserialization.
     */
    public CustomDropEntry() {
        this.commandChance = 100.0f; // Default to 100% if command is specified
        // Default constructor for Gson
    }
    
    /**
     * Constructor for a basic drop without NBT data.
     * 
     * @param itemId The Minecraft item ID
     * @param dropChance Percentage chance to drop (0-100)
     * @param minAmount Minimum number of items to drop
     * @param maxAmount Maximum number of items to drop
     */
    public CustomDropEntry(String itemId, float dropChance, int minAmount, int maxAmount) {
        this(itemId, dropChance, minAmount, maxAmount, null);
    }
    
    /**
     * Constructor for a drop with NBT data.
     * 
     * @param itemId The Minecraft item ID
     * @param dropChance Percentage chance to drop (0-100)
     * @param minAmount Minimum number of items to drop
     * @param maxAmount Maximum number of items to drop
     * @param nbtData Custom NBT data for the item
     */
    public CustomDropEntry(String itemId, float dropChance, int minAmount, int maxAmount, String nbtData) {
        this.itemId = itemId;
        this.dropChance = dropChance;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.nbtData = nbtData;
        this.commandChance = 100.0f; // Default to 100% if command is specified
    }
    
    /**
     * Gets the Minecraft item ID.
     * 
     * @return The item ID
     */
    public String getItemId() {
        return itemId;
    }
    
    /**
     * Gets the drop chance percentage.
     * 
     * @return The drop chance (0-100)
     */
    public float getDropChance() {
        return dropChance;
    }
    
    /**
     * Gets the minimum number of items to drop.
     * 
     * @return The minimum amount
     */
    public int getMinAmount() {
        return minAmount;
    }
    
    /**
     * Gets the maximum number of items to drop.
     * 
     * @return The maximum amount
     */
    public int getMaxAmount() {
        return maxAmount;
    }
    
    /**
     * Gets the custom NBT data for the item.
     * 
     * @return The NBT data string
     */
    public String getNbtData() {
        return nbtData;
    }
    
    /**
     * Gets the required advancement for the drop.
     * 
     * @return The advancement ID
     */
    public String getRequiredAdvancement() {
        return requiredAdvancement;
    }
    
    /**
     * Gets the required potion effect for the drop.
     * 
     * @return The effect ID
     */
    public String getRequiredEffect() {
        return requiredEffect;
    }
    
    /**
     * Gets the required equipment for the drop.
     * 
     * @return The equipment item ID
     */
    public String getRequiredEquipment() {
        return requiredEquipment;
    }

    /**
     * Gets the required dimension for the drop.
     * 
     * @return The dimension ID
     */
    public String getRequiredDimension() {
        return requiredDimension;
    }
    
    /**
     * Gets the command to execute when the drop occurs.
     * 
     * @return The command string
     */
    public String getCommand() {
        return command;
    }
    
    /**
     * Gets the chance to execute the command.
     * 
     * @return The command chance percentage (0-100)
     */
    public float getCommandChance() {
        return commandChance;
    }

    /**
     * Checks if the drop has custom NBT data.
     * 
     * @return True if NBT data is present
     */
    public boolean hasNbtData() {
        return nbtData != null && !nbtData.isEmpty();
    }
    
    /**
     * Checks if the drop has a required advancement.
     * 
     * @return True if a required advancement is specified
     */
    public boolean hasRequiredAdvancement() {
        return requiredAdvancement != null && !requiredAdvancement.isEmpty();
    }
    
    /**
     * Checks if the drop has a required potion effect.
     * 
     * @return True if a required effect is specified
     */
    public boolean hasRequiredEffect() {
        return requiredEffect != null && !requiredEffect.isEmpty();
    }
    
    /**
     * Checks if the drop has required equipment.
     * 
     * @return True if required equipment is specified
     */
    public boolean hasRequiredEquipment() {
        return requiredEquipment != null && !requiredEquipment.isEmpty();
    }
    
    /**
     * Checks if the drop has a required dimension.
     * 
     * @return True if a required dimension is specified
     */
    public boolean hasRequiredDimension() {
        return requiredDimension != null && !requiredDimension.isEmpty();
    }
    
    /**
     * Checks if the drop has a command to execute.
     * 
     * @return True if a command is specified
     */
    public boolean hasCommand() {
        return command != null && !command.isEmpty();
    }
    
    /**
     * Sets the required dimension for the drop.
     * 
     * @param requiredDimension The dimension ID
     */
    public void setRequiredDimension(String requiredDimension) {
        this.requiredDimension = requiredDimension;
    }
    
    /**
     * Sets the Minecraft item ID.
     * 
     * @param itemId The item ID
     */
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    
    /**
     * Sets the drop chance percentage.
     * 
     * @param dropChance The drop chance (0-100)
     */
    public void setDropChance(float dropChance) {
        this.dropChance = dropChance;
    }
    
    /**
     * Sets the minimum number of items to drop.
     * 
     * @param minAmount The minimum amount
     */
    public void setMinAmount(int minAmount) {
        this.minAmount = minAmount;
    }
    
    /**
     * Sets the maximum number of items to drop.
     * 
     * @param maxAmount The maximum amount
     */
    public void setMaxAmount(int maxAmount) {
        this.maxAmount = maxAmount;
    }
    
    /**
     * Sets the custom NBT data for the item.
     * 
     * @param nbtData The NBT data string
     */
    public void setNbtData(String nbtData) {
        this.nbtData = nbtData;
    }
    
    /**
     * Sets the required advancement for the drop.
     * 
     * @param requiredAdvancement The advancement ID
     */
    public void setRequiredAdvancement(String requiredAdvancement) {
        this.requiredAdvancement = requiredAdvancement;
    }
    
    /**
     * Sets the required potion effect for the drop.
     * 
     * @param requiredEffect The effect ID
     */
    public void setRequiredEffect(String requiredEffect) {
        this.requiredEffect = requiredEffect;
    }
    
    /**
     * Sets the required equipment for the drop.
     * 
     * @param requiredEquipment The equipment item ID
     */
    public void setRequiredEquipment(String requiredEquipment) {
        this.requiredEquipment = requiredEquipment;
    }
    
    /**
     * Sets the command to execute when the drop occurs.
     * 
     * @param command The command string
     */
    public void setCommand(String command) {
        this.command = command;
    }
    
    /**
     * Sets the chance to execute the command.
     * 
     * @param commandChance The command chance percentage (0-100)
     */
    public void setCommandChance(float commandChance) {
        this.commandChance = commandChance;
    }
    
    /**
     * Sets the comment for documentation in the JSON file.
     * 
     * @param comment The comment string
     */
    public void setComment(String comment) {
        this._comment = comment;
    }
}

/**
 * Represents an entity-specific drop entry.
 * Extends CustomDropEntry to add an entity ID.
 */
public static class EntityDropEntry extends CustomDropEntry {
    private String entityId;  // The Minecraft entity ID (e.g., "minecraft:zombie")
    
    /**
     * Default constructor for Gson deserialization.
     */
    public EntityDropEntry() {
        // Default constructor for Gson
    }
    
    /**
     * Constructor for a basic entity drop without NBT data.
     * 
     * @param entityId The Minecraft entity ID
     * @param itemId The Minecraft item ID
     * @param dropChance Percentage chance to drop (0-100)
     * @param minAmount Minimum number of items to drop
     * @param maxAmount Maximum number of items to drop
     */
    public EntityDropEntry(String entityId, String itemId, float dropChance, int minAmount, int maxAmount) {
        this(entityId, itemId, dropChance, minAmount, maxAmount, null);
    }
    
    /**
     * Constructor for an entity drop with NBT data.
     * 
     * @param entityId The Minecraft entity ID
     * @param itemId The Minecraft item ID
     * @param dropChance Percentage chance to drop (0-100)
     * @param minAmount Minimum number of items to drop
     * @param maxAmount Maximum number of items to drop
     * @param nbtData Custom NBT data for the item
     */
    public EntityDropEntry(String entityId, String itemId, float dropChance, int minAmount, int maxAmount, String nbtData) {
        super(itemId, dropChance, minAmount, maxAmount, nbtData);
        this.entityId = entityId;
    }
    
    /**
     * Gets the Minecraft entity ID.
     * 
     * @return The entity ID
     */
    public String getEntityId() {
        return entityId;
    }
    
    /**
     * Sets the Minecraft entity ID.
     * 
     * @param entityId The entity ID
     */
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }
}
}
