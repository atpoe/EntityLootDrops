package net.poe.entitylootdrops;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

/**
 * Configuration class for managing fishing drop rewards and commands.
 */
public class FishingConfig {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Configuration directory paths
    private static final String CONFIG_DIR = "config/EntityLootDrops";
    private static final String FISHING_DIR = "Fishing";
    private static final String CONDITIONAL_DIR = "Conditional Drops";
    private static final String BIOME_DIR = "Biome Drops";
    private static final String DIMENSION_DIR = "Dimension Drops";
    
    private static List<FishingDrop> fishingDrops = new ArrayList<>();
    private static List<FishingReward> globalFishingRewards = new ArrayList<>();
    
    /**
     * Represents a custom fishing drop with conditions and rewards.
     */
    public static class FishingDrop {
        private String name;
        private String biome;
        private String dimension;
        private String weather;
        private String timeOfDay;
        private double chance;
        private int minFishingLevel;
        private boolean requiresLure;
        private boolean requiresLuckOfSea;
        private List<FishingReward> rewards;
        private List<String> commands;
        
        public FishingDrop() {
            this.rewards = new ArrayList<>();
            this.commands = new ArrayList<>();
            this.chance = 1.0;
            this.minFishingLevel = 0;
            this.requiresLure = false;
            this.requiresLuckOfSea = false;
        }
        
        // Getters
        public String getName() { return name; }
        public String getBiome() { return biome; }
        public String getDimension() { return dimension; }
        public String getWeather() { return weather; }
        public String getTimeOfDay() { return timeOfDay; }
        public double getChance() { return chance; }
        public int getMinFishingLevel() { return minFishingLevel; }
        public boolean requiresLure() { return requiresLure; }
        public boolean requiresLuckOfSea() { return requiresLuckOfSea; }
        public List<FishingReward> getRewards() { return rewards; }
        public List<String> getCommands() { return commands; }
        
        public boolean hasCommands() {
            return commands != null && !commands.isEmpty();
        }
        
        public boolean hasRewards() {
            return rewards != null && !rewards.isEmpty();
        }
    }
    
    /**
     * Represents a reward item from fishing.
     */
    public static class FishingReward {
        private String item;
        private int count;
        private int minCount;
        private int maxCount;
        private double chance;
        private String nbt;
        
        public FishingReward() {
            this.count = 1;
            this.minCount = 1;
            this.maxCount = 1;
            this.chance = 1.0;
        }
        
        // Getters
        public String getItem() { return item; }
        public int getCount() { return count; }
        public int getMinCount() { return minCount; }
        public int getMaxCount() { return maxCount; }
        public double getChance() { return chance; }
        public String getNbt() { return nbt; }
        
        public boolean hasNbt() {
            return nbt != null && !nbt.isEmpty();
        }
    }
    
    /**
     * Loads the fishing configuration from files.
     */
    public static void loadConfig(File configDir) {
        try {
            // Create directories first
            createConfigDirectories();
            
            // Load all fishing data
            fishingDrops.clear();
            globalFishingRewards.clear();
            
            // Load global fishing rewards
            Path globalFile = Paths.get(CONFIG_DIR, FISHING_DIR, "Global_Fishing_Rewards.json");
            loadGlobalFishingRewards(globalFile.toFile());
            
            // Load conditional drops
            Path conditionalDir = Paths.get(CONFIG_DIR, FISHING_DIR, CONDITIONAL_DIR);
            loadDropsFromDirectory(conditionalDir.toFile());
            
            // Load biome drops
            Path biomeDir = Paths.get(CONFIG_DIR, FISHING_DIR, BIOME_DIR);
            loadDropsFromDirectory(biomeDir.toFile());
            
            // Load dimension drops
            Path dimensionDir = Paths.get(CONFIG_DIR, FISHING_DIR, DIMENSION_DIR);
            loadDropsFromDirectory(dimensionDir.toFile());
            
            LOGGER.info("Loaded {} fishing drops and {} global fishing rewards", 
                       fishingDrops.size(), globalFishingRewards.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load fishing configuration", e);
        }
    }
    
    /**
     * Creates the directory structure for fishing configuration.
     */
    private static void createConfigDirectories() {
        try {
            // Create main config directory
            Path configDir = Paths.get(CONFIG_DIR);
            Files.createDirectories(configDir);
            
            // Create fishing directory
            Path fishingDir = Paths.get(CONFIG_DIR, FISHING_DIR);
            Files.createDirectories(fishingDir);
            
            // Create subdirectories
            Path conditionalDir = Paths.get(CONFIG_DIR, FISHING_DIR, CONDITIONAL_DIR);
            Path biomeDir = Paths.get(CONFIG_DIR, FISHING_DIR, BIOME_DIR);
            Path dimensionDir = Paths.get(CONFIG_DIR, FISHING_DIR, DIMENSION_DIR);
            
            Files.createDirectories(conditionalDir);
            Files.createDirectories(biomeDir);
            Files.createDirectories(dimensionDir);
            
            // Create default files if they don't exist
            createDefaultFiles(fishingDir, conditionalDir, biomeDir, dimensionDir);
            
        } catch (IOException e) {
            LOGGER.error("Failed to create fishing config directories", e);
        }
    }
    
    /**
     * Creates default configuration files.
     */
    private static void createDefaultFiles(Path fishingDir, Path conditionalDir, Path biomeDir, Path dimensionDir) {
        try {
            // Create global rewards file
            Path globalFile = fishingDir.resolve("Global_Fishing_Rewards.json");
            if (!Files.exists(globalFile)) {
                createDefaultGlobalRewards(globalFile.toFile());
            }
            
            // Create conditional drops file
            Path conditionalFile = conditionalDir.resolve("Weather_Time_Drops.json");
            if (!Files.exists(conditionalFile)) {
                createDefaultConditionalDrops(conditionalFile.toFile());
            }
            
            // Create biome drops file
            Path biomeFile = biomeDir.resolve("Ocean_Drops.json");
            if (!Files.exists(biomeFile)) {
                createDefaultBiomeDrops(biomeFile.toFile());
            }
            
            // Create dimension drops file
            Path dimensionFile = dimensionDir.resolve("Nether_Drops.json");
            if (!Files.exists(dimensionFile)) {
                createDefaultDimensionDrops(dimensionFile.toFile());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create default fishing files", e);
        }
    }
    
    /**
     * Loads global fishing rewards from file.
     */
    private static void loadGlobalFishingRewards(File file) {
        if (!file.exists()) {
            return;
        }
        
        try (FileReader reader = new FileReader(file)) {
            List<FishingReward> rewards = GSON.fromJson(reader, new TypeToken<List<FishingReward>>(){}.getType());
            if (rewards != null) {
                globalFishingRewards.addAll(rewards);
                LOGGER.debug("Loaded {} global fishing rewards", rewards.size());
            }
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.error("Failed to load global fishing rewards", e);
        }
    }
    
    /**
     * Loads all JSON files from a directory as fishing drops
     */
    private static void loadDropsFromDirectory(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }
        
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                loadFishingDropsFromFile(file);
            }
        }
    }
    
    /**
     * Loads fishing drops from a specific file.
     */
    private static void loadFishingDropsFromFile(File file) {
        try (FileReader reader = new FileReader(file)) {
            List<FishingDrop> drops = GSON.fromJson(reader, new TypeToken<List<FishingDrop>>(){}.getType());
            if (drops != null) {
                fishingDrops.addAll(drops);
                LOGGER.debug("Loaded {} fishing drops from {}", drops.size(), file.getName());
            }
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.error("Failed to load fishing drops from: {}", file.getName(), e);
        }
    }
    
    /**
     * Creates default global fishing rewards file.
     */
    private static void createDefaultGlobalRewards(File file) {
        List<FishingReward> defaultRewards = new ArrayList<>();
        
        // Experience bottle reward
        FishingReward expReward = new FishingReward();
        expReward.item = "minecraft:experience_bottle";
        expReward.count = 1;
        expReward.chance = 0.1; // 10% chance
        defaultRewards.add(expReward);
        
        // Rare treasure reward
        FishingReward treasureReward = new FishingReward();
        treasureReward.item = "minecraft:emerald";
        treasureReward.minCount = 1;
        treasureReward.maxCount = 2;
        treasureReward.chance = 0.05; // 5% chance
        defaultRewards.add(treasureReward);
        
        writeJsonToFile(file, defaultRewards);
    }
    
    /**
     * Creates default conditional drops.
     */
    private static void createDefaultConditionalDrops(File file) {
        List<FishingDrop> defaultDrops = new ArrayList<>();
        
        // Night fishing drop
        FishingDrop nightDrop = new FishingDrop();
        nightDrop.name = "night_fishing_bonus";
        nightDrop.timeOfDay = "night";
        nightDrop.chance = 0.15; // 15% chance at night
        
        FishingReward nightReward = new FishingReward();
        nightReward.item = "minecraft:glowstone_dust";
        nightReward.minCount = 1;
        nightReward.maxCount = 3;
        nightReward.chance = 0.8;
        nightDrop.rewards.add(nightReward);
        
        nightDrop.commands.add("tellraw {player} {\"text\":\"The night brings mysterious catches...\",\"color\":\"dark_purple\"}");
        defaultDrops.add(nightDrop);
        
        // Rain fishing drop
        FishingDrop rainDrop = new FishingDrop();
        rainDrop.name = "rainy_day_catch";
        rainDrop.weather = "rain";
        rainDrop.chance = 0.2; // 20% chance in rain
        
        FishingReward rainReward = new FishingReward();
        rainReward.item = "minecraft:prismarine_shard";
        rainReward.count = 2;
        rainReward.chance = 0.7;
        rainDrop.rewards.add(rainReward);
        
        rainDrop.commands.add("tellraw {player} {\"text\":\"The rain brings oceanic treasures!\",\"color\":\"aqua\"}");
        defaultDrops.add(rainDrop);
        
        writeJsonToFile(file, defaultDrops);
    }
    
    /**
     * Creates default biome-specific drops.
     */
    private static void createDefaultBiomeDrops(File file) {
        List<FishingDrop> oceanDrops = new ArrayList<>();
        
        // Deep ocean rare catch
        FishingDrop deepOceanDrop = new FishingDrop();
        deepOceanDrop.name = "deep_ocean_treasure";
        deepOceanDrop.biome = "minecraft:deep_ocean";
        deepOceanDrop.chance = 0.03; // 3% chance
        deepOceanDrop.requiresLuckOfSea = true;
        
        FishingReward diamondReward = new FishingReward();
        diamondReward.item = "minecraft:diamond";
        diamondReward.minCount = 1;
        diamondReward.maxCount = 2;
        diamondReward.chance = 0.6;
        deepOceanDrop.rewards.add(diamondReward);
        
        FishingReward enchantedBookReward = new FishingReward();
        enchantedBookReward.item = "minecraft:enchanted_book";
        enchantedBookReward.count = 1;
        enchantedBookReward.chance = 0.4;
        enchantedBookReward.nbt = "{StoredEnchantments:[{id:\"minecraft:mending\",lvl:1}]}";
        deepOceanDrop.rewards.add(enchantedBookReward);
        
        deepOceanDrop.commands.add("tellraw {player} {\"text\":\"You found a deep ocean treasure!\",\"color\":\"gold\"}");
        deepOceanDrop.commands.add("playsound minecraft:entity.player.levelup player {player} ~ ~ ~ 1 0.5");
        oceanDrops.add(deepOceanDrop);
        
        writeJsonToFile(file, oceanDrops);
    }
    
    /**
     * Creates default dimension-specific drops.
     */
    private static void createDefaultDimensionDrops(File file) {
        List<FishingDrop> netherDrops = new ArrayList<>();
        
        // Nether fishing (if possible with mods)
        FishingDrop netherDrop = new FishingDrop();
        netherDrop.name = "nether_fishing_miracle";
        netherDrop.dimension = "minecraft:the_nether";
        netherDrop.chance = 0.01; // 1% chance - very rare
        netherDrop.minFishingLevel = 20;
        
        FishingReward netherStarReward = new FishingReward();
        netherStarReward.item = "minecraft:nether_star";
        netherStarReward.count = 1;
        netherStarReward.chance = 0.1; // 10% of the 1% chance
        netherDrop.rewards.add(netherStarReward);
        
        FishingReward blazeRodReward = new FishingReward();
        blazeRodReward.item = "minecraft:blaze_rod";
        blazeRodReward.minCount = 1;
        blazeRodReward.maxCount = 3;
        blazeRodReward.chance = 0.5;
        netherDrop.rewards.add(blazeRodReward);
        
        netherDrop.commands.add("tellraw {player} {\"text\":\"How did you fish in the Nether?!\",\"color\":\"red\"}");
        netherDrop.commands.add("tellraw @a {\"text\":\"{player} performed a miracle - fishing in the Nether!\",\"color\":\"gold\"}");
        netherDrops.add(netherDrop);
        
        writeJsonToFile(file, netherDrops);
    }
    
    /**
     * Writes JSON data to a file.
     */
    private static void writeJsonToFile(File file, Object data) {
        try {
            String json = GSON.toJson(data);
            Files.write(file.toPath(), json.getBytes());
            LOGGER.info("Created file: {}", file.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to write JSON file: {}", file.getAbsolutePath(), e);
        }
    }
    
    /**
     * Reloads the fishing configuration.
     */
    public static void reloadConfig(File configDir) {
        LOGGER.info("Reloading fishing configuration...");
        loadConfig(configDir);
    }
    
    // Getters for the loaded data
    public static List<FishingDrop> getFishingDrops() {
        return new ArrayList<>(fishingDrops);
    }
    
    public static List<FishingReward> getGlobalFishingRewards() {
        return new ArrayList<>(globalFishingRewards);
    }
}
