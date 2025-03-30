package net.poe.entitylootdrops;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class LootConfig {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR = "entitylootdrops";
    private static final String DROPS_DIR = "drops";
    private static final String WINTER_DIR = "event_winter";
    private static final String EASTER_DIR = "event_easter";
    private static final String HALLOWEEN_DIR = "event_halloween";
    private static final String SUMMER_DIR = "event_summer";

    // Map of entity ID to list of custom drops
    private static Map<String, List<CustomDrop>> entityDrops = new HashMap<>();
    private static Map<String, List<CustomDrop>> winterDrops = new HashMap<>();
    private static Map<String, List<CustomDrop>> easterDrops = new HashMap<>();
    private static Map<String, List<CustomDrop>> halloweenDrops = new HashMap<>();
    private static Map<String, List<CustomDrop>> summerDrops = new HashMap<>();

    // Event flags
    private static boolean dropchanceEnabled = false;
    private static boolean winterEventEnabled = false;
    private static boolean easterEventEnabled = false;
    private static boolean halloweenEventEnabled = false;
    private static boolean summerEventEnabled = false;

    // Inner class for mob drops configuration
    public static class MobDrops {
        public String entityId;
        public List<CustomDrop> drops;

        public MobDrops(String entityId, List<CustomDrop> drops) {
            this.entityId = entityId;
            this.drops = drops;
        }
    }

    // Inner class for custom drop configuration
    public static class CustomDrop {
        private String itemId;
        private float dropRate;
        private int minCount;
        private int maxCount;

        public CustomDrop(String itemId, float dropRate, int minCount, int maxCount) {
            this.itemId = itemId;
            // Ensure drop rate is between 0 and 100
            this.dropRate = Math.max(0.0f, Math.min(100.0f, dropRate));
            this.minCount = Math.max(0, minCount);
            this.maxCount = Math.max(minCount, maxCount);
        }

        public String getItemId() { return itemId; }
        
        public float getDropRate() { 
            float rate = dropRate / 100.0f; // Convert percentage to decimal (e.g., 100.0 -> 1.0)
            if (LootConfig.isDropchanceEnabled()) {
                rate = Math.min(1.0f, rate * 2.0f); // Double the rate but cap at 100%
            }
            return rate;
        }
        
        public int getMinCount() { return minCount; }
        public int getMaxCount() { return maxCount; }
        
        public Item getItem() {
            ResourceLocation resourceLocation = new ResourceLocation(itemId);
            return ForgeRegistries.ITEMS.getValue(resourceLocation);
        }
    }

    public static void loadConfig() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(CONFIG_DIR);
        Path dropsDir = configDir.resolve(DROPS_DIR);
        Path winterDir = configDir.resolve(WINTER_DIR);
        Path easterDir = configDir.resolve(EASTER_DIR);
        Path halloweenDir = configDir.resolve(HALLOWEEN_DIR);
        Path summerDir = configDir.resolve(SUMMER_DIR);
        
        // Create directories if they don't exist
        try {
            Files.createDirectories(dropsDir);
            Files.createDirectories(winterDir);
            Files.createDirectories(easterDir);
            Files.createDirectories(halloweenDir);
            Files.createDirectories(summerDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create config directories", e);
            return;
        }
        
        // Create example configs if directories are empty
        if (!directoryHasFiles(dropsDir)) {
            extractDefaultConfigs(dropsDir, "drops");
        }
        if (!directoryHasFiles(winterDir)) {
            extractDefaultConfigs(winterDir, "event_winter");
        }
        if (!directoryHasFiles(easterDir)) {
            extractDefaultConfigs(easterDir, "event_easter");
        }
        if (!directoryHasFiles(halloweenDir)) {
            extractDefaultConfigs(halloweenDir, "event_halloween");
        }
        if (!directoryHasFiles(summerDir)) {
            extractDefaultConfigs(summerDir, "event_summer");
        }
        
        // Load all drops
        entityDrops.clear();
        winterDrops.clear();
        easterDrops.clear();
        halloweenDrops.clear();
        summerDrops.clear();
        
        loadDropsFromDirectory(dropsDir, entityDrops);
        loadDropsFromDirectory(winterDir, winterDrops);
        loadDropsFromDirectory(easterDir, easterDrops);
        loadDropsFromDirectory(halloweenDir, halloweenDrops);
        loadDropsFromDirectory(summerDir, summerDrops);
        
        LOGGER.info("Loaded {} regular drop configs, {} winter event drops, {} easter event drops, {} halloween event drops, {} summer event drops", 
            entityDrops.size(), winterDrops.size(), easterDrops.size(), halloweenDrops.size(), summerDrops.size());
    }
    
    private static void loadDropsFromDirectory(Path directory, Map<String, List<CustomDrop>> dropsMap) {
        if (!Files.exists(directory)) return;
        
        try (Stream<Path> paths = Files.list(directory)) {
            paths.filter(path -> path.toString().endsWith(".json"))
                 .forEach(path -> loadDropConfig(path, dropsMap));
        } catch (IOException e) {
            LOGGER.error("Error loading drops from {}", directory, e);
        }
    }

    private static void loadDropConfig(Path configPath, Map<String, List<CustomDrop>> dropsMap) {
        try (Reader reader = Files.newBufferedReader(configPath)) {
            MobDrops mobDrops = GSON.fromJson(reader, MobDrops.class);
            
            if (mobDrops != null && mobDrops.entityId != null && mobDrops.drops != null) {
                String entityId = mobDrops.entityId;
                if (!entityId.contains(":")) {
                    entityId = "minecraft:" + entityId;
                }
                dropsMap.put(entityId, mobDrops.drops);
                LOGGER.debug("Loaded {} drops for entity {}", mobDrops.drops.size(), entityId);
            }
        } catch (Exception e) {
            LOGGER.error("Error loading drop config from {}", configPath, e);
        }
    }

    private static boolean directoryHasFiles(Path dir) {
        try (Stream<Path> paths = Files.list(dir)) {
            return paths.filter(path -> path.toString().endsWith(".json")).findAny().isPresent();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Extract default configuration files from the JAR to the specified directory
     */
    private static void extractDefaultConfigs(Path targetDir, String configType) {
        boolean extracted = false;
        
        // Try to extract from the config files inside the JAR
        try {
            // Get the classloader to access resources
            ClassLoader classLoader = LootConfig.class.getClassLoader();
            
            // Define the files to extract based on config type
            String[] filesToExtract;
            switch (configType) {
                case "drops":
                case "event_winter":
                case "event_easter":
                case "event_halloween":
                case "event_summer":
                    filesToExtract = new String[] {
                        "zombie.json", "skeleton.json", "README.txt"
                    };
                    break;
                default:
                    LOGGER.warn("Unknown config type: {}", configType);
                    return;
            }
            
            // Extract each file
            for (String fileName : filesToExtract) {
                String resourcePath = "config/" + configType + "/" + fileName;
                try (InputStream is = classLoader.getResourceAsStream(resourcePath)) {
                    if (is != null) {
                        Files.copy(is, targetDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                        extracted = true;
                    }
                }
            }
            
            if (extracted) {
                LOGGER.info("Extracted default {} configuration files to {}", configType, targetDir);
                return;
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to extract default configuration files: {}", e.getMessage());
        }
        
        // If extraction failed, fall back to creating example configs
        LOGGER.info("Creating example {} configuration files in {}", configType, targetDir);
        switch (configType) {
            case "drops":
                createExampleConfigs(targetDir);
                break;
            case "event_winter":
                createExampleWinterConfigs(targetDir);
                break;
            case "event_easter":
                createExampleEasterConfigs(targetDir);
                break;
            case "event_halloween":
                createExampleHalloweenConfigs(targetDir);
                break;
            case "event_summer":
                createExampleSummerConfigs(targetDir);
                break;
        }
    }

    // Create example configs for regular drops
    private static void createExampleConfigs(Path dropsDir) {
        createExampleZombieConfig(dropsDir.resolve("zombie.json"));
        createExampleSkeletonConfig(dropsDir.resolve("skeleton.json"));
        createReadme(dropsDir.resolve("README.txt"));
    }
    
    // Create example configs for winter drops
    private static void createExampleWinterConfigs(Path winterDir) {
        createExampleWinterZombieConfig(winterDir.resolve("zombie.json"));
        createExampleWinterSkeletonConfig(winterDir.resolve("skeleton.json"));
        createWinterReadme(winterDir.resolve("README.txt"));
    }
    
    // Create example configs for Easter drops
    private static void createExampleEasterConfigs(Path easterDir) {
        createExampleEasterZombieConfig(easterDir.resolve("zombie.json"));
        createExampleEasterSkeletonConfig(easterDir.resolve("skeleton.json"));
        createEasterReadme(easterDir.resolve("README.txt"));
    }

    // Create example configs for Halloween drops
    private static void createExampleHalloweenConfigs(Path halloweenDir) {
        createExampleHalloweenZombieConfig(halloweenDir.resolve("zombie.json"));
        createExampleHalloweenSkeletonConfig(halloweenDir.resolve("skeleton.json"));
        createHalloweenReadme(halloweenDir.resolve("README.txt"));
    }

    // Create example configs for Summer drops
    private static void createExampleSummerConfigs(Path summerDir) {
        createExampleSummerZombieConfig(summerDir.resolve("zombie.json"));
        createExampleSummerSkeletonConfig(summerDir.resolve("skeleton.json"));
        createSummerReadme(summerDir.resolve("README.txt"));
    }

    private static void createExampleZombieConfig(Path path) {
        String json = """
            {
              "entityId": "minecraft:zombie",
              "drops": [
                {
                  "itemId": "minecraft:diamond",
                  "dropRate": 10.0,
                  "minCount": 1,
                  "maxCount": 3
                },
                {
                  "itemId": "minecraft:emerald",
                  "dropRate": 20.0,
                  "minCount": 1,
                  "maxCount": 2
                }
              ]
            }
            """;
        
        try {
            Files.writeString(path, json);
        } catch (IOException e) {
            LOGGER.error("Failed to create example zombie config", e);
        }
    }
    
    private static void createExampleSkeletonConfig(Path path) {
        String json = """
            {
              "entityId": "minecraft:skeleton",
              "drops": [
                {
                  "itemId": "minecraft:gold_ingot",
                  "dropRate": 15.0,
                  "minCount": 1,
                  "maxCount": 2
                },
                {
                  "itemId": "minecraft:bone_meal",
                  "dropRate": 50.0,
                  "minCount": 2,
                  "maxCount": 5
                }
              ]
            }
            """;
        
        try {
            Files.writeString(path, json);
        } catch (IOException e) {
            LOGGER.error("Failed to create example skeleton config", e);
        }
    }
    
    private static void createExampleWinterZombieConfig(Path path) {
        String json = """
            {
              "entityId": "minecraft:zombie",
              "drops": [
                {
                  "itemId": "minecraft:snow_block",
                  "dropRate": 30.0,
                  "minCount": 1,
                  "maxCount": 3
                },
                {
                  "itemId": "minecraft:ice",
                  "dropRate": 20.0,
                  "minCount": 1,
                  "maxCount": 2
                }
              ]
            }
            """;
        
        try {
            Files.writeString(path, json);
        } catch (IOException e) {
            LOGGER.error("Failed to create example winter zombie config", e);
        }
    }
    
    private static void createExampleWinterSkeletonConfig(Path path) {
        String json = """
            {
              "entityId": "minecraft:skeleton",
              "drops": [
                {
                  "itemId": "minecraft:blue_ice",
                  "dropRate": 15.0,
                  "minCount": 1,
                  "maxCount": 2
                },
                {
                  "itemId": "minecraft:snowball",
                  "dropRate": 50.0,
                  "minCount": 3,
                  "maxCount": 8
                }
              ]
            }
            """;
        
        try {
            Files.writeString(path, json);
        } catch (IOException e) {
            LOGGER.error("Failed to create example winter skeleton config", e);
        }
    }
    
    private static void createExampleEasterZombieConfig(Path path) {
        String json = """
            {
              "entityId": "minecraft:zombie",
              "drops": [
                {
                  "itemId": "minecraft:egg",
                  "dropRate": 30.0,
                  "minCount": 1,
                  "maxCount": 3
                },
                {
                  "itemId": "minecraft:carrot",
                  "dropRate": 40.0,
                  "minCount": 1,
                  "maxCount": 4
                }
              ]
            }
            """;
        
        try {
            Files.writeString(path, json);
        } catch (IOException e) {
            LOGGER.error("Failed to create example Easter zombie config", e);
        }
    }

    private static void createExampleEasterSkeletonConfig(Path path) {
        String json = """
            {
              "entityId": "minecraft:skeleton",
              "drops": [
                {
                  "itemId": "minecraft:rabbit_foot",
                  "dropRate": 15.0,
                  "minCount": 1,
                  "maxCount": 2
                },
                {
                  "itemId": "minecraft:cake",
                  "dropRate": 10.0,
                  "minCount": 1,
                  "maxCount": 1
                }
              ]
            }
            """;
        
        try {
            Files.writeString(path, json);
        } catch (IOException e) {
            LOGGER.error("Failed to create example Easter skeleton config", e);
        }
    }

    private static void createExampleHalloweenZombieConfig(Path path) {
        String json = """
            {
              "entityId": "minecraft:zombie",
              "drops": [
                {
                  "itemId": "minecraft:pumpkin",
                  "dropRate": 40.0,
                  "minCount": 1,
                  "maxCount": 2
                },
                {
                  "itemId": "minecraft:jack_o_lantern",
                  "dropRate": 20.0,
                  "minCount": 1,
                  "maxCount": 1
                }
              ]
            }
            """;
        
        try {
            Files.writeString(path, json);
        } catch (IOException e) {
            LOGGER.error("Failed to create example Halloween zombie config", e);
        }
    }

    private static void createExampleHalloweenSkeletonConfig(Path path) {
        String json = """
            {
              "entityId": "minecraft:skeleton",
              "drops": [
                {
                  "itemId": "minecraft:soul_lantern",
                  "dropRate": 25.0,
                  "minCount": 1,
                  "maxCount": 2
                },
                {
                  "itemId": "minecraft:spider_eye",
                  "dropRate": 35.0,
                  "minCount": 1,
                  "maxCount": 3
                }
              ]
            }
            """;
        
        try {
            Files.writeString(path, json);
        } catch (IOException e) {
            LOGGER.error("Failed to create example Halloween skeleton config", e);
        }
    }

    private static void createExampleSummerZombieConfig(Path path) {
        String json = """
            {
              "entityId": "minecraft:zombie",
              "drops": [
                {
                  "itemId": "minecraft:melon_slice",
                  "dropRate": 45.0,
                  "minCount": 1,
                  "maxCount": 4
                },
                {
                  "itemId": "minecraft:tropical_fish",
                  "dropRate": 25.0,
                  "minCount": 1,
                  "maxCount": 2
                }
              ]
            }
            """;
        
        try {
            Files.writeString(path, json);
        } catch (IOException e) {
            LOGGER.error("Failed to create example Summer zombie config", e);
        }
    }

    private static void createExampleSummerSkeletonConfig(Path path) {
        String json = """
            {
              "entityId": "minecraft:skeleton",
              "drops": [
                {
                  "itemId": "minecraft:sea_pickle",
                  "dropRate": 30.0,
                  "minCount": 1,
                  "maxCount": 3
                },
                {
                  "itemId": "minecraft:seagrass",
                  "dropRate": 40.0,
                  "minCount": 1,
                  "maxCount": 4
                }
              ]
            }
            """;
        
        try {
            Files.writeString(path, json);
        } catch (IOException e) {
            LOGGER.error("Failed to create example Summer skeleton config", e);
        }
    }
    
    private static void createReadme(Path path) {
        String readme = """
            Entity Loot Drops Configuration
            ===============================
            
            This directory contains configuration files for custom entity drops.
            You can name the JSON files anything you want, and each file can contain
            drops for one or more entities.
            
            Configuration Format:
            {
              "entityId": "namespace:mob_id",
              "drops": [
                {
                  "itemId": "namespace:item_id",
                  "dropRate": 50.0,
                  "minCount": 1,
                  "maxCount": 3
                }
              ]
            }
            
            Properties:
            - entityId: The full entity ID (e.g., "minecraft:zombie" or "modid:custom_mob")
            - drops: Array of drop entries with the following properties:
              - itemId: The item ID to drop (e.g., "minecraft:diamond" or "modid:custom_item")
              - dropRate: Chance to drop (0.0 to 100.0, where 100.0 is 100%)
              - minCount: Minimum number of items to drop
              - maxCount: Maximum number of items to drop
            
            Drop Chance Boost Event:
            When the drop chance boost event is enabled, all drop rates are doubled (but capped at 100%).
            For example:
            - 25% becomes 50%
            - 40% becomes 80%
            - 60% becomes 100% (capped)
            
            Example:
            {
              "entityId": "minecraft:zombie",
              "drops": [
                {
                  "itemId": "minecraft:diamond",
                  "dropRate": 10.0,
                  "minCount": 1,
                  "maxCount": 3
                },
                {
                  "itemId": "minecraft:emerald",
                  "dropRate": 20.0,
                  "minCount": 1,
                  "maxCount": 2
                }
              ]
            }
            
            These drops are added to the entity's default drops, not replacing them.
            You can have multiple JSON files, and each file can contain drops for different entities.
            """;
        
        try {
            Files.writeString(path, readme);
        } catch (IOException e) {
            LOGGER.error("Failed to create README file", e);
        }
    }
    
    private static void createWinterReadme(Path path) {
        String readme = """
            Winter Event Drops Configuration
            ===============================
            
            This directory contains configuration files for winter event-specific drops.
            These drops will only be active when the winter event is enabled.
            
            Configuration Format:
            {
              "entityId": "namespace:mob_id",
              "drops": [
                {
                  "itemId": "namespace:item_id",
                  "dropRate": 50.0,
                  "minCount": 1,
                  "maxCount": 3
                }
              ]
            }
            
            Winter event drops are separate from regular drops and are not affected by the drop chance boost event.
            Use the command '/entitylootdrops winter enable' to activate winter event drops.
            """;
        
        try {
            Files.writeString(path, readme);
        } catch (IOException e) {
            LOGGER.error("Failed to create winter README file", e);
        }
    }
    
    private static void createEasterReadme(Path path) {
        String readme = """
            Easter Event Drops Configuration
            ==============================
            
            This directory contains configuration files for Easter event-specific drops.
            These drops will only be active when the Easter event is enabled.
            
            Use the command '/entitylootdrops easter enable' to activate Easter event drops.
            """;
        
        try {
            Files.writeString(path, readme);
        } catch (IOException e) {
            LOGGER.error("Failed to create Easter README file", e);
        }
    }

    private static void createHalloweenReadme(Path path) {
        String readme = """
            Halloween Event Drops Configuration
            ================================
            
            This directory contains configuration files for Halloween event-specific drops.
            These drops will only be active when the Halloween event is enabled.
            
            Use the command '/entitylootdrops halloween enable' to activate Halloween event drops.
            """;
        
        try {
            Files.writeString(path, readme);
        } catch (IOException e) {
            LOGGER.error("Failed to create Halloween README file", e);
        }
    }

    private static void createSummerReadme(Path path) {
        String readme = """
            Summer Event Drops Configuration
            =============================
            
            This directory contains configuration files for Summer event-specific drops.
            These drops will only be active when the Summer event is enabled.
            
            Use the command '/entitylootdrops summer enable' to activate Summer event drops.
            """;
        
        try {
            Files.writeString(path, readme);
        } catch (IOException e) {
            LOGGER.error("Failed to create Summer README file", e);
        }
    }

    // Event control methods
    public static Component toggleDropchance(boolean enable) {
        dropchanceEnabled = enable;
        String status = enable ? "enabled" : "disabled";
        LOGGER.info("Drop chance boost {}", status);
        return Component.literal("Drop chance boost has been " + status);
    }

    public static Component toggleWinterEvent(boolean enable) {
        winterEventEnabled = enable;
        String status = enable ? "enabled" : "disabled";
        LOGGER.info("Winter event {}", status);
        return Component.literal("Winter event has been " + status);
    }

    public static Component toggleEasterEvent(boolean enable) {
        easterEventEnabled = enable;
        String status = enable ? "enabled" : "disabled";
        LOGGER.info("Easter event {}", status);
        return Component.literal("Easter event has been " + status);
    }
    
    public static Component toggleHalloweenEvent(boolean enable) {
        halloweenEventEnabled = enable;
        String status = enable ? "enabled" : "disabled";
        LOGGER.info("Halloween event {}", status);
        return Component.literal("Halloween event has been " + status);
    }
    
    public static Component toggleSummerEvent(boolean enable) {
        summerEventEnabled = enable;
        String status = enable ? "enabled" : "disabled";
        LOGGER.info("Summer event {}", status);
        return Component.literal("Summer event has been " + status);
    }

    public static Component getEventStatus() {
        return Component.literal(String.format(
            "Events Status:\n" +
            "Drop Chance Boost: %s\n" +
            "Winter Event: %s\n" +
            "Easter Event: %s\n" +
            "Halloween Event: %s\n" +
            "Summer Event: %s",
            dropchanceEnabled ? "ENABLED" : "DISABLED",
            winterEventEnabled ? "ENABLED" : "DISABLED",
            easterEventEnabled ? "ENABLED" : "DISABLED",
            halloweenEventEnabled ? "ENABLED" : "DISABLED",
            summerEventEnabled ? "ENABLED" : "DISABLED"
        ));
    }

    // Getter methods for drops
    public static List<CustomDrop> getDropsForEntity(String entityId) {
        return entityDrops.getOrDefault(entityId, List.of());
    }

    public static List<CustomDrop> getWinterDropsForEntity(String entityId) {
        if (!winterEventEnabled) return List.of();
        return winterDrops.getOrDefault(entityId, List.of());
    }
    
    public static List<CustomDrop> getEasterDropsForEntity(String entityId) {
        if (!easterEventEnabled) return List.of();
        return easterDrops.getOrDefault(entityId, List.of());
    }
    
    public static List<CustomDrop> getHalloweenDropsForEntity(String entityId) {
        if (!halloweenEventEnabled) return List.of();
        return halloweenDrops.getOrDefault(entityId, List.of());
    }
    
    public static List<CustomDrop> getSummerDropsForEntity(String entityId) {
        if (!summerEventEnabled) return List.of();
        return summerDrops.getOrDefault(entityId, List.of());
    }

    // Event status getters
    public static boolean isDropchanceEnabled() {
        return dropchanceEnabled;
    }

    public static boolean isWinterEventEnabled() {
        return winterEventEnabled;
    }
    
    public static boolean isEasterEventEnabled() {
        return easterEventEnabled;
    }
    
    public static boolean isHalloweenEventEnabled() {
        return halloweenEventEnabled;
    }
    
    public static boolean isSummerEventEnabled() {
        return summerEventEnabled;
    }
}
