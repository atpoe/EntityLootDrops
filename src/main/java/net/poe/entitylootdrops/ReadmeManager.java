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
            
            readme.append("This mod allows you to customize entity drops, block drops, and crafting recipes.\n\n");
            
            readme.append("Directory Structure:\n");
            readme.append("-------------------\n");
            readme.append("config/EntityLootDrops/\n");
            readme.append("  ├── Entities/            # Entity loot drop configurations\n");
            readme.append("  ├── Blocks/              # Block drop configurations\n");
            readme.append("  └── Recipes/             # Custom crafting recipe configurations\n\n");
            
            readme.append("Each directory contains its own README file with detailed instructions.\n\n");
            
            readme.append("Commands:\n");
            readme.append("--------\n");
            readme.append("/lootdrops - Entity loot drop commands\n");
            readme.append("/blockdrops - Block drop commands\n");
            readme.append("/recipes - Recipe commands\n\n");
            
            readme.append("For more information, see the README files in each subdirectory.\n");
            
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
     * Creates the README file for recipe configurations.
     */
    private static void createRecipesReadme(Path recipesDir) throws IOException {
        Path readmePath = recipesDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Custom Recipes Configuration Guide\n");
            readme.append("===============================\n\n");
            
            readme.append("This system allows you to create custom crafting recipes with NBT data support.\n\n");
            
            readme.append("Directory Structure:\n");
            readme.append("-------------------\n");
            readme.append("config/EntityLootDrops/Recipes/\n");
            readme.append("  ├── Shaped/               # Shaped crafting recipes\n");
            readme.append("  └── Shapeless/            # Shapeless crafting recipes\n\n");
            
            readme.append("Recipe Format:\n");
            readme.append("------------\n");
            readme.append("Recipes use JSON format with these properties:\n\n");
            
            readme.append("Common Properties:\n");
            readme.append("- name: Unique name for the recipe (used for recipe ID)\n");
            readme.append("- type: Either \"shaped\" or \"shapeless\"\n");
            readme.append("- outputItem: The Minecraft item ID for the output (e.g., \"minecraft:diamond_sword\")\n");
            readme.append("- outputCount: Number of items to output (default: 1)\n");
            readme.append("- outputNbt: Optional NBT data for the output item\n");
            readme.append("- group: Optional recipe group for the recipe book\n\n");
            
            readme.append("Shaped Recipe Properties:\n");
            readme.append("- pattern: Array of strings representing the crafting grid pattern\n");
            readme.append("- key: Mapping of pattern characters to item IDs\n\n");
            
            readme.append("Shapeless Recipe Properties:\n");
            readme.append("- ingredients: Array of item IDs for the ingredients\n\n");
            
            readme.append("Example Shaped Recipe:\n");
            readme.append("```json\n");
            readme.append("{\n");
            readme.append("  \"name\": \"diamond_helmet_with_enchants\",\n");
            readme.append("  \"type\": \"shaped\",\n");
            readme.append("  \"outputItem\": \"minecraft:diamond_helmet\",\n");
            readme.append("  \"outputCount\": 1,\n");
            readme.append("  \"outputNbt\": \"{display:{Name:'{\\\"text\\\":\\\"Enchanted Helmet\\\",\\\"color\\\":\\\"aqua\\\"}'},Enchantments:[{id:\\\"minecraft:protection\\\",lvl:4},{id:\\\"minecraft:unbreaking\\\",lvl:3}]}\",\n");
            readme.append("  \"pattern\": [\n");
            readme.append("    \"XXX\",\n");
            readme.append("    \"X X\"\n");
            readme.append("  ],\n");
            readme.append("  \"key\": {\n");
            readme.append("    \"X\": \"minecraft:diamond\"\n");
            readme.append("  },\n");
            readme.append("  \"group\": \"helmets\"\n");
            readme.append("}\n");
            readme.append("```\n\n");
            
            readme.append("Example Shapeless Recipe:\n");
            readme.append("```json\n");
            readme.append("{\n");
            readme.append("  \"name\": \"golden_apple_from_gold_blocks\",\n");
            readme.append("  \"type\": \"shapeless\",\n");
            readme.append("  \"outputItem\": \"minecraft:golden_apple\",\n");
            readme.append("  \"outputCount\": 1,\n");
            readme.append("  \"ingredients\": [\n");
            readme.append("    \"minecraft:gold_block\",\n");
            readme.append("    \"minecraft:gold_block\",\n");
            readme.append("    \"minecraft:apple\"\n");
            readme.append("  ]\n");
            readme.append("}\n");
            readme.append("```\n\n");
            
            readme.append("Commands:\n");
            readme.append("--------\n");
            readme.append("/recipes reload - Reload recipe configuration\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
}
