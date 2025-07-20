package net.poe.entitylootdrops.readme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Creates README files for the loot drops configuration system.
 */
public class LootDropsReadmeCreator {

    /**
     * Creates the main README file for the Loot Drops directory.
     */
    public static void createLootDropsReadme(Path lootDropsDir) throws IOException {
        Path readmePath = lootDropsDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Entity Loot Drops Configuration Guide\n");
            readme.append("===================================\n\n");

            readme.append("This directory contains all entity loot drop configurations.\n\n");

            readme.append("FILE MANAGEMENT SYSTEM:\n");
            readme.append("======================\n");
            readme.append("ON FIRST LOAD: All example files and directories are created automatically.\n\n");

            readme.append("AUTO-REGENERATING FILES:\n");
            readme.append("- Global_Hostile_Drops.json: ALWAYS regenerates if deleted (contains your main config)\n\n");

            readme.append("EXAMPLE FILES:\n");
            readme.append("- Clean, simple examples for learning the basic format\n");
            readme.append("- Safe to delete - will NOT regenerate automatically\n");
            readme.append("- Can be renamed, edited, or replaced with custom files\n\n");

            readme.append("COMPREHENSIVE REFERENCE:\n");
            readme.append("- Global_Hostile_Drops.example: Complete reference with ALL possible fields\n");
            readme.append("- Use as template for advanced configurations\n");
            readme.append("- Safe to delete - will NOT regenerate automatically\n\n");

            readme.append("REGENERATE EXAMPLES:\n");
            readme.append("- Delete entire folders (like 'Loot Drops' or 'Mobs') to regenerate examples\n");
            readme.append("- Individual example files will NOT regenerate\n");
            readme.append("- Folder deletion triggers complete example recreation\n\n");

            readme.append("Directory Structure:\n");
            readme.append("-------------------\n");
            readme.append("Loot Drops/\n");
            readme.append("  ├── Normal Drops/           # Always active drops\n");
            readme.append("  │   ├── Global_Hostile_Drops.json        # Main config (AUTO-REGENERATES)\n");
            readme.append("  │   ├── Global_Hostile_Drops.example      # Comprehensive reference\n");
            readme.append("  │   └── Mobs/               # Entity-specific drops\n");
            readme.append("  │       ├── Zombie_Example.json    # Basic examples (safe to delete)\n");
            readme.append("  │       ├── Skeleton_Example.json  # Basic examples (safe to delete)\n");
            readme.append("  │       ├── custom_folder/  # ✅ Nested folders supported!\n");
            readme.append("  │       │   └── your_drops.json\n");
            readme.append("  │       └── organized/      # ✅ Any depth of nesting works\n");
            readme.append("  │           └── subfolder/\n");
            readme.append("  │               └── special_drops.json\n");
            readme.append("  └── Event Drops/            # Event-specific drops\n");
            readme.append("      ├── Winter/             # Winter event drops\n");
            readme.append("      │   ├── Winter_Event_Drops_Example.json  # Basic examples\n");
            readme.append("      │   └── Mobs/\n");
            readme.append("      ├── Summer/             # Summer event drops\n");
            readme.append("      │   ├── Summer_Event_Drops_Example.json\n");
            readme.append("      │   └── Mobs/\n");
            readme.append("      ├── Easter/             # Easter event drops\n");
            readme.append("      │   ├── Easter_Event_Drops_Example.json\n");
            readme.append("      │   └── Mobs/\n");
            readme.append("      └── Halloween/          # Halloween event drops\n");
            readme.append("          ├── Halloween_Event_Drops_Example.json\n");
            readme.append("          └── Mobs/\n\n");

            readme.append("Configuration Format:\n");
            readme.append("--------------------\n");
            readme.append("All drop configurations use JSON format.\n\n");

            readme.append("BASIC FORMAT (used in example files):\n");
            readme.append("- itemId: Item to drop (e.g., \"minecraft:diamond\")\n");
            readme.append("- dropChance: Percentage chance to drop (0-100)\n");
            readme.append("- minAmount: Minimum amount to drop\n");
            readme.append("- maxAmount: Maximum amount to drop\n");
            readme.append("- requirePlayerKill: If true, only drops when killed by a player\n\n");

            readme.append("ADVANCED PROPERTIES (see Global_Hostile_Drops.example):\n");
            readme.append("- nbtData: Custom NBT data for the item\n");
            readme.append("- requiredAdvancement: Required advancement for drop\n");
            readme.append("- requiredEffect: Required potion effect for drop\n");
            readme.append("- requiredEquipment: Required equipment for drop\n");
            readme.append("- requiredDimension: Required dimension for drop\n");
            readme.append("- command: Command to execute on drop\n");
            readme.append("- commandChance: Chance for command execution\n");
            readme.append("- _comment: Documentation comment\n\n");

            readme.append("NESTED FOLDER ORGANIZATION:\n");
            readme.append("==========================\n");
            readme.append("✅ FEATURE: Organize your Mobs configurations using nested folders!\n\n");

            readme.append("Examples of valid organization:\n");
            readme.append("- Mobs/vanilla_mobs/zombie_drops.json\n");
            readme.append("- Mobs/modded_mobs/thermal_expansion/machines.json\n");
            readme.append("- Mobs/boss_mobs/twilight_forest/bosses.json\n");
            readme.append("- Mobs/by_biome/desert/desert_mobs.json\n");
            readme.append("- Mobs/by_difficulty/hard/elite_drops.json\n\n");

            readme.append("Benefits of nested folders:\n");
            readme.append("- Organize drops by mod, biome, difficulty, or any system you prefer\n");
            readme.append("- Keep related configurations together\n");
            readme.append("- Easier to manage large numbers of entity configurations\n");
            readme.append("- Works in both Normal Drops and Event Drops\n\n");

            readme.append("Example Basic Drop Configuration:\n");
            readme.append("[\n");
            readme.append("  {\n");
            readme.append("    \"itemId\": \"minecraft:diamond\",\n");
            readme.append("    \"dropChance\": 5.0,\n");
            readme.append("    \"minAmount\": 1,\n");
            readme.append("    \"maxAmount\": 1,\n");
            readme.append("    \"requirePlayerKill\": true\n");
            readme.append("  }\n");
            readme.append("]\n\n");

            readme.append("Example Entity-Specific Drop Configuration:\n");
            readme.append("[\n");
            readme.append("  {\n");
            readme.append("    \"entityId\": \"minecraft:zombie\",\n");
            readme.append("    \"itemId\": \"minecraft:emerald\",\n");
            readme.append("    \"dropChance\": 10.0,\n");
            readme.append("    \"minAmount\": 1,\n");
            readme.append("    \"maxAmount\": 2,\n");
            readme.append("    \"requirePlayerKill\": false\n");
            readme.append("  }\n");
            readme.append("]\n\n");

            readme.append("CUSTOM FILE LOADING:\n");
            readme.append("===================\n");
            readme.append("- ANY .json file in the correct directory will be loaded\n");
            readme.append("- Rename, edit, or create new files freely\n");
            readme.append("- Use /lootdrops reload to apply changes\n");
            readme.append("- No server restart required!\n\n");

            readme.append("HOW TO DISABLE DROPS:\n");
            readme.append("====================\n");
            readme.append("Option 1 - Empty the file: Replace all content with []\n");
            readme.append("Option 2 - Set drop chances to 0: Change \"dropChance\": [value] to \"dropChance\": 0\n");
            readme.append("Option 3 - Rename file extension: Change .json to .json.disabled\n\n");

            readme.append("DON'T DELETE Global_Hostile_Drops.json - it will just regenerate!\n");
            readme.append("Use the disable methods above instead.\n\n");

            readme.append("Tips:\n");
            readme.append("----\n");
            readme.append("- Use Global_Hostile_Drops.json for drops that apply to all hostile mobs\n");
            readme.append("- Use Mobs/ folder for entity-specific drops\n");
            readme.append("- Create nested folders in Mobs/ to organize your configurations\n");
            readme.append("- Create custom .json files for organized drop categories\n");
            readme.append("- Event drops only activate when the event is enabled\n");
            readme.append("- Reference Global_Hostile_Drops.example for all possible configuration options\n");
            readme.append("- Use /lootdrops reload to apply changes without restarting\n");
            readme.append("- Delete entire folders to regenerate all example files\n");
            readme.append("- Nested folders are scanned recursively - any .json file will be found\n");
            readme.append("- Folder names don't affect functionality - organize however you prefer\n");

            Files.write(readmePath, readme.toString().getBytes());
        }
    }

    /**
     * Creates README for Normal Drops directory.
     */
    public static void createNormalDropsReadme(Path normalDropsDir) throws IOException {
        Path readmePath = normalDropsDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Normal Drops Configuration\n");
            readme.append("=========================\n\n");

            readme.append("This directory contains drops that are ALWAYS active.\n\n");

            readme.append("FILES CREATED ON FIRST LOAD:\n");
            readme.append("============================\n");
            readme.append("- Global_Hostile_Drops.json: Main configuration (AUTO-REGENERATES if deleted)\n");
            readme.append("- Global_Hostile_Drops.example: Comprehensive reference with all properties\n");
            readme.append("- Mobs/Zombie_Example.json: Clean example (safe to delete)\n");
            readme.append("- Mobs/Skeleton_Example.json: Clean example (safe to delete)\n\n");

            readme.append("FILE BEHAVIOR:\n");
            readme.append("- Global_Hostile_Drops.json: Contains your main configuration, regenerates if deleted\n");
            readme.append("- Example files: Will NOT regenerate if individually deleted\n");
            readme.append("- Custom files: Create any .json file - it will be loaded automatically\n");
            readme.append("- Nested folders: ✅ Supports any depth of organization in Mobs/\n\n");

            readme.append("These drops will always be checked when entities are killed,\n");
            readme.append("regardless of any active events.\n\n");

            readme.append("NESTED FOLDER ORGANIZATION:\n");
            readme.append("==========================\n");
            readme.append("The Mobs/ folder supports nested directories for better organization:\n\n");

            readme.append("Suggested organization patterns:\n");
            readme.append("- By mod: Mobs/thermal_expansion/machines.json\n");
            readme.append("- By entity type: Mobs/undead/zombie_variants.json\n");
            readme.append("- By biome: Mobs/nether/nether_mobs.json\n");
            readme.append("- By difficulty: Mobs/boss_mobs/raid_bosses.json\n");
            readme.append("- Mixed: Mobs/custom/my_server/special_drops.json\n\n");

            readme.append("All .json files in any subfolder will be automatically loaded.\n\n");

            readme.append("REGENERATE ALL EXAMPLES:\n");
            readme.append("=======================\n");
            readme.append("To regenerate all example files:\n");
            readme.append("1. Delete the entire 'Mobs' folder\n");
            readme.append("2. Use /lootdrops reload or restart the server\n");
            readme.append("3. All examples will be recreated\n\n");

            readme.append("HOW TO DISABLE DROPS:\n");
            readme.append("====================\n");
            readme.append("To disable drops without losing your configuration:\n\n");

            readme.append("Option 1 - Empty the file:\n");
            readme.append("Replace all content with: []\n\n");

            readme.append("Option 2 - Set drop chances to 0:\n");
            readme.append("Change \"dropChance\": [value] to \"dropChance\": 0\n\n");

            readme.append("Option 3 - Rename file extension:\n");
            readme.append("Change .json to .json.disabled (file will be ignored)\n\n");

            readme.append("Remember: Global_Hostile_Drops.json will regenerate if deleted!\n");
            readme.append("Use the methods above instead of deleting the file.\n");

            Files.write(readmePath, readme.toString().getBytes());
        }
    }

    /**
     * Creates README for Event Drops directory.
     */
    public static void createEventDropsReadme(Path eventDropsDir) throws IOException {
        Path readmePath = eventDropsDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Event Drops Configuration\n");
            readme.append("========================\n\n");

            readme.append("This directory contains drops that are only active during specific events.\n\n");

            readme.append("FILES CREATED ON FIRST LOAD:\n");
            readme.append("============================\n");
            readme.append("Each event folder contains:\n");
            readme.append("- [Event]_Event_Drops_Example.json: Clean basic example (safe to delete)\n");
            readme.append("- Mobs/ folder: For entity-specific event drops\n\n");

            readme.append("Built-in Events:\n");
            readme.append("- Winter/: Winter season drops\n");
            readme.append("- Summer/: Summer season drops\n");
            readme.append("- Easter/: Easter holiday drops\n");
            readme.append("- Halloween/: Halloween holiday drops\n\n");

            readme.append("Custom Events:\n");
            readme.append("You can create your own event folders with any name.\n");
            readme.append("Each event folder can contain:\n");
            readme.append("- Custom .json files: Your event-specific drop configurations\n");
            readme.append("- Mobs/: Entity-specific drops during this event\n");
            readme.append("  * ✅ Supports nested folders for organization!\n");
            readme.append("  * Example: Mobs/event_bosses/special_halloween_boss.json\n\n");

            readme.append("REGENERATE EVENT EXAMPLES:\n");
            readme.append("=========================\n");
            readme.append("To regenerate examples for a specific event:\n");
            readme.append("1. Delete the entire event folder (e.g., 'Winter/')\n");
            readme.append("2. Use /lootdrops reload or restart the server\n");
            readme.append("3. The event folder and examples will be recreated\n\n");

            readme.append("Event Management:\n");
            readme.append("Events are controlled through Active_Events.json in the main config directory.\n");
            readme.append("Use the /lootdrops event [name] [on/off] command to toggle events.\n\n");

            readme.append("Event drops are checked IN ADDITION to normal drops when active.\n\n");

            readme.append("ADVANCED CONFIGURATION:\n");
            readme.append("======================\n");
            readme.append("For advanced event drop configurations, reference:\n");
            readme.append("- Normal Drops/Global_Hostile_Drops.example for all possible properties\n");
            readme.append("- Event drops use the same JSON format as normal drops\n");
            readme.append("- All advanced features (NBT, commands, conditions) work in events\n");

            Files.write(readmePath, readme.toString().getBytes());
        }
    }

    /**
     * Creates README for a Mobs directory.
     */
    public static void createMobsReadme(Path mobsDir) throws IOException {
        Path readmePath = mobsDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Entity-Specific Drops Configuration\n");
            readme.append("==================================\n\n");

            readme.append("This directory contains drops for specific entities.\n\n");

            readme.append("FILES CREATED ON FIRST LOAD:\n");
            readme.append("============================\n");
            readme.append("- Zombie_Example.json: Clean basic example for zombie drops (safe to delete)\n");
            readme.append("- Skeleton_Example.json: Clean basic example for skeleton drops (safe to delete)\n\n");

            readme.append("CUSTOM FILE CREATION:\n");
            readme.append("====================\n");
            readme.append("Create any .json file in this directory or subfolders:\n");
            readme.append("- Files are automatically loaded on /lootdrops reload\n");
            readme.append("- No server restart required\n");
            readme.append("- Use any filename that ends with .json\n\n");

            readme.append("✅ NESTED FOLDER SUPPORT\n");
            readme.append("=============================\n");
            readme.append("You can organize your configurations using nested folders!\n\n");

            readme.append("Example organization:\n");
            readme.append("Mobs/\n");
            readme.append("├── vanilla_mobs/\n");
            readme.append("│   ├── zombie_drops.json\n");
            readme.append("│   ├── skeleton_drops.json\n");
            readme.append("│   └── creeper_drops.json\n");
            readme.append("├── modded_mobs/\n");
            readme.append("│   ├── thermal_expansion/\n");
            readme.append("│   │   └── machines.json\n");
            readme.append("│   └── twilight_forest/\n");
            readme.append("│       ├── bosses.json\n");
            readme.append("│       └── creatures.json\n");
            readme.append("├── boss_mobs/\n");
            readme.append("│   ├── raid_bosses.json\n");
            readme.append("│   └── dungeon_bosses.json\n");
            readme.append("└── Zombie_Example.json       # Direct files still work!\n\n");

            readme.append("Configuration Format:\n");
            readme.append("Each JSON file should contain an array of drop entries:\n\n");

            readme.append("[\n");
            readme.append("  {\n");
            readme.append("    \"entityId\": \"minecraft:zombie\",\n");
            readme.append("    \"itemId\": \"minecraft:diamond\",\n");
            readme.append("    \"dropChance\": 5.0,\n");
            readme.append("    \"minAmount\": 1,\n");
            readme.append("    \"maxAmount\": 3,\n");
            readme.append("    \"requirePlayerKill\": true\n");
            readme.append("  }\n");
            readme.append("]\n\n");

            readme.append("ADVANCED PROPERTIES:\n");
            readme.append("===================\n");
            readme.append("For all possible configuration options, see:\n");
            readme.append("Normal Drops/Global_Hostile_Drops.example\n\n");

            readme.append("Key Benefits:\n");
            readme.append("- Organize by mod, entity type, biome, or difficulty\n");
            readme.append("- Keep related configurations together\n");
            readme.append("- Easier to manage large numbers of entities\n");
            readme.append("- All .json files are automatically found and loaded\n");
            readme.append("- Folder names don't affect functionality\n\n");

            readme.append("REGENERATE EXAMPLES:\n");
            readme.append("===================\n");
            readme.append("To regenerate example files:\n");
            readme.append("1. Delete this entire 'Mobs' folder\n");
            readme.append("2. Use /lootdrops reload or restart the server\n");
            readme.append("3. Example files will be recreated\n\n");

            readme.append("Entity IDs:\n");
            readme.append("Use the full entity ID including namespace (e.g., \"minecraft:zombie\")\n");
            readme.append("For modded entities, use the mod's namespace (e.g., \"thermal:blaze\")\n");

            Files.write(readmePath, readme.toString().getBytes());
        }
    }
}