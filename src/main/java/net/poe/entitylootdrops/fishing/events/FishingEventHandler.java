package net.poe.entitylootdrops.fishing.events;

import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.poe.entitylootdrops.EntityLootDrops;
import net.poe.entitylootdrops.fishing.FishingDrop;
import net.poe.entitylootdrops.fishing.FishingReward;
import net.poe.entitylootdrops.fishing.config.FishingConfig;

/**
 * Event handler for fishing events to provide custom drops and execute commands.
 */
@Mod.EventBusSubscriber(modid = EntityLootDrops.MOD_ID)
public class FishingEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random RANDOM = new Random();
    
    /**
     * Handles fishing events to provide custom drops and execute commands.
     */
    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = player.blockPosition();
        
        // Process conditional fishing drops
        for (FishingDrop drop : FishingConfig.getFishingDrops()) {
            if (shouldTriggerDrop(drop, player, level, pos)) {
                processFishingDrop(drop, player, level, pos);
            }
        }
        
        // Process global fishing rewards
        for (FishingReward reward : FishingConfig.getGlobalFishingRewards()) {
            if (RANDOM.nextDouble() < reward.getChance()) {
                giveReward(reward, player, level, pos);
            }
        }
    }
    
    // ... rest of the methods remain the same as in the previous version
    
    /**
     * Checks if a fishing drop should trigger based on its conditions.
     */
    private static boolean shouldTriggerDrop(FishingDrop drop, ServerPlayer player, ServerLevel level, BlockPos pos) {
        // Check basic chance
        if (RANDOM.nextDouble() >= drop.getChance()) {
            return false;
        }
        
        // Check biome condition
        if (drop.getBiome() != null) {
            ResourceLocation currentBiome = level.getBiome(pos).unwrapKey().get().location();
            if (!drop.getBiome().equals(currentBiome.toString())) {
                return false;
            }
        }
        
        // Check dimension condition
        if (drop.getDimension() != null) {
            ResourceLocation currentDimension = level.dimension().location();
            if (!drop.getDimension().equals(currentDimension.toString())) {
                return false;
            }
        }
        
        // Check weather condition
        if (drop.getWeather() != null) {
            boolean isRaining = level.isRaining();
            boolean isThundering = level.isThundering();
            
            switch (drop.getWeather().toLowerCase()) {
                case "clear":
                    if (isRaining || isThundering) return false;
                    break;
                case "rain":
                    if (!isRaining || isThundering) return false;
                    break;
                case "thunder":
                    if (!isThundering) return false;
                    break;
                default:
                    LOGGER.warn("Unknown weather condition: {}", drop.getWeather());
                    return false;
            }
        }
        
        // Check time of day condition
        if (drop.getTimeOfDay() != null) {
            long timeOfDay = level.getDayTime() % 24000;
            boolean isDay = timeOfDay >= 0 && timeOfDay < 13000;
            
            switch (drop.getTimeOfDay().toLowerCase()) {
                case "day":
                    if (!isDay) return false;
                    break;
                case "night":
                    if (isDay) return false;
                    break;
                default:
                    LOGGER.warn("Unknown time of day: {}", drop.getTimeOfDay());
                    return false;
            }
        }
        
        // Check fishing level requirement
        if (drop.getMinFishingLevel() > 0 && player.experienceLevel < drop.getMinFishingLevel()) {
            return false;
        }
        
        // Check enchantment requirements
        ItemStack fishingRod = player.getMainHandItem(); // Assuming they're fishing with main hand
        
        if (drop.requiresLure() && fishingRod.getEnchantmentLevel(Enchantments.FISHING_LUCK) == 0) {
            return false;
        }
        
        if (drop.requiresLuckOfSea() && fishingRod.getEnchantmentLevel(Enchantments.FISHING_LUCK) == 0) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Processes a fishing drop by giving rewards and executing commands.
     */
    private static void processFishingDrop(FishingDrop drop, ServerPlayer player, ServerLevel level, BlockPos pos) {
        // Give rewards
        if (drop.hasRewards()) {
            for (FishingReward reward : drop.getRewards()) {
                if (RANDOM.nextDouble() < reward.getChance()) {
                    giveReward(reward, player, level, pos);
                }
            }
        }
        
        // Execute commands
        if (drop.hasCommands()) {
            executeCommands(drop.getCommands(), player, level, pos);
        }
    }
    
    /**
     * Gives a fishing reward to a player.
     */
    private static void giveReward(FishingReward reward, ServerPlayer player, ServerLevel level, BlockPos pos) {
        try {
            // Get the item
            ResourceLocation itemId = new ResourceLocation(reward.getItem());
            var item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) {
                LOGGER.error("Invalid item ID in fishing reward: {}", reward.getItem());
                return;
            }
            
            // Calculate amount
            int amount;
            if (reward.getCount() > 0) {
                amount = reward.getCount();
            } else {
                amount = RANDOM.nextInt(reward.getMinCount(), reward.getMaxCount() + 1);
            }
            
            // Create the item stack
            ItemStack stack = new ItemStack(item, amount);
            
            // Apply NBT if present
            if (reward.hasNbt()) {
                try {
                    CompoundTag nbt = TagParser.parseTag(reward.getNbt());
                    stack.setTag(nbt);
                } catch (Exception e) {
                    LOGGER.error("Invalid NBT data in fishing reward: {}", reward.getNbt(), e);
                }
            }
            
            // Spawn the item in the world
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;
            
            ItemEntity itemEntity = new ItemEntity(level, x, y, z, stack);
            itemEntity.setPickUpDelay(10); // Short delay before pickup
            level.addFreshEntity(itemEntity);
            
        } catch (Exception e) {
            LOGGER.error("Failed to give fishing reward", e);
        }
    }
    
    /**
     * Executes a list of commands with player context.
     */
    private static void executeCommands(List<String> commands, ServerPlayer player, ServerLevel level, BlockPos pos) {
        MinecraftServer server = level.getServer();
        if (server == null) return;
        
        for (String command : commands) {
            try {
                // Replace placeholders
                String processedCommand = command
                    .replace("{player}", player.getGameProfile().getName())
                    .replace("{uuid}", player.getStringUUID())
                    .replace("{x}", String.valueOf(pos.getX()))
                    .replace("{y}", String.valueOf(pos.getY()))
                    .replace("{z}", String.valueOf(pos.getZ()));
                
                // Execute command
                CommandSourceStack source = server.createCommandSourceStack()
                    .withLevel(level)
                    .withPosition(player.position())
                    .withEntity(player);
                
                server.getCommands().performPrefixedCommand(source, processedCommand);
                
            } catch (Exception e) {
                LOGGER.error("Failed to execute fishing command: {}", command, e);
            }
        }
    }
}
