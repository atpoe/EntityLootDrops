package net.poe.entitylootdrops.readme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Creates README files for recipe configurations.
 */
public class RecipeReadmeCreator {
    
    /**
     * Creates the main README file for recipe configurations.
     */
    public static void createRecipesReadme(Path recipesDir) throws IOException {
        Path readmePath = recipesDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Custom Recipes Configuration Guide\n");
            readme.append("================================\n\n");
            
            readme.append("This system allows you to add custom crafting recipes to the game.\n\n");
            
            readme.append("Directory Structure:\n");
            readme.append("-------------------\n");
            readme.append("config/EntityLootDrops/Recipes/\n");
            readme.append("  ├── Shaped/              # Shaped crafting recipes\n");
            readme.append("  ├── Shapeless/           # Shapeless crafting recipes\n");
            readme.append("  ├── Brewing/             # Brewing stand recipes\n");
            readme.append("  ├── Furnace/             # Furnace/smelting recipes\n");
            readme.append("  └── Smithing/            # Smithing table recipes\n\n");
            
            readme.append("Recipe Types:\n");
            readme.append("------------\n");
            readme.append("1. Shaped: Recipes where ingredient placement matters (like vanilla crafting)\n");
            readme.append("2. Shapeless: Recipes where ingredients can be placed anywhere\n");
            readme.append("3. Brewing: Potion brewing recipes for brewing stands\n");
            readme.append("4. Furnace: Smelting recipes for furnaces and similar blocks\n");
            readme.append("5. Smithing: Upgrade recipes for smithing tables\n\n");
            
            readme.append("For detailed configuration instructions, see the README files in each subdirectory.\n\n");
            
            readme.append("Commands:\n");
            readme.append("--------\n");
            readme.append("/recipes reload - Reload recipe configuration\n");
            readme.append("/recipes list - List all custom recipes\n");
            readme.append("/recipes give <player> <recipeId> - Give a recipe to a player\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
    
    /**
     * Creates the README file for shaped recipes.
     */
    public static void createShapedRecipesReadme(Path shapedDir) throws IOException {
        if (!Files.exists(shapedDir)) return;
        
        Path readmePath = shapedDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Shaped Recipes Configuration\n");
            readme.append("==========================\n\n");
            
            readme.append("Shaped recipes require ingredients to be placed in specific positions,\n");
            readme.append("just like vanilla Minecraft crafting recipes.\n\n");
            
            readme.append("Configuration Format:\n");
            readme.append("-------------------\n");
            readme.append("```json\n");
            readme.append("{\n");
            readme.append("  \"recipeId\": \"custom:example_recipe\",\n");
            readme.append("  \"result\": {\n");
            readme.append("    \"item\": \"minecraft:diamond_sword\",\n");
            readme.append("    \"count\": 1,\n");
            readme.append("    \"nbt\": \"{display:{Name:'\\\"Custom Sword\\\"}}\"\n");
            readme.append("  },\n");
            readme.append("  \"pattern\": [\n");
            readme.append("    \" D \",\n");
            readme.append("    \" D \",\n");
            readme.append("    \" S \"\n");
            readme.append("  ],\n");
            readme.append("  \"key\": {\n");
            readme.append("    \"D\": \"minecraft:diamond\",\n");
            readme.append("    \"S\": \"minecraft:stick\"\n");
            readme.append("  },\n");
            readme.append("  \"command\": \"tellraw @a {\\\"text\\\":\\\"Someone crafted a custom sword!\\\"}\",\n");
            readme.append("  \"commandChance\": 1.0\n");
            readme.append("}\n");
            readme.append("```\n\n");
            
            readme.append("Properties:\n");
            readme.append("----------\n");
            readme.append("- recipeId: Unique identifier for the recipe\n");
            readme.append("- result: The item produced by the recipe\n");
            readme.append("- pattern: 3x3 grid pattern using single characters\n");
            readme.append("- key: Maps pattern characters to actual items\n");
            readme.append("- command: Optional command to execute when crafted\n");
            readme.append("- commandChance: Probability to execute command (0.0-1.0)\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
    
    /**
     * Creates the README file for shapeless recipes.
     */
    public static void createShapelessRecipesReadme(Path shapelessDir) throws IOException {
        if (!Files.exists(shapelessDir)) return;
        
        Path readmePath = shapelessDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Shapeless Recipes Configuration\n");
            readme.append("=============================\n\n");
            
            readme.append("Shapeless recipes allow ingredients to be placed in any position\n");
            readme.append("in the crafting grid, like vanilla dye recipes.\n\n");
            
            readme.append("Configuration Format:\n");
            readme.append("-------------------\n");
            readme.append("```json\n");
            readme.append("{\n");
            readme.append("  \"recipeId\": \"custom:magic_dust\",\n");
            readme.append("  \"result\": {\n");
            readme.append("    \"item\": \"minecraft:glowstone_dust\",\n");
            readme.append("    \"count\": 4,\n");
            readme.append("    \"nbt\": \"{display:{Name:'\\\"Magic Dust\\\"}}\"\n");
            readme.append("  },\n");
            readme.append("  \"ingredients\": [\n");
            readme.append("    \"minecraft:redstone\",\n");
            readme.append("    \"minecraft:gunpowder\",\n");
            readme.append("    \"minecraft:sugar\"\n");
            readme.append("  ],\n");
            readme.append("  \"command\": \"particle minecraft:enchant ~ ~ ~ 1 1 1 0.1 50\",\n");
            readme.append("  \"commandChance\": 0.5\n");
            readme.append("}\n");
            readme.append("```\n\n");
            
            readme.append("Properties:\n");
            readme.append("----------\n");
            readme.append("- recipeId: Unique identifier for the recipe\n");
            readme.append("- result: The item produced by the recipe\n");
            readme.append("- ingredients: List of required items (order doesn't matter)\n");
            readme.append("- command: Optional command to execute when crafted\n");
            readme.append("- commandChance: Probability to execute command (0.0-1.0)\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
    
    /**
     * Creates the README file for brewing recipes.
     */
    public static void createBrewingRecipesReadme(Path brewingDir) throws IOException {
        if (!Files.exists(brewingDir)) return;
        
        Path readmePath = brewingDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Brewing Recipes Configuration\n");
            readme.append("===========================\n\n");
            
            readme.append("Brewing recipes allow you to create custom potion brewing combinations\n");
            readme.append("for brewing stands.\n\n");
            
            readme.append("Configuration Format:\n");
            readme.append("-------------------\n");
            readme.append("```json\n");
            readme.append("{\n");
            readme.append("  \"recipeId\": \"custom:healing_brew\",\n");
            readme.append("  \"basePotion\": \"minecraft:awkward_potion\",\n");
            readme.append("  \"ingredient\": \"minecraft:glistering_melon_slice\",\n");
            readme.append("  \"result\": {\n");
            readme.append("    \"item\": \"minecraft:potion\",\n");
            readme.append("    \"count\": 1,\n");
            readme.append("    \"nbt\": \"{Potion:\\\"minecraft:healing\\\"}\"\n");
            readme.append("  },\n");
            readme.append("  \"brewingTime\": 400,\n");
            readme.append("  \"experience\": 0.2,\n");
            readme.append("  \"command\": \"tellraw @a {\\\"text\\\":\\\"A healing potion has been brewed!\\\"}\",\n");
            readme.append("  \"commandChance\": 0.1\n");
            readme.append("}\n");
            readme.append("```\n\n");
            
            readme.append("Properties:\n");
            readme.append("----------\n");
            readme.append("- recipeId: Unique identifier for the recipe\n");
            readme.append("- basePotion: The base potion required (water bottle, awkward potion, etc.)\n");
            readme.append("- ingredient: The brewing ingredient to add\n");
            readme.append("- result: The resulting potion\n");
            readme.append("- brewingTime: Time in ticks to brew (400 = 20 seconds)\n");
            readme.append("- experience: Experience points awarded for brewing\n");
            readme.append("- command: Optional command to execute when brewed\n");
            readme.append("- commandChance: Probability to execute command (0.0-1.0)\n\n");
            
            readme.append("Common Base Potions:\n");
            readme.append("------------------\n");
            readme.append("- minecraft:water_bottle - Plain water bottle\n");
            readme.append("- minecraft:awkward_potion - Nether wart + water bottle\n");
            readme.append("- minecraft:thick_potion - Glowstone dust + water bottle\n");
            readme.append("- minecraft:mundane_potion - Various ingredients + water bottle\n\n");
            
            readme.append("Common Brewing Ingredients:\n");
            readme.append("-------------------------\n");
            readme.append("- minecraft:nether_wart - Creates awkward potions\n");
            readme.append("- minecraft:glistering_melon_slice - Healing potions\n");
            readme.append("- minecraft:spider_eye - Poison potions\n");
            readme.append("- minecraft:sugar - Speed potions\n");
            readme.append("- minecraft:magma_cream - Fire resistance potions\n");
            readme.append("- minecraft:blaze_powder - Strength potions\n");
            readme.append("- minecraft:ghast_tear - Regeneration potions\n");
            readme.append("- minecraft:rabbit_foot - Jump boost potions\n");
            readme.append("- minecraft:golden_carrot - Night vision potions\n");
            readme.append("- minecraft:pufferfish - Water breathing potions\n\n");
            
            readme.append("Potion Modifiers:\n");
            readme.append("---------------\n");
            readme.append("- minecraft:redstone - Extends duration\n");
            readme.append("- minecraft:glowstone_dust - Increases potency\n");
            readme.append("- minecraft:gunpowder - Creates splash potions\n");
            readme.append("- minecraft:dragon_breath - Creates lingering potions\n\n");
            
            readme.append("Tips:\n");
            readme.append("----\n");
            readme.append("1. Test potion NBT data in-game before adding to config\n");
            readme.append("2. Standard brewing time is 400 ticks (20 seconds)\n");
            readme.append("3. Experience values are typically 0.1-0.5 per brew\n");
            readme.append("4. Use commands to announce rare potion brewing\n");
            readme.append("5. Consider creating custom potion effects with NBT\n");
            readme.append("6. Multiple recipes can use the same base potion\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
    
    /**
     * Creates the README file for furnace recipes.
     */
    public static void createFurnaceRecipesReadme(Path furnaceDir) throws IOException {
        if (!Files.exists(furnaceDir)) return;
        
        Path readmePath = furnaceDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Furnace Recipes Configuration\n");
            readme.append("===========================\n\n");
            
            readme.append("Furnace recipes allow you to create custom smelting combinations\n");
            readme.append("for furnaces, blast furnaces, and smokers.\n\n");
            
            readme.append("Configuration Format:\n");
            readme.append("-------------------\n");
            readme.append("```json\n");
            readme.append("{\n");
            readme.append("  \"recipeId\": \"custom:diamond_ingot\",\n");
            readme.append("  \"ingredient\": \"minecraft:diamond_ore\",\n");
            readme.append("  \"result\": {\n");
            readme.append("    \"item\": \"minecraft:diamond\",\n");
            readme.append("    \"count\": 2,\n");
            readme.append("    \"nbt\": \"{display:{Name:'\\\"Refined Diamond\\\"}}\"\n");
            readme.append("  },\n");
            readme.append("  \"experience\": 1.0,\n");
            readme.append("  \"cookingTime\": 200,\n");
            readme.append("  \"furnaceType\": \"all\",\n");
            readme.append("  \"command\": \"tellraw @a {\\\"text\\\":\\\"Someone smelted refined diamonds!\\\"}\",\n");
            readme.append("  \"commandChance\": 0.05\n");
            readme.append("}\n");
            readme.append("```\n\n");
            
            readme.append("Properties:\n");
            readme.append("----------\n");
            readme.append("- recipeId: Unique identifier for the recipe\n");
            readme.append("- ingredient: The item to be smelted\n");
            readme.append("- result: The resulting item after smelting\n");
            readme.append("- experience: Experience points awarded for smelting\n");
            readme.append("- cookingTime: Time in ticks to smelt (200 = 10 seconds)\n");
            readme.append("- furnaceType: Which furnaces can use this recipe\n");
            readme.append("- command: Optional command to execute when smelted\n");
            readme.append("- commandChance: Probability to execute command (0.0-1.0)\n\n");
            
            readme.append("Furnace Types:\n");
            readme.append("-------------\n");
            readme.append("- \"all\" - Works in all furnace types\n");
            readme.append("- \"furnace\" - Only regular furnaces\n");
            readme.append("- \"blast_furnace\" - Only blast furnaces (faster for ores/armor)\n");
            readme.append("- \"smoker\" - Only smokers (faster for food)\n");
            readme.append("- \"campfire\" - Only campfires (slower cooking)\n\n");
            
            readme.append("Standard Cooking Times:\n");
            readme.append("---------------------\n");
            readme.append("- Regular Furnace: 200 ticks (10 seconds)\n");
            readme.append("- Blast Furnace: 100 ticks (5 seconds) - for ores/armor\n");
            readme.append("- Smoker: 100 ticks (5 seconds) - for food\n");
            readme.append("- Campfire: 600 ticks (30 seconds)\n\n");
            
            readme.append("Experience Values:\n");
            readme.append("----------------\n");
            readme.append("Common experience values for reference:\n");
            readme.append("- Raw food → Cooked food: 0.1-0.35\n");
            readme.append("- Raw ores → Ingots: 0.1-1.0\n");
            readme.append("- Stone → Smooth stone: 0.1\n");
            readme.append("- Sand → Glass: 0.1\n");
            readme.append("- Custom recipes: 0.1-2.0 (balance as needed)\n\n");
            
            readme.append("Tips:\n");
            readme.append("----\n");
            readme.append("1. Match furnace types to recipe themes (blast furnace for ores)\n");
            readme.append("2. Use appropriate cooking times for balance\n");
            readme.append("3. Consider fuel efficiency when setting cooking times\n");
            readme.append("4. Test recipes in-game to ensure they work as expected\n");
            readme.append("5. Use commands to announce rare smelting results\n");
            readme.append("6. Balance experience rewards with vanilla recipes\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
    
    /**
     * Creates the README file for smithing recipes.
     */
    public static void createSmithingRecipesReadme(Path smithingDir) throws IOException {
        if (!Files.exists(smithingDir)) return;
        
        Path readmePath = smithingDir.resolve("README.txt");
        if (!Files.exists(readmePath)) {
            StringBuilder readme = new StringBuilder();
            readme.append("Smithing Recipes Configuration\n");
            readme.append("============================\n\n");
            
            readme.append("Smithing recipes allow you to create custom upgrade combinations\n");
            readme.append("for smithing tables, similar to netherite upgrades.\n\n");
            
            readme.append("Configuration Format:\n");
            readme.append("-------------------\n");
            readme.append("```json\n");
            readme.append("{\n");
            readme.append("  \"recipeId\": \"custom:enhanced_diamond_sword\",\n");
            readme.append("  \"template\": \"minecraft:netherite_upgrade_smithing_template\",\n");
            readme.append("  \"base\": \"minecraft:diamond_sword\",\n");
            readme.append("  \"addition\": \"minecraft:emerald\",\n");
            readme.append("  \"result\": {\n");
            readme.append("    \"item\": \"minecraft:diamond_sword\",\n");
            readme.append("    \"count\": 1,\n");
            readme.append("    \"nbt\": \"{display:{Name:'\\\"Enhanced Diamond Sword\\\"},Enchantments:[{id:\\\"minecraft:sharpness\\\",lvl:6}]}\"\n");
            readme.append("  },\n");
            readme.append("  \"copyNbt\": true,\n");
            readme.append("  \"copyEnchantments\": true,\n");
            readme.append("  \"command\": \"tellraw @a {\\\"text\\\":\\\"Someone created an enhanced weapon!\\\"}\",\n");
            readme.append("  \"commandChance\": 1.0\n");
            readme.append("}\n");
            readme.append("```\n\n");
            
            readme.append("Properties:\n");
            readme.append("----------\n");
            readme.append("- recipeId: Unique identifier for the recipe\n");
            readme.append("- template: Smithing template required (1.20+ feature)\n");
            readme.append("- base: The base item to be upgraded\n");
            readme.append("- addition: The upgrade material\n");
            readme.append("- result: The resulting upgraded item\n");
            readme.append("- copyNbt: Whether to copy NBT data from base item\n");
            readme.append("- copyEnchantments: Whether to copy enchantments from base item\n");
            readme.append("- command: Optional command to execute when smithed\n");
            readme.append("- commandChance: Probability to execute command (0.0-1.0)\n\n");
            
            readme.append("Smithing Templates (1.20+):\n");
            readme.append("-------------------------\n");
            readme.append("- minecraft:netherite_upgrade_smithing_template\n");
            readme.append("- minecraft:armor_trim_smithing_template\n");
            readme.append("- minecraft:coast_armor_trim_smithing_template\n");
            readme.append("- minecraft:dune_armor_trim_smithing_template\n");
            readme.append("- minecraft:eye_armor_trim_smithing_template\n");
            readme.append("- minecraft:host_armor_trim_smithing_template\n");
            readme.append("- minecraft:raiser_armor_trim_smithing_template\n");
            readme.append("- minecraft:rib_armor_trim_smithing_template\n");
            readme.append("- minecraft:sentry_armor_trim_smithing_template\n");
            readme.append("- minecraft:shaper_armor_trim_smithing_template\n");
            readme.append("- minecraft:silence_armor_trim_smithing_template\n");
            readme.append("- minecraft:snout_armor_trim_smithing_template\n");
            readme.append("- minecraft:spire_armor_trim_smithing_template\n");
            readme.append("- minecraft:tide_armor_trim_smithing_template\n");
            readme.append("- minecraft:vex_armor_trim_smithing_template\n");
            readme.append("- minecraft:ward_armor_trim_smithing_template\n");
            readme.append("- minecraft:wayfinder_armor_trim_smithing_template\n");
            readme.append("- minecraft:wild_armor_trim_smithing_template\n\n");
            
            readme.append("Legacy Format (Pre-1.20):\n");
            readme.append("------------------------\n");
            readme.append("For versions before 1.20, omit the template field:\n");
            readme.append("```json\n");
            readme.append("{\n");
            readme.append("  \"recipeId\": \"custom:legacy_upgrade\",\n");
            readme.append("  \"base\": \"minecraft:iron_sword\",\n");
            readme.append("  \"addition\": \"minecraft:diamond\",\n");
            readme.append("  \"result\": {\n");
            readme.append("    \"item\": \"minecraft:diamond_sword\",\n");
            readme.append("    \"count\": 1\n");
            readme.append("  }\n");
            readme.append("}\n");
            readme.append("```\n\n");
            
            readme.append("Common Upgrade Materials:\n");
            readme.append("-----------------------\n");
            readme.append("- minecraft:netherite_ingot - Netherite upgrades\n");
            readme.append("- minecraft:diamond - Diamond upgrades\n");
            readme.append("- minecraft:emerald - Emerald enhancements\n");
            readme.append("- minecraft:gold_ingot - Gold plating\n");
            readme.append("- minecraft:iron_ingot - Iron reinforcement\n");
            readme.append("- Custom items - Your mod's upgrade materials\n\n");
            
            readme.append("NBT and Enchantment Copying:\n");
            readme.append("---------------------------\n");
            readme.append("- copyNbt: true - Preserves custom names, lore, and other NBT data\n");
            readme.append("- copyEnchantments: true - Preserves all enchantments from base item\n");
            readme.append("- Both can be combined with additional NBT in the result\n");
            readme.append("- Result NBT will be merged with copied NBT\n\n");
            
            readme.append("Tips:\n");
            readme.append("----\n");
            readme.append("1. Use appropriate templates for the upgrade type\n");
            readme.append("2. Consider preserving enchantments and NBT for upgrades\n");
            readme.append("3. Test NBT merging behavior in-game\n");
            readme.append("4. Balance upgrade costs with the benefits provided\n");
            readme.append("5. Use commands to announce significant upgrades\n");
            readme.append("6. Consider creating upgrade chains (iron → diamond → custom)\n");
            readme.append("7. Armor trims require specific templates and materials\n");
            
            Files.write(readmePath, readme.toString().getBytes());
        }
    }
}
