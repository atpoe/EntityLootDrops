package net.poe.entitylootdrops;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles the creation and management of README files for the EntityLootDrops mod.
 * This centralizes all documentation to prevent conflicts between configuration classes.
 */
public class ReadmeManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String CONFIG_DIR = "config/EntityLootDrops";
    
    /**
     * Creates all README files for the mod.
     * This should be called during mod initialization.
     */
    public static void createAllReadmeFiles() {
        try {
            // Create main config directory if it doesn't exist
            Path configDir = Paths.get(CONFIG_DIR);
            Files.createDirectories(configDir);
            
            // Create main README
            createMainReadme(configDir);
            
            // Create entity loot README
            Path entitiesDir = configDir.resolve("Entities");
            if (Files.exists(entitiesDir)) {
                createEntityLootReadme(entitiesDir);
            }
            
            // Create block drops README
            Path blocksDir = configDir.resolve("Blocks");
            if (Files.exists(blocksDir)) {
                createBlockDropsReadme(blocksDir);
                
                // Create normal drops README
                Path normalDropsDir = blocksDir.resolve("Normal Drops");
                if (Files.exists(normalDropsDir)) {
                    createNormalDropsReadme(normalDropsDir);
                }
                
                // Create event drops README
                Path eventDropsDir = blocksDir.resolve("Event Drops");
                if (Files.exists(eventDropsDir)) {
                    createEventDropsReadme(eventDropsDir);
                    
                    // Create README for each event type
                    Files.list(eventDropsDir)
                        .filter(Files::isDirectory)
                        .forEach(eventDir -> {
                            try {
                                String eventName = eventDir.getFileName().toString();
                                createEventTypeReadme(eventDir, eventName);
                            } catch (IOException e) {
                                LOGGER.error("Failed to create README for event: {}", eventDir, e);
                            }
                        });
                }
            }
            
            // Create recipes README
            Path recipesDir = configDir.resolve("Recipes");
            if (Files.exists(recipesDir)) {
                createRecipesReadme(recipesDir);
                
                // Create README for each recipe type
                createShapedRecipesReadme(recipesDir.resolve("Shaped"));
                createShapelessRecipesReadme(recipesDir.resolve("Shapeless"));
                createBrewingRecipesReadme(recipesDir.resolve("Brewing"));
                createFurnaceRecipesReadme(recipesDir.resolve("Furnace"));
                createSmithingRecipesReadme(recipesDir.resolve("Smithing"));
            }
            
            // Create fishing drops README
            Path fishingDir = configDir.resolve("Fishing");
            if (Files.exists(fishingDir)) {
                createFishingDropsReadme(fishingDir);
                
                // Create README for fishing subdirectories
                createConditionalFishingReadme(fishingDir.resolve("Conditional Drops"));
                createBiomeFishingReadme(fishingDir.resolve("Biome Drops"));
                createDimensionFishingReadme(fishingDir.resolve("Dimension Drops"));
            }
            
            LOGGER.info("Created all README files");
        } catch (IOException e) {
            LOGGER.error("Failed to create README files", e);
        }
    }
    
    /**
     * Creates the main README file for the mod.
     */
    private static void createMainReadme(Path configDir) throws IOException {
        Path readmePath = configDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("EntityLootDrops Mod Configuration Guide\n");
            readme.append("===================================\n\n");
            
            readme.append("This mod allows you to customize entity drops, block drops, fishing drops, and crafting recipes.\n\n");
            
            readme.append("Directory Structure:\n");
            readme.append("-------------------\n");
            readme.append("config/EntityLootDrops/\n");
            readme.append("  ├── Entities/            # Entity loot drop configurations\n");
            readme.append("  ├── Blocks/              # Block drop configurations\n");
            readme.append("  ├── Recipes/             # Custom recipe configurations\n");
            readme.append("  │   ├── Shaped/          # Shaped crafting recipes\n");
            readme.append("  │   ├── Shapeless/       # Shapeless crafting recipes\n");
            readme.append("  │   ├── Brewing/         # Brewing stand recipes\n");
            readme.append("  │   ├── Furnace/         # Furnace/smelting recipes\n");
            readme.append("  │   └── Smithing/        # Smithing table recipes\n");
            readme.append("  └── Fishing/             # Fishing drop configurations\n");
            readme.append("      ├── Global_Fishing_Rewards.json  # Global fishing rewards\n");
            readme.append("      ├── Conditional Drops/           # Weather/time-based drops\n");
            readme.append("      ├── Biome Drops/                 # Biome-specific drops\n");
            readme.append("      └── Dimension Drops/             # Dimension-specific drops\n\n");
            
            readme.append("Each directory contains its own README file with detailed instructions.\n\n");
            
            readme.append("Commands:\n");
            readme.append("--------\n");
            readme.append("/lootdrops - Entity loot drop commands\n");
            readme.append("/blockdrops - Block drop commands\n");
            readme.append("/recipes - Recipe commands\n");
            readme.append("/fishingdrops - Fishing drop commands\n\n");
            
            readme.append("Features:\n");
            readme.append("--------\n");
            readme.append("- Custom entity loot drops with NBT support\n");
            readme.append("- Custom block drops with tool requirements\n");
            readme.append("- Custom fishing drops with environmental conditions\n");
            readme.append("- Custom crafting recipes (shaped and shapeless)\n");
            readme.append("- Custom brewing recipes for potions\n");
            readme.append("- Custom furnace/smelting recipes\n");
            readme.append("- Custom smithing table recipes\n");
            readme.append("- Command execution when items are crafted/dropped/fished\n");
            readme.append("- Event-based drops (seasonal events, custom events)\n");
            readme.append("- Biome, dimension, weather, and time-based fishing conditions\n\n");
            
            readme.append("For more information, see the README files in each subdirectory.\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
    
    /**
     * Creates the main README file for fishing drop configurations.
     */
    private static void createFishingDropsReadme(Path fishingDir) throws IOException {
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
    private static void createConditionalFishingReadme(Path conditionalDir) throws IOException {
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
    private static void createBiomeFishingReadme(Path biomeDir) throws IOException {
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
            readme.append("- minecraft:frozen_ocean\n\n");
            
            readme.append("River Biomes:\n");
            readme.append("- minecraft:river\n");
            readme.append("- minecraft:frozen_river\n\n");
            
            readme.append("Swamp Biomes:\n");
            readme.append("- minecraft:swamp\n");
            readme.append("- minecraft:mangrove_swamp\n\n");
            
            readme.append("Other Water Biomes:\n");
            readme.append("- minecraft:beach\n");
            readme.append("- minecraft:mushroom_fields (if fishing in water)\n\n");
            
            readme.append("Suggested File Organization:\n");
            readme.append("-------------------------\n");
            readme.append("- Ocean_Drops.json (all ocean variants)\n");
            readme.append("- River_Drops.json (river and frozen river)\n");
            readme.append("- Swamp_Drops.json (swamp biomes)\n");
            readme.append("- Tropical_Drops.json (warm ocean, beach)\n");
            readme.append("- Arctic_Drops.json (frozen ocean, frozen river)\n\n");
            
            readme.append("Example Biome-Specific Ideas:\n");
            readme.append("---------------------------\n");
            readme.append("- Deep ocean: Rare treasures, pearls, deep sea items\n");
            readme.append("- Swamp: Lily pads, slime balls, witch-related items\n");
            readme.append("- Frozen biomes: Ice, packed ice, polar bear drops\n");
            readme.append("- Warm ocean: Tropical fish, coral, warm climate items\n");
            readme.append("- Rivers: Freshwater fish, river stones, clay\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
    
    /**
     * Creates the README file for dimension-specific fishing drops.
     */
    private static void createDimensionFishingReadme(Path dimensionDir) throws IOException {
        if (!Files.exists(dimensionDir)) return;
        
        Path readmePath = dimensionDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Dimension-Specific Fishing Drops\n");
            readme.append("===============================\n\n");
            
            readme.append("This directory contains fishing drops that only trigger in specific dimensions.\n");
            readme.append("Note: Fishing in the Nether and End requires special setups or mods.\n\n");
            
            readme.append("Available Dimensions:\n");
            readme.append("-------------------\n");
            readme.append("- minecraft:overworld (normal world)\n");
            readme.append("- minecraft:the_nether (Nether dimension)\n");
            readme.append("- minecraft:the_end (End dimension)\n");
            readme.append("- [modded dimensions] (if using dimension mods)\n\n");
            
            readme.append("Suggested File Organization:\n");
            readme.append("-------------------------\n");
            readme.append("- Overworld_Drops.json (overworld-specific fishing)\n");
            readme.append("- Nether_Drops.json (nether fishing - very rare/special)\n");
            readme.append("- End_Drops.json (end fishing - extremely rare)\n");
            readme.append("- [ModName]_Drops.json (for modded dimensions)\n\n");
            
            readme.append("Dimension-Specific Ideas:\n");
            readme.append("-----------------------\n");
            readme.append("Overworld:\n");
            readme.append("- Standard fishing with bonus rewards\n");
            readme.append("- Overworld-exclusive materials\n");
            readme.append("- Peaceful dimension bonuses\n\n");
            
            readme.append("Nether (if fishing is possible):\n");
            readme.append("- Extremely rare and valuable rewards\n");
            readme.append("- Fire-resistant items\n");
            readme.append("- Nether-themed materials\n");
            readme.append("- High risk, high reward concept\n\n");
            
            readme.append("End (if fishing is possible):\n");
            readme.append("- Endgame materials\n");
            readme.append("- Void-themed items\n");
            readme.append("- Dragon-related rewards\n");
            readme.append("- Ultimate rare catches\n\n");
            
            readme.append("Special Considerations:\n");
            readme.append("---------------------\n");
            readme.append("- Nether and End fishing may require water placement mods\n");
            readme.append("- Consider very low chances for dimension-specific drops\n");
            readme.append("- Use high minFishingLevel requirements for powerful rewards\n");
            readme.append("- Add special commands/messages for impossible fishing scenarios\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
    
    /**
     * Creates the README file for entity loot configurations.
     */
    private static void createEntityLootReadme(Path entitiesDir) throws IOException {
        Path readmePath = entitiesDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Entity Loot Drop Configuration Guide\n");
            readme.append("================================\n\n");
            
            readme.append("This system allows you to configure custom drops when entities are killed.\n\n");
            
            readme.append("Directory Structure:\n");
            readme.append("-------------------\n");
            readme.append("config/EntityLootDrops/Entities/\n");
            readme.append("  ├── Normal Drops/           # Always active drops\n");
            readme.append("  │   ├── Global_Drops.json   # Drops for all entities\n");
            readme.append("  │   ├── Hostile_Drops.json  # Drops for hostile mobs\n");
            readme.append("  │   └── Entity Types/       # Entity-specific drops\n");
            readme.append("  └── Event Drops/            # Event-specific drops\n");
            readme.append("      ├── Winter/             # Winter event drops\n");
            readme.append("      ├── Summer/             # Summer event drops\n");
            readme.append("      └── [Custom Event]/     # Your custom events\n\n");
            
            readme.append("Configuration Format:\n");
            readme.append("-------------------\n");
            readme.append("Entity drops use JSON format with these properties:\n\n");
            
            readme.append("Basic Properties:\n");
            readme.append("- entityId: The Minecraft entity ID (e.g., \"minecraft:zombie\") - only for entity-specific drops\n");
            readme.append("- itemId: The item to drop (e.g., \"minecraft:diamond\")\n");
            readme.append("- dropChance: Percentage chance to drop (0-100)\n");
            readme.append("- minAmount: Minimum amount to drop\n");
            readme.append("- maxAmount: Maximum amount to drop\n");
            readme.append("- requirePlayerKill: If true, only drops when killed by a player\n\n");
            
            readme.append("Advanced Features:\n");
            readme.append("- nbtData: Custom NBT data for the dropped item\n");
            readme.append("- command: Command to execute when the entity dies\n");
            readme.append("- commandChance: Percentage chance to execute the command\n");
            readme.append("- minLevel: Minimum entity level required (if using a level mod)\n");
            readme.append("- maxLevel: Maximum entity level required (if using a level mod)\n\n");
            
            readme.append("Commands:\n");
            readme.append("--------\n");
            readme.append("/lootdrops event <eventName> <true|false> - Toggle an entity event\n");
            readme.append("/lootdrops event dropchance <true|false> - Toggle double drop chance\n");
            readme.append("/lootdrops event doubledrops <true|false> - Toggle double drops\n");
            readme.append("/lootdrops active_events - List active entity events\n");
            readme.append("/lootdrops listall - List all available entity events\n");
            readme.append("/lootdrops reload - Reload entity configuration\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
    
    /**
     * Creates the README file for block drop configurations.
     */
    private static void createBlockDropsReadme(Path blocksDir) throws IOException {
        Path readmePath = blocksDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Block Drops Configuration Guide\n");
            readme.append("============================\n\n");
            
            readme.append("This system allows you to configure custom drops when blocks are broken.\n\n");
            
            readme.append("Directory Structure:\n");
            readme.append("-------------------\n");
            readme.append("config/EntityLootDrops/Blocks/\n");
            readme.append("  ├── Normal Drops/           # Always active drops\n");
            readme.append("  │   ├── Global_Block_Drops.json  # Drops for all blocks\n");
            readme.append("  │   └── Block Types/        # Block-specific drops\n");
            readme.append("  └── Event Drops/            # Event-specific drops\n");
            readme.append("      ├── Winter/             # Winter event drops\n");
            readme.append("      ├── Summer/             # Summer event drops\n");
            readme.append("      ├── Easter/             # Easter event drops\n");
            readme.append("      ├── Halloween/          # Halloween event drops\n");
            readme.append("      └── [Custom Event]/     # Your custom events\n\n");
            
            readme.append("For detailed configuration instructions, see the README files in each directory.\n\n");
            
            readme.append("Commands:\n");
            readme.append("--------\n");
            readme.append("/blockdrops event <eventName> <true|false> - Toggle a block event\n");
            readme.append("/blockdrops event dropchance <true|false> - Toggle double drop chance\n");
            readme.append("/blockdrops event doubledrops <true|false> - Toggle double drops\n");
            readme.append("/blockdrops active_events - List active block events\n");
            readme.append("/blockdrops listall - List all available block events\n");
            readme.append("/blockdrops reload - Reload block configuration\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
    
    /**
     * Creates the README file for normal drops directory.
     */
    private static void createNormalDropsReadme(Path normalDropsDir) throws IOException {
        Path readmePath = normalDropsDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Normal Drops Configuration\n");
            readme.append("========================\n\n");
            
            readme.append("Configuration Format:\n");
            readme.append("-------------------\n");
            readme.append("Block drops use JSON format with these properties:\n\n");
            
            readme.append("Basic Properties:\n");
            readme.append("- blockId: The Minecraft block ID (e.g., \"minecraft:stone\") - only for block-specific drops\n");
            readme.append("- itemId: The item to drop (e.g., \"minecraft:diamond\")\n");
            readme.append("- dropChance: Percentage chance to drop (0-100)\n");
            readme.append("- minAmount: Minimum amount to drop\n");
            readme.append("- maxAmount: Maximum amount to drop\n");
            readme.append("- requirePlayerBreak: If true, only drops when broken by a player\n");
            readme.append("- allowDefaultDrops: If true, keeps vanilla drops\n\n");
            
            readme.append("Tool Requirements:\n");
            readme.append("- requiredTool: Specific tool required (e.g., \"minecraft:diamond_pickaxe\")\n");
            readme.append("- requiredToolTier: Required tool tier (e.g., \"diamond\")\n");
            readme.append("- requiredToolLevel: Required tool level (e.g., 3 for diamond)\n");
            readme.append("- requiredEnchantment: Required enchantment (e.g., \"minecraft:fortune\")\n");
            readme.append("- requiredEnchantLevel: Required enchantment level\n\n");
            
            readme.append("Advanced Features:\n");
            readme.append("- nbtData: Custom NBT data for the dropped item\n");
            readme.append("- command: Command to execute when the block breaks\n");
            readme.append("- commandChance: Percentage chance to execute the command\n");
            readme.append("- allowModIDs: List of mod IDs allowed to drop items when allowDefaultDrops is false\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
    
    /**
     * Creates the README file for event drops directory.
     */
    private static void createEventDropsReadme(Path eventsDir) throws IOException {
        Path readmePath = eventsDir.resolve("README_CUSTOM_EVENTS.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Custom Block Events Guide\n");
            readme.append("======================\n\n");
            
            readme.append("You can create your own custom block events by following these steps:\n\n");
            
            readme.append("1. Create a new folder inside the 'Event Drops' directory\n");
            readme.append("   Example: config/EntityLootDrops/Blocks/Event Drops/MyCustomEvent/\n\n");
            
            readme.append("2. Inside your custom event folder, create:\n");
            readme.append("   - A Global_Block_Drops.json file for drops that apply to all blocks\n");
            readme.append("   - A Block Types/ folder with block-specific drop files\n\n");
            
            readme.append("3. Use the command to activate your event:\n");
            readme.append("   /blockdrops event MyCustomEvent true\n\n");
            
            readme.append("4. You can toggle your event on and off at any time:\n");
            readme.append("   /blockdrops event MyCustomEvent false\n\n");
            
            readme.append("5. Check active events with:\n");
            readme.append("   /blockdrops active_events\n\n");
            
            readme.append("6. See all available events with:\n");
            readme.append("   /blockdrops listall\n\n");
            
            readme.append("Note: Event names are case-insensitive in commands, but the folder name will be displayed as created.\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
    
    /**
     * Creates the README file for a specific event type.
     */
    private static void createEventTypeReadme(Path eventDir, String eventType) throws IOException {
        Path readmePath = eventDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append(eventType + " Event Configuration\n");
            readme.append("=".repeat(eventType.length() + 19) + "\n\n");
            
            readme.append("This directory contains drops that only occur during the " + eventType + " event.\n\n");
            
            readme.append("To activate this event, use the command:\n");
            readme.append("/blockdrops event " + eventType + " true\n\n");
            
            readme.append("To deactivate:\n");
            readme.append("/blockdrops event " + eventType + " false\n\n");
            
            readme.append("Configuration follows the same format as normal drops.\n");
            readme.append("See config/EntityLootDrops/Blocks/Normal Drops/README.txt for full property documentation.\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
    
    /**
     * Creates the main README file for recipe configurations.
     */
    private static void createRecipesReadme(Path recipesDir) throws IOException {
        Path readmePath = recipesDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Custom Recipes Configuration Guide\n");
            readme.append("===============================\n\n");
            
            readme.append("This system allows you to create custom recipes with NBT data support and command execution.\n\n");
            
            readme.append("Directory Structure:\n");
            readme.append("-------------------\n");
            readme.append("config/EntityLootDrops/Recipes/\n");
            readme.append("  ├── Shaped/               # Shaped crafting table recipes\n");
            readme.append("  ├── Shapeless/            # Shapeless crafting table recipes\n");
            readme.append("  ├── Brewing/              # Brewing stand recipes\n");
            readme.append("  ├── Furnace/              # Furnace/smelting recipes\n");
            readme.append("  └── Smithing/             # Smithing table recipes\n\n");
            
            readme.append("Recipe Types:\n");
            readme.append("------------\n");
            readme.append("1. Shaped Recipes: Traditional crafting grid patterns (like vanilla recipes)\n");
            readme.append("2. Shapeless Recipes: Ingredients can be placed anywhere in the crafting grid\n");
            readme.append("3. Brewing Recipes: Custom potion brewing in brewing stands\n");
            readme.append("4. Furnace Recipes: Custom smelting recipes for furnaces\n");
            readme.append("5. Smithing Recipes: Custom upgrade recipes for smithing tables\n\n");
            
            readme.append("Common Features:\n");
            readme.append("---------------\n");
            readme.append("All recipe types support:\n");
            readme.append("- Custom NBT data for output items (enchantments, custom names, etc.)\n");
            readme.append("- Command execution when the item is crafted/brewed/smelted\n");
            readme.append("- Placeholder support in commands: {player} and {uuid}\n");
            readme.append("- Comments for documentation\n\n");
            
            readme.append("Commands:\n");
            readme.append("--------\n");
            readme.append("/recipes reload - Reload all recipe configurations\n\n");
            
            readme.append("Getting Started:\n");
            readme.append("---------------\n");
            readme.append("1. Check the example files in each recipe type directory\n");
            readme.append("2. Copy and modify the examples to create your own recipes\n");
            readme.append("3. Use /recipes reload to apply changes without restarting\n");
            readme.append("4. Each recipe type directory has its own detailed README\n\n");
            
            readme.append("For detailed configuration instructions for each recipe type,\n");
            readme.append("see the README files in each subdirectory.\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
    
    /**
     * Creates the README file for shaped recipe configurations.
     */
    private static void createShapedRecipesReadme(Path shapedDir) throws IOException {
        if (!Files.exists(shapedDir)) return;
        
        Path readmePath = shapedDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Shaped Recipes Configuration\n");
            readme.append("==========================\n\n");
            
            readme.append("Shaped recipes use a pattern-based system similar to vanilla Minecraft recipes.\n\n");
            
            readme.append("Required Properties:\n");
            readme.append("------------------\n");
            readme.append("- name: Unique identifier for the recipe\n");
            readme.append("- type: Must be \"shaped\"\n");
            readme.append("- outputItem: The item ID to create (e.g., \"minecraft:diamond_sword\")\n");
            readme.append("- pattern: Array of strings representing the crafting grid\n");
            readme.append("- key: Mapping of pattern characters to item IDs\n\n");
            
            readme.append("Optional Properties:\n");
            readme.append("------------------\n");
            readme.append("- outputCount: Number of items to output (default: 1)\n");
            readme.append("- outputNbt: NBT data for the output item\n");
            readme.append("- group: Recipe group for the recipe book\n");
            readme.append("- craftCommands: Commands to execute when crafted\n");
            readme.append("- _comment: Documentation comment\n\n");
            
            readme.append("Pattern Format:\n");
            readme.append("--------------\n");
            readme.append("The pattern is a 3x3 grid represented as strings:\n");
            readme.append("- Each string represents a row\n");
            readme.append("- Each character represents a slot\n");
            readme.append("- Space character represents an empty slot\n");
            readme.append("- Maximum 3 rows, maximum 3 characters per row\n\n");
            
            readme.append("Example:\n");
            readme.append("-------\n");
            readme.append("```json\n");
            readme.append("{\n");
            readme.append("  \"name\": \"custom_diamond_sword\",\n");
            readme.append("  \"type\": \"shaped\",\n");
            readme.append("  \"outputItem\": \"minecraft:diamond_sword\",\n");
            readme.append("  \"outputCount\": 1,\n");
            readme.append("  \"pattern\": [\n");
            readme.append("    \" D \",\n");
            readme.append("    \" D \",\n");
            readme.append("    \" S \"\n");
            readme.append("  ],\n");
            readme.append("  \"key\": {\n");
            readme.append("    \"D\": \"minecraft:diamond\",\n");
            readme.append("    \"S\": \"minecraft:stick\"\n");
            readme.append("  },\n");
            readme.append("  \"outputNbt\": \"{Enchantments:[{id:\\\"minecraft:sharpness\\\",lvl:5}],display:{Name:\\\"{\\\\\\\"text\\\\\\\":\\\\\\\"Super Sword\\\\\\\",\\\\\\\"color\\\\\\\":\\\\\\\"gold\\\\\\\"}\\\"}}\",\n");
            readme.append("  \"craftCommands\": [\n");
            readme.append("    \"tellraw {player} {\\\"text\\\":\\\"You crafted a Super Sword!\\\",\\\"color\\\":\\\"gold\\\"}\",\n");
            readme.append("    \"playsound minecraft:entity.player.levelup player {player} ~ ~ ~ 1 0.5\"\n");
            readme.append("  ],\n");
            readme.append("  \"_comment\": \"Creates an enchanted diamond sword with custom name\"\n");
            readme.append("}\n");
            readme.append("```\n\n");
            
            readme.append("Pattern Tips:\n");
            readme.append("-----------\n");
            readme.append("1. You can use any character in the pattern (A-Z, a-z, 0-9, symbols)\n");
            readme.append("2. The same character must map to the same item in the key\n");
            readme.append("3. Empty spaces in the pattern represent empty crafting slots\n");
            readme.append("4. Patterns can be smaller than 3x3 (e.g., 2x2 or 1x3)\n");
            readme.append("5. The recipe will work in any position on the crafting grid\n\n");
            
            readme.append("NBT Data:\n");
            readme.append("--------\n");
            readme.append("You can add custom NBT data to output items:\n");
            readme.append("- Enchantments: {Enchantments:[{id:\\\"minecraft:sharpness\\\",lvl:5}]}\n");
            readme.append("- Custom names: {display:{Name:\\\"{\\\\\\\"text\\\\\\\":\\\\\\\"Custom Name\\\\\\\",\\\\\\\"color\\\\\\\":\\\\\\\"gold\\\\\\\"}\\\"}}\n");
            readme.append("- Lore: {display:{Lore:[\\\"{\\\\\\\"text\\\\\\\":\\\\\\\"Line 1\\\\\\\"}\\\",\\\"{\\\\\\\"text\\\\\\\":\\\\\\\"Line 2\\\\\\\"}\\\"]}\n");
            readme.append("- Custom attributes, durability, etc.\n\n");
            
            readme.append("Command Placeholders:\n");
            readme.append("-------------------\n");
            readme.append("- {player}: Player's name\n");
            readme.append("- {uuid}: Player's UUID\n");
            readme.append("- {x}: Player's X coordinate\n");
            readme.append("- {y}: Player's Y coordinate\n");
            readme.append("- {z}: Player's Z coordinate\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
    
    /**
     * Creates the README file for shapeless recipe configurations.
     */
    private static void createShapelessRecipesReadme(Path shapelessDir) throws IOException {
        if (!Files.exists(shapelessDir)) return;
        
        Path readmePath = shapelessDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Shapeless Recipes Configuration\n");
            readme.append("=============================\n\n");
            
            readme.append("Shapeless recipes allow ingredients to be placed anywhere in the crafting grid.\n\n");
            
            readme.append("Required Properties:\n");
            readme.append("------------------\n");
            readme.append("- name: Unique identifier for the recipe\n");
            readme.append("- type: Must be \"shapeless\"\n");
            readme.append("- outputItem: The item ID to create\n");
            readme.append("- ingredients: Array of ingredient item IDs\n\n");
            
            readme.append("Optional Properties:\n");
            readme.append("------------------\n");
            readme.append("- outputCount: Number of items to output (default: 1)\n");
            readme.append("- outputNbt: NBT data for the output item\n");
            readme.append("- group: Recipe group for the recipe book\n");
            readme.append("- craftCommands: Commands to execute when crafted\n");
            readme.append("- _comment: Documentation comment\n\n");
            
            readme.append("Example:\n");
            readme.append("-------\n");
            readme.append("```json\n");
            readme.append("{\n");
            readme.append("  \"name\": \"magic_potion\",\n");
            readme.append("  \"type\": \"shapeless\",\n");
            readme.append("  \"outputItem\": \"minecraft:potion\",\n");
            readme.append("  \"outputCount\": 1,\n");
            readme.append("  \"ingredients\": [\n");
            readme.append("    \"minecraft:glass_bottle\",\n");
            readme.append("    \"minecraft:diamond\",\n");
            readme.append("    \"minecraft:gold_ingot\",\n");
            readme.append("    \"minecraft:emerald\"\n");
            readme.append("  ],\n");
            readme.append("  \"outputNbt\": \"{Potion:\\\"minecraft:healing\\\",CustomPotionEffects:[{Id:10,Amplifier:1,Duration:1200}]}\",\n");
            readme.append("  \"craftCommands\": [\n");
            readme.append("    \"tellraw {player} {\\\"text\\\":\\\"You brewed a magic potion!\\\",\\\"color\\\":\\\"purple\\\"}\"\n");
            readme.append("  ],\n");
            readme.append("  \"_comment\": \"Creates a healing potion with regeneration effect\"\n");
            readme.append("}\n");
            readme.append("```\n\n");
            
            readme.append("Ingredient Tips:\n");
            readme.append("--------------\n");
            readme.append("1. Order doesn't matter - ingredients can be placed anywhere\n");
            readme.append("2. You can have 1-9 ingredients\n");
            readme.append("3. Duplicate ingredients are allowed\n");
            readme.append("4. Each ingredient consumes one item from the crafting grid\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
    
    /**
     * Creates the README file for brewing recipe configurations.
     */
    private static void createBrewingRecipesReadme(Path brewingDir) throws IOException {
        if (!Files.exists(brewingDir)) return;
        
        Path readmePath = brewingDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Brewing Recipes Configuration\n");
            readme.append("===========================\n\n");
            
            readme.append("Custom brewing recipes for brewing stands.\n\n");
            
            readme.append("Required Properties:\n");
            readme.append("------------------\n");
            readme.append("- name: Unique identifier for the recipe\n");
            readme.append("- type: Must be \"brewing\"\n");
            readme.append("- basePotion: Base potion item ID\n");
            readme.append("- ingredient: Brewing ingredient item ID\n");
            readme.append("- outputPotion: Result potion item ID\n\n");
            
            readme.append("Optional Properties:\n");
            readme.append("------------------\n");
            readme.append("- brewingTime: Time in ticks (default: 400)\n");
            readme.append("- craftCommands: Commands to execute when brewed\n");
            readme.append("- _comment: Documentation comment\n\n");
            
            readme.append("Example:\n");
            readme.append("-------\n");
            readme.append("```json\n");
            readme.append("{\n");
            readme.append("  \"name\": \"custom_strength_potion\",\n");
            readme.append("  \"type\": \"brewing\",\n");
            readme.append("  \"basePotion\": \"minecraft:awkward_potion\",\n");
            readme.append("  \"ingredient\": \"minecraft:diamond\",\n");
            readme.append("  \"outputPotion\": \"minecraft:strength_potion\",\n");
            readme.append("  \"brewingTime\": 600,\n");
            readme.append("  \"craftCommands\": [\n");
            readme.append("    \"tellraw {player} {\\\"text\\\":\\\"You brewed a diamond strength potion!\\\",\\\"color\\\":\\\"blue\\\"}\"\n");
            readme.append("  ],\n");
            readme.append("  \"_comment\": \"Creates strength potion using diamond\"\n");
            readme.append("}\n");
            readme.append("```\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
    
    /**
     * Creates the README file for furnace recipe configurations.
     */
    private static void createFurnaceRecipesReadme(Path furnaceDir) throws IOException {
        if (!Files.exists(furnaceDir)) return;
        
        Path readmePath = furnaceDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Furnace Recipes Configuration\n");
            readme.append("===========================\n\n");
            
            readme.append("Custom smelting recipes for furnaces, blast furnaces, and smokers.\n\n");
            
            readme.append("Required Properties:\n");
            readme.append("------------------\n");
            readme.append("- name: Unique identifier for the recipe\n");
            readme.append("- type: Must be \"furnace\"\n");
            readme.append("- input: Input item ID to smelt\n");
            readme.append("- output: Output item ID\n\n");
            
            readme.append("Optional Properties:\n");
            readme.append("------------------\n");
            readme.append("- outputCount: Number of items to output (default: 1)\n");
            readme.append("- experience: Experience points to give (default: 0.1)\n");
            readme.append("- cookingTime: Time in ticks (default: 200)\n");
            readme.append("- craftCommands: Commands to execute when smelted\n");
            readme.append("- _comment: Documentation comment\n\n");
            
            readme.append("Example:\n");
            readme.append("-------\n");
            readme.append("```json\n");
            readme.append("{\n");
            readme.append("  \"name\": \"diamond_to_emerald\",\n");
            readme.append("  \"type\": \"furnace\",\n");
            readme.append("  \"input\": \"minecraft:diamond\",\n");
            readme.append("  \"output\": \"minecraft:emerald\",\n");
            readme.append("  \"outputCount\": 2,\n");
            readme.append("  \"experience\": 1.0,\n");
            readme.append("  \"cookingTime\": 400,\n");
            readme.append("  \"craftCommands\": [\n");
            readme.append("    \"tellraw {player} {\\\"text\\\":\\\"You transmuted diamond to emerald!\\\",\\\"color\\\":\\\"green\\\"}\"\n");
            readme.append("  ],\n");
            readme.append("  \"_comment\": \"Converts diamond to 2 emeralds\"\n");
            readme.append("}\n");
            readme.append("```\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
    
    /**
     * Creates the README file for smithing recipe configurations.
     */
    private static void createSmithingRecipesReadme(Path smithingDir) throws IOException {
        if (!Files.exists(smithingDir)) return;
        
        Path readmePath = smithingDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Smithing Recipes Configuration\n");
            readme.append("============================\n\n");
            
            readme.append("Custom upgrade recipes for smithing tables.\n\n");
            
            readme.append("Required Properties:\n");
            readme.append("------------------\n");
            readme.append("- name: Unique identifier for the recipe\n");
            readme.append("- type: Must be \"smithing\"\n");
            readme.append("- base: Base item to upgrade\n");
            readme.append("- addition: Upgrade material\n");
            readme.append("- outputItem: Result item\n\n");
            
            readme.append("Optional Properties:\n");
            readme.append("------------------\n");
            readme.append("- outputCount: Number of items to output (default: 1)\n");
            readme.append("- outputNbt: NBT data for the output item\n");
            readme.append("- craftCommands: Commands to execute when smithed\n");
            readme.append("- _comment: Documentation comment\n\n");
            
            readme.append("Example:\n");
            readme.append("-------\n");
            readme.append("```json\n");
            readme.append("{\n");
            readme.append("  \"name\": \"super_diamond_sword\",\n");
            readme.append("  \"type\": \"smithing\",\n");
            readme.append("  \"base\": \"minecraft:diamond_sword\",\n");
            readme.append("  \"addition\": \"minecraft:nether_star\",\n");
            readme.append("  \"outputItem\": \"minecraft:diamond_sword\",\n");
            readme.append("  \"outputCount\": 1,\n");
            readme.append("  \"outputNbt\": \"{Enchantments:[{id:\\\"minecraft:sharpness\\\",lvl:10}],display:{Name:\\\"{\\\\\\\"text\\\\\\\":\\\\\\\"Stellar Blade\\\\\\\",\\\\\\\"color\\\\\\\":\\\\\\\"aqua\\\\\\\"}\\\"}}\",\n");
            readme.append("  \"craftCommands\": [\n");
            readme.append("    \"tellraw {player} {\\\"text\\\":\\\"You forged the Stellar Blade!\\\",\\\"color\\\":\\\"aqua\\\"}\",\n");
            readme.append("    \"playsound minecraft:block.anvil.use player {player} ~ ~ ~ 1 0.8\"\n");
            readme.append("  ],\n");
            readme.append("  \"_comment\": \"Upgrades diamond sword with nether star to create Stellar Blade\"\n");
            readme.append("}\n");
            readme.append("```\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
}