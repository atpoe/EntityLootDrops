package net.poe.entitylootdrops.readme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Creates README files for entity loot configurations.
 */
public class EntityReadmeCreator {
    
    /**
     * Creates the README file for entity loot configurations.
     */
    public static void createEntityLootReadme(Path entitiesDir) throws IOException {
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
}
