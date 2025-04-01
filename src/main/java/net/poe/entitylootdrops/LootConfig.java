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

public class LootConfig {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String CONFIG_DIR = "config/EntityLootDrops";
    private static final String NORMAL_DROPS_DIR = "normal";
    private static final String EVENTS_DIR = "events";
    private static final String MOBS_DIR = "mobs";
    private static final String HOSTILE_DROPS_FILE = "hostile_drops.json";
    private static final String[] EVENT_TYPES = {"winter", "summer", "easter", "halloween"};
    
    private static Map<String, List<EntityDropEntry>> entityDrops = new HashMap<>(); // directory -> drops
    private static Map<String, List<CustomDropEntry>> hostileDrops = new HashMap<>(); // directory -> drops
    private static Set<String> activeEvents = new HashSet<>();
    private static boolean dropChanceEventActive = false;
    private static boolean doubleDropsActive = false;

    private static Map<String, String> eventEnableMessages = new HashMap<>();
    private static Map<String, String> eventDisableMessages = new HashMap<>();
    private static String dropChanceEnableMessage = "§6[Events] §aDouble Drop Chance event has been enabled! §e(2x drop rates)";
    private static String dropChanceDisableMessage = "§6[Events] §cDouble Drop Chance event has been disabled!";
    private static String doubleDropsEnableMessage = "§6[Events] §aDouble Drops event has been enabled! §e(2x drop amounts)";
    private static String doubleDropsDisableMessage = "§6[Events] §cDouble Drops event has been disabled!";

    public static void loadConfig() {
        // Store current active events and dropchance state
        Set<String> previousActiveEvents = new HashSet<>(activeEvents);
        boolean previousDropChanceState = dropChanceEventActive;
        boolean previousDoubleDropsState = doubleDropsActive;
        
        // Create directories if they don't exist
        createConfigDirectories();
        
        // Load all drops from files
        loadAllDrops();
        
        // Load message configurations
        loadMessages();
        
        // Restore active events (but only if they still exist in the config)
        activeEvents.clear();
        for (String event : previousActiveEvents) {
            if (entityDrops.containsKey(event) || hostileDrops.containsKey(event)) {
                activeEvents.add(event);
            }
        }
        
        // Restore event states
        dropChanceEventActive = previousDropChanceState;
        doubleDropsActive = previousDoubleDropsState;
        
        LOGGER.info("Reloaded configuration: {} entity drop types, {} hostile drop types, {} active events", 
            entityDrops.size(), hostileDrops.size(), activeEvents.size());
        
        if (dropChanceEventActive) {
            LOGGER.info("Drop chance bonus event is active (2x drop rates)");
        }
        
        if (doubleDropsActive) {
            LOGGER.info("Double drops event is active (2x drop amounts)");
        }
    }
    
    private static void loadAllDrops() {
        // Clear existing drops
        entityDrops.clear();
        hostileDrops.clear();
        
        // Load normal drops
        Path normalDropsDir = Paths.get(CONFIG_DIR, NORMAL_DROPS_DIR);
        loadDirectoryDrops(normalDropsDir, NORMAL_DROPS_DIR);
        
        // Load event drops
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
// Add this to LootConfig class
private static final String MESSAGES_FILE = "messages.json";

// Add this class to store message configurations
public static class MessageConfig {
    private Map<String, String> eventEnableMessages = new HashMap<>();
    private Map<String, String> eventDisableMessages = new HashMap<>();
    private String dropChanceEnableMessage = "§6[Events] §aDouble Drop Chance event has been enabled! §e(2x drop rates)";
    private String dropChanceDisableMessage = "§6[Events] §cDouble Drop Chance event has been disabled!";
    private String doubleDropsEnableMessage = "§6[Events] §aDouble Drops event has been enabled! §e(2x drop amounts)";
    private String doubleDropsDisableMessage = "§6[Events] §cDouble Drops event has been disabled!";
    private String _comment = "Configure broadcast messages for events";
}

// Add this to the loadConfig method
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
                // Apply loaded configuration
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
            
            for (String eventType : EVENT_TYPES) {
                Path eventTypeDir = Paths.get(CONFIG_DIR, EVENTS_DIR, eventType);
                Files.createDirectories(eventTypeDir);
                createDefaultDrops(eventTypeDir, eventType);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create config directories", e);
        }
    }
    
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
            readme.append("|-- normal/                  # Regular drops (always active)\n");
            readme.append("|   |-- hostile_drops.json   # Drops for all hostile mobs\n");
            readme.append("|   `-- mobs/                # Entity-specific drops\n");
            readme.append("|       |-- zombie_drops.json\n");
            readme.append("|       |-- skeleton_drops.json\n");
            readme.append("|       `-- ...\n");
            readme.append("|-- events/                  # Event-specific drops\n");
            readme.append("    |-- winter/              # Winter event drops\n");
            readme.append("    |   |-- hostile_drops.json\n");
            readme.append("    |   `-- mobs/\n");
            readme.append("    |       |-- skeleton_ice.json\n");
            readme.append("    |       `-- ...\n");
            readme.append("    |-- summer/              # Summer event drops\n");
            readme.append("    |-- easter/              # Easter event drops\n");
            readme.append("    |-- halloween/           # Halloween event drops\n");
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
            readme.append("- /lootdrops event doubledrops true     - Enable 2x item drops bonus\n");
            readme.append("- /lootdrops event doubledrops false    - Disable item drop bonus\n\n");
            
            readme.append("B. Information Commands:\n");
            readme.append("- /lootdrops list                      - List all active events\n");
            readme.append("- /lootdrops reload                    - Reload all configuration files\n");
            readme.append("- /lootdrops help                      - Show command help\n\n");
            
            readme.append("C. Examples:\n");
            readme.append("- /lootdrops event winter true         - Enable winter event drops\n");
            readme.append("- /lootdrops event halloween false     - Disable halloween event drops\n");
            readme.append("- /lootdrops event mycustomevent true  - Enable a custom event\n\n");
            
            readme.append("D. Permissions:\n");
            readme.append("- entitylootdrops.command.event       - Permission to enable/disable events\n");
            readme.append("- entitylootdrops.command.list        - Permission to list active events\n");
            readme.append("- entitylootdrops.command.reload      - Permission to reload configurations\n");
            readme.append("- entitylootdrops.command.admin       - Permission for all commands\n\n");
            
            readme.append("8. Events System\n");
            readme.append("-------------\n");
            readme.append("- Events are special drop configurations that can be toggled on/off\n");
            readme.append("- Multiple events can be active simultaneously\n");
            readme.append("- Event drops stack with normal drops\n");
            readme.append("- Custom events can be created by adding new folders in the events directory\n");
            readme.append("- The drop chance event doubles all drop chances when active\n");
            readme.append("- The double drops event doubles all item drops when active\n\n");
            
            readme.append("11. Customizing Event Messages\n");
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
            readme.append("5. Case Sensitivity: IDs and commands are case-sensitive\n\n");
                        
            Files.write(readmePath, readme.toString().getBytes());
            LOGGER.info("Created README.txt in config directory");
        }
    }
    
    private static void createDefaultDrops(Path directory, String eventType) throws IOException {
    // First create the mobs directory
    Path mobsDir = directory.resolve(MOBS_DIR);
    Files.createDirectories(mobsDir);
    LOGGER.info("Created mobs directory at: {}", mobsDir);

    // Create a simple test hostile drops file
    Path hostileDropsPath = directory.resolve(HOSTILE_DROPS_FILE);
    if (!Files.exists(hostileDropsPath)) {
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
            switch (eventType) {
                case "winter":
                    eventDrop.setItemId("minecraft:blue_ice");
                    eventDrop.setNbtData("{display:{Name:'{\"text\":\"Frozen Treasure\",\"color\":\"aqua\"}'},Enchantments:[{id:\"minecraft:frost_walker\",lvl:2}]}");
                    eventDrop.setCommand("particle minecraft:snowflake {entity_x} {entity_y} {entity_z} 1 1 1 0.1 50");
                    break;
                case "summer":
                    eventDrop.setItemId("minecraft:magma_block");
                    eventDrop.setNbtData("{display:{Name:'{\"text\":\"Summer Heat\",\"color\":\"red\"}'},Enchantments:[{id:\"minecraft:fire_aspect\",lvl:2}]}");
                    eventDrop.setCommand("particle minecraft:flame {entity_x} {entity_y} {entity_z} 1 1 1 0.1 50");
                    break;
                case "halloween":
                    eventDrop.setItemId("minecraft:jack_o_lantern");
                    eventDrop.setNbtData("{display:{Name:'{\"text\":\"Spooky Treasure\",\"color\":\"dark_purple\"}'},Enchantments:[{id:\"minecraft:soul_speed\",lvl:3}]}");
                    eventDrop.setCommand("particle minecraft:soul {entity_x} {entity_y} {entity_z} 1 1 1 0.1 50");
                    break;
                case "easter":
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
        
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(defaultHostileDrops);
        Files.write(hostileDropsPath, json.getBytes());
        LOGGER.info("Created test hostile drops at: {}", hostileDropsPath);
    }

    // Log the contents of the directory
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

    
    private static void broadcastEventMessage(String message) {
    // Get the server instance
    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    if (server != null) {
        // Send message to all players
        server.getPlayerList().broadcastSystemMessage(
            Component.literal(message), false);
    }
}


    private static void loadDirectoryDrops(Path directory, String dirKey) {
        if (!Files.exists(directory)) {
            return;
        }
        
        try {
            // Load hostile drops from the directory root
            Path hostileDropsPath = directory.resolve(HOSTILE_DROPS_FILE);
            if (Files.exists(hostileDropsPath)) {
                String json = new String(Files.readAllBytes(hostileDropsPath));
                Gson gson = new Gson();
                Type listType = new TypeToken<ArrayList<CustomDropEntry>>(){}.getType();
                List<CustomDropEntry> drops = gson.fromJson(json, listType);
                if (drops != null && !drops.isEmpty()) {
                    hostileDrops.put(dirKey, drops);
                    LOGGER.debug("Loaded {} hostile drops from {}", drops.size(), dirKey);
                }
            }
            
            // Load entity-specific drops from the mobs subdirectory
            Path mobsDir = directory.resolve(MOBS_DIR);
            if (Files.exists(mobsDir)) {
                List<EntityDropEntry> dirDrops = new ArrayList<>();
                Files.list(mobsDir)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            String json = new String(Files.readAllBytes(path));
                            Gson gson = new Gson();
                            EntityDropEntry entry = gson.fromJson(json, EntityDropEntry.class);
                            if (entry != null) {
                                dirDrops.add(entry);
                                LOGGER.debug("Loaded entity drop: {} -> {} from {}", 
                                    entry.getEntityId(), entry.getItemId(), path.getFileName());
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
    
    public static List<EntityDropEntry> getNormalDrops() {
        return entityDrops.getOrDefault(NORMAL_DROPS_DIR, Collections.emptyList());
    }
    
    public static List<CustomDropEntry> getNormalHostileDrops() {
        return hostileDrops.getOrDefault(NORMAL_DROPS_DIR, Collections.emptyList());
    }
    
    public static Map<String, List<EntityDropEntry>> getEventDrops() {
        return entityDrops.entrySet().stream()
            .filter(e -> !e.getKey().equals(NORMAL_DROPS_DIR))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    public static List<CustomDropEntry> getEventHostileDrops(String eventName) {
        return hostileDrops.getOrDefault(eventName, Collections.emptyList());
    }
    
    public static boolean isEventActive(String eventName) {
        return activeEvents.contains(eventName.toLowerCase());
    }
    
    public static void toggleEvent(String eventName, boolean active) {
        if (active) {
            activeEvents.add(eventName.toLowerCase());
            LOGGER.info("Enabled event: {}", eventName);
            String message = eventEnableMessages.getOrDefault(eventName.toLowerCase(), 
                "§6[Events] §a" + eventName + " event has been enabled!");
            broadcastEventMessage(message);
        } else {
            activeEvents.remove(eventName.toLowerCase());
            LOGGER.info("Disabled event: {}", eventName);
            String message = eventDisableMessages.getOrDefault(eventName.toLowerCase(), 
                "§6[Events] §c" + eventName + " event has been disabled!");
            broadcastEventMessage(message);
        }
    }
    
    public static void toggleDropChanceEvent(boolean active) {
        dropChanceEventActive = active;
        LOGGER.info("Drop chance event set to: {}", active);
        if (active) {
            broadcastEventMessage(dropChanceEnableMessage);
        } else {
            broadcastEventMessage(dropChanceDisableMessage);
        }
    }
    
    public static void toggleDoubleDrops(boolean active) {
        doubleDropsActive = active;
        LOGGER.info("Double drops set to: {}", active);
        if (active) {
            broadcastEventMessage(doubleDropsEnableMessage);
        } else {
            broadcastEventMessage(doubleDropsDisableMessage);
        }
    }

    
    public static void setEventEnableMessage(String eventName, String message) {
    eventEnableMessages.put(eventName.toLowerCase(), message);
}

public static void setEventDisableMessage(String eventName, String message) {
    eventDisableMessages.put(eventName.toLowerCase(), message);
}

public static void setDropChanceEnableMessage(String message) {
    dropChanceEnableMessage = message;
}

public static void setDropChanceDisableMessage(String message) {
    dropChanceDisableMessage = message;
}

public static void setDoubleDropsEnableMessage(String message) {
    doubleDropsEnableMessage = message;
}

public static void setDoubleDropsDisableMessage(String message) {
    doubleDropsDisableMessage = message;
}

// Getters for the messages
public static String getEventEnableMessage(String eventName) {
    return eventEnableMessages.getOrDefault(eventName.toLowerCase(), 
        "§6[Events] §a" + eventName + " event has been enabled!");
}
// Add these missing getter methods to LootConfig.java
public static String getEventDisableMessage(String eventName) {
    return eventDisableMessages.getOrDefault(eventName.toLowerCase(), 
        "§6[Events] §c" + eventName + " event has been disabled!");
}

public static String getDropChanceEnableMessage() {
    return dropChanceEnableMessage;
}

public static String getDropChanceDisableMessage() {
    return dropChanceDisableMessage;
}

public static String getDoubleDropsEnableMessage() {
    return doubleDropsEnableMessage;
}

public static String getDoubleDropsDisableMessage() {
    return doubleDropsDisableMessage;
}

// Add methods to get active events and check event states
public static Set<String> getActiveEvents() {
    return Collections.unmodifiableSet(activeEvents);
}

public static boolean isDropChanceEventActive() {
    return dropChanceEventActive;
}

public static boolean isDoubleDropsActive() {
    return doubleDropsActive;
}
	
public static class CustomDropEntry {
    private String itemId;
    private float dropChance;
    private int minAmount;
    private int maxAmount;
    private String nbtData; // Added field for NBT data
    private String requiredAdvancement; // Player must have this advancement
    private String requiredEffect;      // Player must have this effect
    private String requiredEquipment;   // Player must have this item equipped
    private String requiredDimension; // New field for dimension requirement
    private String command;         // Command to execute when drop occurs
    private float commandChance;    // Chance to execute command (0-100)
    private String _comment; // For documentation in the JSON file
    
    public CustomDropEntry() {
        // Default constructor for Gson
    }
    
    public CustomDropEntry(String itemId, float dropChance, int minAmount, int maxAmount) {
        this(itemId, dropChance, minAmount, maxAmount, null);
    }
    
    public CustomDropEntry(String itemId, float dropChance, int minAmount, int maxAmount, String nbtData) {
        this.itemId = itemId;
        this.dropChance = dropChance;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.nbtData = nbtData;
        this.commandChance = 100.0f; // Default to 100% if command is specified
    }
    
    public String getItemId() {
        return itemId;
    }
    
    public float getDropChance() {
        return dropChance;
    }
    
    public int getMinAmount() {
        return minAmount;
    }
    
    public int getMaxAmount() {
        return maxAmount;
    }
    
    // Add getter for NBT data
    public String getNbtData() {
        return nbtData;
    }
    
    // Getters for new fields
    public String getRequiredAdvancement() {
        return requiredAdvancement;
    }
    
    public String getRequiredEffect() {
        return requiredEffect;
    }
    
    public String getRequiredEquipment() {
        return requiredEquipment;
    }

    public String getRequiredDimension() {
        return requiredDimension;
    }
    
    public String getCommand() {
        return command;
    }
    
    public float getCommandChance() {
        return commandChance;
    }

    public boolean hasNbtData() {
        return nbtData != null && !nbtData.isEmpty();
    }
    
    public boolean hasRequiredAdvancement() {
        return requiredAdvancement != null && !requiredAdvancement.isEmpty();
    }
    
    public boolean hasRequiredEffect() {
        return requiredEffect != null && !requiredEffect.isEmpty();
    }
    
    public boolean hasRequiredEquipment() {
        return requiredEquipment != null && !requiredEquipment.isEmpty();
    }
    
    public boolean hasRequiredDimension() {
        return requiredDimension != null && !requiredDimension.isEmpty();
    }
    
    public boolean hasCommand() {
        return command != null && !command.isEmpty();
    }
    
    // Add setter methods
    public void setRequiredDimension(String requiredDimension) {
        this.requiredDimension = requiredDimension;
    }
    
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    
    public void setDropChance(float dropChance) {
        this.dropChance = dropChance;
    }
    
    public void setMinAmount(int minAmount) {
        this.minAmount = minAmount;
    }
    
    public void setMaxAmount(int maxAmount) {
        this.maxAmount = maxAmount;
    }
    
    public void setNbtData(String nbtData) {
        this.nbtData = nbtData;
    }
    
    public void setRequiredAdvancement(String requiredAdvancement) {
        this.requiredAdvancement = requiredAdvancement;
    }
    
    public void setRequiredEffect(String requiredEffect) {
        this.requiredEffect = requiredEffect;
    }
    
    public void setRequiredEquipment(String requiredEquipment) {
        this.requiredEquipment = requiredEquipment;
    }
    
    public void setCommand(String command) {
        this.command = command;
    }
    
    public void setCommandChance(float commandChance) {
        this.commandChance = commandChance;
    }
    
    public void setComment(String comment) {
        this._comment = comment;
    }
}

	public static class EntityDropEntry extends CustomDropEntry {
    private String entityId;
    
    public EntityDropEntry() {
        // Default constructor for Gson
    }
    
    public EntityDropEntry(String entityId, String itemId, float dropChance, int minAmount, int maxAmount) {
        this(entityId, itemId, dropChance, minAmount, maxAmount, null);
    }
    
    public EntityDropEntry(String entityId, String itemId, float dropChance, int minAmount, int maxAmount, String nbtData) {
        super(itemId, dropChance, minAmount, maxAmount, nbtData);
        this.entityId = entityId;
    }
    
    public String getEntityId() {
        return entityId;
    }
    
    // Add setter method
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }
}
}
