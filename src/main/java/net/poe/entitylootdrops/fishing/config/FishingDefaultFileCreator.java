package net.poe.entitylootdrops.fishing.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.poe.entitylootdrops.fishing.FishingDrop;
import net.poe.entitylootdrops.fishing.FishingReward;

/**
 * Creates default fishing configuration files.
 */
public class FishingDefaultFileCreator {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Creates default configuration files.
     */
    public static void createDefaultFiles(Path fishingDir, Path conditionalDir, Path biomeDir, Path dimensionDir) {
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
     * Creates default global fishing rewards file.
     */
    public static void createDefaultGlobalRewards(File file) {
        List<FishingReward> defaultRewards = new ArrayList<>();
        
        // Experience bottle reward
        FishingReward expReward = new FishingReward();
        expReward.setItem("minecraft:experience_bottle");
        expReward.setCount(1);
        expReward.setChance(0.1); // 10% chance
        defaultRewards.add(expReward);
        
        // Rare treasure reward
        FishingReward treasureReward = new FishingReward();
        treasureReward.setItem("minecraft:emerald");
        treasureReward.setMinCount(1);
        treasureReward.setMaxCount(2);
        treasureReward.setChance(0.05); // 5% chance
        defaultRewards.add(treasureReward);
        
        writeJsonToFile(file, defaultRewards);
    }
    
    /**
     * Creates default conditional drops.
     */
    public static void createDefaultConditionalDrops(File file) {
        List<FishingDrop> defaultDrops = new ArrayList<>();
        
        // Night fishing drop
        FishingDrop nightDrop = new FishingDrop();
        nightDrop.setName("night_fishing_bonus");
        nightDrop.setTimeOfDay("night");
        nightDrop.setChance(0.15); // 15% chance at night
        
        FishingReward nightReward = new FishingReward();
        nightReward.setItem("minecraft:glowstone_dust");
        nightReward.setMinCount(1);
        nightReward.setMaxCount(3);
        nightReward.setChance(0.8);
        nightDrop.getRewards().add(nightReward);
        
        nightDrop.getCommands().add("tellraw {player} {\"text\":\"The night brings mysterious catches...\",\"color\":\"dark_purple\"}");
        defaultDrops.add(nightDrop);
        
        // Rain fishing drop
        FishingDrop rainDrop = new FishingDrop();
        rainDrop.setName("rainy_day_catch");
        rainDrop.setWeather("rain");
        rainDrop.setChance(0.2); // 20% chance in rain
        
        FishingReward rainReward = new FishingReward();
        rainReward.setItem("minecraft:prismarine_shard");
        rainReward.setCount(2);
        rainReward.setChance(0.7);
        rainDrop.getRewards().add(rainReward);
        
        rainDrop.getCommands().add("tellraw {player} {\"text\":\"The rain brings oceanic treasures!\",\"color\":\"aqua\"}");
        defaultDrops.add(rainDrop);
        
        writeJsonToFile(file, defaultDrops);
    }
    
    /**
     * Creates default biome-specific drops.
     */
    public static void createDefaultBiomeDrops(File file) {
        List<FishingDrop> oceanDrops = new ArrayList<>();
        
        // Deep ocean rare catch
        FishingDrop deepOceanDrop = new FishingDrop();
        deepOceanDrop.setName("deep_ocean_treasure");
        deepOceanDrop.setBiome("minecraft:deep_ocean");
        deepOceanDrop.setChance(0.03); // 3% chance
        deepOceanDrop.setRequiresLuckOfSea(true);
        
        FishingReward diamondReward = new FishingReward();
        diamondReward.setItem("minecraft:diamond");
        diamondReward.setMinCount(1);
        diamondReward.setMaxCount(2);
        diamondReward.setChance(0.6);
        deepOceanDrop.getRewards().add(diamondReward);
        
        FishingReward enchantedBookReward = new FishingReward();
        enchantedBookReward.setItem("minecraft:enchanted_book");
        enchantedBookReward.setCount(1);
        enchantedBookReward.setChance(0.4);
        enchantedBookReward.setNbt("{StoredEnchantments:[{id:\"minecraft:mending\",lvl:1}]}");
        deepOceanDrop.getRewards().add(enchantedBookReward);
        
        deepOceanDrop.getCommands().add("tellraw {player} {\"text\":\"You found a deep ocean treasure!\",\"color\":\"gold\"}");
        deepOceanDrop.getCommands().add("playsound minecraft:entity.player.levelup player {player} ~ ~ ~ 1 0.5");
        oceanDrops.add(deepOceanDrop);
        
        writeJsonToFile(file, oceanDrops);
    }
    
    /**
     * Creates default dimension-specific drops.
     */
    public static void createDefaultDimensionDrops(File file) {
        List<FishingDrop> netherDrops = new ArrayList<>();
        
        // Nether fishing (if possible with mods)
        FishingDrop netherDrop = new FishingDrop();
        netherDrop.setName("nether_fishing_miracle");
        netherDrop.setDimension("minecraft:the_nether");
        netherDrop.setChance(0.01); // 1% chance - very rare
        netherDrop.setMinFishingLevel(20);
        
        FishingReward netherStarReward = new FishingReward();
        netherStarReward.setItem("minecraft:nether_star");
        netherStarReward.setCount(1);
        netherStarReward.setChance(0.1); // 10% of the 1% chance
        netherDrop.getRewards().add(netherStarReward);
        
        FishingReward blazeRodReward = new FishingReward();
        blazeRodReward.setItem("minecraft:blaze_rod");
        blazeRodReward.setMinCount(1);
        blazeRodReward.setMaxCount(3);
        blazeRodReward.setChance(0.5);
        netherDrop.getRewards().add(blazeRodReward);
        
        netherDrop.getCommands().add("tellraw {player} {\"text\":\"How did you fish in the Nether?!\",\"color\":\"red\"}");
        netherDrop.getCommands().add("tellraw @a {\"text\":\"{player} performed a miracle - fishing in the Nether!\",\"color\":\"gold\"}");
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
}
