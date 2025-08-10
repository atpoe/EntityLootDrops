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
            readme.append("  │       ├── custom_folder/  # Nested folders supported!\n");
            readme.append("  │       │   └── your_drops.json\n");
            readme.append("  │       └── organized/      # Any depth of nesting works\n");
            readme.append("  │           └── subfolder/\n");
            readme.append("  │               └── special_drops.json\n");
            readme.append("  └── Event Drops/            # Event-specific drops\n");
            readme.append("      ├── Winter/             # Winter event drops\n");
            readme.append("      │   ├── Winter_Event_Drops_Example.json  # Basic examples\n");
            readme.append("      │   ├── Drop_Count.json # Drop counting data (auto-generated)\n");
            readme.append("      │   └── Mobs/\n");
            readme.append("      ├── Summer/             # Summer event drops\n");
            readme.append("      │   ├── Summer_Event_Drops_Example.json\n");
            readme.append("      │   ├── Drop_Count.json # Drop counting data (auto-generated)\n");
            readme.append("      │   └── Mobs/\n");
            readme.append("      ├── Easter/             # Easter event drops\n");
            readme.append("      │   ├── Easter_Event_Drops_Example.json\n");
            readme.append("      │   ├── Drop_Count.json # Drop counting data (auto-generated)\n");
            readme.append("      │   └── Mobs/\n");
            readme.append("      └── Halloween/          # Halloween event drops\n");
            readme.append("          ├── Halloween_Event_Drops_Example.json\n");
            readme.append("          ├── Drop_Count.json # Drop counting data (auto-generated)\n");
            readme.append("          └── Mobs/\n\n");

            readme.append("Configuration Format:\n");
            readme.append("--------------------\n");
            readme.append("All drop configurations use JSON format.\n\n");

            readme.append("BASIC FORMAT (used in example files):\n");
            readme.append("- itemId: Item to drop (e.g., \"minecraft:diamond\")\n");
            readme.append("- dropChance: Percentage chance to drop (0-100)\n");
            readme.append("- minAmount: Minimum amount to drop\n");
            readme.append("- maxAmount: Maximum amount to drop\n");
            readme.append("- requirePlayerKill: If true, only drops when killed by a player\n");
            readme.append("- enableDropCount: If true, tracks drops for competitions/leaderboards\n\n");

            readme.append("ADVANCED PROPERTIES (see Global_Hostile_Drops.example):\n");
            readme.append("- nbtData: Custom NBT data for the item\n");
            readme.append("- requiredAdvancement: Required advancement for drop\n");
            readme.append("- requiredEffect: Required potion effect for drop\n");
            readme.append("- requiredEquipment: Required equipment for drop\n");
            readme.append("- requiredDimension: Required dimension for drop\n");
            readme.append("- command: Command to execute on drop\n");
            readme.append("- commandChance: Chance for command execution\n");
            readme.append("- allowDefaultDrops: Allow default drops (requires \"allowModIDs\")\n");
            readme.append("- allowModIDs: Allows drops from modids if allowDefaultDrops is false\n");
            readme.append("- enableDropCount: Enable drop tracking for this item\n");
            readme.append("- _comment: Documentation comment\n\n");

            readme.append("DROP COUNTING SYSTEM:\n");
            readme.append("====================\n");
            readme.append("COMPETITIVE EVENT FEATURE: Track player drop statistics!\n\n");

            readme.append("How Drop Counting Works:\n");
            readme.append("- Add \"enableDropCount\": true to any drop configuration\n");
            readme.append("- Only works for Event Drops (not Normal Drops)\n");
            readme.append("- Creates Drop_Count.json in each event folder automatically\n");
            readme.append("- Tracks individual player statistics per event\n");
            readme.append("- Perfect for competitive events and leaderboards\n\n");

            readme.append("Drop Count Commands:\n");
            readme.append("- /lootdrops dropcount <true|false>: Enable/disable drop counting globally\n");
            readme.append("- /lootdrops top [count]: Show top players across all events\n");
            readme.append("- /lootdrops eventtop <event> [count]: Show top players for specific event\n");
            readme.append("- /lootdrops playerstats <player>: Show detailed player statistics\n");
            readme.append("- /lootdrops reset_counts: Reset all drop count data\n\n");

            readme.append("Drop_Count.json Structure:\n");
            readme.append("{\n");
            readme.append("  \"players\": {\n");
            readme.append("    \"uuid\": {\n");
            readme.append("      \"playerName\": \"PlayerName\",\n");
            readme.append("      \"totalDrops\": 15,\n");
            readme.append("      \"itemCounts\": {\n");
            readme.append("        \"minecraft:diamond\": 10,\n");
            readme.append("        \"minecraft:emerald\": 5\n");
            readme.append("      }\n");
            readme.append("    }\n");
            readme.append("  },\n");
            readme.append("  \"summary\": {\n");
            readme.append("    \"totalItems\": 15,\n");
            readme.append("    \"totalPlayers\": 1,\n");
            readme.append("    \"itemBreakdown\": {\n");
            readme.append("      \"minecraft:diamond\": 10,\n");
            readme.append("      \"minecraft:emerald\": 5\n");
            readme.append("    }\n");
            readme.append("  }\n");
            readme.append("}\n\n");

            readme.append("NESTED FOLDER ORGANIZATION:\n");
            readme.append("==========================\n");
            readme.append("FEATURE: Organize your Mobs configurations using nested folders!\n\n");

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
            readme.append("    \"requirePlayerKill\": true,\n");
            readme.append("    \"enableDropCount\": true\n");
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
            readme.append("    \"requirePlayerKill\": false,\n");
            readme.append("    \"enableDropCount\": true\n");
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

            readme.append("COMPETITIVE EVENT SETUP:\n");
            readme.append("=======================\n");
            readme.append("Perfect for server events and competitions:\n\n");

            readme.append("1. Create Event Configuration:\n");
            readme.append("   - Set up your event drops with \"enableDropCount\": true\n");
            readme.append("   - Use special items with NBT data for unique prizes\n");
            readme.append("   - Set appropriate drop chances for competition balance\n\n");

            readme.append("2. Enable Drop Counting:\n");
            readme.append("   - Use /lootdrops dropcount true\n");
            readme.append("   - Activate your event with /lootdrops event [name] on\n\n");

            readme.append("3. Monitor Progress:\n");
            readme.append("   - Check leaderboards with /lootdrops eventtop [event]\n");
            readme.append("   - View individual stats with /lootdrops playerstats [player]\n");
            readme.append("   - Drop_Count.json files update automatically\n\n");

            readme.append("4. End Event:\n");
            readme.append("   - Review final statistics in Drop_Count.json\n");
            readme.append("   - Award prizes to top performers\n");
            readme.append("   - Use /lootdrops reset_counts to clear for next event\n\n");

            readme.append("Tips:\n");
            readme.append("----\n");
            readme.append("- Use Global_Hostile_Drops.json for drops that apply to all hostile mobs\n");
            readme.append("- Use Mobs/ folder for entity-specific drops\n");
            readme.append("- Create nested folders in Mobs/ to organize your configurations\n");
            readme.append("- Create custom .json files for organized drop categories\n");
            readme.append("- Event drops only activate when the event is enabled\n");
            readme.append("- Drop counting only works for Event Drops, not Normal Drops\n");
            readme.append("- Add \"enableDropCount\": true to track specific items\n");
            readme.append("- Reference Global_Hostile_Drops.example for all possible configuration options\n");
            readme.append("- Use /lootdrops reload to apply changes without restarting\n");
            readme.append("- Delete entire folders to regenerate all example files\n");
            readme.append("- Nested folders are scanned recursively - any .json file will be found\n");
            readme.append("- Folder names don't affect functionality - organize however you prefer\n");
            readme.append("- Drop_Count.json files are created automatically when needed\n");
            readme.append("- Multiple events can have drop counting enabled simultaneously\n");
            readme.append("- Each event maintains separate drop count statistics\n");

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
            readme.append("- Nested folders: Supports any depth of organization in Mobs/\n\n");

            readme.append("These drops will always be checked when entities are killed,\n");
            readme.append("regardless of any active events.\n\n");

            readme.append("DROP COUNTING LIMITATION:\n");
            readme.append("========================\n");
            readme.append("IMPORTANT: Drop counting (\"enableDropCount\": true) does NOT work for Normal Drops!\n\n");

            readme.append("Drop counting is only available for Event Drops because:\n");
            readme.append("- Normal drops are always active and would create massive data files\n");
            readme.append("- Competitive events need controlled, time-limited tracking\n");
            readme.append("- Event-based counting provides clear start/end boundaries\n\n");

            readme.append("For drop counting features, use Event Drops instead:\n");
            readme.append("- Create an event folder in Event Drops/\n");
            readme.append("- Add your drop configurations with \"enableDropCount\": true\n");
            readme.append("- Enable the event when you want tracking to begin\n");
            readme.append("- Use /lootdrops dropcount true to enable the system\n\n");

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
            readme.append("- Mobs/ folder: For entity-specific event drops\n");
            readme.append("- Drop_Count.json: Auto-generated drop counting data (when enabled)\n\n");

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
            readme.append("  * Supports nested folders for organization!\n");
            readme.append("  * Example: Mobs/event_bosses/special_halloween_boss.json\n");
            readme.append("- Drop_Count.json: Automatically created when drop counting is used\n\n");

            readme.append("DROP COUNTING SYSTEM:\n");
            readme.append("====================\n");
            readme.append("EXCLUSIVE FEATURE: Drop counting only works in Event Drops!\n\n");

            readme.append("Setting Up Drop Counting:\n");
            readme.append("1. Add \"enableDropCount\": true to any drop configuration\n");
            readme.append("3. Activate your event: /lootdrops event [name] on\n");
            readme.append("4. Drop_Count.json will be created automatically\n\n");

            readme.append("Drop Counting Features:\n");
            readme.append("- Tracks individual player statistics per event\n");
            readme.append("- Supports multiple simultaneous events with separate tracking\n");
            readme.append("- Perfect for competitive events and leaderboards\n");
            readme.append("- Auto-generates comprehensive statistics\n");
            readme.append("- Real-time updates as players collect items\n\n");

            readme.append("Example Drop Count Configuration:\n");
            readme.append("[\n");
            readme.append("  {\n");
            readme.append("    \"itemId\": \"minecraft:diamond\",\n");
            readme.append("    \"dropChance\": 10.0,\n");
            readme.append("    \"minAmount\": 1,\n");
            readme.append("    \"maxAmount\": 3,\n");
            readme.append("    \"requirePlayerKill\": true,\n");
            readme.append("    \"enableDropCount\": true,\n");
            readme.append("    \"_comment\": \"Special competition diamonds\"\n");
            readme.append("  }\n");
            readme.append("]\n\n");

            readme.append("Drop Count Commands:\n");
            readme.append("- /lootdrops top: Show top players across all events\n");
            readme.append("- /lootdrops eventtop [event]: Show top players for specific event\n");
            readme.append("- /lootdrops playerstats [player]: Show detailed player statistics\n");
            readme.append("- /lootdrops reset_counts: Reset all drop count data\n\n");

            readme.append("REGENERATE EVENT EXAMPLES:\n");
            readme.append("=========================\n");
            readme.append("To regenerate examples for a specific event:\n");
            readme.append("1. Delete the entire event folder (e.g., 'Winter/')\n");
            readme.append("2. Use /lootdrops reload or restart the server\n");
            readme.append("3. The event folder and examples will be recreated\n\n");

            readme.append("WARNING: Deleting an event folder will also delete Drop_Count.json!\n");
            readme.append("Back up your drop count data before regenerating examples.\n\n");

            readme.append("Event Management:\n");
            readme.append("Events are controlled through Active_Events.json in the main config directory.\n");
            readme.append("Use the /lootdrops event [name] [on/off] command to toggle events.\n\n");

            readme.append("Event drops are checked IN ADDITION to normal drops when active.\n\n");

            readme.append("COMPETITIVE EVENT WORKFLOW:\n");
            readme.append("==========================\n");
            readme.append("1. PREPARATION:\n");
            readme.append("   - Design your event drops with balanced chances\n");
            readme.append("   - Add \"enableDropCount\": true to tracked items\n");
            readme.append("   - Test configurations in creative mode\n\n");

            readme.append("2. EVENT START:\n");
            readme.append("   - Enable drop counting: /lootdrops dropcount true\n");
            readme.append("   - Activate event: /lootdrops event [name] on\n");
            readme.append("   - Announce event to players\n\n");

            readme.append("3. DURING EVENT:\n");
            readme.append("   - Monitor progress: /lootdrops alltop\n");
            readme.append("   - Check individual stats: /lootdrops playerstats [player]\n");
            readme.append("   - Drop_Count.json updates automatically\n\n");

            readme.append("4. EVENT END:\n");
            readme.append("   - Deactivate event: /lootdrops event [name] off\n");
            readme.append("   - Review final Drop_Count.json for winners\n");
            readme.append("   - Award prizes based on statistics\n");
            readme.append("   - Optional: Reset counts for next event\n\n");

            readme.append("MULTI-EVENT COMPETITIONS:\n");
            readme.append("========================\n");
            readme.append("Run multiple events simultaneously:\n");
            readme.append("- Each event maintains separate drop counts\n");
            readme.append("- Players can participate in multiple events\n");
            readme.append("- /lootdrops top shows combined statistics\n");
            readme.append("- /lootdrops eventtop [event] shows event-specific stats\n\n");

            readme.append("ADVANCED CONFIGURATION:\n");
            readme.append("======================\n");
            readme.append("For advanced event drop configurations, reference:\n");
            readme.append("- Normal Drops/Global_Hostile_Drops.example for all possible properties\n");
            readme.append("- Event drops use the same JSON format as normal drops\n");
            readme.append("- All advanced features (NBT, commands, conditions) work in events\n");
            readme.append("- Combine drop counting with custom NBT for unique competition items\n");
            readme.append("- Use commands with drops to create special effects or announcements\n");

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

            readme.append("NESTED FOLDER SUPPORT\n");
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
            readme.append("    \"requirePlayerKill\": true,\n");
            readme.append("    \"enableDropCount\": false,\n");
            readme.append("    \"_comment\": \"Drop counting only works in Event Drops!\"\n");
            readme.append("  },\n");
            readme.append("  {\n");
            readme.append("    \"entityId\": \"minecraft:skeleton\",\n");
            readme.append("    \"itemId\": \"minecraft:emerald\",\n");
            readme.append("    \"dropChance\": 8.0,\n");
            readme.append("    \"minAmount\": 1,\n");
            readme.append("    \"maxAmount\": 2,\n");
            readme.append("    \"requirePlayerKill\": false,\n");
            readme.append("    \"nbtData\": \"{display:{Name:'{\\\"text\\\":\\\"Skeleton Emerald\\\",\\\"color\\\":\\\"green\\\"}'}}\"\n");
            readme.append("  }\n");
            readme.append("]\n\n");

            readme.append("DROP COUNTING LIMITATION:\n");
            readme.append("========================\n");
            readme.append("IMPORTANT: Drop counting (\"enableDropCount\": true) does NOT work in Mobs directories!\n\n");

            readme.append("This limitation applies to:\n");
            readme.append("- Normal Drops/Mobs/ (always active drops)\n");
            readme.append("- Event Drops/[Event]/Mobs/ (event-specific entity drops)\n\n");

            readme.append("Why drop counting is disabled for entity-specific drops:\n");
            readme.append("- Entity drops can be very frequent and would create massive data files\n");
            readme.append("- Drop counting is designed for special competition items, not regular mob drops\n");
            readme.append("- Global event drops (outside Mobs folders) provide better control for competitions\n\n");

            readme.append("FOR DROP COUNTING, USE INSTEAD:\n");
            readme.append("- Event Drops/[Event]/[YourFile].json (global drops during events)\n");
            readme.append("- Add \"enableDropCount\": true to those configurations\n");
            readme.append("- Enable events with /lootdrops event [name] on\n\n");

            readme.append("ADVANCED PROPERTIES AVAILABLE:\n");
            readme.append("=============================\n");
            readme.append("All entity-specific drops support the full range of advanced properties:\n\n");

            readme.append("Required Conditions:\n");
            readme.append("- requiredAdvancement: Player must have specific advancement\n");
            readme.append("- requiredEffect: Player must have potion effect\n");
            readme.append("- requiredEquipment: Player must wear specific equipment\n");
            readme.append("- requiredDimension: Drop only in specific dimension\n");
            readme.append("- requiredBiome: Drop only in specific biome\n");
            readme.append("- requiredWeather: Drop only in specific weather\n");
            readme.append("- requiredTime: Drop only at specific time (day/night)\n\n");

            readme.append("Special Features:\n");
            readme.append("- nbtData: Custom NBT data for dropped items\n");
            readme.append("- command: Execute command when item drops\n");
            readme.append("- commandChance: Chance for command execution\n");
            readme.append("- dropCommand: Execute command specific to this drop\n");
            readme.append("- allowDefaultDrops: Control vanilla/mod drops\n");
            readme.append("- allowModIDs: Whitelist specific mod drops\n\n");

            readme.append("ORGANIZATION BEST PRACTICES:\n");
            readme.append("============================\n");
            readme.append("Suggested folder structures for better organization:\n\n");

            readme.append("By Mod Source:\n");
            readme.append("- vanilla_mobs/hostile.json (minecraft mobs)\n");
            readme.append("- thermal_expansion/machines.json (mod-specific)\n");
            readme.append("- twilight_forest/bosses.json (mod bosses)\n\n");

            readme.append("By Entity Type:\n");
            readme.append("- undead/zombies_and_skeletons.json\n");
            readme.append("- arthropods/spiders_and_silverfish.json\n");
            readme.append("- bosses/raid_and_dungeon_bosses.json\n\n");

            readme.append("By Drop Category:\n");
            readme.append("- rare_materials/special_drops.json\n");
            readme.append("- currency/emerald_drops.json\n");
            readme.append("- equipment/armor_and_tools.json\n\n");

            readme.append("By Difficulty/Rarity:\n");
            readme.append("- common/everyday_drops.json\n");
            readme.append("- uncommon/moderate_drops.json\n");
            readme.append("- rare/special_occasion_drops.json\n");
            readme.append("- legendary/ultra_rare_drops.json\n\n");

            readme.append("EXAMPLE CONFIGURATIONS:\n");
            readme.append("======================\n");

            readme.append("Basic Entity Drop:\n");
            readme.append("{\n");
            readme.append("  \"entityId\": \"minecraft:creeper\",\n");
            readme.append("  \"itemId\": \"minecraft:tnt\",\n");
            readme.append("  \"dropChance\": 15.0,\n");
            readme.append("  \"minAmount\": 1,\n");
            readme.append("  \"maxAmount\": 1,\n");
            readme.append("  \"requirePlayerKill\": true\n");
            readme.append("}\n\n");

            readme.append("Advanced Entity Drop with NBT:\n");
            readme.append("{\n");
            readme.append("  \"entityId\": \"minecraft:wither_skeleton\",\n");
            readme.append("  \"itemId\": \"minecraft:diamond_sword\",\n");
            readme.append("  \"dropChance\": 2.0,\n");
            readme.append("  \"minAmount\": 1,\n");
            readme.append("  \"maxAmount\": 1,\n");
            readme.append("  \"requirePlayerKill\": true,\n");
            readme.append("  \"nbtData\": \"{display:{Name:'{\\\"text\\\":\\\"Wither Blade\\\",\\\"color\\\":\\\"dark_red\\\"}'},Enchantments:[{id:\\\"minecraft:sharpness\\\",lvl:5}]}\",\n");
            readme.append("  \"requiredDimension\": \"minecraft:the_nether\",\n");
            readme.append("  \"command\": \"particle minecraft:soul_fire_flame ~ ~ ~ 1 1 1 0.1 20\"\n");
            readme.append("}\n\n");

            readme.append("Conditional Drop:\n");
            readme.append("{\n");
            readme.append("  \"entityId\": \"minecraft:enderman\",\n");
            readme.append("  \"itemId\": \"minecraft:ender_pearl\",\n");
            readme.append("  \"dropChance\": 25.0,\n");
            readme.append("  \"minAmount\": 2,\n");
            readme.append("  \"maxAmount\": 4,\n");
            readme.append("  \"requirePlayerKill\": true,\n");
            readme.append("  \"requiredDimension\": \"minecraft:the_end\",\n");
            readme.append("  \"requiredAdvancement\": \"minecraft:end/enter_end_gateway\"\n");
            readme.append("}\n\n");

            readme.append("TESTING YOUR CONFIGURATIONS:\n");
            readme.append("===========================\n");
            readme.append("1. Use /lootdrops reload to apply changes\n");
            readme.append("2. Test in creative mode with spawn eggs\n");
            readme.append("3. Use /kill @e[type=!player] to clear test mobs\n");
            readme.append("4. Check server logs for any configuration errors\n");
            readme.append("5. Use high drop chances (50-100%) for testing\n");
            readme.append("6. Lower drop chances for production use\n\n");

            readme.append("PERFORMANCE CONSIDERATIONS:\n");
            readme.append("==========================\n");
            readme.append("- Avoid extremely low drop chances (<0.1%) for common mobs\n");
            readme.append("- Use requirePlayerKill: true for valuable drops\n");
            readme.append("- Complex NBT data may impact performance on busy servers\n");
            readme.append("- Commands with every drop can cause lag - use commandChance\n");
            readme.append("- Test configurations on your server before deploying\n\n");

            readme.append("REGENERATE EXAMPLES:\n");
            readme.append("===================\n");
            readme.append("To regenerate example files in this directory:\n");
            readme.append("1. Delete the entire parent 'Mobs' folder\n");
            readme.append("2. Use /lootdrops reload or restart the server\n");
            readme.append("3. Example files will be recreated automatically\n\n");

            readme.append("TROUBLESHOOTING:\n");
            readme.append("===============\n");
            readme.append("Common Issues:\n");
            readme.append("- Invalid entityId: Check spelling and use tab completion\n");
            readme.append("- Invalid itemId: Ensure item exists and spelling is correct\n");
            readme.append("- JSON syntax errors: Use a JSON validator to check format\n");
            readme.append("- NBT data not working: Test NBT in /give command first\n");
            readme.append("- Drops not appearing: Check drop chances and player kill requirements\n");
            readme.append("- Performance issues: Reduce drop chances or simplify configurations\n\n");

            readme.append("Remember:\n");
            readme.append("- Files in this directory affect specific entities only\n");
            readme.append("- For global drops affecting all hostile mobs, use Global_Hostile_Drops.json\n");
            readme.append("- Drop counting only works in Event Drops, not in Mobs directories\n");
            readme.append("- All .json files in nested folders are automatically loaded\n");
            readme.append("- Use /lootdrops reload to apply changes without restart\n");
            readme.append("- Reference Global_Hostile_Drops.example for all possible properties\n");

            Files.write(readmePath, readme.toString().getBytes());
        }
    }
}