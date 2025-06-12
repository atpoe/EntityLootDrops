package net.poe.entitylootdrops.readme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Creates README files for the Loot Drops directory structure.
 */
public class LootDropsReadmeCreator {

    /**
     * Creates the main README file for the Loot Drops directory.
     */
    public static void createLootDropsReadme(Path lootDropsDir) throws IOException {
        Path readmePath = lootDropsDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Entity Loot Drops Configuration Guide\n");
            readme.append("===================================\n\n");

            readme.append("This directory contains all entity loot drop configurations.\n\n");

            readme.append("Directory Structure:\n");
            readme.append("-------------------\n");
            readme.append("Loot Drops/\n");
            readme.append("  ├── Normal Drops/           # Always active drops\n");
            readme.append("  │   ├── Global_Hostile_Drops.json        # Main hostile mob drops\n");
            readme.append("  │   ├── Global_Hostile_Drops_Example.json # Comprehensive examples\n");
            readme.append("  │   ├── [Custom].json       # Your custom drop files\n");
            readme.append("  │   └── Mobs/               # Entity-specific drops\n");
            readme.append("  │       └── [entity]_drops.json # Per-entity configurations\n");
            readme.append("  └── Event Drops/            # Event-specific drops\n");
            readme.append("      ├── Winter/             # Winter event drops\n");
            readme.append("      ├── Summer/             # Summer event drops\n");
            readme.append("      ├── Easter/             # Easter event drops\n");
            readme.append("      ├── Halloween/          # Halloween event drops\n");
            readme.append("      └── [Custom Event]/     # Your custom events\n");
            readme.append("          ├── Global_Hostile_Drops.json\n");
            readme.append("          ├── Global_Hostile_Drops_Example.json\n");
            readme.append("          └── Mobs/           # Event-specific entity drops\n\n");

            readme.append("Configuration Format:\n");
            readme.append("--------------------\n");
            readme.append("All drop configurations use JSON format with these properties:\n\n");

            readme.append("BASIC PROPERTIES:\n");
            readme.append("- entityId: Entity ID (e.g., \"minecraft:zombie\") - only for entity-specific drops\n");
            readme.append("- itemId: Item to drop (e.g., \"minecraft:diamond\")\n");
            readme.append("- dropChance: Percentage chance to drop (0-100)\n");
            readme.append("- minAmount: Minimum amount to drop\n");
            readme.append("- maxAmount: Maximum amount to drop\n");
            readme.append("- requirePlayerKill: If true, only drops when killed by a player\n\n");

            readme.append("ADVANCED PROPERTIES:\n");
            readme.append("- nbtData: Custom NBT data for the dropped item\n");
            readme.append("- allowDefaultDrops: If false, prevents normal drops (default: true)\n");
            readme.append("- allowModIDs: List of mod IDs allowed to trigger this drop\n\n");

            readme.append("EXTRA DROPS:\n");
            readme.append("- extraDropChance: Chance for bonus drops (0-100)\n");
            readme.append("- extraAmountMin: Minimum bonus amount\n");
            readme.append("- extraAmountMax: Maximum bonus amount\n\n");

            readme.append("REQUIREMENTS:\n");
            readme.append("- requiredDimension: Dimension where drop can occur (e.g., \"minecraft:overworld\")\n");
            readme.append("- requiredBiome: Biome where drop can occur (e.g., \"minecraft:desert\")\n");
            readme.append("- requiredAdvancement: Player advancement required\n");
            readme.append("- requiredEffect: Player effect required (e.g., \"minecraft:luck\")\n");
            readme.append("- requiredEquipment: Equipment player must have\n");
            readme.append("- minLevel: Minimum entity level (if using level mods)\n");
            readme.append("- maxLevel: Maximum entity level (if using level mods)\n\n");

            readme.append("COMMANDS:\n");
            readme.append("- command: Command to execute when entity dies (regardless of drop)\n");
            readme.append("- commandChance: Chance to execute the command (0-100)\n");
            readme.append("- dropCommand: Command to execute ONLY when item actually drops\n");
            readme.append("- dropCommandChance: Chance to execute the drop command (0-100)\n\n");

            readme.append("PLACEHOLDERS (for commands):\n");
            readme.append("- @killer: The player who killed the entity\n");
            readme.append("- @entity: The entity that was killed\n");
            readme.append("- @item: The dropped item\n");
            readme.append("- @amount: The amount dropped\n\n");

            readme.append("Example Configuration:\n");
            readme.append("---------------------\n");
            readme.append("[\n");
            readme.append("  {\n");
            readme.append("    \"comment\": \"Zombies drop diamonds rarely with notification\",\n");
            readme.append("    \"entityId\": \"minecraft:zombie\",\n");
            readme.append("    \"itemId\": \"minecraft:diamond\",\n");
            readme.append("    \"dropChance\": 5.0,\n");
            readme.append("    \"minAmount\": 1,\n");
            readme.append("    \"maxAmount\": 2,\n");
            readme.append("    \"requirePlayerKill\": true,\n");
            readme.append("    \"requiredDimension\": \"minecraft:overworld\",\n");
            readme.append("    \"command\": \"tellraw @a {\\\"text\\\":\\\"A zombie was slain!\\\",\\\"color\\\":\\\"yellow\\\"}\",\n");
            readme.append("    \"commandChance\": 25.0,\n");
            readme.append("    \"dropCommand\": \"tellraw @a {\\\"text\\\":\\\"Rare diamond drop!\\\",\\\"color\\\":\\\"aqua\\\"}\",\n");
            readme.append("    \"dropCommandChance\": 100.0,\n");
            readme.append("    \"extraDropChance\": 15.0,\n");
            readme.append("    \"extraAmountMin\": 1,\n");
            readme.append("    \"extraAmountMax\": 3\n");
            readme.append("  }\n");
            readme.append("]\n\n");

            readme.append("Tips:\n");
            readme.append("----\n");
            readme.append("- Use Global_Hostile_Drops.json for drops that apply to all hostile mobs\n");
            readme.append("- Use Mobs/ folder for entity-specific drops\n");
            readme.append("- Create custom .json files for organized drop categories\n");
            readme.append("- Event drops only activate when the event is enabled\n");
            readme.append("- Delete example files to regenerate them with updates\n");
            readme.append("- Use /lootdrops reload to apply changes without restarting\n");

            Files.write(readmePath, readme.toString().getBytes());
        }
    }

    /**
     * Creates README for Normal Drops directory.
     */
    public static void createNormalDropsReadme(Path normalDropsDir) throws IOException {
        Path readmePath = normalDropsDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Normal Drops Configuration\n");
            readme.append("=========================\n\n");

            readme.append("This directory contains drops that are ALWAYS active.\n\n");

            readme.append("Files:\n");
            readme.append("- Global_Hostile_Drops.json: Main configuration for all hostile mobs\n");
            readme.append("  * Contains your primary drop configurations\n");
            readme.append("  * Will regenerate with basic example if deleted\n");
            readme.append("  * Only delete if you want to reset to defaults\n");
            readme.append("- Global_Hostile_Drops_Example.json: Comprehensive examples (Safe to delete)\n");
            readme.append("- Custom .json files: Your additional drop configurations\n");
            readme.append("- Mobs/: Entity-specific drop configurations\n\n");

            readme.append("These drops will always be checked when entities are killed,\n");
            readme.append("regardless of any active events.\n\n");

            readme.append("IMPORTANT:\n");
            readme.append("- Global_Hostile_Drops.json is your main configuration file\n");
            readme.append("- It will regenerate if deleted, but you'll lose your custom settings\n");
            readme.append("- Example files can be safely deleted and will regenerate with updates\n");

            Files.write(readmePath, readme.toString().getBytes());
        }
    }

    /**
     * Creates README for Event Drops directory.
     */
    public static void createEventDropsReadme(Path eventDropsDir) throws IOException {
        Path readmePath = eventDropsDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Event Drops Configuration\n");
            readme.append("========================\n\n");

            readme.append("This directory contains drops that only activate during specific events.\n\n");

            readme.append("Built-in Events:\n");
            readme.append("- Winter/: Winter-themed drops\n");
            readme.append("- Summer/: Summer-themed drops\n");
            readme.append("- Easter/: Easter-themed drops\n");
            readme.append("- Halloween/: Halloween-themed drops\n\n");

            readme.append("Custom Events:\n");
            readme.append("You can create your own event folders with any name.\n");
            readme.append("Each event folder should contain:\n");
            readme.append("- Global_Hostile_Drops.json: Event-specific hostile drops\n");
            readme.append("- Mobs/: Event-specific entity drops\n\n");

            readme.append("Event Management:\n");
            readme.append("- /lootdrops event <eventName> true - Enable event\n");
            readme.append("- /lootdrops event <eventName> false - Disable event\n");
            readme.append("- /lootdrops active_events - List active events\n");
            readme.append("- /lootdrops listall - List all available events\n");

            Files.write(readmePath, readme.toString().getBytes());
        }
    }
}