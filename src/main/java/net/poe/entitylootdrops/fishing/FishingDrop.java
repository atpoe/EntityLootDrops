package net.poe.entitylootdrops.fishing;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a custom fishing drop with conditions and rewards.
 */
public class FishingDrop {
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
    
    // Setters
    public void setName(String name) { this.name = name; }
    public void setBiome(String biome) { this.biome = biome; }
    public void setDimension(String dimension) { this.dimension = dimension; }
    public void setWeather(String weather) { this.weather = weather; }
    public void setTimeOfDay(String timeOfDay) { this.timeOfDay = timeOfDay; }
    public void setChance(double chance) { this.chance = chance; }
    public void setMinFishingLevel(int minFishingLevel) { this.minFishingLevel = minFishingLevel; }
    public void setRequiresLure(boolean requiresLure) { this.requiresLure = requiresLure; }
    public void setRequiresLuckOfSea(boolean requiresLuckOfSea) { this.requiresLuckOfSea = requiresLuckOfSea; }
    public void setRewards(List<FishingReward> rewards) { this.rewards = rewards; }
    public void setCommands(List<String> commands) { this.commands = commands; }
    
    public boolean hasCommands() {
        return commands != null && !commands.isEmpty();
    }
    
    public boolean hasRewards() {
        return rewards != null && !rewards.isEmpty();
    }
}
