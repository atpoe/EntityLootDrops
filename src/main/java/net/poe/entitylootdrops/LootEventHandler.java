package net.poe.entitylootdrops;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LootEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public void onEntityDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityId == null) return;

        String entityIdString = entityId.toString();
        
        // Process regular drops (affected by drop chance boost event)
        processDrops(event, entity, LootConfig.getDropsForEntity(entityIdString), 
                    LootConfig.isDropchanceEnabled());
        
        // Process seasonal event drops (not affected by drop chance boost)
        if (LootConfig.isWinterEventEnabled()) {
            processDrops(event, entity, LootConfig.getWinterDropsForEntity(entityIdString), false);
        }
        
        if (LootConfig.isEasterEventEnabled()) {
            processDrops(event, entity, LootConfig.getEasterDropsForEntity(entityIdString), false);
        }
        
        if (LootConfig.isHalloweenEventEnabled()) {
            processDrops(event, entity, LootConfig.getHalloweenDropsForEntity(entityIdString), false);
        }
        
        if (LootConfig.isSummerEventEnabled()) {
            processDrops(event, entity, LootConfig.getSummerDropsForEntity(entityIdString), false);
        }
    }

    /**
     * Process drops for a specific entity and drop list
     */
    private void processDrops(LivingDropsEvent event, LivingEntity entity, 
                            List<LootConfig.CustomDrop> drops, boolean dropchanceEnabled) {
        for (LootConfig.CustomDrop drop : drops) {
            // The getDropRate method now handles the drop chance boost internally
            float dropRate = drop.getDropRate();
            
            if (RANDOM.nextFloat() <= dropRate) {
                int count = drop.getMinCount();
                if (drop.getMaxCount() > drop.getMinCount()) {
                    count += RANDOM.nextInt(drop.getMaxCount() - drop.getMinCount() + 1);
                }

                Item item = drop.getItem();
                if (item != null) {
                    ItemStack stack = new ItemStack(item, count);
                    ItemEntity itemEntity = new ItemEntity(
                        entity.level(),
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        stack
                    );

                    itemEntity.setDeltaMovement(
                        RANDOM.nextDouble() * 0.2 - 0.1,
                        RANDOM.nextDouble() * 0.2,
                        RANDOM.nextDouble() * 0.2 - 0.1
                    );

                    event.getDrops().add(itemEntity);
                    LOGGER.debug("Added drop: {} x{} (rate: {})", drop.getItemId(), count, dropRate);
                }
            }
        }
    }
    
    /**
     * Get all additional drops for an entity without actually spawning them
     * This can be used for preview or testing purposes
     */
    public List<ItemStack> getAdditionalDrops(String entityId) {
        List<ItemStack> drops = new ArrayList<>();
        
        // Add drops from regular drops and each enabled event
        addDropsToList(drops, LootConfig.getDropsForEntity(entityId));
        
        if (LootConfig.isWinterEventEnabled()) {
            addDropsToList(drops, LootConfig.getWinterDropsForEntity(entityId));
        }
        
        if (LootConfig.isEasterEventEnabled()) {
            addDropsToList(drops, LootConfig.getEasterDropsForEntity(entityId));
        }
        
        if (LootConfig.isHalloweenEventEnabled()) {
            addDropsToList(drops, LootConfig.getHalloweenDropsForEntity(entityId));
        }
        
        if (LootConfig.isSummerEventEnabled()) {
            addDropsToList(drops, LootConfig.getSummerDropsForEntity(entityId));
        }
        
        return drops;
    }
    
    /**
     * Helper method to convert CustomDrop objects to ItemStacks and add them to a list
     */
    private void addDropsToList(List<ItemStack> dropsList, List<LootConfig.CustomDrop> customDrops) {
        for (LootConfig.CustomDrop drop : customDrops) {
            Item item = drop.getItem();
            if (item != null) {
                // For preview purposes, we use the average count
                int avgCount = (drop.getMinCount() + drop.getMaxCount()) / 2;
                ItemStack stack = new ItemStack(item, avgCount);
                dropsList.add(stack);
            }
        }
    }
}
