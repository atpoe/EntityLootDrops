package net.poe.entitylootdrops.lootdrops.model;

/**
 * Represents an entity-specific drop entry.
 * Extends CustomDropEntry to add an entity ID.
 */
public class EntityDropEntry extends CustomDropEntry {
    private String entityId;  // The Minecraft entity ID (e.g., "minecraft:zombie")

    /**
     * Default constructor for Gson deserialization.
     */
    public EntityDropEntry() {
        super();
    }

    /**
     * Constructor for a basic entity drop without NBT data.
     */
    public EntityDropEntry(String entityId, String itemId, float dropChance, int minAmount, int maxAmount) {
        this(entityId, itemId, dropChance, minAmount, maxAmount, null);
    }

    /**
     * Constructor for an entity drop with NBT data.
     */
    public EntityDropEntry(String entityId, String itemId, float dropChance, int minAmount, int maxAmount, String nbtData) {
        super(itemId, dropChance, minAmount, maxAmount, nbtData);
        this.entityId = entityId;
    }

    /**
     * Gets the Minecraft entity ID.
     */
    public String getEntityId() {
        return entityId;
    }

    /**
     * Sets the Minecraft entity ID.
     */
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }
}