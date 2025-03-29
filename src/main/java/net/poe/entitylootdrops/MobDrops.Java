package net.poe.entitylootdrops;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.List;

public class MobDrops {
    public String entityId;
    public List<CustomDrop> drops;

    public MobDrops(String entityId, List<CustomDrop> drops) {
        this.entityId = entityId;
        this.drops = drops;
    }

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
}
