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
}
