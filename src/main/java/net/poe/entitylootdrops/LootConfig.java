package net.poe.entitylootdrops;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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

public class LootConfig {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String CONFIG_DIR = "config/entitylootdrops";
    private static final String NORMAL_DROPS_DIR = "normal";
    private static final String EVENTS_DIR = "events";
    private static final String HOSTILE_DROPS_FILE = "hostile_drops.json";
    private static final String[] EVENT_TYPES = {"winter", "summer", "easter", "halloween"};
    
    private static Map<String, List<EntityDropEntry>> entityDrops = new HashMap<>(); // directory -> drops
    private static Map<String, List<CustomDropEntry>> hostileDrops = new HashMap<>(); // directory -> drops
    private static Set<String> activeEvents = new HashSet<>();
    private static boolean dropChanceEventActive = false;
    
    public static void loadConfig() {
        // Store current active events and dropchance state
        Set<String> previousActiveEvents = new HashSet<>(activeEvents);
        boolean previousDropChanceState = dropChanceEventActive;
        
        // Create directories if they don't exist
        createConfigDirectories();
        
        // Load all drops from files
        loadAllDrops();
        
        // Restore active events (but only if they still exist in the config)
        activeEvents.clear();
        for (String event : previousActiveEvents) {
            if (entityDrops.containsKey(event) || hostileDrops.containsKey(event)) {
                activeEvents.add(event);
            }
        }
        
        // Restore dropchance event state
        dropChanceEventActive = previousDropChanceState;
        
        LOGGER.info("Reloaded configuration: {} entity drop types, {} hostile drop types, {} active events", 
            entityDrops.size(), hostileDrops.size(), activeEvents.size());
        
        if (dropChanceEventActive) {
            LOGGER.info("Drop chance bonus event is active (2x drop rates)");
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
            
            readme.append("1. Directory Structure\n");
            readme.append("----------------------\n");
            readme.append("config/entitylootdrops/\n");
            readme.append("|-- normal/                  # Regular drops (always active)\n");
            readme.append("|   |-- hostile_drops.json   # Drops for all hostile mobs\n");
            readme.append("|   `-- [entity]_drops.json  # Entity-specific drops\n");
            readme.append("|-- events/                  # Event-specific drops\n");
            readme.append("    |-- winter/              # Winter event drops\n");
            readme.append("    |-- summer/              # Summer event drops\n");
            readme.append("    |-- easter/              # Easter event drops\n");
            readme.append("    |-- halloween/           # Halloween event drops\n");
            readme.append("    `-- [custom event]/      # Custom event drops (create your own folder)\n\n");
            
            readme.append("2. Drop Configuration Format\n");
            readme.append("-------------------------\n");
            readme.append("All drop configurations use JSON format. Here's a detailed breakdown:\n\n");
            
            readme.append("Basic Properties:\n");
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
            readme.append("- requiredEquipment: Player must have this item equipped\n\n");
            readme.append("- requiredDimension: Player must be in this dimension (e.g., \"minecraft:overworld\")\n");
            
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
            readme.append("- The drop chance event doubles all drop chances when active\n\n");
            
            readme.append("9. Tips and Best Practices\n");
            readme.append("-----------------------\n");
            readme.append("1. Start with small drop chances (1-5%) for valuable items\n");
            readme.append("2. Use commands sparingly to avoid spam\n");
            readme.append("3. Test configurations on a test server first\n");
            readme.append("4. Back up your config files before making major changes\n");
            readme.append("5. Use meaningful comments in your JSON files\n");
            readme.append("6. Keep drop chances balanced for game progression\n\n");
            readme.append("7. You can delete most of the default example generated files, but keep the hostile_drops.json file\n\n");
            
            readme.append("10. Common Issues\n");
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
        // Create hostile drops file if it doesn't exist
        Path hostileDropsPath = directory.resolve(HOSTILE_DROPS_FILE);
        if (!Files.exists(hostileDropsPath)) {
            List<CustomDropEntry> defaultHostileDrops = new ArrayList<>();
            
            if (eventType == null) { // Normal drops
                // Basic drop example
                CustomDropEntry boneEntry = new CustomDropEntry();
                boneEntry.setItemId("minecraft:bone");
                boneEntry.setDropChance(20.0f);
                boneEntry.setMinAmount(1);
                boneEntry.setMaxAmount(2);
                // Add comment
                boneEntry.setComment("Basic drop example - bones from any hostile mob");
                defaultHostileDrops.add(boneEntry);
                
                // Example with NBT data
                CustomDropEntry rottenFleshEntry = new CustomDropEntry();
                rottenFleshEntry.setItemId("minecraft:rotten_flesh");
                rottenFleshEntry.setDropChance(30.0f);
                rottenFleshEntry.setMinAmount(1);
                rottenFleshEntry.setMaxAmount(3);
                rottenFleshEntry.setComment("Standard drop with higher chance");
                defaultHostileDrops.add(rottenFleshEntry);
                
                // Example with NBT data and command
                CustomDropEntry goldenAppleEntry = new CustomDropEntry();
                goldenAppleEntry.setItemId("minecraft:golden_apple");
                goldenAppleEntry.setDropChance(5.0f);
                goldenAppleEntry.setMinAmount(1);
                goldenAppleEntry.setMaxAmount(1);
                goldenAppleEntry.setNbtData("{display:{Name:'{\"text\":\"Blessed Apple\",\"color\":\"gold\",\"italic\":false}',Lore:['{\"text\":\"A rare gift from the gods\",\"color\":\"purple\",\"italic\":true}']}}");
                goldenAppleEntry.setCommand("effect give {player} minecraft:regeneration 10 1");
                goldenAppleEntry.setCommandChance(100.0f);
                goldenAppleEntry.setComment("Rare drop with custom name, lore, and gives regeneration effect when obtained");
                defaultHostileDrops.add(goldenAppleEntry);
                
                // Example with required advancement
                CustomDropEntry emeraldEntry = new CustomDropEntry();
                emeraldEntry.setItemId("minecraft:emerald");
                emeraldEntry.setDropChance(15.0f);
                emeraldEntry.setMinAmount(1);
                emeraldEntry.setMaxAmount(3);
                emeraldEntry.setRequiredAdvancement("minecraft:story/mine_diamond");
                emeraldEntry.setComment("Only drops if player has mined diamonds (advancement requirement)");
                defaultHostileDrops.add(emeraldEntry);
                
                // Example with required effect
                CustomDropEntry diamondEntry = new CustomDropEntry();
                diamondEntry.setItemId("minecraft:diamond");
                diamondEntry.setDropChance(3.0f);
                diamondEntry.setMinAmount(1);
                diamondEntry.setMaxAmount(1);
                diamondEntry.setRequiredEffect("minecraft:strength");
                diamondEntry.setComment("Only drops if player has strength effect active");
                defaultHostileDrops.add(diamondEntry);
                
                // Example with required equipment
                CustomDropEntry netheriteEntry = new CustomDropEntry();
                netheriteEntry.setItemId("minecraft:netherite_scrap");
                netheriteEntry.setDropChance(2.0f);
                netheriteEntry.setMinAmount(1);
                netheriteEntry.setMaxAmount(1);
                netheriteEntry.setRequiredEquipment("minecraft:diamond_sword");
                netheriteEntry.setComment("Only drops if player is holding a diamond sword");
                defaultHostileDrops.add(netheriteEntry);

                // Example with required dimension
                CustomDropEntry netherDrop = new CustomDropEntry();
                netherDrop.setItemId("minecraft:glowstone_dust");
                netherDrop.setDropChance(25.0f);
                netherDrop.setMinAmount(2);
                netherDrop.setMaxAmount(4);
                netherDrop.setRequiredDimension("minecraft:the_nether");
                netherDrop.setCommand("particle minecraft:flame {entity_x} {entity_y} {entity_z} 0.5 0.5 0.5 0.1 20");
                netherDrop.setCommandChance(100.0f);
                netherDrop.setComment("Nether-only drop example");
                defaultHostileDrops.add(netherDrop);
                
                // Example with just a command (no item)
                CustomDropEntry commandOnlyEntry = new CustomDropEntry();
                commandOnlyEntry.setItemId("minecraft:air");
                commandOnlyEntry.setDropChance(0.0f);
                commandOnlyEntry.setMinAmount(0);
                commandOnlyEntry.setMaxAmount(0);
                commandOnlyEntry.setCommand("tellraw {player} {\"text\":\"You feel a strange energy...\",\"color\":\"light_purple\"}");
                commandOnlyEntry.setCommandChance(5.0f);
                commandOnlyEntry.setComment("No item drops, just a 5% chance to show a message to the player");
                defaultHostileDrops.add(commandOnlyEntry);
            } else {
                switch (eventType) {
                    case "halloween":
                        // Regular pumpkin drop
                        CustomDropEntry pumpkinEntry = new CustomDropEntry();
                        pumpkinEntry.setItemId("minecraft:pumpkin");
                        pumpkinEntry.setDropChance(25.0f);
                        pumpkinEntry.setMinAmount(1);
                        pumpkinEntry.setMaxAmount(1);
                        pumpkinEntry.setComment("Halloween event: Basic pumpkin drop");
                        defaultHostileDrops.add(pumpkinEntry);
                        
                        // Jack o'lantern with custom name and spooky sound command
                        CustomDropEntry lanternEntry = new CustomDropEntry();
                        lanternEntry.setItemId("minecraft:jack_o_lantern");
                        lanternEntry.setDropChance(10.0f);
                        lanternEntry.setMinAmount(1);
                        lanternEntry.setMaxAmount(1);
                        lanternEntry.setNbtData("{display:{Name:'{\"text\":\"Spooky Lantern\",\"color\":\"dark_purple\"}'}}");
                        lanternEntry.setCommand("playsound minecraft:ambient.cave master {player} {entity_x} {entity_y} {entity_z} 1.0 0.5");
                        lanternEntry.setComment("Halloween event: Spooky lantern that plays a cave sound when obtained");
                        defaultHostileDrops.add(lanternEntry);
                        
                        // Cursed pumpkin pie with blindness effect
                        CustomDropEntry pumpkinPieEntry = new CustomDropEntry();
                        pumpkinPieEntry.setItemId("minecraft:pumpkin_pie");
                        pumpkinPieEntry.setDropChance(15.0f);
                        pumpkinPieEntry.setMinAmount(1);
                        pumpkinPieEntry.setMaxAmount(2);
                        pumpkinPieEntry.setNbtData("{display:{Name:'{\"text\":\"Cursed Pumpkin Pie\",\"color\":\"dark_red\"}',Lore:['{\"text\":\"Eat if you dare...\",\"color\":\"gray\",\"italic\":true}']}}");
                        pumpkinPieEntry.setCommand("effect give {player} minecraft:blindness 5 0");
                        pumpkinPieEntry.setCommandChance(50.0f);
                        pumpkinPieEntry.setComment("Halloween event: Cursed pie with 50% chance to give blindness");
                        defaultHostileDrops.add(pumpkinPieEntry);
                        break;
                        
                    case "winter":
                        // Regular snowball drop
                        CustomDropEntry snowballEntry = new CustomDropEntry();
                        snowballEntry.setItemId("minecraft:snowball");
                        snowballEntry.setDropChance(35.0f);
                        snowballEntry.setMinAmount(1);
                        snowballEntry.setMaxAmount(3);
                        snowballEntry.setComment("Winter event: Basic snowball drop");
                        defaultHostileDrops.add(snowballEntry);
                        
                        // Enchanted diamond sword with frost aspect theme
                        CustomDropEntry frostSwordEntry = new CustomDropEntry();
                        frostSwordEntry.setItemId("minecraft:diamond_sword");
                        frostSwordEntry.setDropChance(2.0f);
                        frostSwordEntry.setMinAmount(1);
                        frostSwordEntry.setMaxAmount(1);
                        frostSwordEntry.setNbtData("{display:{Name:'{\"text\":\"Frostbite\",\"color\":\"aqua\"}'},Enchantments:[{id:\"minecraft:sharpness\",lvl:3},{id:\"minecraft:knockback\",lvl:2}]}");
                        frostSwordEntry.setCommand("effect give {player} minecraft:slowness 5 0");
                        frostSwordEntry.setCommandChance(50.0f);
                        frostSwordEntry.setRequiredAdvancement("minecraft:story/enter_the_nether");
                        frostSwordEntry.setComment("Winter event: Rare frost sword that only drops for players who've been to the Nether");
                        defaultHostileDrops.add(frostSwordEntry);
                        
                        // Ice block with weather command
                        CustomDropEntry iceEntry = new CustomDropEntry();
                        iceEntry.setItemId("minecraft:ice");
                        iceEntry.setDropChance(10.0f);
                        iceEntry.setMinAmount(1);
                        iceEntry.setMaxAmount(3);
                        iceEntry.setCommand("weather rain 600");
                        iceEntry.setCommandChance(25.0f);
                        iceEntry.setComment("Winter event: Ice with 25% chance to make it rain");
                        defaultHostileDrops.add(iceEntry);
                        break;
                        
                    case "easter":
                        // Regular egg drop
                        CustomDropEntry eggEntry = new CustomDropEntry();
                        eggEntry.setItemId("minecraft:egg");
                        eggEntry.setDropChance(40.0f);
                        eggEntry.setMinAmount(1);
                        eggEntry.setMaxAmount(2);
                        eggEntry.setComment("Easter event: Basic egg drop");
                        defaultHostileDrops.add(eggEntry);
                        
                        // Special colored leather boots with jump boost
                        CustomDropEntry bootsEntry = new CustomDropEntry();
                        bootsEntry.setItemId("minecraft:leather_boots");
                        bootsEntry.setDropChance(8.0f);
                        bootsEntry.setMinAmount(1);
                        bootsEntry.setMaxAmount(1);
                        bootsEntry.setNbtData("{display:{Name:'{\"text\":\"Bunny Hoppers\",\"color\":\"light_purple\"}',color:16756218},Enchantments:[{id:\"minecraft:feather_falling\",lvl:4}]}");
                        bootsEntry.setCommand("effect give {player} minecraft:jump_boost 30 1");
                        bootsEntry.setComment("Easter event: Special boots that give jump boost when obtained");
                        defaultHostileDrops.add(bootsEntry);
                        
                        // Golden carrot with speed boost
                        CustomDropEntry carrotEntry = new CustomDropEntry();
                        carrotEntry.setItemId("minecraft:golden_carrot");
                        carrotEntry.setDropChance(15.0f);
                        carrotEntry.setMinAmount(1);
                        carrotEntry.setMaxAmount(3);
                        carrotEntry.setNbtData("{display:{Name:'{\"text\":\"Lucky Carrot\",\"color\":\"gold\"}'}}");
                        carrotEntry.setCommand("effect give {player} minecraft:speed 30 1");
                        carrotEntry.setRequiredEquipment("minecraft:leather_boots");
                        carrotEntry.setComment("Easter event: Lucky carrot that only drops if wearing boots");
                        defaultHostileDrops.add(carrotEntry);
                        break;
                        
                    case "summer":
                        // Regular melon drop
                        CustomDropEntry melonEntry = new CustomDropEntry();
                        melonEntry.setItemId("minecraft:melon");
                        melonEntry.setDropChance(30.0f);
                        melonEntry.setMinAmount(1);
                        melonEntry.setMaxAmount(2);
                        melonEntry.setComment("Summer event: Basic melon drop");
                        defaultHostileDrops.add(melonEntry);
                        
                        // Enchanted fishing rod with weather command
                        CustomDropEntry fishingRodEntry = new CustomDropEntry();
                        fishingRodEntry.setItemId("minecraft:fishing_rod");
                        fishingRodEntry.setDropChance(5.0f);
                        fishingRodEntry.setMinAmount(1);
                        fishingRodEntry.setMaxAmount(1);
                        fishingRodEntry.setNbtData("{display:{Name:'{\"text\":\"Summer Catcher\",\"color\":\"yellow\"}'},Enchantments:[{id:\"minecraft:luck_of_the_sea\",lvl:3},{id:\"minecraft:lure\",lvl:2}]}");
                        fishingRodEntry.setCommand("weather clear");
                        fishingRodEntry.setCommandChance(25.0f);
                        fishingRodEntry.setComment("Summer event: Special fishing rod with 25% chance to clear weather");
                        defaultHostileDrops.add(fishingRodEntry);
                        
                        // Sunflower with time set command
                        CustomDropEntry sunflowerEntry = new CustomDropEntry();
                        sunflowerEntry.setItemId("minecraft:sunflower");
                        sunflowerEntry.setDropChance(20.0f);
                        sunflowerEntry.setMinAmount(1);
                        sunflowerEntry.setMaxAmount(1);
                        sunflowerEntry.setCommand("time set day");
                        sunflowerEntry.setRequiredEffect("minecraft:night_vision");
                        sunflowerEntry.setComment("Summer event: Sunflower that sets time to day, only drops if player has night vision");
                        defaultHostileDrops.add(sunflowerEntry);
                        break;
                }
            }
            
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(defaultHostileDrops);
            Files.write(hostileDropsPath, json.getBytes());
        }
    
        // Create example entity-specific drops if no JSON files exist yet
        try {
            boolean hasEntityDrops = Files.list(directory)
                .anyMatch(p -> p.toString().endsWith(".json") && 
                           !p.getFileName().toString().equals(HOSTILE_DROPS_FILE));
                           
            if (!hasEntityDrops) {
                if (eventType == null) {
                    // Regular zombie drop
                    EntityDropEntry zombieEntry = new EntityDropEntry();
                    zombieEntry.setEntityId("minecraft:zombie");
                    zombieEntry.setItemId("minecraft:apple");
                    zombieEntry.setDropChance(20.0f);
                    zombieEntry.setMinAmount(1);
                    zombieEntry.setMaxAmount(3);
					zombieEntry.setComment("Basic zombie drop example");
                    createDropFile(directory, "zombie_drops.json", zombieEntry);
                    
                    // Cow drop with NBT
                    EntityDropEntry cowEntry = new EntityDropEntry();
                    cowEntry.setEntityId("minecraft:cow");
                    cowEntry.setItemId("minecraft:leather");
                    cowEntry.setDropChance(25.0f);
                    cowEntry.setMinAmount(1);
                    cowEntry.setMaxAmount(3);
                    cowEntry.setComment("Basic cow drop example");
                    createDropFile(directory, "cow_drops.json", cowEntry);
                    
                    // Skeleton drop with custom arrow
                    EntityDropEntry skeletonEntry = new EntityDropEntry();
                    skeletonEntry.setEntityId("minecraft:skeleton");
                    skeletonEntry.setItemId("minecraft:tipped_arrow");
                    skeletonEntry.setDropChance(15.0f);
                    skeletonEntry.setMinAmount(2);
                    skeletonEntry.setMaxAmount(5);
                    skeletonEntry.setNbtData("{Potion:\"minecraft:poison\"}");
                    skeletonEntry.setComment("Skeleton drops poison arrows");
                    createDropFile(directory, "skeleton_drops.json", skeletonEntry);
                    
                    // Creeper with command example
                    EntityDropEntry creeperEntry = new EntityDropEntry();
                    creeperEntry.setEntityId("minecraft:creeper");
                    creeperEntry.setItemId("minecraft:gunpowder");
                    creeperEntry.setDropChance(100.0f);
                    creeperEntry.setMinAmount(1);
                    creeperEntry.setMaxAmount(3);
                    creeperEntry.setCommand("playsound minecraft:entity.generic.explode master {player} {entity_x} {entity_y} {entity_z}");
                    creeperEntry.setComment("Creeper always drops gunpowder and plays explosion sound");
                    createDropFile(directory, "creeper_drops.json", creeperEntry);
                    
                    // Spider with requirement example
                    EntityDropEntry spiderEntry = new EntityDropEntry();
                    spiderEntry.setEntityId("minecraft:spider");
                    spiderEntry.setItemId("minecraft:fermented_spider_eye");
                    spiderEntry.setDropChance(10.0f);
                    spiderEntry.setMinAmount(1);
                    spiderEntry.setMaxAmount(1);
                    spiderEntry.setRequiredAdvancement("minecraft:story/enter_the_end");
                    spiderEntry.setComment("Rare spider drop that only appears for players who've been to The End");
                    createDropFile(directory, "spider_drops.json", spiderEntry);
                } else {
                    switch (eventType) {
                        case "halloween":
                            // Zombie with jack o'lantern
                            EntityDropEntry zombieEntry = new EntityDropEntry();
                            zombieEntry.setEntityId("minecraft:zombie");
                            zombieEntry.setItemId("minecraft:jack_o_lantern");
                            zombieEntry.setDropChance(30.0f);
                            zombieEntry.setMinAmount(1);
                            zombieEntry.setMaxAmount(1);
                            zombieEntry.setComment("Halloween event: Zombies drop jack o'lanterns");
                            createDropFile(directory, "zombie_pumpkin.json", zombieEntry);
                            
                            // Witch with custom potion
                            EntityDropEntry witchEntry = new EntityDropEntry();
                            witchEntry.setEntityId("minecraft:witch");
                            witchEntry.setItemId("minecraft:potion");
                            witchEntry.setDropChance(20.0f);
                            witchEntry.setMinAmount(1);
                            witchEntry.setMaxAmount(1);
                            witchEntry.setNbtData("{Potion:\"minecraft:strong_healing\",display:{Name:'{\"text\":\"Witch Brew\",\"color\":\"dark_purple\"}'}}");
                            witchEntry.setCommand("effect give {player} minecraft:nausea 10 0");
                            witchEntry.setCommandChance(50.0f);
                            witchEntry.setComment("Halloween event: Witches drop special potions with 50% chance of nausea");
                            createDropFile(directory, "witch_potion.json", witchEntry);
                            
                            // Skeleton with soul lantern
                            EntityDropEntry skeletonEntry = new EntityDropEntry();
                            skeletonEntry.setEntityId("minecraft:skeleton");
                            skeletonEntry.setItemId("minecraft:soul_lantern");
                            skeletonEntry.setDropChance(15.0f);
                            skeletonEntry.setMinAmount(1);
                            skeletonEntry.setMaxAmount(1);
                            skeletonEntry.setNbtData("{display:{Name:'{\"text\":\"Skeleton Soul\",\"color\":\"aqua\"}'}}");
                            skeletonEntry.setRequiredEquipment("minecraft:bow");
                            skeletonEntry.setComment("Halloween event: Skeletons drop soul lanterns if killed with a bow");
                            createDropFile(directory, "skeleton_soul.json", skeletonEntry);
                            break;
                            
                        case "winter":
                            // Skeleton with ice
                            EntityDropEntry skeletonIceEntry = new EntityDropEntry();
                            skeletonIceEntry.setEntityId("minecraft:skeleton");
                            skeletonIceEntry.setItemId("minecraft:ice");
                            skeletonIceEntry.setDropChance(25.0f);
                            skeletonIceEntry.setMinAmount(1);
                            skeletonIceEntry.setMaxAmount(2);
                            skeletonIceEntry.setComment("Winter event: Skeletons drop ice blocks");
                            createDropFile(directory, "skeleton_ice.json", skeletonIceEntry);
                            
                            // Stray with frost bow
                            EntityDropEntry strayEntry = new EntityDropEntry();
                            strayEntry.setEntityId("minecraft:stray");
                            strayEntry.setItemId("minecraft:bow");
                            strayEntry.setDropChance(10.0f);
                            strayEntry.setMinAmount(1);
                            strayEntry.setMaxAmount(1);
                            strayEntry.setNbtData("{display:{Name:'{\"text\":\"Frost Bow\",\"color\":\"aqua\"}'},Enchantments:[{id:\"minecraft:power\",lvl:3}]}");
                            strayEntry.setCommand("summon minecraft:snow_golem {entity_x} {entity_y} {entity_z}");
                            strayEntry.setCommandChance(10.0f);
                            strayEntry.setComment("Winter event: Strays drop special bows with 10% chance to summon a snow golem");
                            createDropFile(directory, "stray_bow.json", strayEntry);
                            
                            // Polar bear with special item
                            EntityDropEntry polarBearEntry = new EntityDropEntry();
                            polarBearEntry.setEntityId("minecraft:polar_bear");
                            polarBearEntry.setItemId("minecraft:packed_ice");
                            polarBearEntry.setDropChance(100.0f);
                            polarBearEntry.setMinAmount(1);
                            polarBearEntry.setMaxAmount(3);
                            polarBearEntry.setRequiredEffect("minecraft:invisibility");
                            polarBearEntry.setComment("Winter event: Polar bears always drop packed ice if killed while player is invisible");
                            createDropFile(directory, "polar_bear_ice.json", polarBearEntry);
                            break;
                            
                        case "easter":
                            // Chicken with egg
                            EntityDropEntry chickenEntry = new EntityDropEntry();
                            chickenEntry.setEntityId("minecraft:chicken");
                            chickenEntry.setItemId("minecraft:egg");
                            chickenEntry.setDropChance(50.0f);
                            chickenEntry.setMinAmount(1);
                            chickenEntry.setMaxAmount(3);
                            chickenEntry.setComment("Easter event: Chickens drop more eggs");
                            createDropFile(directory, "chicken_egg.json", chickenEntry);
                            
                            // Rabbit with special carrot
                            EntityDropEntry rabbitEntry = new EntityDropEntry();
                            rabbitEntry.setEntityId("minecraft:rabbit");
                            rabbitEntry.setItemId("minecraft:golden_carrot");
                            rabbitEntry.setDropChance(15.0f);
                            rabbitEntry.setMinAmount(1);
                            rabbitEntry.setMaxAmount(2);
                            rabbitEntry.setNbtData("{display:{Name:'{\"text\":\"Magic Carrot\",\"color\":\"gold\"}'}}");
                            rabbitEntry.setCommand("summon minecraft:rabbit {entity_x} {entity_y} {entity_z} {Type:99}");
                            rabbitEntry.setCommandChance(5.0f);
                            rabbitEntry.setComment("Easter event: Rabbits drop golden carrots with 5% chance to spawn the killer bunny");
                            createDropFile(directory, "rabbit_carrot.json", rabbitEntry);
                            
                            // Fox with egg basket
                            EntityDropEntry foxEntry = new EntityDropEntry();
                            foxEntry.setEntityId("minecraft:fox");
                            foxEntry.setItemId("minecraft:basket");
                            foxEntry.setDropChance(25.0f);
                            foxEntry.setMinAmount(1);
                            foxEntry.setMaxAmount(1);
                            foxEntry.setNbtData("{BlockEntityTag:{Items:[{id:\"minecraft:egg\",Count:5b},{id:\"minecraft:golden_carrot\",Count:2b}]}}");
                            foxEntry.setRequiredAdvancement("minecraft:husbandry/breed_an_animal");
                            foxEntry.setComment("Easter event: Foxes drop baskets with eggs if player has bred animals");
                            createDropFile(directory, "fox_basket.json", foxEntry);
                            break;
                            
                        case "summer":
                            // Cow with sunflower
                            EntityDropEntry cowEntry = new EntityDropEntry();
                            cowEntry.setEntityId("minecraft:cow");
                            cowEntry.setItemId("minecraft:sunflower");
                            cowEntry.setDropChance(30.0f);
                            cowEntry.setMinAmount(1);
                            cowEntry.setMaxAmount(2);
                            cowEntry.setComment("Summer event: Cows drop sunflowers");
                            createDropFile(directory, "cow_flower.json", cowEntry);
                            
                            // Drowned with trident
                            EntityDropEntry drownedEntry = new EntityDropEntry();
                            drownedEntry.setEntityId("minecraft:drowned");
                            drownedEntry.setItemId("minecraft:trident");
                            drownedEntry.setDropChance(5.0f);
                            drownedEntry.setMinAmount(1);
                            drownedEntry.setMaxAmount(1);
                            drownedEntry.setNbtData("{display:{Name:'{\"text\":\"Ocean Spear\",\"color\":\"dark_aqua\"}'},Enchantments:[{id:\"minecraft:loyalty\",lvl:2}]}");
                            drownedEntry.setCommand("weather thunder 600");
                            drownedEntry.setCommandChance(20.0f);
                            drownedEntry.setComment("Summer event: Drowned have increased chance to drop tridents with 20% chance to cause a thunderstorm");
                            createDropFile(directory, "drowned_trident.json", drownedEntry);
                            
                            // Husk with special drop
                            EntityDropEntry huskEntry = new EntityDropEntry();
                            huskEntry.setEntityId("minecraft:husk");
                            huskEntry.setItemId("minecraft:sand");
                            huskEntry.setDropChance(100.0f);
                            huskEntry.setMinAmount(3);
                            huskEntry.setMaxAmount(7);
                            huskEntry.setRequiredEquipment("minecraft:golden_shovel");
                            huskEntry.setComment("Summer event: Husks always drop sand if killed with a golden shovel");
                            createDropFile(directory, "husk_sand.json", huskEntry);
                            break;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error checking for entity drops in {}: {}", directory, e.getMessage());
        }
    }
    
    private static void createDropFile(Path directory, String filename, EntityDropEntry entry) throws IOException {
        Path filePath = directory.resolve(filename);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(entry);
        Files.write(filePath, json.getBytes());
    }

    private static void loadAllDrops() {
        entityDrops.clear();
        hostileDrops.clear();
        
        // Load normal drops
        loadDirectoryDrops(Paths.get(CONFIG_DIR, NORMAL_DROPS_DIR), NORMAL_DROPS_DIR);
        
        // Load event drops
        for (String eventType : EVENT_TYPES) {
            Path eventDir = Paths.get(CONFIG_DIR, EVENTS_DIR, eventType);
            loadDirectoryDrops(eventDir, eventType);
        }
        
        // Check for any custom event folders that aren't in the default list
        try {
            Path eventsDir = Paths.get(CONFIG_DIR, EVENTS_DIR);
            if (Files.exists(eventsDir)) {
                Files.list(eventsDir)
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> !Arrays.asList(EVENT_TYPES).contains(name))
                    .forEach(customEvent -> {
                        Path customEventDir = Paths.get(CONFIG_DIR, EVENTS_DIR, customEvent);
                        loadDirectoryDrops(customEventDir, customEvent);
                        LOGGER.info("Loaded custom event directory: {}", customEvent);
                    });
            }
        } catch (IOException e) {
            LOGGER.error("Error checking for custom event directories", e);
        }
    }
    
    private static void loadDirectoryDrops(Path directory, String dirKey) {
        if (!Files.exists(directory)) {
            return;
        }
        
        try {
            // Load hostile drops
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
            
            // Load entity-specific drops
            List<EntityDropEntry> dirDrops = new ArrayList<>();
            Files.list(directory)
                .filter(path -> path.toString().endsWith(".json") && 
                               !path.getFileName().toString().equals(HOSTILE_DROPS_FILE))
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
        } else {
            activeEvents.remove(eventName.toLowerCase());
            LOGGER.info("Disabled event: {}", eventName);
        }
    }
    
    public static Set<String> getActiveEvents() {
        return activeEvents;
    }
    
    public static boolean isDropChanceEventActive() {
        return dropChanceEventActive;
    }
    
    public static void toggleDropChanceEvent(boolean active) {
        dropChanceEventActive = active;
        LOGGER.info("Drop chance event set to: {}", active);
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
