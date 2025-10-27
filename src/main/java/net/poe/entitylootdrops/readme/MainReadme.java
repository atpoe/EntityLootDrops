package net.poe.entitylootdrops.readme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Creates the main README file for the Loot Drops directory.
 */
public class MainReadme {

    public static void create(Path lootDropsDir) throws IOException {
        Path readmePath = lootDropsDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();

            readme.append("================================================================================\n");
            readme.append("                    ENTITY LOOT DROPS - CONFIGURATION GUIDE\n");
            readme.append("================================================================================\n\n");

            readme.append("OVERVIEW\n");
            readme.append("--------\n");
            readme.append("This mod provides a comprehensive system for customizing entity loot drops in\n");
            readme.append("Minecraft. Configure drops with advanced conditions, NBT data checks, player\n");
            readme.append("requirements, and competitive event tracking.\n\n");

            readme.append("DIRECTORY STRUCTURE\n");
            readme.append("-------------------\n");
            readme.append("Loot Drops/\n");
            readme.append("├── README.txt                              # This file\n");
            readme.append("├── Normal Drops/                           # Always-active drop configurations\n");
            readme.append("│   ├── README.txt                          # Normal Drops documentation\n");
            readme.append("│   ├── Global_Hostile_Drops.json           # Main config (auto-regenerates)\n");
            readme.append("│   ├── Global_Hostile_Drops.example        # Comprehensive reference\n");
            readme.append("│   ├── NBT_Entity_Data_Examples.example    # NBT condition examples\n");
            readme.append("│   └── Mobs/                               # Entity-specific configurations\n");
            readme.append("│       ├── Zombie_Example.json             # Basic zombie drops\n");
            readme.append("│       ├── Skeleton_Example.json           # Basic skeleton drops\n");
            readme.append("│       ├── NBT_Zombie_Example.json         # NBT condition example\n");
            readme.append("│       └── [custom folders]/               # Organize as needed\n");
            readme.append("└── Event Drops/                            # Event-specific drop configurations\n");
            readme.append("    ├── README.txt                          # Event Drops documentation\n");
            readme.append("    ├── Winter/                             # Winter event\n");
            readme.append("    │   ├── Winter_Event_Drops_Example.json\n");
            readme.append("    │   ├── NBT_Entity_Data_Examples.example\n");
            readme.append("    │   ├── Drop_Count.json                 # Auto-generated statistics\n");
            readme.append("    │   └── Mobs/\n");
            readme.append("    ├── Summer/                             # Summer event\n");
            readme.append("    ├── Easter/                             # Easter event\n");
            readme.append("    ├── Halloween/                          # Halloween event\n");
            readme.append("    └── [custom events]/                    # Create your own events\n\n");

            readme.append("QUICK START\n");
            readme.append("-----------\n");
            readme.append("1. Navigate to Normal Drops/ or Event Drops/ depending on your needs\n");
            readme.append("2. Review the .example files for comprehensive property documentation\n");
            readme.append("3. Edit .json files to configure your custom drops\n");
            readme.append("4. Use /lootdrops reload to apply changes without restarting\n");
            readme.append("5. For events, use /lootdrops event <name> on/off to control activation\n\n");

            readme.append("CONFIGURATION FORMAT\n");
            readme.append("--------------------\n");
            readme.append("All configurations use JSON array format:\n\n");
            readme.append("[\n");
            readme.append("  {\n");
            readme.append("    \"itemId\": \"minecraft:diamond\",\n");
            readme.append("    \"dropChance\": 5.0,\n");
            readme.append("    \"minAmount\": 1,\n");
            readme.append("    \"maxAmount\": 3\n");
            readme.append("  }\n");
            readme.append("]\n\n");

            readme.append("CORE PROPERTIES\n");
            readme.append("---------------\n");
            readme.append("Required:\n");
            readme.append("  • itemId          - Minecraft item ID (e.g., \"minecraft:diamond\")\n");
            readme.append("  • dropChance      - Percentage chance (0-100)\n");
            readme.append("  • minAmount       - Minimum drop quantity\n");
            readme.append("  • maxAmount       - Maximum drop quantity\n\n");

            readme.append("Optional:\n");
            readme.append("  • nbtData         - Custom NBT data for the dropped item\n");
            readme.append("  • requirePlayerKill - Only drop if killed by player (default: true)\n");
            readme.append("  • allowDefaultDrops - Allow vanilla drops alongside custom (default: true)\n");
            readme.append("  • enableDropCount - Track drops for statistics (Event Drops only)\n\n");

            readme.append("ADVANCED FEATURES\n");
            readme.append("-----------------\n\n");

            readme.append("1. NBT ENTITY DATA CONDITIONS\n");
            readme.append("   Check entity NBT data before dropping items:\n");
            readme.append("   • nbtEntityData      - NBT path (e.g., \"Health\", \"ForgeCaps.mod:data.level\")\n");
            readme.append("   • nbtDataCondition   - Operator: \"<\", \">\", \"<=\", \">=\", \"==\", \"!=\", \"contains\"\n");
            readme.append("   • nbtDataValue       - Value to compare (string, number, or boolean)\n");
            readme.append("   • nbtEntityDrop      - Item to drop if condition is met\n");
            readme.append("   • nbtEntityDropChance - Drop chance for NBT-specific drop\n");
            readme.append("   • nbtEntityDropMin   - Minimum amount for NBT drop\n");
            readme.append("   • nbtEntityDropMax   - Maximum amount for NBT drop\n\n");

            readme.append("   Use Cases:\n");
            readme.append("   - MMORPG mobs with level/health requirements\n");
            readme.append("   - Boss mobs with phase-based drops\n");
            readme.append("   - Custom modded entities with special data\n");
            readme.append("   - Equipment-based conditional drops\n\n");

            readme.append("2. PLAYER REQUIREMENTS\n");
            readme.append("   Require specific player conditions:\n");
            readme.append("   • requiredAdvancement - Player must have advancement\n");
            readme.append("   • requiredEffect      - Player must have potion effect\n");
            readme.append("   • requiredEquipment   - Player must have item equipped\n\n");

            readme.append("3. ENVIRONMENTAL CONDITIONS\n");
            readme.append("   Require specific world conditions:\n");
            readme.append("   • requiredWeather    - \"clear\", \"rain\", or \"thunder\"\n");
            readme.append("   • requiredTime       - \"day\", \"night\", \"dawn\", or \"dusk\"\n");
            readme.append("   • requiredDimension  - Dimension ID (e.g., \"minecraft:the_nether\")\n");
            readme.append("   • requiredBiome      - Biome ID (e.g., \"minecraft:desert\")\n\n");

            readme.append("4. COMMAND EXECUTION\n");
            readme.append("   Execute commands when drops occur:\n");
            readme.append("   • command            - Command to execute on entity death\n");
            readme.append("   • commandChance      - Percentage chance to execute (default: 100)\n");
            readme.append("   • dropCommand        - Command to execute only when item drops\n");
            readme.append("   • dropCommandChance  - Percentage chance for drop command (default: 100)\n");
            readme.append("   • commandCoolDown    - Cooldown in seconds between executions\n\n");

            readme.append("5. EXTRA VANILLA DROPS\n");
            readme.append("   Increase vanilla drop quantities:\n");
            readme.append("   • extraDropChance    - Percentage chance for extra drops (0-100)\n");
            readme.append("   • extraAmountMin     - Minimum extra amount\n");
            readme.append("   • extraAmountMax     - Maximum extra amount\n\n");

            readme.append("6. MOD COMPATIBILITY\n");
            readme.append("   Control which mods can trigger drops:\n");
            readme.append("   • allowModIDs        - Array of allowed mod IDs\n");
            readme.append("                          Empty array = all mods allowed\n\n");

            readme.append("DROP COUNTING SYSTEM (Event Drops Only)\n");
            readme.append("----------------------------------------\n");
            readme.append("Track player statistics for competitive events:\n\n");

            readme.append("Setup:\n");
            readme.append("  1. Add \"enableDropCount\": true to drop configurations\n");
            readme.append("  2. Enable event: /lootdrops event <name> on\n");
            readme.append("  3. Enable counting: /lootdrops dropcount true\n");
            readme.append("  4. Drop_Count.json is created automatically\n\n");

            readme.append("Commands:\n");
            readme.append("  • /lootdrops dropcount true/false  - Toggle drop counting\n");
            readme.append("  • /lootdrops dropcount reset       - Reset all statistics\n");
            readme.append("  • /lootdrops top [count]           - Global leaderboard\n");
            readme.append("  • /lootdrops eventtop <event> [n]  - Event-specific leaderboard\n");
            readme.append("  • /lootdrops playerstats <player>  - Player statistics\n");
            readme.append("  • /lootdrops alltop                - Comprehensive leaderboard\n\n");

            readme.append("Drop_Count.json Structure:\n");
            readme.append("{\n");
            readme.append("  \"players\": {\n");
            readme.append("    \"<uuid>\": {\n");
            readme.append("      \"playerName\": \"PlayerName\",\n");
            readme.append("      \"totalDrops\": 15,\n");
            readme.append("      \"itemCounts\": {\n");
            readme.append("        \"minecraft:diamond\": 10,\n");
            readme.append("        \"minecraft:emerald\": 5\n");
            readme.append("      },\n");
            readme.append("      \"lastUpdated\": \"2024-01-15T10:30:00Z\"\n");
            readme.append("    }\n");
            readme.append("  },\n");
            readme.append("  \"summary\": {\n");
            readme.append("    \"totalItems\": 15,\n");
            readme.append("    \"totalPlayers\": 1,\n");
            readme.append("    \"itemBreakdown\": {\n");
            readme.append("      \"minecraft:diamond\": 10,\n");
            readme.append("      \"minecraft:emerald\": 5\n");
            readme.append("    },\n");
            readme.append("    \"lastUpdated\": \"2024-01-15T10:30:00Z\"\n");
            readme.append("  }\n");
            readme.append("}\n\n");

            readme.append("NESTED FOLDER ORGANIZATION\n");
            readme.append("--------------------------\n");
            readme.append("Both Normal Drops/Mobs/ and Event Drops/[Event]/Mobs/ support unlimited\n");
            readme.append("folder nesting for organization:\n\n");

            readme.append("Examples:\n");
            readme.append("  • Mobs/vanilla/undead/zombie_variants.json\n");
            readme.append("  • Mobs/modded/thermal_expansion/machines.json\n");
            readme.append("  • Mobs/by_biome/nether/nether_mobs.json\n");
            readme.append("  • Mobs/by_difficulty/hard/elite_drops.json\n");
            readme.append("  • Mobs/nbt_conditions/mmorpg/high_level.json\n");
            readme.append("  • Mobs/events/competition/tier1/common.json\n\n");

            readme.append("All .json files in any subfolder are automatically loaded.\n\n");

            readme.append("FILE MANAGEMENT\n");
            readme.append("---------------\n\n");

            readme.append("Auto-Regenerating Files:\n");
            readme.append("  • Global_Hostile_Drops.json - Regenerates if deleted\n\n");

            readme.append("Safe to Delete:\n");
            readme.append("  • All *_Example.json files\n");
            readme.append("  • All .example files (reference documentation)\n");
            readme.append("  • Custom .json files you create\n\n");

            readme.append("Auto-Generated:\n");
            readme.append("  • Drop_Count.json files (when drop counting is enabled)\n\n");

            readme.append("Disabling Drops Without Deletion:\n");
            readme.append("  1. Empty file: Replace content with []\n");
            readme.append("  2. Zero chance: Set \"dropChance\": 0 and \"nbtEntityDropChance\": 0\n");
            readme.append("  3. Rename: Change .json to .json.disabled\n");
            readme.append("  4. For events: Use /lootdrops event <name> off\n\n");

            readme.append("COMMANDS\n");
            readme.append("--------\n");
            readme.append("Configuration:\n");
            readme.append("  • /lootdrops reload                    - Reload all configurations\n");
            readme.append("  • /lootdrops event <name> on/off       - Toggle event\n");
            readme.append("  • /lootdrops event list                - List all events\n");
            readme.append("  • /lootdrops dropcount true/false      - Toggle drop counting\n");
            readme.append("  • /lootdrops dropcount reset           - Reset statistics\n\n");

            readme.append("Statistics:\n");
            readme.append("  • /lootdrops top [count]               - Global leaderboard\n");
            readme.append("  • /lootdrops eventtop <event> [count]  - Event leaderboard\n");
            readme.append("  • /lootdrops playerstats <player>      - Player statistics\n");
            readme.append("  • /lootdrops alltop                    - All events leaderboard\n\n");

            readme.append("EXAMPLES\n");
            readme.append("--------\n\n");

            readme.append("Basic Drop:\n");
            readme.append("{\n");
            readme.append("  \"itemId\": \"minecraft:diamond\",\n");
            readme.append("  \"dropChance\": 5.0,\n");
            readme.append("  \"minAmount\": 1,\n");
            readme.append("  \"maxAmount\": 3\n");
            readme.append("}\n\n");

            readme.append("NBT Condition Drop (MMORPG mob level check):\n");
            readme.append("{\n");
            readme.append("  \"itemId\": \"minecraft:air\",\n");
            readme.append("  \"dropChance\": 0.0,\n");
            readme.append("  \"minAmount\": 0,\n");
            readme.append("  \"maxAmount\": 0,\n");
            readme.append("  \"nbtEntityData\": \"ForgeCaps.mmorpg:entity_data.level\",\n");
            readme.append("  \"nbtDataCondition\": \">\",\n");
            readme.append("  \"nbtDataValue\": \"50\",\n");
            readme.append("  \"nbtEntityDrop\": \"minecraft:nether_star\",\n");
            readme.append("  \"nbtEntityDropChance\": 100.0,\n");
            readme.append("  \"nbtEntityDropMin\": 1,\n");
            readme.append("  \"nbtEntityDropMax\": 1\n");
            readme.append("}\n\n");

            readme.append("Conditional Drop with Requirements:\n");
            readme.append("{\n");
            readme.append("  \"itemId\": \"minecraft:emerald\",\n");
            readme.append("  \"dropChance\": 10.0,\n");
            readme.append("  \"minAmount\": 1,\n");
            readme.append("  \"maxAmount\": 5,\n");
            readme.append("  \"requiredWeather\": \"thunder\",\n");
            readme.append("  \"requiredTime\": \"night\",\n");
            readme.append("  \"requiredBiome\": \"minecraft:plains\"\n");
            readme.append("}\n\n");

            readme.append("Event Drop with Counting:\n");
            readme.append("{\n");
            readme.append("  \"itemId\": \"minecraft:gold_ingot\",\n");
            readme.append("  \"dropChance\": 15.0,\n");
            readme.append("  \"minAmount\": 2,\n");
            readme.append("  \"maxAmount\": 4,\n");
            readme.append("  \"enableDropCount\": true\n");
            readme.append("}\n\n");

            readme.append("TROUBLESHOOTING\n");
            readme.append("---------------\n");
            readme.append("• Drops not working: Check console for JSON parsing errors\n");
            readme.append("• NBT conditions failing: Use /data get entity to verify NBT paths\n");
            readme.append("• Drop counting not working: Only works in Event Drops, not Normal Drops\n");
            readme.append("• File not loading: Ensure .json extension and valid JSON syntax\n");
            readme.append("• Event not active: Use /lootdrops event <name> on\n\n");

            readme.append("BEST PRACTICES\n");
            readme.append("--------------\n");
            readme.append("• Use .example files as reference, don't edit them directly\n");
            readme.append("• Organize Mobs/ folders by mod, biome, or difficulty\n");
            readme.append("• Test drop chances on test server before production\n");
            readme.append("• Use comments (\"_comment\") to document complex configurations\n");
            readme.append("• Back up configurations before major changes\n");
            readme.append("• Use /lootdrops reload instead of restarting server\n");
            readme.append("• For competitions, reset drop counts before each event\n\n");

            readme.append("SUPPORT\n");
            readme.append("-------\n");
            readme.append("For detailed examples and documentation:\n");
            readme.append("• Review .example files in each directory\n");
            readme.append("• Check Normal Drops/README.txt\n");
            readme.append("• Check Event Drops/README.txt\n");
            readme.append("• Examine example JSON files in Mobs/ folders\n\n");

            readme.append("================================================================================\n");

            Files.write(readmePath, readme.toString().getBytes());
        }
    }
}