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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class LootConfig {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR = "entitylootdrops";
    private static final String DROPS_DIR = "drops";
    private static final String WINTER_DIR = "winter";
    
    // Map of entity ID to list of custom drops
    private static Map<String, List<CustomDrop>> entityDrops = new HashMap<>();
    private static Map<String, List<CustomDrop>> winterDrops = new HashMap<>();
    
    // Event flags
    private static boolean doubleDropsEnabled = false;
    private static boolean winterEventEnabled = false;

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
            this.dropRate = dropRate;
            this.minCount = minCount;
            this.maxCount = maxCount;
        }

        public String getItemId() { return itemId; }
        public float getDropRate() { return dropRate; }
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
        
        // Create directories if they don't exist
        try {
            Files.createDirectories(dropsDir);
            Files.createDirectories(winterDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create config directories", e);
            return;
        }
        
        // Create example configs if directories are empty
        if (!directoryHasFiles(dropsDir)) {
            createExampleConfigs(dropsDir);
        }
        
        if (!directoryHasFiles(winterDir)) {
            createExampleWinterConfigs(winterDir);
        }
        
        // Load regular drops
        entityDrops.clear();
        loadDropsFromDirectory(dropsDir, entityDrops);
        
        // Load winter drops
        winterDrops.clear();
        loadDropsFromDirectory(winterDir, winterDrops);
        
        LOGGER.info("Loaded {} regular drop configs and {} winter drop configs", 
            entityDrops.size(), winterDrops.size());
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
    
    private static void createExampleZombieConfig(Path path) {
        String json = """
            {
              "entityId": "minecraft:zombie",
              "drops": [
                {
                  "itemId": "minecraft:diamond",
                  "dropRate": 0.1,
                  "minCount": 1,
                  "maxCount": 3
                },
                {
                  "itemId": "minecraft:emerald",
                  "dropRate": 0.2,
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
                  "dropRate": 0.15,
                  "minCount": 1,
                  "maxCount": 2
                },
                {
                  "itemId": "minecraft:bone_meal",
                  "dropRate": 0.5,
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
                  "dropRate": 0.3,
                  "minCount": 1,
                  "maxCount": 3
                },
                {
                  "itemId": "minecraft:ice",
                  "dropRate": 0.2,
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
                  "dropRate": 0.15,
                  "minCount": 1,
                  "maxCount": 2
                },
                {
                  "itemId": "minecraft:snowball",
                  "dropRate": 0.5,
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
                  "dropRate": 0.5,
                  "minCount": 1,
                  "maxCount": 3
                }
              ]
            }
            
            Properties:
            - entityId: The full entity ID (e.g., "minecraft:zombie" or "modid:custom_mob")
            - drops: Array of drop entries with the following properties:
              - itemId: The item ID to drop (e.g., "minecraft:diamond" or "modid:custom_item")
              - dropRate: Chance to drop (0.0 to 1.0, where 1.0 is 100%)
              - minCount: Minimum number of items to drop
              - maxCount: Maximum number of items to drop
            
            Example:
            {
              "entityId": "minecraft:zombie",
              "drops": [
                {
                  "itemId": "minecraft:diamond",
                  "dropRate": 0.1,
                  "minCount": 1,
                  "maxCount": 3
                },
                {
                  "itemId": "minecraft:emerald",
                  "dropRate": 0.2,
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
                  "dropRate": 0.5,
                  "minCount": 1,
                  "maxCount": 3
                }
              ]
            }
            
            Winter drops are separate from regular drops and are not affected by the double drops event.
            Use the command '/entitylootdrops winter enable' to activate winter drops.
            """;
        
        try {
            Files.writeString(path, readme);
        } catch (IOException e) {
            LOGGER.error("Failed to create winter README file", e);
        }
    }

    // Event control methods
    public static Component toggleDoubleDrops(boolean enable) {
        doubleDropsEnabled = enable;
        String status = enable ? "enabled" : "disabled";
        LOGGER.info("Double drops {}", status);
        return Component.literal("Double drops have been " + status);
    }

    public static Component toggleWinterEvent(boolean enable) {
        winterEventEnabled = enable;
        String status = enable ? "enabled" : "disabled";
        LOGGER.info("Winter event {}", status);
        return Component.literal("Winter event has been " + status);
    }

    public static Component getEventStatus() {
        return Component.literal(String.format(
            "Events Status:\nDouble Drops: %s\nWinter Event: %s",
            doubleDropsEnabled ? "ENABLED" : "DISABLED",
            winterEventEnabled ? "ENABLED" : "DISABLED"
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

    public static boolean isDoubleDropsEnabled() {
        return doubleDropsEnabled;
    }

    public static boolean isWinterEventEnabled() {
        return winterEventEnabled;
    }
}
