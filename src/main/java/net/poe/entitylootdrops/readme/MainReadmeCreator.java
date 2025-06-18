package net.poe.entitylootdrops.readme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Creates the main README file for the EntityLootDrops mod.
 */
public class MainReadmeCreator {
    
    /**
     * Creates the main README file for the mod.
     */
    public static void createMainReadme(Path configDir) throws IOException {
        Path readmePath = configDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("EntityLootDrops Mod Configuration Guide\n");
            readme.append("=======================================\n\n");

            readme.append("This mod allows you to customize entity drops, block drops, fishing drops, and crafting recipes.\n\n");

            readme.append("Directory Structure:\n");
            readme.append("-------------------\n");
            readme.append("config/EntityLootDrops/\n");
            readme.append("  ├── Loot Drops/         # Entity loot drop configurations\n");
            readme.append("  │   ├── Normal Drops/   # Always active drops\n");
            readme.append("  │   │   ├── Global_Hostile_Drops.json\n");
            readme.append("  │   │   ├── Global_Hostile_Drops_Example.json\n");
            readme.append("  │   │   └── Mobs/       # Entity-specific drops\n");
            readme.append("  │   └── Event Drops/    # Event-specific drops\n");
            readme.append("  │       ├── Winter/     # Winter event drops\n");
            readme.append("  │       ├── Summer/     # Summer event drops\n");
            readme.append("  │       ├── Easter/     # Easter event drops\n");
            readme.append("  │       └── Halloween/  # Halloween event drops\n");
            readme.append("  ├── Blocks/             # Block drop configurations\n");
            readme.append("  ├── Recipes/            # Custom recipe configurations\n");
            readme.append("  │   ├── Shaped/         # Shaped crafting recipes\n");
            readme.append("  │   ├── Shapeless/      # Shapeless crafting recipes\n");
            readme.append("  │   ├── Brewing/        # Brewing stand recipes\n");
            readme.append("  │   ├── Furnace/        # Furnace/smelting recipes\n");
            readme.append("  │   └── Smithing/       # Smithing table recipes\n");
            readme.append("  ├── Fishing/            # Fishing drop configurations\n");
            readme.append("  │   ├── Global_Fishing_Rewards.json\n");
            readme.append("  │   ├── Conditional Drops/  # Weather/time-based drops\n");
            readme.append("  │   ├── Biome Drops/        # Biome-specific drops\n");
            readme.append("  │   └── Dimension Drops/    # Dimension-specific drops\n");
            readme.append("  └── messages.json       # Custom event messages\n\n");

            readme.append("Entity Loot Drops:\n");
            readme.append("-----------------\n");
            readme.append("The main feature of this mod. Configure custom drops when entities are killed.\n\n");

            readme.append("Key Features:\n");
            readme.append("- Global drops for all hostile mobs\n");
            readme.append("- Entity-specific drops\n");
            readme.append("- Event-based seasonal drops\n");
            readme.append("- Command execution on kill/drop\n");
            readme.append("- Advanced conditions (biome, dimension, player requirements)\n");
            readme.append("- Extra drop chances and bonus amounts\n");
            readme.append("- NBT data support for custom items\n\n");

            readme.append("Commands:\n");
            readme.append("--------\n");
            readme.append("Entity Loot Drops:\n");
            readme.append("  /lootdrops reload - Reload all configurations\n");
            readme.append("  /lootdrops event <name> <true|false> - Toggle event\n");
            readme.append("  /lootdrops event dropchance <true|false> - Toggle double drop chance\n");
            readme.append("  /lootdrops event doubledrops <true|false> - Toggle double drops\n");
            readme.append("  /lootdrops active_events - List active events\n");
            readme.append("  /lootdrops listall - List all available events\n");
            readme.append("  /lootdrops debug <true|false> - Toggle debug logging\n\n");

            readme.append("Other Features:\n");
            readme.append("  /blockdrops - Block drop commands\n");
            readme.append("  /recipes - Recipe commands\n");
            readme.append("  /fishingdrops - Fishing drop commands\n\n");

            readme.append("UNDERSTANDING 'GLOBAL' FILES:\n");
            readme.append("============================\n");
            readme.append("Files with 'Global' in the name apply to ALL entities/blocks/fishing of that type:\n");
            readme.append("- Global_Hostile_Drops.json: ALL hostile mobs\n");
            readme.append("- Global_Block_Drops.json: ALL blocks\n");
            readme.append("- Global_Fishing_Rewards.json: ALL fishing attempts\n\n");

            readme.append("More specific files (like individual mob files) take priority over global files.\n\n");

            readme.append("IMPORTANT - HOW TO DISABLE DROPS:\n");
            readme.append("=================================\n");
            readme.append("Many users ask how to disable drops without deleting files.\n");
            readme.append("Here are the methods that work for ALL drop types:\n\n");

            readme.append("Method 1 - Empty Array (Disables ALL drops in a file):\n");
            readme.append("Replace the entire file content with: []\n\n");

            readme.append("Method 2 - Set Drop Chance/Chance to 0 (and Command Chance):\n");
            readme.append("- For entity drops: Set \"dropChance\": 0 AND \"commandChance\": 0\n");
            readme.append("- For fishing drops: Set \"chance\": 0\n");
            readme.append("- For block drops: Set \"dropChance\": 0 AND \"commandChance\": 0\n");
            readme.append("(This disables both item drops AND command execution)\n\n");

            readme.append("Method 3 - Set Amounts to 0 (Alternative):\n");
            readme.append("Set \"minAmount\": 0 and \"maxAmount\": 0\n\n");

            readme.append("WHY NOT DELETE FILES?\n");
            readme.append("- Main config files (like Global_Hostile_Drops.json) will regenerate\n");
            readme.append("- You'll lose your custom configurations\n");
            readme.append("- Example files are safe to delete and will regenerate with updates\n\n");

            readme.append("Configuration Tips:\n");
            readme.append("------------------\n");
            readme.append("1. Start with Global_Hostile_Drops.json for drops that apply to all hostile mobs\n");
            readme.append("2. Use the Mobs/ folders for entity-specific configurations\n");
            readme.append("3. Create custom .json files to organize your drops by theme\n");
            readme.append("4. Use events for seasonal or temporary drops\n");
            readme.append("5. Test with debug logging enabled: /lootdrops debug true\n");
            readme.append("6. Use /lootdrops reload to apply changes without restarting\n");
            readme.append("7. To disable drops: Use [] or set chances to 0 - DON'T delete main files!\n\n");

            readme.append("Each directory contains its own README file with detailed instructions.\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
}
