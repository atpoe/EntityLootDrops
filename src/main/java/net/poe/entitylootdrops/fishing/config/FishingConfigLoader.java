package net.poe.entitylootdrops.fishing.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import net.poe.entitylootdrops.fishing.FishingDrop;
import net.poe.entitylootdrops.fishing.FishingReward;

/**
 * Handles loading of fishing configuration files.
 */
public class FishingConfigLoader {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Loads global fishing rewards from file.
     */
    public static void loadGlobalFishingRewards(File file, List<FishingReward> globalFishingRewards) {
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
    public static void loadDropsFromDirectory(File directory, List<FishingDrop> fishingDrops) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }
        
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                loadFishingDropsFromFile(file, fishingDrops);
            }
        }
    }
    
    /**
     * Loads fishing drops from a specific file.
     */
    public static void loadFishingDropsFromFile(File file, List<FishingDrop> fishingDrops) {
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
}
