package net.poe.entitylootdrops.readme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Creates README files for fishing drop configurations.
 */
public class FishingReadmeCreator {
    
    /**
     * Creates the main README file for fishing drop configurations.
     */
    public static void createFishingDropsReadme(Path fishingDir) throws IOException {
        Path readmePath = fishingDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Fishing Drops Configuration Guide\n");
            readme.append("===============================\n\n");
            
            readme.append("The fishing drops system allows you to create custom rewards and execute commands when players fish.\n");
            readme.append("This system uses a folder structure with separate JSON files for different types of drops.\n\n");
            
            readme.append("Directory Structure:\n");
            readme.append("-------------------\n");
            readme.append("config/EntityLootDrops/Fishing/\n");
            readme.append("  ├── Global_Fishing_Rewards.json    # Global rewards for any fishing\n");
            readme.append("  ├── Conditional Drops/             # Weather/time-based drops\n");
            readme.append("  ├── Biome Drops/                   # Biome-specific drops\n");
            readme.append("  └── Dimension Drops/               # Dimension-specific drops\n\n");
            
            readme.append("File Types:\n");
            readme.append("----------\n");
            readme.append("1. Global_Fishing_Rewards.json: Simple rewards that can trigger on any fishing event\n");
            readme.append("2. Conditional Drops: Drops with weather, time, or enchantment requirements\n");
            readme.append("3. Biome Drops: Drops specific to certain biomes (ocean, river, etc.)\n");
            readme.append("4. Dimension Drops: Drops specific to dimensions (overworld, nether, end)\n\n");
            
            readme.append("Configuration Format:\n");
            readme.append("--------------------\n");
            readme.append("All fishing drop files use JSON arrays containing drop objects.\n\n");
            
            readme.append("Fishing Drop Properties:\n");
            readme.append("----------------------\n");
            readme.append("Required Properties:\n");
            readme.append("- name: Unique identifier for the fishing drop\n");
            readme.append("- chance: Probability to trigger (0.0 to 1.0, where 1.0 = 100%)\n\n");
            
            readme.append("Optional Condition Properties:\n");
            readme.append("- biome: Required biome (e.g., \"minecraft:ocean\", \"minecraft:river\")\n");
            readme.append("- dimension: Required dimension (e.g., \"minecraft:overworld\", \"minecraft:nether\")\n");
            readme.append("- weather: Required weather (\"clear\", \"rain\", \"thunder\")\n");
            readme.append("- timeOfDay: Required time (\"day\", \"night\")\n");
            readme.append("- minFishingLevel: Minimum player experience level required\n");
            readme.append("- requiresLure: Whether Lure enchantment is required (true/false)\n");
            readme.append("- requiresLuckOfSea: Whether Luck of the Sea enchantment is required (true/false)\n\n");
            
            readme.append("Reward Properties:\n");
            readme.append("- rewards: Array of items to give when the drop triggers\n");
            readme.append("- commands: Array of commands to execute when the drop triggers\n\n");
            
            readme.append("Fishing Reward Properties:\n");
            readme.append("------------------------\n");
            readme.append("- item: Item ID to give (e.g., \"minecraft:diamond\")\n");
            readme.append("- count: Fixed amount to give (use this OR min/maxCount)\n");
            readme.append("- minCount: Minimum amount to give (random between min and max)\n");
            readme.append("- maxCount: Maximum amount to give (random between min and max)\n");
            readme.append("- chance: Probability for this specific reward (0.0 to 1.0)\n");
            readme.append("- nbt: Optional NBT data for the item\n\n");
            
            readme.append("Command Placeholders:\n");
            readme.append("-------------------\n");
            readme.append("You can use these placeholders in commands:\n");
            readme.append("- {player}: Player's name\n");
            readme.append("- {uuid}: Player's UUID\n");
            readme.append("- {x}: Player's X coordinate\n");
            readme.append("- {y}: Player's Y coordinate\n");
            readme.append("- {z}: Player's Z coordinate\n\n");
            
            readme.append("Example Global Fishing Rewards:\n");
            readme.append("------------------------------\n");
            readme.append("```json\n");
            readme.append("[\n");
            readme.append("  {\n");
            readme.append("    \"item\": \"minecraft:experience_bottle\",\n");
            readme.append("    \"count\": 1,\n");
            readme.append("    \"chance\": 0.1\n");
            readme.append("  },\n");
            readme.append("  {\n");
            readme.append("    \"item\": \"minecraft:emerald\",\n");
            readme.append("    \"minCount\": 1,\n");
            readme.append("    \"maxCount\": 2,\n");
            readme.append("    \"chance\": 0.05\n");
            readme.append("  }\n");
            readme.append("]\n");
            readme.append("```\n\n");
            
            readme.append("Example Conditional Drop:\n");
            readme.append("-----------------------\n");
            readme.append("```json\n");
            readme.append("[\n");
            readme.append("  {\n");
            readme.append("    \"name\": \"night_fishing_bonus\",\n");
            readme.append("    \"timeOfDay\": \"night\",\n");
            readme.append("    \"chance\": 0.15,\n");
            readme.append("    \"rewards\": [\n");
            readme.append("      {\n");
            readme.append("        \"item\": \"minecraft:glowstone_dust\",\n");
            readme.append("        \"minCount\": 1,\n");
            readme.append("        \"maxCount\": 3,\n");
            readme.append("        \"chance\": 0.8\n");
            readme.append("      }\n");
            readme.append("    ],\n");
            readme.append("    \"commands\": [\n");
            readme.append("      \"tellraw {player} {\\\"text\\\":\\\"The night brings mysterious catches...\\\",\\\"color\\\":\\\"dark_purple\\\"}\"\n");
            readme.append("    ]\n");
            readme.append("  }\n");
            readme.append("]\n");
            readme.append("```\n\n");
            
            readme.append("Tips:\n");
            readme.append("----\n");
            readme.append("1. Use low chance values (0.01-0.1) for rare drops to maintain balance\n");
            readme.append("2. Combine multiple conditions to create very specific scenarios\n");
            readme.append("3. Global fishing rewards are good for common bonuses\n");
            readme.append("4. Test your NBT data in-game before adding to config\n");
            readme.append("5. Use commands to provide feedback to players about special catches\n");
            readme.append("6. Consider using minFishingLevel to gate powerful rewards\n");
            readme.append("7. Organize drops by type using the different folders\n\n");
            
            readme.append("Commands:\n");
            readme.append("--------\n");
            readme.append("(Note: These commands would need to be implemented in a FishingCommand class)\n");
            readme.append("/fishingdrops reload - Reload fishing configuration\n");
            readme.append("/fishingdrops list - List all configured fishing drops\n");
            readme.append("/fishingdrops test <dropName> - Test a specific fishing drop\n\n");
            
            readme.append("For detailed information about each drop type, see the README files in the subdirectories.\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
    
    /**
     * Creates the README file for conditional fishing drops.
     */
    public static void createConditionalFishingReadme(Path conditionalDir) throws IOException {
        if (!Files.exists(conditionalDir)) return;
        
        Path readmePath = conditionalDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Conditional Fishing Drops\n");
            readme.append("========================\n\n");
            
            readme.append("This directory contains fishing drops that trigger based on specific conditions\n");
            readme.append("like weather, time of day, enchantments, or player level.\n\n");
            
            readme.append("Supported Conditions:\n");
            readme.append("-------------------\n");
            readme.append("- weather: \"clear\", \"rain\", \"thunder\"\n");
            readme.append("- timeOfDay: \"day\", \"night\"\n");
            readme.append("- minFishingLevel: Minimum player experience level\n");
            readme.append("- requiresLure: Requires Lure enchantment on fishing rod\n");
            readme.append("- requiresLuckOfSea: Requires Luck of the Sea enchantment\n\n");
            
            readme.append("Example Conditions:\n");
            readme.append("-----------------\n");
            readme.append("1. Night fishing with special rewards\n");
            readme.append("2. Rain fishing for oceanic items\n");
            readme.append("3. Thunder storms for rare catches\n");
            readme.append("4. High-level fishing for exclusive rewards\n");
            readme.append("5. Enchanted rod requirements for magical catches\n\n");
            
            readme.append("File Format:\n");
            readme.append("-----------\n");
            readme.append("Each JSON file should contain an array of fishing drop objects.\n");
            readme.append("You can create multiple files to organize different types of conditions.\n\n");
            
            readme.append("Suggested File Names:\n");
            readme.append("-------------------\n");
            readme.append("- Weather_Time_Drops.json\n");
            readme.append("- Enchantment_Drops.json\n");
            readme.append("- Level_Gated_Drops.json\n");
            readme.append("- Special_Conditions.json\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
    
    /**
     * Creates the README file for biome-specific fishing drops.
     */
    public static void createBiomeFishingReadme(Path biomeDir) throws IOException {
        if (!Files.exists(biomeDir)) return;
        
        Path readmePath = biomeDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Biome-Specific Fishing Drops\n");
            readme.append("===========================\n\n");
            
            readme.append("This directory contains fishing drops that only trigger in specific biomes.\n");
            readme.append("Each file can contain drops for one or multiple related biomes.\n\n");
            
            readme.append("Common Fishing Biomes:\n");
            readme.append("--------------------\n");
            readme.append("Ocean Biomes:\n");
            readme.append("- minecraft:ocean\n");
            readme.append("- minecraft:deep_ocean\n");
            readme.append("- minecraft:warm_ocean\n");
            readme.append("- minecraft:lukewarm_ocean\n");
            readme.append("- minecraft:cold_ocean\n");
            readme.append("- minecraft:frozen_ocean\n");
            readme.append("- minecraft:deep_warm_ocean\n");
            readme.append("- minecraft:deep_lukewarm_ocean\n");
            readme.append("- minecraft:deep_cold_ocean\n");
            readme.append("- minecraft:deep_frozen_ocean\n\n");
            
            readme.append("River Biomes:\n");
            readme.append("- minecraft:river\n");
            readme.append("- minecraft:frozen_river\n\n");
            
            readme.append("Swamp Biomes:\n");
            readme.append("- minecraft:swamp\n");
            readme.append("- minecraft:mangrove_swamp\n\n");
            
            readme.append("Other Water Biomes:\n");
            readme.append("- minecraft:beach\n");
            readme.append("- minecraft:stony_shore\n");
            readme.append("- minecraft:mushroom_fields\n\n");
            
            readme.append("Suggested File Organization:\n");
            readme.append("-------------------------\n");
            readme.append("- Ocean_Drops.json - All ocean biome drops\n");
            readme.append("- River_Drops.json - River and stream drops\n");
            readme.append("- Swamp_Drops.json - Swamp-specific drops\n");
            readme.append("- Cold_Water_Drops.json - Frozen/cold biome drops\n");
            readme.append("- Warm_Water_Drops.json - Tropical biome drops\n\n");
            
            readme.append("Example Biome Drop Configuration:\n");
            readme.append("--------------------------------\n");
            readme.append("```json\n");
            readme.append("[\n");
            readme.append("  {\n");
            readme.append("    \"name\": \"ocean_treasure\",\n");
            readme.append("    \"biome\": \"minecraft:deep_ocean\",\n");
            readme.append("    \"chance\": 0.05,\n");
            readme.append("    \"rewards\": [\n");
            readme.append("      {\n");
            readme.append("        \"item\": \"minecraft:prismarine_shard\",\n");
            readme.append("        \"minCount\": 1,\n");
            readme.append("        \"maxCount\": 3,\n");
            readme.append("        \"chance\": 0.7\n");
            readme.append("      },\n");
            readme.append("      {\n");
            readme.append("        \"item\": \"minecraft:heart_of_the_sea\",\n");
            readme.append("        \"count\": 1,\n");
            readme.append("        \"chance\": 0.01\n");
            readme.append("      }\n");
            readme.append("    ],\n");
            readme.append("    \"commands\": [\n");
            readme.append("      \"tellraw {player} {\\\"text\\\":\\\"The deep ocean yields its treasures...\\\",\\\"color\\\":\\\"aqua\\\"}\"\n");
            readme.append("    ]\n");
            readme.append("  }\n");
            readme.append("]\n");
            readme.append("```\n\n");
            
            readme.append("Tips for Biome-Specific Drops:\n");
            readme.append("-----------------------------\n");
            readme.append("1. Match drops to biome themes (coral in warm oceans, ice in frozen biomes)\n");
            readme.append("2. Use different rarities for different biomes\n");
            readme.append("3. Consider combining biome conditions with weather/time conditions\n");
            readme.append("4. Deep ocean biomes can have rarer drops than regular oceans\n");
            readme.append("5. Rivers might have different drops than oceans\n");
            readme.append("6. Swamps could have unique magical or mysterious drops\n");
            readme.append("7. Test biome detection in-game to ensure drops trigger correctly\n\n");
            
            readme.append("Biome Detection:\n");
            readme.append("---------------\n");
            readme.append("The system detects the biome where the fishing bobber lands, not where the player stands.\n");
            readme.append("This allows for fishing across biome boundaries with appropriate drops.\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
    
    /**
     * Creates the README file for dimension-specific fishing drops.
     */
    public static void createDimensionFishingReadme(Path dimensionDir) throws IOException {
        if (!Files.exists(dimensionDir)) return;
        
        Path readmePath = dimensionDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Dimension-Specific Fishing Drops\n");
            readme.append("===============================\n\n");
            
            readme.append("This directory contains fishing drops that only trigger in specific dimensions.\n");
            readme.append("Each file can contain drops for one or multiple related dimensions.\n\n");
            
            readme.append("Standard Minecraft Dimensions:\n");
            readme.append("----------------------------\n");
            readme.append("- minecraft:overworld - The main world\n");
            readme.append("- minecraft:the_nether - The Nether dimension\n");
            readme.append("- minecraft:the_end - The End dimension\n\n");
            
            readme.append("Modded Dimensions:\n");
            readme.append("----------------\n");
            readme.append("You can also use dimensions from other mods by using their full dimension ID.\n");
            readme.append("Examples might include:\n");
            readme.append("- twilightforest:twilight_forest\n");
            readme.append("- aether:the_aether\n");
            readme.append("- undergarden:undergarden\n\n");
            
            readme.append("Suggested File Organization:\n");
            readme.append("-------------------------\n");
            readme.append("- Overworld_Drops.json - Standard overworld fishing\n");
            readme.append("- Nether_Drops.json - Nether lava fishing (if possible)\n");
            readme.append("- End_Drops.json - End dimension fishing\n");
            readme.append("- Modded_Dimension_Drops.json - Other mod dimensions\n\n");
            
            readme.append("Example Dimension Drop Configuration:\n");
            readme.append("-----------------------------------\n");
            readme.append("```json\n");
            readme.append("[\n");
            readme.append("  {\n");
            readme.append("    \"name\": \"nether_fishing\",\n");
            readme.append("    \"dimension\": \"minecraft:the_nether\",\n");
            readme.append("    \"chance\": 0.1,\n");
            readme.append("    \"rewards\": [\n");
            readme.append("      {\n");
            readme.append("        \"item\": \"minecraft:magma_cream\",\n");
            readme.append("        \"minCount\": 1,\n");
            readme.append("        \"maxCount\": 2,\n");
            readme.append("        \"chance\": 0.6\n");
            readme.append("      },\n");
            readme.append("      {\n");
            readme.append("        \"item\": \"minecraft:blaze_powder\",\n");
            readme.append("        \"count\": 1,\n");
            readme.append("        \"chance\": 0.3\n");
            readme.append("      },\n");
            readme.append("      {\n");
            readme.append("        \"item\": \"minecraft:ghast_tear\",\n");
            readme.append("        \"count\": 1,\n");
            readme.append("        \"chance\": 0.05\n");
            readme.append("      }\n");
            readme.append("    ],\n");
            readme.append("    \"commands\": [\n");
            readme.append("      \"tellraw {player} {\\\"text\\\":\\\"The lava yields fiery treasures...\\\",\\\"color\\\":\\\"red\\\"}\"\n");
            readme.append("    ]\n");
            readme.append("  },\n");
            readme.append("  {\n");
            readme.append("    \"name\": \"end_fishing\",\n");
            readme.append("    \"dimension\": \"minecraft:the_end\",\n");
            readme.append("    \"chance\": 0.08,\n");
            readme.append("    \"rewards\": [\n");
            readme.append("      {\n");
            readme.append("        \"item\": \"minecraft:ender_pearl\",\n");
            readme.append("        \"count\": 1,\n");
            readme.append("        \"chance\": 0.5\n");
            readme.append("      },\n");
            readme.append("      {\n");
            readme.append("        \"item\": \"minecraft:chorus_fruit\",\n");
            readme.append("        \"minCount\": 1,\n");
            readme.append("        \"maxCount\": 3,\n");
            readme.append("        \"chance\": 0.4\n");
            readme.append("      },\n");
            readme.append("      {\n");
            readme.append("        \"item\": \"minecraft:dragon_breath\",\n");
            readme.append("        \"count\": 1,\n");
            readme.append("        \"chance\": 0.02\n");
            readme.append("      }\n");
            readme.append("    ],\n");
            readme.append("    \"commands\": [\n");
            readme.append("      \"tellraw {player} {\\\"text\\\":\\\"The void whispers secrets...\\\",\\\"color\\\":\\\"dark_purple\\\"}\"\n");
            readme.append("    ]\n");
            readme.append("  }\n");
            readme.append("]\n");
            readme.append("```\n\n");
            
            readme.append("Tips for Dimension-Specific Drops:\n");
            readme.append("--------------------------------\n");
            readme.append("1. Match drops to dimension themes (fire items in Nether, void items in End)\n");
            readme.append("2. Consider if fishing is even possible in the dimension (Nether has lava)\n");
            readme.append("3. Use higher rarity for dimension-specific materials\n");
            readme.append("4. Combine with other conditions for very specific scenarios\n");
            readme.append("5. Test fishing mechanics in each dimension\n");
            readme.append("6. Consider dimension-specific enchantments or tools\n");
            readme.append("7. Use appropriate command feedback for each dimension's atmosphere\n\n");
            
            readme.append("Special Considerations:\n");
            readme.append("---------------------\n");
            readme.append("- Nether: Fishing in lava may require special mechanics\n");
            readme.append("- End: Limited water sources, may need custom water placement\n");
            readme.append("- Modded Dimensions: Check if fishing mechanics work as expected\n");
            readme.append("- Dimension Detection: System detects where the fishing occurs, not player location\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
}
