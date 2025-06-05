package net.poe.entitylootdrops.readme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Creates README files for block drop configurations.
 */
public class BlockReadmeCreator {
    
    /**
     * Creates the README file for block drop configurations.
     */
    public static void createBlockDropsReadme(Path blocksDir) throws IOException {
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
    public static void createNormalDropsReadme(Path normalDropsDir) throws IOException {
        if (!Files.exists(normalDropsDir)) return;
        
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
    public static void createEventDropsReadme(Path eventsDir) throws IOException {
        if (!Files.exists(eventsDir)) return;
        
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
    public static void createEventTypeReadme(Path eventDir, String eventType) throws IOException {
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
}
