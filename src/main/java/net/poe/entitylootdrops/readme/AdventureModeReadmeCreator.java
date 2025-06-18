package net.poe.entitylootdrops.readme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AdventureModeReadmeCreator {

    public static void createReadme() {
        Path readmePath = Paths.get("config/EntityLootDrops/Blocks/Adventure_Mode-README.txt");
        StringBuilder readme = new StringBuilder();

        readme.append("# Adventure Mode Block Restrictions\n\n");
        readme.append("This is Disabled by default.\n\n");
        readme.append("## Overview\n\n");
        readme.append("Adventure Mode allows you to restrict which blocks players can break, place, or modify in specific dimensions.\n");
        readme.append("These restrictions only apply to players in Survival or Adventure mode. Players in Creative mode are not affected.\n\n");

        readme.append("## How It Works\n\n");
        readme.append("- Each entry in the JSON file defines rules for a specific dimension.\n");
        readme.append("- Only blocks listed in `allowedBlockBreakIDs` can be broken if the rule is enabled.\n");
        readme.append("- Only blocks listed in `allowedPlacementIDs` can be placed if `preventBlockPlacement` is true.\n");
        readme.append("- Only blocks listed in `allowedModificationIDs` can be modified (e.g., right-clicked) if `preventBlockModification` is true.\n");
        readme.append("- Use `tag:namespace:tagname` to allow all blocks in a tag (e.g., `tag:minecraft:logs`).\n");
        readme.append("- Each rule can be enabled/disabled and set for a specific dimension.\n");
        readme.append("- Players in Creative mode are not restricted.\n");
        readme.append("- Custom messages can be shown when a player is prevented from breaking, placing, or modifying a block.\n\n");

        readme.append("## Example JSON Entry\n\n");
        readme.append("```\n");
        readme.append("[\n");
        readme.append("  {\n");
        readme.append("    \"enabled\": true,\n");
        readme.append("    \"dimension\": \"minecraft:overworld\",\n");
        readme.append("    \"allowedBlockBreakIDs\": [\n");
        readme.append("      \"minecraft:oak_log\",\n");
        readme.append("      \"minecraft:coal_ore\",\n");
        readme.append("      \"tag:forge:ores\",\n");
        readme.append("      \"tag:minecraft:logs\",\n");
        readme.append("      \"minecraft:stone\"\n");
        readme.append("    ],\n");
        readme.append("    \"preventBlockPlacement\": true,\n");
        readme.append("    \"allowedPlacementIDs\": [\n");
        readme.append("      \"minecraft:torch\",\n");
        readme.append("      \"minecraft:ladder\",\n");
        readme.append("      \"tag:minecraft:signs\",\n");
        readme.append("      \"tag:minecraft:beds\"\n");
        readme.append("    ],\n");
        readme.append("    \"preventBlockModification\": true,\n");
        readme.append("    \"allowedModificationIDs\": [\n");
        readme.append("      \"minecraft:dirt\",\n");
        readme.append("      \"minecraft:grass_block\",\n");
        readme.append("      \"tag:minecraft:logs\"\n");
        readme.append("    ],\n");
        readme.append("    \"breakMessage\": \"This block is protected and cannot be broken!\",\n");
        readme.append("    \"placeMessage\": \"You cannot place blocks here!\",\n");
        readme.append("    \"modifyMessage\": \"You cannot modify blocks here!\"\n");
        readme.append("  }\n");
        readme.append("]\n");
        readme.append("```\n\n");

        readme.append("## Regenerating Blocks\n\n");
        readme.append("You can make any block automatically regenerate after being broken by configuring it in the `Blocks/Normal Drops` folder.\n");
        readme.append("To do this, open or create a JSON file for the block you want to regenerate (for example, `Blocks/Normal Drops/stone_drops.json`).\n\n");
        readme.append("Add or edit a drop entry to include the following properties:\n\n");
        readme.append("- `\"canRegenerate\": true` — Enables regeneration for this block.\n");
        readme.append("- `\"brokenBlockReplace\": \"minecraft:bedrock\"` — The block to temporarily replace the broken block with (e.g., bedrock, barrier, or air).\n");
        readme.append("- `\"respawnTime\": 20` — Time in seconds before the original block is restored.\n\n");
        readme.append("### Example Regenerating Block Entry\n");
        readme.append("```json\n");
        readme.append("{\n");
        readme.append("  \"blockId\": \"minecraft:stone\",\n");
        readme.append("  \"itemId\": \"minecraft:diamond\",\n");
        readme.append("  \"dropChance\": 100.0,\n");
        readme.append("  \"minAmount\": 1,\n");
        readme.append("  \"maxAmount\": 1,\n");
        readme.append("  \"canRegenerate\": true,\n");
        readme.append("  \"brokenBlockReplace\": \"minecraft:bedrock\",\n");
        readme.append("  \"respawnTime\": 20\n");
        readme.append("}\n");
        readme.append("```\n\n");
        readme.append("You can add these properties to any block drop entry in the `Blocks/Normal Drops` folder. When a player breaks the block, it will be replaced with the specified block (e.g., bedrock) and then automatically restored after the given time.\n");
        readme.append("This system is separate from Adventure Mode and works for any block you configure.\n\n");

        readme.append("---\n");
        readme.append("For further help, see the main README or ask on the mod's support channels.\n");

        try {
            Files.createDirectories(readmePath.getParent());
            Files.write(readmePath, readme.toString().getBytes());
        } catch (IOException e) {
            System.err.println("Failed to create Adventure_Mode-README.txt: " + e.getMessage());
        }
    }
}