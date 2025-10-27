package net.poe.entitylootdrops.readme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Creates the README file for the Event Drops directory.
 */
public class EventDropsReadme {

    public static void create(Path eventDropsDir) throws IOException {
        Path readmePath = eventDropsDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();

            readme.append("================================================================================\n");
            readme.append("                         EVENT DROPS CONFIGURATION\n");
            readme.append("================================================================================\n\n");

            readme.append("OVERVIEW\n");
            readme.append("--------\n");
            readme.append("Event Drops are time-limited, competitive drop configurations that can be\n");
            readme.append("toggled on/off. Perfect for seasonal events, competitions, and special\n");
            readme.append("occasions with player statistics tracking.\n\n");

            readme.append("DIRECTORY STRUCTURE\n");
            readme.append("-------------------\n");
            readme.append("Event Drops/\n");
            readme.append("├── README.txt                              # This file\n");
            readme.append("├── Winter/                                 # Winter event\n");
            readme.append("│   ├── Winter_Event_Drops_Example.json     # Example configuration\n");
            readme.append("│   ├── Drop_Count.json                     # Auto-generated statistics\n");
            readme.append("│   └── Mobs/                               # Entity-specific configs\n");
            readme.append("│       └── [custom folders]/               # Organize as needed\n");
            readme.append("├── Summer/                                 # Summer event\n");
            readme.append("├── Easter/                                 # Easter event\n");
            readme.append("├── Halloween/                              # Halloween event\n");
            readme.append("└── [custom events]/                        # Create your own events\n\n");

            readme.append("CREATING A NEW EVENT\n");
            readme.append("--------------------\n");
            readme.append("1. Create a new folder in Event Drops/ with your event name\n");
            readme.append("2. Add .json configuration files (directly or in Mobs/ subfolder)\n");
            readme.append("3. Enable the event: /lootdrops event <name> on\n");
            readme.append("4. Optionally enable drop counting: /lootdrops dropcount true\n\n");

            readme.append("Example Structure:\n");
            readme.append("  Event Drops/\n");
            readme.append("  └── MyCustomEvent/\n");
            readme.append("      ├── custom_drops.json\n");
            readme.append("      └── Mobs/\n");
            readme.append("          ├── boss_drops.json\n");
            readme.append("          └── elite_drops.json\n\n");

            readme.append("CONFIGURATION PROPERTIES\n");
            readme.append("------------------------\n\n");

            readme.append("REQUIRED PROPERTIES:\n");
            readme.append("  itemId       - Minecraft item ID (e.g., \"minecraft:diamond\")\n");
            readme.append("  dropChance   - Percentage chance to drop (0-100)\n");
            readme.append("  minAmount    - Minimum number of items to drop\n");
            readme.append("  maxAmount    - Maximum number of items to drop\n\n");

            readme.append("ITEM CUSTOMIZATION:\n");
            readme.append("  nbtData      - Custom NBT data for the dropped item\n");
            readme.append("                 Example: \"{Enchantments:[{id:\\\"sharpness\\\",lvl:5}]}\"\n\n");

            readme.append("DROP BEHAVIOR:\n");
            readme.append("  requirePlayerKill  - Only drop if killed by player (default: true)\n");
            readme.append("  allowDefaultDrops  - Allow vanilla drops alongside custom (default: true)\n");
            readme.append("  allowModIDs        - Array of mod IDs that can trigger drops\n");
            readme.append("                       Empty = all mods allowed\n\n");

            readme.append("EVENT-SPECIFIC FEATURES:\n");
            readme.append("  enableDropCount    - Track drops for statistics (default: false)\n");
            readme.append("                       Creates Drop_Count.json automatically\n\n");

            readme.append("EXTRA VANILLA DROPS:\n");
            readme.append("  extraDropChance    - Percentage chance for extra vanilla drops (0-100)\n");
            readme.append("  extraAmountMin     - Minimum amount of extra drops\n");
            readme.append("  extraAmountMax     - Maximum amount of extra drops\n\n");

            readme.append("PLAYER REQUIREMENTS:\n");
            readme.append("  requiredAdvancement - Player must have this advancement\n");
            readme.append("  requiredEffect      - Player must have this potion effect\n");
            readme.append("  requiredEquipment   - Player must have this item equipped\n\n");

            readme.append("ENVIRONMENTAL CONDITIONS:\n");
            readme.append("  requiredWeather    - \"clear\", \"rain\", or \"thunder\"\n");
            readme.append("  requiredTime       - \"day\", \"night\", \"dawn\", or \"dusk\"\n");
            readme.append("  requiredDimension  - Dimension ID (e.g., \"minecraft:the_nether\")\n");
            readme.append("  requiredBiome      - Biome ID (e.g., \"minecraft:desert\")\n\n");

            readme.append("COMMAND EXECUTION:\n");
            readme.append("  command            - Command to execute on entity death\n");
            readme.append("  commandChance      - Percentage chance to execute (default: 100)\n");
            readme.append("  dropCommand        - Command to execute only when item drops\n");
            readme.append("  dropCommandChance  - Percentage chance for drop command (default: 100)\n");
            readme.append("  commandCoolDown    - Cooldown in seconds between executions\n\n");

            readme.append("DOCUMENTATION:\n");
            readme.append("  _comment           - Add comments to your configuration for documentation\n\n");

            readme.append("NBT ENTITY DATA SYSTEM\n");
            readme.append("----------------------\n");
            readme.append("Check entity NBT data before dropping items - perfect for modded mobs!\n\n");

            readme.append("Properties:\n");
            readme.append("  • nbtEntityData      - NBT path (e.g., \"Health\", \"ForgeCaps.mod:data.level\")\n");
            readme.append("  • nbtDataCondition   - Operator: \"<\", \">\", \"<=\", \">=\", \"==\", \"!=\", \"contains\"\n");
            readme.append("  • nbtDataValue       - Value to compare (string, number, or boolean)\n");
            readme.append("  • nbtEntityDrop      - Item to drop if condition is met\n");
            readme.append("  • nbtEntityDropChance - Drop chance for NBT-specific drop\n");
            readme.append("  • nbtEntityDropMin   - Minimum amount for NBT drop\n");
            readme.append("  • nbtEntityDropMax   - Maximum amount for NBT drop\n\n");

            readme.append("Use Cases:\n");
            readme.append("  • MMORPG mobs with level/health/stats requirements\n");
            readme.append("  • Boss mobs with phase-based drops\n");
            readme.append("  • Custom entities with special abilities or states\n");
            readme.append("  • Equipment-based conditional drops\n");
            readme.append("  • Modded mob data stored in NBT or ForgeCaps\n\n");

            readme.append("How to Use:\n");
            readme.append("  1. Find NBT path: Use /data get entity <selector>\n");
            readme.append("  2. Set nbtEntityData to the path (e.g., \"Health\", \"ForgeCaps.mod:data.level\")\n");
            readme.append("  3. Set nbtDataCondition (\"<\", \">\", \"<=\", \">=\", \"==\", \"!=\", \"contains\")\n");
            readme.append("  4. Set nbtDataValue to compare against\n");
            readme.append("  5. Configure nbtEntityDrop and related fields\n\n");

            readme.append("DROP COUNTING SYSTEM\n");
            readme.append("--------------------\n");
            readme.append("Track player statistics for competitive events.\n\n");

            readme.append("Setup:\n");
            readme.append("  1. Add \"enableDropCount\": true to drop configurations\n");
            readme.append("  2. Enable event: /lootdrops event <name> on\n");
            readme.append("  3. Enable counting: /lootdrops dropcount true\n");
            readme.append("  4. Drop_Count.json is created automatically in event folder\n\n");

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
            readme.append("Each event's Mobs/ folder supports unlimited nested directories.\n\n");

            readme.append("Suggested Organization Patterns:\n");
            readme.append("  By Tier:\n");
            readme.append("    • Mobs/tier1/common_drops.json\n");
            readme.append("    • Mobs/tier2/rare_drops.json\n");
            readme.append("    • Mobs/tier3/legendary_drops.json\n\n");

            readme.append("  By Entity Type:\n");
            readme.append("    • Mobs/bosses/raid_bosses.json\n");
            readme.append("    • Mobs/elites/champions.json\n");
            readme.append("    • Mobs/common/regular_mobs.json\n\n");

            readme.append("  By Reward Type:\n");
            readme.append("    • Mobs/currency/event_tokens.json\n");
            readme.append("    • Mobs/cosmetics/special_items.json\n");
            readme.append("    • Mobs/materials/crafting_drops.json\n\n");

            readme.append("All .json files in any subfolder are automatically loaded.\n\n");

            readme.append("EVENT MANAGEMENT\n");
            readme.append("----------------\n");
            readme.append("Commands:\n");
            readme.append("  • /lootdrops event <name> on       - Enable event\n");
            readme.append("  • /lootdrops event <name> off      - Disable event\n");
            readme.append("  • /lootdrops event list            - List all events and their status\n");
            readme.append("  • /lootdrops reload                - Reload all configurations\n\n");

            readme.append("Multiple Events:\n");
            readme.append("  • Multiple events can be active simultaneously\n");
            readme.append("  • Each event has independent drop counting\n");
            readme.append("  • Event-specific leaderboards track individual event progress\n\n");

            readme.append("CONFIGURATION EXAMPLES\n");
            readme.append("----------------------\n\n");

            readme.append("Basic Event Drop:\n");
            readme.append("[\n");
            readme.append("  {\n");
            readme.append("    \"itemId\": \"minecraft:diamond\",\n");
            readme.append("    \"dropChance\": 10.0,\n");
            readme.append("    \"minAmount\": 1,\n");
            readme.append("    \"maxAmount\": 3,\n");
            readme.append("    \"enableDropCount\": true\n");
            readme.append("  }\n");
            readme.append("]\n\n");

            readme.append("With NBT Condition and Counting:\n");
            readme.append("[\n");
            readme.append("  {\n");
            readme.append("    \"itemId\": \"minecraft:iron_sword\",\n");
            readme.append("    \"dropChance\": 15.0,\n");
            readme.append("    \"minAmount\": 1,\n");
            readme.append("    \"maxAmount\": 1,\n");
            readme.append("    \"nbtEntityData\": \"ForgeCaps.mmorpg:entity_data.level\",\n");
            readme.append("    \"nbtDataCondition\": \">\",\n");
            readme.append("    \"nbtDataValue\": \"50\",\n");
            readme.append("    \"nbtEntityDrop\": \"minecraft:diamond_sword\",\n");
            readme.append("    \"nbtEntityDropChance\": 50.0,\n");
            readme.append("    \"nbtEntityDropMin\": 1,\n");
            readme.append("    \"nbtEntityDropMax\": 1,\n");
            readme.append("    \"enableDropCount\": true\n");
            readme.append("  }\n");
            readme.append("]\n\n");

            readme.append("Competitive Event with Requirements:\n");
            readme.append("[\n");
            readme.append("  {\n");
            readme.append("    \"itemId\": \"minecraft:emerald\",\n");
            readme.append("    \"dropChance\": 20.0,\n");
            readme.append("    \"minAmount\": 5,\n");
            readme.append("    \"maxAmount\": 10,\n");
            readme.append("    \"requiredWeather\": \"thunder\",\n");
            readme.append("    \"requiredTime\": \"night\",\n");
            readme.append("    \"enableDropCount\": true,\n");
            readme.append("    \"_comment\": \"Bonus drops during thunderstorms at night\"\n");
            readme.append("  }\n");
            readme.append("]\n\n");

            readme.append("FILE MANAGEMENT\n");
            readme.append("---------------\n\n");

            readme.append("Auto-Generated:\n");
            readme.append("  • Drop_Count.json - Created when drop counting is enabled\n");
            readme.append("                      Contains player statistics and leaderboards\n\n");

            readme.append("Safe to Delete:\n");
            readme.append("  • All *_Example.json files\n");
            readme.append("  • Custom .json files you create\n");
            readme.append("  • Drop_Count.json (will regenerate when counting resumes)\n\n");

            readme.append("Disabling Event Drops:\n");
            readme.append("  1. Disable event: /lootdrops event <name> off\n");
            readme.append("  2. Empty file: Replace content with []\n");
            readme.append("  3. Zero chance: Set \"dropChance\": 0 and \"nbtEntityDropChance\": 0\n");
            readme.append("  4. Rename: Change .json to .json.disabled\n\n");

            readme.append("BEST PRACTICES\n");
            readme.append("--------------\n");
            readme.append("• Use descriptive event folder names (e.g., \"Winter2024\", \"SummerCompetition\")\n");
            readme.append("• Enable drop counting for competitive events\n");
            readme.append("• Use _comment fields to document your configurations\n");
            readme.append("• Organize complex events with Mobs/ subfolders\n");
            readme.append("• Test configurations with /lootdrops reload before announcing events\n");
            readme.append("• Reset statistics between events: /lootdrops dropcount reset\n");
            readme.append("• Disable events when not in use to reduce server load\n\n");

            readme.append("TROUBLESHOOTING\n");
            readme.append("---------------\n");
            readme.append("• Event not active: Use /lootdrops event <name> on\n");
            readme.append("• Drops not counting: Ensure \"enableDropCount\": true and /lootdrops dropcount true\n");
            readme.append("• Drop_Count.json not created: Enable drop counting and trigger at least one drop\n");
            readme.append("• Leaderboard empty: Check that event is active and drops have occurred\n");
            readme.append("• Multiple events conflicting: Each event operates independently\n\n");

            readme.append("For comprehensive examples, see:\n");
            readme.append("  • Winter_Event_Drops_Example.json - Complete event configuration\n");
            readme.append("  • Mobs/ folder - Entity-specific examples\n");
            readme.append("  • Main README.txt - Full property documentation\n\n");

            readme.append("================================================================================\n");

            Files.write(readmePath, readme.toString().getBytes());
        }
    }
}