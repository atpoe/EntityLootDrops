package net.poe.entitylootdrops.fishing;

/**
 * Represents a reward item from fishing.
 */
public class FishingReward {
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
    
    // Setters
    public void setItem(String item) { this.item = item; }
    public void setCount(int count) { this.count = count; }
    public void setMinCount(int minCount) { this.minCount = minCount; }
    public void setMaxCount(int maxCount) { this.maxCount = maxCount; }
    public void setChance(double chance) { this.chance = chance; }
    public void setNbt(String nbt) { this.nbt = nbt; }
    
    public boolean hasNbt() {
        return nbt != null && !nbt.isEmpty();
    }
}
