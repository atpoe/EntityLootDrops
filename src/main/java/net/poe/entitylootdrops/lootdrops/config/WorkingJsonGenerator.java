package net.poe.entitylootdrops.lootdrops.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates working .json files for immediate use.
 * These files contain functional configurations that can be used as-is.
 */
public class WorkingJsonGenerator {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String CONFIG_DIR = "config/EntityLootDrops";
    private static final String LOOT_DROPS_DIR = "Loot Drops";
    private static final String NORMAL_DROPS_DIR = "Normal Drops";
    private static final String EVENT_DROPS_DIR = "Event Drops";
    private static final String MOBS_DIR = "Mobs";

    /**
     * Creates all working JSON files.
     */
    public static void createAllWorkingFiles() {
        createGlobalHostileDropsFile();
        createNormalMobsExamples();
    }

    /**
     * Creates the Global_Hostile_Drops.json file.
     */
    public static void createGlobalHostileDropsFile() {
        try {
            Path globalFile = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, NORMAL_DROPS_DIR, "Global_Hostile_Drops.json");

            String exampleJson = "[\n" +
                    "  {\n" +
                    "    \"requiredDimension\": \"minecraft:overworld\",\n" +
                    "    \"itemId\": \"minecraft:diamond\",\n" +
                    "    \"dropChance\": 5.0,\n" +
                    "    \"minAmount\": 1,\n" +
                    "    \"maxAmount\": 1,\n" +
                    "    \"command\": \"effect give {player} minecraft:speed 10 1\",\n" +
                    "    \"commandChance\": 100.0,\n" +
                    "    \"commandCoolDown\": 300,\n" +
                    "    \"_comment\": \"Speed effect on diamond drop\",\n" +
                    "    \"requirePlayerKill\": true,\n" +
                    "    \"extraDropChance\": 10.0,\n" +
                    "    \"extraAmountMin\": 1,\n" +
                    "    \"extraAmountMax\": 3\n" +
                    "  }\n" +
                    "]";

            Files.writeString(globalFile, exampleJson, StandardCharsets.UTF_8);
            LOGGER.info("Created Global_Hostile_Drops.json");

        } catch (Exception e) {
            LOGGER.error("Failed to create Global_Hostile_Drops.json", e);
        }
    }

    /**
     * Creates example files for normal mobs.
     */
    public static void createNormalMobsExamples() {
        LOGGER.info("Creating normal mobs examples...");

        try {
            Path mobsDir = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, NORMAL_DROPS_DIR, MOBS_DIR);

            // Create Zombie example
            Path zombieFile = mobsDir.resolve("Zombie_Example.json");
            String zombieJson = "[\n" +
                    "  {\n" +
                    "    \"entityId\": \"minecraft:zombie\",\n" +
                    "    \"itemId\": \"minecraft:iron_ingot\",\n" +
                    "    \"dropChance\": 10.0,\n" +
                    "    \"minAmount\": 1,\n" +
                    "    \"maxAmount\": 2,\n" +
                    "    \"_comment\": \"Iron ingot from zombies\",\n" +
                    "    \"requirePlayerKill\": true\n" +
                    "  }\n" +
                    "]";

            Files.writeString(zombieFile, zombieJson, StandardCharsets.UTF_8);
            LOGGER.info("Created Zombie_Example.json");

            // Create Skeleton example
            Path skeletonFile = mobsDir.resolve("Skeleton_Example.json");
            String skeletonJson = "[\n" +
                    "  {\n" +
                    "    \"entityId\": \"minecraft:skeleton\",\n" +
                    "    \"itemId\": \"minecraft:flint\",\n" +
                    "    \"dropChance\": 15.0,\n" +
                    "    \"minAmount\": 1,\n" +
                    "    \"maxAmount\": 3,\n" +
                    "    \"_comment\": \"Flint from skeletons\",\n" +
                    "    \"requirePlayerKill\": false\n" +
                    "  }\n" +
                    "]";

            Files.writeString(skeletonFile, skeletonJson, StandardCharsets.UTF_8);
            LOGGER.info("Created Skeleton_Example.json");

        } catch (Exception e) {
            LOGGER.error("Failed to create normal mobs examples", e);
        }
    }

    /**
     * Creates example files for event drops.
     */
    public static void createEventDropsExample(String eventType) {
        LOGGER.info("Creating event drops example for: {}", eventType);

        try {
            Path eventDir = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, EVENT_DROPS_DIR, eventType);
            Path exampleFile = eventDir.resolve(eventType + "_Event_Drops_Example.json");

            String exampleJson = "";
            switch (eventType) {
                case "Winter":
                    exampleJson = "[\n" +
                            "  {\n" +
                            "    \"itemId\": \"minecraft:snowball\",\n" +
                            "    \"dropChance\": 20.0,\n" +
                            "    \"minAmount\": 2,\n" +
                            "    \"maxAmount\": 5,\n" +
                            "    \"_comment\": \"Winter event snowballs\",\n" +
                            "    \"requirePlayerKill\": true\n" +
                            "  }\n" +
                            "]";
                    break;
                case "Summer":
                    exampleJson = "[\n" +
                            "  {\n" +
                            "    \"itemId\": \"minecraft:sunflower\",\n" +
                            "    \"dropChance\": 15.0,\n" +
                            "    \"minAmount\": 1,\n" +
                            "    \"maxAmount\": 2,\n" +
                            "    \"_comment\": \"Summer event sunflowers\",\n" +
                            "    \"requirePlayerKill\": true\n" +
                            "  }\n" +
                            "]";
                    break;
                case "Easter":
                    exampleJson = "[\n" +
                            "  {\n" +
                            "    \"itemId\": \"minecraft:egg\",\n" +
                            "    \"dropChance\": 25.0,\n" +
                            "    \"minAmount\": 1,\n" +
                            "    \"maxAmount\": 3,\n" +
                            "    \"_comment\": \"Easter event eggs\",\n" +
                            "    \"requirePlayerKill\": true\n" +
                            "  }\n" +
                            "]";
                    break;
                case "Halloween":
                    exampleJson = "[\n" +
                            "  {\n" +
                            "    \"itemId\": \"minecraft:pumpkin\",\n" +
                            "    \"dropChance\": 30.0,\n" +
                            "    \"minAmount\": 1,\n" +
                            "    \"maxAmount\": 2,\n" +
                            "    \"_comment\": \"Halloween event pumpkins\",\n" +
                            "    \"requirePlayerKill\": true\n" +
                            "  }\n" +
                            "]";
                    break;
            }

            Files.writeString(exampleFile, exampleJson, StandardCharsets.UTF_8);
            LOGGER.info("Created {}_Event_Drops_Example.json", eventType);

        } catch (Exception e) {
            LOGGER.error("Failed to create event drops example for {}", eventType, e);
        }
    }

    /**
     * Creates example files for event mobs.
     */
    public static void createEventMobsExample(String eventType) {
        LOGGER.info("Creating event mobs example for: {}", eventType);

        try {
            Path eventMobsDir = Paths.get(CONFIG_DIR, LOOT_DROPS_DIR, EVENT_DROPS_DIR, eventType, MOBS_DIR);
            Path exampleFile = eventMobsDir.resolve(eventType + "_Mob_Drops_Example.json");

            String exampleJson = "";
            switch (eventType) {
                case "Winter":
                    exampleJson = "[\n" +
                            "  {\n" +
                            "    \"entityId\": \"minecraft:zombie\",\n" +
                            "    \"itemId\": \"minecraft:ice\",\n" +
                            "    \"dropChance\": 12.0,\n" +
                            "    \"minAmount\": 1,\n" +
                            "    \"maxAmount\": 2,\n" +
                            "    \"_comment\": \"Winter event ice from zombie\",\n" +
                            "    \"requirePlayerKill\": true\n" +
                            "  }\n" +
                            "]";
                    break;
                case "Summer":
                    exampleJson = "[\n" +
                            "  {\n" +
                            "    \"entityId\": \"minecraft:skeleton\",\n" +
                            "    \"itemId\": \"minecraft:melon_slice\",\n" +
                            "    \"dropChance\": 18.0,\n" +
                            "    \"minAmount\": 2,\n" +
                            "    \"maxAmount\": 4,\n" +
                            "    \"_comment\": \"Summer event melon from skeleton\",\n" +
                            "    \"requirePlayerKill\": true\n" +
                            "  }\n" +
                            "]";
                    break;
                case "Easter":
                    exampleJson = "[\n" +
                            "  {\n" +
                            "    \"entityId\": \"minecraft:chicken\",\n" +
                            "    \"itemId\": \"minecraft:golden_egg\",\n" +
                            "    \"dropChance\": 5.0,\n" +
                            "    \"minAmount\": 1,\n" +
                            "    \"maxAmount\": 1,\n" +
                            "    \"_comment\": \"Easter event golden egg from chicken\",\n" +
                            "    \"requirePlayerKill\": false\n" +
                            "  }\n" +
                            "]";
                    break;
                case "Halloween":
                    exampleJson = "[\n" +
                            "  {\n" +
                            "    \"entityId\": \"minecraft:zombie\",\n" +
                            "    \"itemId\": \"minecraft:jack_o_lantern\",\n" +
                            "    \"dropChance\": 8.0,\n" +
                            "    \"minAmount\": 1,\n" +
                            "    \"maxAmount\": 1,\n" +
                            "    \"_comment\": \"Halloween event jack o lantern from zombie\",\n" +
                            "    \"requirePlayerKill\": true\n" +
                            "  }\n" +
                            "]";
                    break;
            }

            Files.writeString(exampleFile, exampleJson, StandardCharsets.UTF_8);
            LOGGER.info("Created {}_Mob_Drops_Example.json", eventType);

        } catch (Exception e) {
            LOGGER.error("Failed to create event mobs example for {}", eventType, e);
        }
    }
}