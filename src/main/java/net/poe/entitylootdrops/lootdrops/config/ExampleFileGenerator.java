package net.poe.entitylootdrops.lootdrops.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates comprehensive .example files for configuration reference.
 * These files demonstrate all available features and options.
 */
public class ExampleFileGenerator {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CONFIG_DIR = "config/EntityLootDrops";
    private static final String LOOT_DROPS_DIR = "Loot Drops";
    private static final String NORMAL_DROPS_DIR = "Normal Drops";
    private static final String MOBS_DIR = "Mobs";

    /**
     * Creates all example files.
     */
    public static void createAllExamples() {
        createGlobalHostileDropsExample();
        createNBTEntityDataExamples();
        createNBTEquipmentExamples();
        createNBTAdvancedExamples();
    }

    /**
     * Creates the comprehensive Global_Hostile_Drops.example file with all possible fields.
     */
    public static void createGlobalHostileDropsExample() {
        try {
            Path exampleFile = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, NORMAL_DROPS_DIR, "Global_Hostile_Drops.example");

            String exampleJson = "[\n" +
                    "  {\n" +
                    "    \"itemId\": \"minecraft:golden_apple\",\n" +
                    "    \"dropChance\": 0.5,\n" +
                    "    \"minAmount\": 1,\n" +
                    "    \"maxAmount\": 1,\n" +
                    "    \"nbtData\": \"{Enchantments:[{id:\\\"minecraft:sharpness\\\",lvl:5s}]}\",\n" +
                    "    \"requiredAdvancement\": \"minecraft:story/mine_diamond\",\n" +
                    "    \"requiredEffect\": \"minecraft:strength\",\n" +
                    "    \"requiredEquipment\": \"minecraft:diamond_sword\",\n" +
                    "    \"requiredWeather\": \"rain\",\n" +
                    "    \"requiredTime\": \"night\",\n" +
                    "    \"requiredDimension\": \"minecraft:overworld\",\n" +
                    "    \"requiredBiome\": \"minecraft:forest\",\n" +
                    "    \"command\": \"effect give {player} minecraft:regeneration 30 1\",\n" +
                    "    \"commandChance\": 50.0,\n" +
                    "    \"commandCoolDown\": 300,\n" +
                    "    \"dropCommand\": \"playsound minecraft:entity.player.levelup player {player}\",\n" +
                    "    \"dropCommandChance\": 100.0,\n" +
                    "    \"_comment\": \"Comprehensive example with all possible fields\",\n" +
                    "    \"requirePlayerKill\": true,\n" +
                    "    \"allowDefaultDrops\": true,\n" +
                    "    \"allowModIDs\": [\"examplemod:special_item\"],\n" +
                    "    \"extraDropChance\": 10.0,\n" +
                    "    \"extraAmountMin\": 1,\n" +
                    "    \"extraAmountMax\": 3\n" +
                    "  }\n" +
                    "]";

            Files.writeString(exampleFile, exampleJson, StandardCharsets.UTF_8);
            LOGGER.info("Created Global_Hostile_Drops.example");

        } catch (Exception e) {
            LOGGER.error("Failed to create Global_Hostile_Drops.example", e);
        }
    }

    /**
     * Creates NBT entity data examples file with MMORPG mod examples.
     */
    public static void createNBTEntityDataExamples() {
        try {
            Path exampleFile = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, NORMAL_DROPS_DIR, "NBT_Entity_Data_Examples.example");

            String exampleJson = "[\n" +
                    "  {\n" +
                    "    \"_comment\": \"Example 1: MMORPG Health Check - Drop diamonds from mobs with health < 100\",\n" +
                    "    \"itemId\": \"minecraft:air\",\n" +
                    "    \"dropChance\": 0.0,\n" +
                    "    \"minAmount\": 0,\n" +
                    "    \"maxAmount\": 0,\n" +
                    "    \"nbtEntityData\": \"ForgeCaps.mmorpg:entity_data.hp\",\n" +
                    "    \"nbtDataCondition\": \"<\",\n" +
                    "    \"nbtDataValue\": \"100\",\n" +
                    "    \"nbtEntityDrop\": \"minecraft:diamond\",\n" +
                    "    \"nbtEntityDropChance\": 100.0,\n" +
                    "    \"nbtEntityDropMin\": 1,\n" +
                    "    \"nbtEntityDropMax\": 3,\n" +
                    "    \"requirePlayerKill\": true\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"_comment\": \"Example 2: MMORPG Level Check - Drop emeralds from high-level mobs (>= 50)\",\n" +
                    "    \"itemId\": \"minecraft:air\",\n" +
                    "    \"dropChance\": 0.0,\n" +
                    "    \"minAmount\": 0,\n" +
                    "    \"maxAmount\": 0,\n" +
                    "    \"nbtEntityData\": \"ForgeCaps.mmorpg:entity_data.level\",\n" +
                    "    \"nbtDataCondition\": \">=\",\n" +
                    "    \"nbtDataValue\": \"50\",\n" +
                    "    \"nbtEntityDrop\": \"minecraft:emerald\",\n" +
                    "    \"nbtEntityDropChance\": 75.0,\n" +
                    "    \"nbtEntityDropMin\": 2,\n" +
                    "    \"nbtEntityDropMax\": 5,\n" +
                    "    \"command\": \"tellraw {player} {\\\"text\\\":\\\"You defeated a high-level mob!\\\",\\\"color\\\":\\\"gold\\\"}\",\n" +
                    "    \"commandChance\": 100.0\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"_comment\": \"Example 3: MMORPG Rarity Check - Drop nether star from legendary mobs\",\n" +
                    "    \"itemId\": \"minecraft:air\",\n" +
                    "    \"dropChance\": 0.0,\n" +
                    "    \"minAmount\": 0,\n" +
                    "    \"maxAmount\": 0,\n" +
                    "    \"nbtEntityData\": \"ForgeCaps.mmorpg:entity_data.rarity\",\n" +
                    "    \"nbtDataCondition\": \"==\",\n" +
                    "    \"nbtDataValue\": \"legendary\",\n" +
                    "    \"nbtEntityDrop\": \"minecraft:nether_star\",\n" +
                    "    \"nbtEntityDropChance\": 100.0,\n" +
                    "    \"nbtEntityDropMin\": 1,\n" +
                    "    \"nbtEntityDropMax\": 1,\n" +
                    "    \"dropCommand\": \"playsound minecraft:ui.toast.challenge_complete player {player}\",\n" +
                    "    \"dropCommandChance\": 100.0\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"_comment\": \"Example 4: MMORPG Boss Check - Drop special loot from boss mobs\",\n" +
                    "    \"itemId\": \"minecraft:air\",\n" +
                    "    \"dropChance\": 0.0,\n" +
                    "    \"minAmount\": 0,\n" +
                    "    \"maxAmount\": 0,\n" +
                    "    \"nbtEntityData\": \"ForgeCaps.mmorpg:entity_data.is_boss\",\n" +
                    "    \"nbtDataCondition\": \"==\",\n" +
                    "    \"nbtDataValue\": \"true\",\n" +
                    "    \"nbtEntityDrop\": \"minecraft:enchanted_golden_apple\",\n" +
                    "    \"nbtEntityDropChance\": 100.0,\n" +
                    "    \"nbtEntityDropMin\": 1,\n" +
                    "    \"nbtEntityDropMax\": 2,\n" +
                    "    \"command\": \"tellraw @a {\\\"text\\\":\\\"A boss has been defeated!\\\",\\\"color\\\":\\\"red\\\",\\\"bold\\\":true}\",\n" +
                    "    \"commandChance\": 100.0\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"_comment\": \"Example 5: Vanilla Health Check - Drop from low health mobs\",\n" +
                    "    \"itemId\": \"minecraft:air\",\n" +
                    "    \"dropChance\": 0.0,\n" +
                    "    \"minAmount\": 0,\n" +
                    "    \"maxAmount\": 0,\n" +
                    "    \"nbtEntityData\": \"Health\",\n" +
                    "    \"nbtDataCondition\": \"<\",\n" +
                    "    \"nbtDataValue\": \"10\",\n" +
                    "    \"nbtEntityDrop\": \"minecraft:golden_apple\",\n" +
                    "    \"nbtEntityDropChance\": 50.0,\n" +
                    "    \"nbtEntityDropMin\": 1,\n" +
                    "    \"nbtEntityDropMax\": 1\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"_comment\": \"Example 6: Custom Name Check - Drop from named mobs\",\n" +
                    "    \"itemId\": \"minecraft:air\",\n" +
                    "    \"dropChance\": 0.0,\n" +
                    "    \"minAmount\": 0,\n" +
                    "    \"maxAmount\": 0,\n" +
                    "    \"nbtEntityData\": \"CustomName\",\n" +
                    "    \"nbtDataCondition\": \"contains\",\n" +
                    "    \"nbtDataValue\": \"Boss\",\n" +
                    "    \"nbtEntityDrop\": \"minecraft:diamond\",\n" +
                    "    \"nbtEntityDropChance\": 100.0,\n" +
                    "    \"nbtEntityDropMin\": 5,\n" +
                    "    \"nbtEntityDropMax\": 10\n" +
                    "  }\n" +
                    "]";

            Files.writeString(exampleFile, exampleJson, StandardCharsets.UTF_8);
            LOGGER.info("Created NBT_Entity_Data_Examples.example");

        } catch (Exception e) {
            LOGGER.error("Failed to create NBT_Entity_Data_Examples.example", e);
        }
    }

    /**
     * Creates NBT equipment check examples (HandItems, ArmorItems).
     */
    public static void createNBTEquipmentExamples() {
        try {
            Path exampleFile = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, NORMAL_DROPS_DIR, MOBS_DIR, "NBT_Equipment_Examples.example");

            String exampleJson = "[\n" +
                    "  {\n" +
                    "    \"_comment\": \"Example 1: Skeleton holding a bow - Drop extra arrows\",\n" +
                    "    \"entityId\": \"minecraft:skeleton\",\n" +
                    "    \"itemId\": \"minecraft:air\",\n" +
                    "    \"dropChance\": 0.0,\n" +
                    "    \"minAmount\": 0,\n" +
                    "    \"maxAmount\": 0,\n" +
                    "    \"nbtEntityData\": \"HandItems[0].id\",\n" +
                    "    \"nbtDataCondition\": \"==\",\n" +
                    "    \"nbtDataValue\": \"minecraft:bow\",\n" +
                    "    \"nbtEntityDrop\": \"minecraft:arrow\",\n" +
                    "    \"nbtEntityDropChance\": 100.0,\n" +
                    "    \"nbtEntityDropMin\": 16,\n" +
                    "    \"nbtEntityDropMax\": 32,\n" +
                    "    \"requirePlayerKill\": true\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"_comment\": \"Example 2: Zombie holding a sword - Drop iron ingots\",\n" +
                    "    \"entityId\": \"minecraft:zombie\",\n" +
                    "    \"itemId\": \"minecraft:air\",\n" +
                    "    \"dropChance\": 0.0,\n" +
                    "    \"minAmount\": 0,\n" +
                    "    \"maxAmount\": 0,\n" +
                    "    \"nbtEntityData\": \"HandItems[0].id\",\n" +
                    "    \"nbtDataCondition\": \"contains\",\n" +
                    "    \"nbtDataValue\": \"sword\",\n" +
                    "    \"nbtEntityDrop\": \"minecraft:iron_ingot\",\n" +
                    "    \"nbtEntityDropChance\": 75.0,\n" +
                    "    \"nbtEntityDropMin\": 2,\n" +
                    "    \"nbtEntityDropMax\": 5\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"_comment\": \"Example 3: Mob wearing diamond helmet - Drop diamonds\",\n" +
                    "    \"itemId\": \"minecraft:air\",\n" +
                    "    \"dropChance\": 0.0,\n" +
                    "    \"minAmount\": 0,\n" +
                    "    \"maxAmount\": 0,\n" +
                    "    \"nbtEntityData\": \"ArmorItems[3].id\",\n" +
                    "    \"nbtDataCondition\": \"==\",\n" +
                    "    \"nbtDataValue\": \"minecraft:diamond_helmet\",\n" +
                    "    \"nbtEntityDrop\": \"minecraft:diamond\",\n" +
                    "    \"nbtEntityDropChance\": 100.0,\n" +
                    "    \"nbtEntityDropMin\": 1,\n" +
                    "    \"nbtEntityDropMax\": 3\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"_comment\": \"Example 4: Mob wearing any armor - Drop leather\",\n" +
                    "    \"itemId\": \"minecraft:air\",\n" +
                    "    \"dropChance\": 0.0,\n" +
                    "    \"minAmount\": 0,\n" +
                    "    \"maxAmount\": 0,\n" +
                    "    \"nbtEntityData\": \"ArmorItems[0].id\",\n" +
                    "    \"nbtDataCondition\": \"!=\",\n" +
                    "    \"nbtDataValue\": \"\",\n" +
                    "    \"nbtEntityDrop\": \"minecraft:leather\",\n" +
                    "    \"nbtEntityDropChance\": 50.0,\n" +
                    "    \"nbtEntityDropMin\": 1,\n" +
                    "    \"nbtEntityDropMax\": 2\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"_comment\": \"Example 5: Piglin holding gold - Drop extra gold\",\n" +
                    "    \"entityId\": \"minecraft:piglin\",\n" +
                    "    \"itemId\": \"minecraft:air\",\n" +
                    "    \"dropChance\": 0.0,\n" +
                    "    \"minAmount\": 0,\n" +
                    "    \"maxAmount\": 0,\n" +
                    "    \"nbtEntityData\": \"HandItems[0].id\",\n" +
                    "    \"nbtDataCondition\": \"contains\",\n" +
                    "    \"nbtDataValue\": \"gold\",\n" +
                    "    \"nbtEntityDrop\": \"minecraft:gold_ingot\",\n" +
                    "    \"nbtEntityDropChance\": 100.0,\n" +
                    "    \"nbtEntityDropMin\": 3,\n" +
                    "    \"nbtEntityDropMax\": 7\n" +
                    "  }\n" +
                    "]";

            Files.writeString(exampleFile, exampleJson, StandardCharsets.UTF_8);
            LOGGER.info("Created NBT_Equipment_Examples.example");

        } catch (Exception e) {
            LOGGER.error("Failed to create NBT_Equipment_Examples.example", e);
        }
    }

    /**
     * Creates advanced NBT examples with complex conditions.
     */
    public static void createNBTAdvancedExamples() {
        try {
            Path exampleFile = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, NORMAL_DROPS_DIR, MOBS_DIR, "NBT_Advanced_Examples.example");

            String exampleJson = "[\n" +
                    "  {\n" +
                    "    \"_comment\": \"Example 1: Baby mob check - Drop milk bucket\",\n" +
                    "    \"itemId\": \"minecraft:air\",\n" +
                    "    \"dropChance\": 0.0,\n" +
                    "    \"minAmount\": 0,\n" +
                    "    \"maxAmount\": 0,\n" +
                    "    \"nbtEntityData\": \"IsBaby\",\n" +
                    "    \"nbtDataCondition\": \"==\",\n" +
                    "    \"nbtDataValue\": \"true\",\n" +
                    "    \"nbtEntityDrop\": \"minecraft:milk_bucket\",\n" +
                    "    \"nbtEntityDropChance\": 100.0,\n" +
                    "    \"nbtEntityDropMin\": 1,\n" +
                    "    \"nbtEntityDropMax\": 1\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"_comment\": \"Example 2: Mob on fire - Drop blaze powder\",\n" +
                    "    \"itemId\": \"minecraft:air\",\n" +
                    "    \"dropChance\": 0.0,\n" +
                    "    \"minAmount\": 0,\n" +
                    "    \"maxAmount\": 0,\n" +
                    "    \"nbtEntityData\": \"Fire\",\n" +
                    "    \"nbtDataCondition\": \">\",\n" +
                    "    \"nbtDataValue\": \"0\",\n" +
                    "    \"nbtEntityDrop\": \"minecraft:blaze_powder\",\n" +
                    "    \"nbtEntityDropChance\": 50.0,\n" +
                    "    \"nbtEntityDropMin\": 1,\n" +
                    "    \"nbtEntityDropMax\": 3\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"_comment\": \"Example 3: Persistent mob (won't despawn) - Drop name tag\",\n" +
                    "    \"itemId\": \"minecraft:air\",\n" +
                    "    \"dropChance\": 0.0,\n" +
                    "    \"minAmount\": 0,\n" +
                    "    \"maxAmount\": 0,\n" +
                    "    \"nbtEntityData\": \"PersistenceRequired\",\n" +
                    "    \"nbtDataCondition\": \"==\",\n" +
                    "    \"nbtDataValue\": \"true\",\n" +
                    "    \"nbtEntityDrop\": \"minecraft:name_tag\",\n" +
                    "    \"nbtEntityDropChance\": 100.0,\n" +
                    "    \"nbtEntityDropMin\": 1,\n" +
                    "    \"nbtEntityDropMax\": 1\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"_comment\": \"Example 4: Mob with no AI - Drop command block\",\n" +
                    "    \"itemId\": \"minecraft:air\",\n" +
                    "    \"dropChance\": 0.0,\n" +
                    "    \"minAmount\": 0,\n" +
                    "    \"maxAmount\": 0,\n" +
                    "    \"nbtEntityData\": \"NoAI\",\n" +
                    "    \"nbtDataCondition\": \"==\",\n" +
                    "    \"nbtDataValue\": \"true\",\n" +
                    "    \"nbtEntityDrop\": \"minecraft:command_block\",\n" +
                    "    \"nbtEntityDropChance\": 100.0,\n" +
                    "    \"nbtEntityDropMin\": 1,\n" +
                    "    \"nbtEntityDropMax\": 1\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"_comment\": \"Example 5: Old mob (high age) - Drop experience bottle\",\n" +
                    "    \"itemId\": \"minecraft:air\",\n" +
                    "    \"dropChance\": 0.0,\n" +
                    "    \"minAmount\": 0,\n" +
                    "    \"maxAmount\": 0,\n" +
                    "    \"nbtEntityData\": \"Age\",\n" +
                    "    \"nbtDataCondition\": \">\",\n" +
                    "    \"nbtDataValue\": \"6000\",\n" +
                    "    \"nbtEntityDrop\": \"minecraft:experience_bottle\",\n" +
                    "    \"nbtEntityDropChance\": 75.0,\n" +
                    "    \"nbtEntityDropMin\": 1,\n" +
                    "    \"nbtEntityDropMax\": 5\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"_comment\": \"Example 6: Combine multiple conditions - MMORPG boss with low health\",\n" +
                    "    \"itemId\": \"minecraft:air\",\n" +
                    "    \"dropChance\": 0.0,\n" +
                    "    \"minAmount\": 0,\n" +
                    "    \"maxAmount\": 0,\n" +
                    "    \"nbtEntityData\": \"ForgeCaps.mmorpg:entity_data.is_boss\",\n" +
                    "    \"nbtDataCondition\": \"==\",\n" +
                    "    \"nbtDataValue\": \"true\",\n" +
                    "    \"nbtEntityDrop\": \"minecraft:totem_of_undying\",\n" +
                    "    \"nbtEntityDropChance\": 100.0,\n" +
                    "    \"nbtEntityDropMin\": 1,\n" +
                    "    \"nbtEntityDropMax\": 1,\n" +
                    "    \"command\": \"tellraw @a {\\\"text\\\":\\\"A boss dropped a Totem of Undying!\\\",\\\"color\\\":\\\"gold\\\",\\\"bold\\\":true}\",\n" +
                    "    \"commandChance\": 100.0\n" +
                    "  }\n" +
                    "]";

            Files.writeString(exampleFile, exampleJson, StandardCharsets.UTF_8);
            LOGGER.info("Created NBT_Advanced_Examples.example");

        } catch (Exception e) {
            LOGGER.error("Failed to create NBT_Advanced_Examples.example", e);
        }
    }
}