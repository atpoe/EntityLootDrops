package net.poe.entitylootdrops.readme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Creates the README file for the Normal Drops directory.
 */
public class NormalDropsReadme {

    public static void create(Path normalDropsDir) throws IOException {
        Path readmePath = normalDropsDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();

            readme.append("================================================================================\n");
            readme.append("                         NORMAL DROPS CONFIGURATION\n");
            readme.append("================================================================================\n\n");

            readme.append("OVERVIEW\n");
            readme.append("--------\n");
            readme.append("Normal Drops are ALWAYS ACTIVE regardless of events. Use this directory for\n");
            readme.append("permanent server-wide loot modifications that should apply at all times.\n\n");

            readme.append("DIRECTORY CONTENTS\n");
            readme.append("------------------\n");
            readme.append("Normal Drops/\n");
            readme.append("├── README.txt                          # This file\n");
            readme.append("├── Global_Hostile_Drops.json           # Main configuration (auto-regenerates)\n");
            readme.append("├── Global_Hostile_Drops.example        # Comprehensive property reference\n");
            readme.append("├── NBT_Entity_Data_Examples.example    # NBT condition examples\n");
            readme.append("└── Mobs/                               # Entity-specific configurations\n");
            readme.append("    ├── Zombie_Example.json             # Basic zombie example\n");
            readme.append("    ├── Skeleton_Example.json           # Basic skeleton example\n");
            readme.append("    ├── NBT_Zombie_Example.json         # NBT condition example\n");
            readme.append("    └── [custom folders]/               # Your custom organization\n\n");

            readme.append("FILE BEHAVIOR\n");
            readme.append("-------------\n");
            readme.append("Auto-Regenerating:\n");
            readme.append("  • Global_Hostile_Drops.json - Recreated if deleted (contains your main config)\n\n");

            readme.append("Safe to Delete:\n");
            readme.append("  • *_Example.json files - Will NOT regenerate individually\n");
            readme.append("  • *.example files - Reference documentation only\n");
            readme.append("  • Custom .json files - Your configurations\n\n");

            readme.append("To Regenerate All Examples:\n");
            readme.append("  1. Delete entire Mobs/ folder\n");
            readme.append("  2. Use /lootdrops reload or restart server\n");
            readme.append("  3. All example files will be recreated\n\n");

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
            readme.append("  commandChance      - Percentage chance to execute command (default: 100)\n");
            readme.append("  dropCommand        - Command to execute only when item drops\n");
            readme.append("  dropCommandChance  - Percentage chance for drop command (default: 100)\n");
            readme.append("  commandCoolDown    - Cooldown in seconds between command executions\n\n");

            readme.append("NBT ENTITY DATA CONDITIONS:\n");
            readme.append("  nbtEntityData      - NBT path to check (e.g., \"Health\", \"ForgeCaps.mod:data\")\n");
            readme.append("  nbtDataCondition   - Comparison operator:\n");
            readme.append("                       \"<\", \">\", \"<=\", \">=\", \"==\", \"!=\", \"contains\"\n");
            readme.append("  nbtDataValue       - Value to compare against (string, number, or boolean)\n");
            readme.append("  nbtEntityDrop      - Item to drop if NBT condition is met\n");
            readme.append("  nbtEntityDropChance - Drop chance for NBT-specific drop\n");
            readme.append("  nbtEntityDropMin   - Minimum amount for NBT drop\n");
            readme.append("  nbtEntityDropMax   - Maximum amount for NBT drop\n\n");

            readme.append("DOCUMENTATION:\n");
            readme.append("  _comment           - Add comments to your configuration for documentation\n\n");

            readme.append("NBT ENTITY DATA SYSTEM\n");
            readme.append("----------------------\n");
            readme.append("Check entity NBT data before dropping items - perfect for modded mobs!\n\n");

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

            readme.append("Supported Operators:\n");
            readme.append("  <, >, <=, >=  - Numeric comparisons\n");
            readme.append("  ==, !=        - Equality checks (works with strings, numbers, booleans)\n");
            readme.append("  contains      - String contains check\n\n");

            readme.append("Examples:\n");
            readme.append("  • Health check: \"nbtEntityData\": \"Health\", \"nbtDataCondition\": \"<\", \"nbtDataValue\": \"10\"\n");
            readme.append("  • Level check: \"nbtEntityData\": \"ForgeCaps.mmorpg:entity_data.level\", \"nbtDataCondition\": \">\", \"nbtDataValue\": \"50\"\n");
            readme.append("  • Name check: \"nbtEntityData\": \"CustomName\", \"nbtDataCondition\": \"contains\", \"nbtDataValue\": \"Boss\"\n");
            readme.append("  • Equipment: \"nbtEntityData\": \"ArmorItems[3].id\", \"nbtDataCondition\": \"contains\", \"nbtDataValue\": \"diamond\"\n\n");

            readme.append("For comprehensive NBT examples, see:\n");
            readme.append("  • NBT_Entity_Data_Examples.example - Detailed reference\n");
            readme.append("  • Mobs/NBT_Zombie_Example.json - Working example\n\n");

            readme.append("DROP COUNTING LIMITATION\n");
            readme.append("------------------------\n");
            readme.append("IMPORTANT: \"enableDropCount\": true does NOT work in Normal Drops!\n\n");

            readme.append("Reason:\n");
            readme.append("  Normal drops are always active and would create massive data files over time.\n");
            readme.append("  Drop counting is designed for time-limited competitive events.\n\n");

            readme.append("For Drop Counting:\n");
            readme.append("  1. Use Event Drops/ directory instead\n");
            readme.append("  2. Create or use an existing event folder\n");
            readme.append("  3. Add \"enableDropCount\": true to configurations\n");
            readme.append("  4. Enable event: /lootdrops event <name> on\n");
            readme.append("  5. Enable counting: /lootdrops dropcount true\n\n");

            readme.append("NESTED FOLDER ORGANIZATION\n");
            readme.append("--------------------------\n");
            readme.append("The Mobs/ folder supports unlimited nested directories for organization.\n\n");

            readme.append("Suggested Organization Patterns:\n");
            readme.append("  By Mod:\n");
            readme.append("    • Mobs/thermal_expansion/machines.json\n");
            readme.append("    • Mobs/twilight_forest/bosses.json\n\n");

            readme.append("  By Entity Type:\n");
            readme.append("    • Mobs/undead/zombie_variants.json\n");
            readme.append("    • Mobs/arthropods/spider_types.json\n\n");

            readme.append("  By Biome:\n");
            readme.append("    • Mobs/nether/nether_mobs.json\n");
            readme.append("    • Mobs/end/end_mobs.json\n\n");

            readme.append("  By Difficulty:\n");
            readme.append("    • Mobs/boss_mobs/raid_bosses.json\n");
            readme.append("    • Mobs/elite_mobs/champions.json\n\n");

            readme.append("  By NBT Conditions:\n");
            readme.append("    • Mobs/nbt_conditions/mmorpg/high_level.json\n");
            readme.append("    • Mobs/nbt_conditions/health_based/low_health.json\n\n");

            readme.append("All .json files in any subfolder are automatically loaded.\n\n");

            readme.append("CONFIGURATION EXAMPLES\n");
            readme.append("----------------------\n\n");

            readme.append("Basic Drop:\n");
            readme.append("[\n");
            readme.append("  {\n");
            readme.append("    \"itemId\": \"minecraft:diamond\",\n");
            readme.append("    \"dropChance\": 5.0,\n");
            readme.append("    \"minAmount\": 1,\n");
            readme.append("    \"maxAmount\": 3\n");
            readme.append("  }\n");
            readme.append("]\n\n");

            readme.append("With NBT Condition:\n");
            readme.append("[\n");
            readme.append("  {\n");
            readme.append("    \"itemId\": \"minecraft:iron_sword\",\n");
            readme.append("    \"dropChance\": 10.0,\n");
            readme.append("    \"minAmount\": 1,\n");
            readme.append("    \"maxAmount\": 1,\n");
            readme.append("    \"nbtEntityData\": \"Health\",\n");
            readme.append("    \"nbtDataCondition\": \"<\",\n");
            readme.append("    \"nbtDataValue\": \"10\",\n");
            readme.append("    \"nbtEntityDrop\": \"minecraft:diamond_sword\",\n");
            readme.append("    \"nbtEntityDropChance\": 50.0,\n");
            readme.append("    \"nbtEntityDropMin\": 1,\n");
            readme.append("    \"nbtEntityDropMax\": 1\n");
            readme.append("  }\n");
            readme.append("]\n\n");

            readme.append("With Player Requirements:\n");
            readme.append("[\n");
            readme.append("  {\n");
            readme.append("    \"itemId\": \"minecraft:emerald\",\n");
            readme.append("    \"dropChance\": 15.0,\n");
            readme.append("    \"minAmount\": 1,\n");
            readme.append("    \"maxAmount\": 5,\n");
            readme.append("    \"requiredAdvancement\": \"minecraft:story/mine_diamond\",\n");
            readme.append("    \"requiredEffect\": \"minecraft:luck\"\n");
            readme.append("  }\n");
            readme.append("]\n\n");

            readme.append("COMMANDS\n");
            readme.append("--------\n");
            readme.append("  • /lootdrops reload  - Reload all configurations\n\n");

            readme.append("TROUBLESHOOTING\n");
            readme.append("---------------\n");
            readme.append("• Drops not working: Check console for JSON parsing errors\n");
            readme.append("• NBT conditions failing: Use /data get entity to verify NBT paths\n");
            readme.append("• File not loading: Ensure .json extension and valid JSON syntax\n\n");

            readme.append("For comprehensive examples, see:\n");
            readme.append("  • Global_Hostile_Drops.example - All available properties\n");
            readme.append("  • NBT_Entity_Data_Examples.example - NBT condition examples\n");
            readme.append("  • Mobs/ folder - Working configuration examples\n\n");

            readme.append("================================================================================\n");

            Files.write(readmePath, readme.toString().getBytes());
        }
    }
}