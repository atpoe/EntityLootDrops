package net.poe.entitylootdrops;

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
        for (FishingConfig.FishingDrop drop : FishingConfig.getFishingDrops()) {
            if (shouldTriggerDrop(drop, player, level, pos)) {
                processFishingDrop(drop, player, level, pos);
            }
        }
        
        // Process global fishing rewards
        for (FishingConfig.FishingReward reward : FishingConfig.getGlobalFishingRewards()) {
            if (RANDOM.nextDouble() < reward.getChance()) {
                giveReward(reward, player, level, pos);
            }
        }
    }
    
    /**
     * Checks if a fishing drop should trigger based on its conditions.
     */
    private static boolean shouldTriggerDrop(FishingConfig.FishingDrop drop, ServerPlayer player, ServerLevel level, BlockPos pos) {
        // Check chance
        if (RANDOM.nextDouble() > drop.getChance()) {
            return false;
        }
        
        // Check biome requirement
        if (drop.getBiome() != null) {
            Biome biome = level.getBiome(pos).value();
            ResourceLocation biomeId = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getKey(biome);
            if (biomeId == null || !biomeId.toString().equals(drop.getBiome())) {
                return false;
            }
        }
        
        // Check dimension requirement
        if (drop.getDimension() != null) {
            ResourceLocation dimensionId = level.dimension().location();
            if (!dimensionId.toString().equals(drop.getDimension())) {
                return false;
            }
        }
        
        // Check weather requirement
        if (drop.getWeather() != null) {
            boolean isRaining = level.isRaining();
            boolean isThundering = level.isThundering();
            
            switch (drop.getWeather().toLowerCase()) {
                case "clear":
                    if (isRaining) return false;
                    break;
                case "rain":
                    if (!isRaining || isThundering) return false;
                    break;
                case "thunder":
                    if (!isThundering) return false;
                    break;
            }
        }
        
        // Check time of day requirement
        if (drop.getTimeOfDay() != null) {
            long timeOfDay = level.getDayTime() % 24000;
            boolean isDay = timeOfDay >= 0 && timeOfDay < 12000;
            
            switch (drop.getTimeOfDay().toLowerCase()) {
                case "day":
                    if (!isDay) return false;
                    break;
                case "night":
                    if (isDay) return false;
                    break;
            }
        }
        
        // Check fishing rod enchantments
        ItemStack fishingRod = player.getMainHandItem();
        if (fishingRod.isEmpty()) {
            fishingRod = player.getOffhandItem();
        }
        
        if (!fishingRod.isEmpty()) {
            int lureLevel = fishingRod.getEnchantmentLevel(Enchantments.FISHING_LUCK);
            int luckLevel = fishingRod.getEnchantmentLevel(Enchantments.FISHING_SPEED);
            
            if (drop.requiresLure() && lureLevel == 0) {
                return false;
            }
            
            if (drop.requiresLuckOfSea() && luckLevel == 0) {
                return false;
            }
        }
        
        // Check minimum fishing level (using player's experience level as proxy)
        if (player.experienceLevel < drop.getMinFishingLevel()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Processes a fishing drop by giving rewards and executing commands.
     */
    private static void processFishingDrop(FishingConfig.FishingDrop drop, ServerPlayer player, ServerLevel level, BlockPos pos) {
        LOGGER.debug("Processing fishing drop: {} for player: {}", drop.getName(), player.getName().getString());
        
        // Give rewards
        if (drop.hasRewards()) {
            for (FishingConfig.FishingReward reward : drop.getRewards()) {
                if (RANDOM.nextDouble() < reward.getChance()) {
                    giveReward(reward, player, level, pos);
                }
            }
        }
        
        // Execute commands
        if (drop.hasCommands()) {
            executeCommands(player, drop.getCommands());
        }
    }
    
    /**
     * Gives a fishing reward to the player.
     */
    private static void giveReward(FishingConfig.FishingReward reward, ServerPlayer player, ServerLevel level, BlockPos pos) {
        try {
            ResourceLocation itemId = new ResourceLocation(reward.getItem());
            ItemStack itemStack = new ItemStack(ForgeRegistries.ITEMS.getValue(itemId));
            
            if (itemStack.isEmpty()) {
                LOGGER.warn("Invalid item for fishing reward: {}", reward.getItem());
                return;
            }
            
            // Determine count
            int count = reward.getCount();
            if (reward.getMaxCount() > reward.getMinCount()) {
                count = RANDOM.nextInt(reward.getMaxCount() - reward.getMinCount() + 1) + reward.getMinCount();
            }
            itemStack.setCount(count);
            
            // Apply NBT if present
            if (reward.hasNbt()) {
                try {
                    CompoundTag nbt = TagParser.parseTag(reward.getNbt());
                    itemStack.setTag(nbt);
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse NBT for fishing reward {}: {}", reward.getItem(), e.getMessage());
                }
            }
            
            // Give item to player or drop it
            if (!player.getInventory().add(itemStack)) {
                ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, itemStack);
                level.addFreshEntity(itemEntity);
            }
            
            LOGGER.debug("Gave fishing reward: {} x{} to player: {}", reward.getItem(), count, player.getName().getString());
            
        } catch (Exception e) {
            LOGGER.error("Failed to give fishing reward {}: {}", reward.getItem(), e.getMessage());
        }
    }
    
    /**
     * Executes commands for a fishing drop.
     */
    private static void executeCommands(ServerPlayer player, List<String> commands) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            LOGGER.error("Server is null, cannot execute fishing commands");
            return;
        }
        
        CommandSourceStack commandSource = server.createCommandSourceStack()
            .withSource(server)
            .withLevel(player.serverLevel())
            .withPosition(player.position())
            .withPermission(4);
        
        for (String command : commands) {
            try {
                String processedCommand = command
                    .replace("{player}", player.getName().getString())
                    .replace("{uuid}", player.getStringUUID())
                    .replace("{x}", String.valueOf((int)player.getX()))
                    .replace("{y}", String.valueOf((int)player.getY()))
                    .replace("{z}", String.valueOf((int)player.getZ()));
                
                LOGGER.debug("Executing fishing command: {}", processedCommand);
                server.getCommands().performPrefixedCommand(commandSource, processedCommand);
                
            } catch (Exception e) {
                LOGGER.error("Failed to execute fishing command: {} - Error: {}", command, e.getMessage());
            }
        }
    }
}
