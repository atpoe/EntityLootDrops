package net.poe.entitylootdrops.lootdrops.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a custom drop entry for hostile mobs.
 * This is the base class for all drop configurations.
 */
public class CustomDropEntry {
    private String itemId;              // The Minecraft item ID (e.g., "minecraft:diamond")
    private float dropChance;           // Percentage chance to drop (0-100)
    private int minAmount;              // Minimum number of items to drop
    private int maxAmount;              // Maximum number of items to drop
    private String nbtData;             // Custom NBT data for the item (for enchantments, names, etc.)
    private String requiredAdvancement; // Player must have this advancement to get the drop
    private String requiredEffect;      // Player must have this potion effect to get the drop
    private String requiredEquipment;   // Player must have this item equipped to get the drop
    private String requiredDimension;   // Player must be in this dimension to get the drop
    private String command;             // Command to execute when the drop occurs
    private float commandChance;        // Percentage chance to execute the command (0-100)
    private String _comment;            // Comment for documentation in the JSON file
    private boolean requirePlayerKill = true; // Default to true for backward compatibility
    private boolean allowDefaultDrops = true; // Default to true for backward compatibility
    private List<String> allowModIDs = new ArrayList<>(); // List of mod IDs that are allowed to drop items
    
    /**
     * Default constructor for Gson deserialization.
     */
    public CustomDropEntry() {
        this.commandChance = 100.0f; // Default to 100% if command is specified
    }
    
    /**
     * Constructor for a basic drop without NBT data.
     */
    public CustomDropEntry(String itemId, float dropChance, int minAmount, int maxAmount) {
        this(itemId, dropChance, minAmount, maxAmount, null);
    }
    
    /**
     * Constructor for a drop with NBT data.
     */
    public CustomDropEntry(String itemId, float dropChance, int minAmount, int maxAmount, String nbtData) {
        this.itemId = itemId;
        this.dropChance = dropChance;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.nbtData = nbtData;
        this.commandChance = 100.0f;
        this.requirePlayerKill = true;
        this.allowDefaultDrops = true;
        this.allowModIDs = new ArrayList<>();
    }
    
    // Getters
    public String getItemId() { return itemId; }
    public float getDropChance() { return dropChance; }
    public int getMinAmount() { return minAmount; }
    public int getMaxAmount() { return maxAmount; }
    public String getNbtData() { return nbtData; }
    public String getRequiredAdvancement() { return requiredAdvancement; }
    public String getRequiredEffect() { return requiredEffect; }
    public String getRequiredEquipment() { return requiredEquipment; }
    public String getRequiredDimension() { return requiredDimension; }
    public String getCommand() { return command; }
    public float getCommandChance() { return commandChance; }
    public boolean isRequirePlayerKill() { return requirePlayerKill; }
    public boolean isAllowDefaultDrops() { return allowDefaultDrops; }
    public List<String> getAllowModIDs() { return allowModIDs; }
    
    // Setters
    public void setItemId(String itemId) { this.itemId = itemId; }
    public void setDropChance(float dropChance) { this.dropChance = dropChance; }
    public void setMinAmount(int minAmount) { this.minAmount = minAmount; }
    public void setMaxAmount(int maxAmount) { this.maxAmount = maxAmount; }
    public void setNbtData(String nbtData) { this.nbtData = nbtData; }
    public void setRequiredAdvancement(String requiredAdvancement) { this.requiredAdvancement = requiredAdvancement; }
    public void setRequiredEffect(String requiredEffect) { this.requiredEffect = requiredEffect; }
    public void setRequiredEquipment(String requiredEquipment) { this.requiredEquipment = requiredEquipment; }
    public void setRequiredDimension(String requiredDimension) { this.requiredDimension = requiredDimension; }
    public void setCommand(String command) { this.command = command; }
    public void setCommandChance(float commandChance) { this.commandChance = commandChance; }
    public void setRequirePlayerKill(boolean requirePlayerKill) { this.requirePlayerKill = requirePlayerKill; }
    public void setAllowDefaultDrops(boolean allowDefaultDrops) { this.allowDefaultDrops = allowDefaultDrops; }
    public void setAllowModIDs(List<String> allowModIDs) { this.allowModIDs = allowModIDs != null ? allowModIDs : new ArrayList<>(); }
    public void setComment(String comment) { this._comment = comment; }
    
    // Helper methods
    public boolean hasNbtData() { return nbtData != null && !nbtData.isEmpty(); }
    public boolean hasRequiredAdvancement() { return requiredAdvancement != null && !requiredAdvancement.isEmpty(); }
    public boolean hasRequiredEffect() { return requiredEffect != null && !requiredEffect.isEmpty(); }
    public boolean hasRequiredEquipment() { return requiredEquipment != null && !requiredEquipment.isEmpty(); }
    public boolean hasRequiredDimension() { return requiredDimension != null && !requiredDimension.isEmpty(); }
    public boolean hasCommand() { return command != null && !command.isEmpty(); }
    
    public boolean isModIDAllowed(String modId) {
        if (allowDefaultDrops) {
            return true;
        }
        return allowModIDs != null && allowModIDs.contains(modId);
    }
}
