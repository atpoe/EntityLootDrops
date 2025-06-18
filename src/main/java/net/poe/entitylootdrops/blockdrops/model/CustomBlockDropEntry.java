package net.poe.entitylootdrops.blockdrops.model;

import java.util.List;

/**
 * Represents a custom block drop entry for global drops that can apply to any block.
 */
public class CustomBlockDropEntry {
    // Basic drop properties
    private String itemId;
    private float dropChance;
    private int minAmount;
    private int maxAmount;
    private boolean requirePlayerBreak;
    private boolean allowDefaultDrops;
    private boolean replaceDefaultDrops;
    
    // Tool requirements
    private String requiredTool;
    //private String requiredToolTier;
    //private int requiredToolLevel;
    private String requiredEnchantment;
    private int requiredEnchantLevel;
    
    // Advanced properties
    private String nbtData;
    private String command;
    private float commandChance;
    private List<String> allowModIDs;
    private String comment;
    
    // New regeneration properties
    private boolean canRegenerate;
    private String brokenBlockReplace;
    private int respawnTime; // Time in seconds
    
    public CustomBlockDropEntry() {
        this.dropChance = 100.0f;
        this.minAmount = 1;
        this.maxAmount = 1;
        this.requirePlayerBreak = false;
        this.allowDefaultDrops = true;
        this.replaceDefaultDrops = false;
        //this.requiredToolLevel = 0;
        this.requiredEnchantLevel = 0;
        this.commandChance = 100.0f;
        
        // Initialize new regeneration properties
        this.canRegenerate = false;
        this.brokenBlockReplace = "minecraft:bedrock";
        this.respawnTime = 20; // Default 20 seconds
    }
    
    // Basic getters and setters
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    
    public float getDropChance() { return dropChance; }
    public void setDropChance(float dropChance) { this.dropChance = dropChance; }
    
    public int getMinAmount() { return minAmount; }
    public void setMinAmount(int minAmount) { this.minAmount = minAmount; }
    
    public int getMaxAmount() { return maxAmount; }
    public void setMaxAmount(int maxAmount) { this.maxAmount = maxAmount; }
    
    public boolean isRequirePlayerBreak() { return requirePlayerBreak; }
    public void setRequirePlayerBreak(boolean requirePlayerBreak) { this.requirePlayerBreak = requirePlayerBreak; }
    
    public boolean isAllowDefaultDrops() { return allowDefaultDrops; }
    public void setAllowDefaultDrops(boolean allowDefaultDrops) { this.allowDefaultDrops = allowDefaultDrops; }
    
    public boolean isReplaceDefaultDrops() { return replaceDefaultDrops; }
    public void setReplaceDefaultDrops(boolean replaceDefaultDrops) { this.replaceDefaultDrops = replaceDefaultDrops; }
    
    // Tool requirement getters and setters
    public String getRequiredTool() { return requiredTool; }
    public void setRequiredTool(String requiredTool) { this.requiredTool = requiredTool; }
    
    //public String getRequiredToolTier() { return requiredToolTier; }
    //public void setRequiredToolTier(String requiredToolTier) { this.requiredToolTier = requiredToolTier; }
    
    //public int getRequiredToolLevel() { return requiredToolLevel; }
    //public void setRequiredToolLevel(int requiredToolLevel) { this.requiredToolLevel = requiredToolLevel; }
    
    public String getRequiredEnchantment() { return requiredEnchantment; }
    public void setRequiredEnchantment(String requiredEnchantment) { this.requiredEnchantment = requiredEnchantment; }
    
    public int getRequiredEnchantLevel() { return requiredEnchantLevel; }
    public void setRequiredEnchantLevel(int requiredEnchantLevel) { this.requiredEnchantLevel = requiredEnchantLevel; }
    
    // Advanced property getters and setters
    public String getNbtData() { return nbtData; }
    public void setNbtData(String nbtData) { this.nbtData = nbtData; }
    
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    
    public float getCommandChance() { return commandChance; }
    public void setCommandChance(float commandChance) { this.commandChance = commandChance; }
    
    public List<String> getAllowModIDs() { return allowModIDs; }
    public void setAllowModIDs(List<String> allowModIDs) { this.allowModIDs = allowModIDs; }
    
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    
    // New regeneration getters and setters
    public boolean canRegenerate() { return canRegenerate; }
    public void setCanRegenerate(boolean canRegenerate) { this.canRegenerate = canRegenerate; }
    
    public String getBrokenBlockReplace() { return brokenBlockReplace; }
    public void setBrokenBlockReplace(String brokenBlockReplace) { this.brokenBlockReplace = brokenBlockReplace; }
    
    public int getRespawnTime() { return respawnTime; }
    public void setRespawnTime(int respawnTime) { this.respawnTime = respawnTime; }
    
    // Utility methods
    public boolean hasNbtData() {
        return nbtData != null && !nbtData.isEmpty();
    }
    
    public boolean hasCommand() {
        return command != null && !command.isEmpty();
    }
    
    public boolean hasRequiredTool() {
        return requiredTool != null && !requiredTool.isEmpty();
    }
    
    public boolean hasRequiredEnchantment() {
        return requiredEnchantment != null && !requiredEnchantment.isEmpty();
    }
    
    //public boolean hasRequiredToolTier() {
    //    return requiredToolTier != null && !requiredToolTier.isEmpty();
    //}
    
    public boolean hasAllowModIDs() {
        return allowModIDs != null && !allowModIDs.isEmpty();
    }
}
