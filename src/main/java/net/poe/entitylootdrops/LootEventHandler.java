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
        
        // Process regular drops
        processDrops(event, entity, LootConfig.getDropsForEntity(entityIdString), 
                    LootConfig.isDoubleDropsEnabled());
        
        // Process winter drops (these are separate and not affected by double drops)
        processDrops(event, entity, LootConfig.getWinterDropsForEntity(entityIdString), false);
    }

    private void processDrops(LivingDropsEvent event, LivingEntity entity, 
                            List<LootConfig.CustomDrop> drops, boolean doubleDrops) {
        for (LootConfig.CustomDrop drop : drops) {
            float dropRate = drop.getDropRate();
            if (doubleDrops) {
                dropRate = Math.min(dropRate * 2, 1.0f);
            }

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
}
