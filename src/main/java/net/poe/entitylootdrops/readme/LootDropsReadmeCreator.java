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

            readme.append("Directory Structure:\n");
            readme.append("-------------------\n");
            readme.append("Loot Drops/\n");
            readme.append("  ├── Normal Drops/           # Always active drops\n");
            readme.append("  │   ├── Global_Hostile_Drops.json        # Main hostile mob drops\n");
            readme.append("  │   ├── Global_Hostile_Drops_Example.json # Comprehensive examples\n");
            readme.append("  │   ├── [Custom].json       # Your custom drop files\n");
            readme.append("  │   └── Mobs/               # Entity-specific drops\n");
            readme.append("  │       ├── [entity]_drops.json # Per-entity configurations\n");
            readme.append("  │       ├── custom_folder/  # ✅ NEW: Nested folders supported!\n");
            readme.append("  │       │   ├── mod_mobs.json\n");
            readme.append("  │       │   └── boss_mobs.json\n");
            readme.append("  │       └── organized/      # ✅ Any depth of nesting works\n");
            readme.append("  │           └── subfolder/\n");
            readme.append("  │               └── special_drops.json\n");
            readme.append("  └── Event Drops/            # Event-specific drops\n");
            readme.append("      ├── Winter/             # Winter event drops\n");
            readme.append("      ├── Summer/             # Summer event drops\n");
            readme.append("      ├── Easter/             # Easter event drops\n");
            readme.append("      ├── Halloween/          # Halloween event drops\n");
            readme.append("      └── [Custom Event]/     # Your custom events\n");
            readme.append("          ├── Global_Hostile_Drops.json\n");
            readme.append("          ├── Global_Hostile_Drops_Example.json\n");
            readme.append("          └── Mobs/           # Event-specific entity drops\n");
            readme.append("              ├── event_mobs.json\n");
            readme.append("              └── custom_event_folder/ # ✅ Nested folders in events too!\n");
            readme.append("                  └── special_event_drops.json\n\n");

            readme.append("Configuration Format:\n");
            readme.append("--------------------\n");
            readme.append("All drop configurations use JSON format with these properties:\n\n");

            readme.append("BASIC PROPERTIES:\n");
            readme.append("- entityId: Entity ID (e.g., \"minecraft:zombie\") - only for entity-specific drops\n");
            readme.append("- itemId: Item to drop (e.g., \"minecraft:diamond\")\n");
            readme.append("- dropChance: Percentage chance to drop (0-100)\n");
            readme.append("- minAmount: Minimum amount to drop\n");
            readme.append("- maxAmount: Maximum amount to drop\n");
            readme.append("- requirePlayerKill: If true, only drops when killed by a player\n\n");

            readme.append("ADVANCED PROPERTIES:\n");
            readme.append("- enchantments: List of enchantments to apply\n");
            readme.append("- nbtData: Custom NBT data for the item\n");
            readme.append("- customName: Custom display name for the item\n");
            readme.append("- lore: List of lore lines for the item\n");
            readme.append("- conditions: Additional conditions for the drop\n\n");

            readme.append("NESTED FOLDER ORGANIZATION:\n");
            readme.append("==========================\n");
            readme.append("✅ NEW FEATURE: You can now organize your Mobs configurations using nested folders!\n\n");

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

            readme.append("Example Entity Drop Configuration:\n");
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

            readme.append("Example Global Hostile Drop Configuration:\n");
            readme.append("[\n");
            readme.append("  {\n");
            readme.append("    \"itemId\": \"minecraft:emerald\",\n");
            readme.append("    \"dropChance\": 2.0,\n");
            readme.append("    \"minAmount\": 1,\n");
            readme.append("    \"maxAmount\": 1,\n");
            readme.append("    \"requirePlayerKill\": false\n");
            readme.append("  }\n");
            readme.append("]\n\n");

            readme.append("Tips:\n");
            readme.append("----\n");
            readme.append("- Use Global_Hostile_Drops.json for drops that apply to all hostile mobs\n");
            readme.append("- Use Mobs/ folder for entity-specific drops\n");
            readme.append("- ✅ NEW: Create nested folders in Mobs/ to organize your configurations\n");
            readme.append("- Create custom .json files for organized drop categories\n");
            readme.append("- Event drops only activate when the event is enabled\n");
            readme.append("- Delete example files to regenerate them with updates\n");
            readme.append("- Use /lootdrops reload to apply changes without restarting\n");
            readme.append("- To disable drops, use [] or set dropChance to 0 - DON'T delete the main files!\n");
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

            readme.append("Files:\n");
            readme.append("- Global_Hostile_Drops.json: Main configuration for all hostile mobs\n");
            readme.append("  * Contains your primary drop configurations\n");
            readme.append("  * Will regenerate with basic example if deleted\n");
            readme.append("  * Only delete if you want to reset to defaults\n");
            readme.append("- Global_Hostile_Drops_Example.json: Comprehensive examples (Safe to delete)\n");
            readme.append("- Custom .json files: Your additional drop configurations\n");
            readme.append("- Mobs/: Entity-specific drop configurations\n");
            readme.append("  * ✅ NEW: Supports nested folders for organization!\n");
            readme.append("  * Example: Mobs/modded_mobs/thermal_expansion/machines.json\n");
            readme.append("  * Example: Mobs/boss_mobs/twilight_forest/bosses.json\n\n");

            readme.append("These drops will always be checked when entities are killed,\n");
            readme.append("regardless of any active events.\n\n");

            readme.append("NESTED FOLDER ORGANIZATION:\n");
            readme.append("==========================\n");
            readme.append("The Mobs/ folder now supports nested directories for better organization:\n\n");

            readme.append("Suggested organization patterns:\n");
            readme.append("- By mod: Mobs/thermal_expansion/machines.json\n");
            readme.append("- By entity type: Mobs/undead/zombie_variants.json\n");
            readme.append("- By biome: Mobs/nether/nether_mobs.json\n");
            readme.append("- By difficulty: Mobs/boss_mobs/raid_bosses.json\n");
            readme.append("- Mixed: Mobs/custom/my_server/special_drops.json\n\n");

            readme.append("All .json files in any subfolder will be automatically loaded.\n\n");

            readme.append("HOW TO DISABLE DROPS:\n");
            readme.append("====================\n");
            readme.append("To disable drops without losing your file:\n\n");

            readme.append("Option 1 - Empty the file:\n");
            readme.append("Replace all content with: []\n\n");

            readme.append("Option 2 - Set drop chances to 0:\n");
            readme.append("Change \"dropChance\": [value] to \"dropChance\": 0\n\n");

            readme.append("Option 3 - Move to a disabled folder:\n");
            readme.append("Create a 'disabled' folder and move files there temporarily\n");
            readme.append("(Files in subfolders are still loaded, so this won't disable them)\n\n");

            readme.append("Remember: The system will recreate Global_Hostile_Drops.json\n");
            readme.append("if it's deleted, so use the empty array [] method instead.\n");

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

            readme.append("Built-in Events:\n");
            readme.append("- Winter/: Winter season drops\n");
            readme.append("- Summer/: Summer season drops\n");
            readme.append("- Easter/: Easter holiday drops\n");
            readme.append("- Halloween/: Halloween holiday drops\n\n");

            readme.append("Custom Events:\n");
            readme.append("You can create your own event folders with any name.\n");
            readme.append("Each event folder can contain:\n");
            readme.append("- Global_Hostile_Drops.json: Drops for all hostile mobs during this event\n");
            readme.append("- Global_Hostile_Drops_Example.json: Example configurations\n");
            readme.append("- Custom .json files: Additional drop configurations\n");
            readme.append("- Mobs/: Entity-specific drops during this event\n");
            readme.append("  * ✅ NEW: Supports nested folders for organization!\n");
            readme.append("  * Example: Mobs/event_bosses/special_halloween_boss.json\n\n");

            readme.append("Event Management:\n");
            readme.append("Events are controlled through Active_Events.json in the main config directory.\n");
            readme.append("Use the /lootdrops event [name] [on/off] command to toggle events.\n\n");

            readme.append("Event drops are checked IN ADDITION to normal drops when active.\n");

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

            readme.append("✅ NEW: NESTED FOLDER SUPPORT\n");
            readme.append("=============================\n");
            readme.append("You can now organize your configurations using nested folders!\n\n");

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
            readme.append("└── zombie_drops.json       # Direct files still work too!\n\n");

            readme.append("Configuration Format:\n");
            readme.append("Each JSON file should contain an array of EntityDropEntry objects:\n\n");

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

            readme.append("Key Benefits:\n");
            readme.append("- Organize by mod, entity type, biome, or difficulty\n");
            readme.append("- Keep related configurations together\n");
            readme.append("- Easier to manage large numbers of entities\n");
            readme.append("- All .json files are automatically found and loaded\n");
            readme.append("- Folder names don't affect functionality\n\n");

            readme.append("Entity IDs:\n");
            readme.append("Use the full entity ID including namespace (e.g., \"minecraft:zombie\")\n");
            readme.append("For modded entities, use the mod's namespace (e.g., \"thermal:blaze\")\n");

            Files.write(readmePath, readme.toString().getBytes());
        }
    }
}