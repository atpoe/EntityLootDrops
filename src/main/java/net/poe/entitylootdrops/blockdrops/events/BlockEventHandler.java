package net.poe.entitylootdrops.blockdrops.events;

import java.util.ArrayList;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.poe.entitylootdrops.EntityLootDrops;
import net.poe.entitylootdrops.blockdrops.BlockConfig;
import net.poe.entitylootdrops.blockdrops.model.BlockDropEntry;
import net.poe.entitylootdrops.blockdrops.regeneration.BlockRegenerationManager;

/**
 * Event handler for block-related events to provide custom drops and handle regeneration.
 */
@Mod.EventBusSubscriber(modid = EntityLootDrops.MOD_ID)
public class BlockEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random RANDOM = new Random();
    
    /**
     * Handles block break events to provide custom drops and manage regeneration.
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        // Only process on server side
        if (event.getLevel().isClientSide()) {
            return;
        }
        
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        
        // Check if this block is currently regenerating
        if (BlockRegenerationManager.isScheduledForRegeneration(level, pos)) {
            // This is a replacement block that's scheduled for regeneration
            if (event.getPlayer() instanceof ServerPlayer serverPlayer) {
                if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
                    // Creative mode player can break it - cancel regeneration
                    BlockRegenerationManager.cancelRegeneration(level, pos);
                    LOGGER.debug("Creative mode player {} broke regenerating block at {} - regeneration cancelled", 
                               serverPlayer.getGameProfile().getName(), pos);
                    return; // Allow the break to proceed
                } else {
                    // Non-creative player cannot break regenerating blocks
                    event.setCanceled(true);
                    LOGGER.debug("Non-creative player {} tried to break regenerating block at {} - break cancelled", 
                               serverPlayer.getGameProfile().getName(), pos);
                    return;
                }
            } else {
                // Non-player break (like explosions) - cancel the break
                event.setCanceled(true);
                LOGGER.debug("Non-player attempted to break regenerating block at {} - break cancelled", pos);
                return;
            }
        }
        
        // Check if player is in creative mode - skip custom drops and regeneration for creative mode
        if (event.getPlayer() instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
                LOGGER.debug("Skipping custom drops and regeneration for creative mode player: {}", 
                           serverPlayer.getGameProfile().getName());
                return;
            }
        }
        
        // Get the block ID
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockId == null) {
            return;
        }
        
        String blockIdString = blockId.toString();
        
        // Find applicable block drop entries using existing BlockConfig methods
        List<BlockDropEntry> applicableEntries = getApplicableDrops(blockIdString);
        
        if (applicableEntries.isEmpty()) {
            return;
        }
        
        LOGGER.debug("Processing {} applicable drop entries for block: {}", applicableEntries.size(), blockIdString);
        
        boolean hasRegeneratingEntry = false;
        BlockDropEntry regeneratingEntry = null;
        
        // Process each applicable entry
        for (BlockDropEntry entry : applicableEntries) {
            // Check if this entry requires player break
            if (entry.isRequirePlayerBreak() && !(event.getPlayer() instanceof ServerPlayer)) {
                continue;
            }
            
            // Check tool requirements
            if (!checkToolRequirements(entry, event.getPlayer())) {
                continue;
            }
            
            // Check drop chance
            if (RANDOM.nextFloat() * 100.0f > entry.getDropChance()) {
                continue;
            }
            
            // Process the drop (commands are executed here AFTER all requirements are met)
            processBlockDrop(entry, level, pos, (ServerPlayer) event.getPlayer());
            
            // Check for regeneration (only one entry should handle regeneration per block)
            if (entry.canRegenerate() && !hasRegeneratingEntry) {
                hasRegeneratingEntry = true;
                regeneratingEntry = entry;
            }
            
            // Handle default drops replacement
            if (entry.isReplaceDefaultDrops()) {
                event.setCanceled(true);
                LOGGER.debug("Cancelled default drops for block: {}", blockIdString);
            }
        }
        
        // Handle block regeneration after processing all drops
        if (hasRegeneratingEntry && regeneratingEntry != null) {
            handleBlockRegeneration(level, pos, state, regeneratingEntry);
        }
    }
    
    /**
     * Gets all applicable drop entries for a specific block ID using existing BlockConfig methods.
     */
    private static List<BlockDropEntry> getApplicableDrops(String blockId) {
        List<BlockDropEntry> applicableEntries = new ArrayList<>();
        
        try {
            // Check normal drops
            List<BlockDropEntry> normalDrops = BlockConfig.getNormalDrops();
            for (BlockDropEntry entry : normalDrops) {
                if (entry.getBlockId() == null || entry.getBlockId().equals(blockId)) {
                    applicableEntries.add(entry);
                }
            }
            
            // Check active event drops
            for (String eventName : BlockConfig.getActiveBlockEvents()) {
                List<BlockDropEntry> eventDrops = BlockConfig.getEventDrops(eventName);
                if (eventDrops != null) {
                    for (BlockDropEntry entry : eventDrops) {
                        if (entry.getBlockId() == null || entry.getBlockId().equals(blockId)) {
                            applicableEntries.add(entry);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to get applicable drops for block: {}", blockId, e);
        }
        
        return applicableEntries;
    }
    
    /**
     * Checks if the player's tool meets the requirements for the drop.
     */
    private static boolean checkToolRequirements(BlockDropEntry entry, net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return true; // Non-player breaks don't have tool requirements
        }
        
        ItemStack tool = serverPlayer.getMainHandItem();
        
        // Check required tool
        if (entry.hasRequiredTool()) {
            ResourceLocation requiredToolId = new ResourceLocation(entry.getRequiredTool());
            Item requiredTool = ForgeRegistries.ITEMS.getValue(requiredToolId);
            if (requiredTool == null || !tool.is(requiredTool)) {
                LOGGER.debug("Tool requirement not met. Required: {}, Used: {}", 
                           entry.getRequiredTool(), ForgeRegistries.ITEMS.getKey(tool.getItem()));
                return false;
            }
        }
        
        // Check required tool tier
        if (entry.hasRequiredToolTier()) {
            // This would need to be implemented based on your tool tier system
            // For now, we'll skip this check
            LOGGER.debug("Tool tier check not implemented yet: {}", entry.getRequiredToolTier());
        }
        
        // Check required enchantment
        if (entry.hasRequiredEnchantment()) {
            ResourceLocation enchantmentId = new ResourceLocation(entry.getRequiredEnchantment());
            Enchantment requiredEnchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
            if (requiredEnchantment == null) {
                LOGGER.warn("Invalid enchantment ID: {}", entry.getRequiredEnchantment());
                return false;
            }
            
            int enchantLevel = tool.getEnchantmentLevel(requiredEnchantment);
            if (enchantLevel < entry.getRequiredEnchantLevel()) {
                LOGGER.debug("Enchantment level requirement not met. Required: {} level {}, Has: level {}", 
                           entry.getRequiredEnchantment(), entry.getRequiredEnchantLevel(), enchantLevel);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Processes a block drop entry by giving items and executing commands.
     * Commands are now executed ONLY after all requirements are validated.
     */
    private static void processBlockDrop(BlockDropEntry entry, ServerLevel level, BlockPos pos, ServerPlayer player) {
        try {
            // Execute command FIRST if present (since we've already validated all requirements)
            if (entry.hasCommand() && RANDOM.nextFloat() * 100.0f <= entry.getCommandChance()) {
                executeCommand(entry.getCommand(), level, pos, player);
            }
            
            // Give the item drop
            giveItemDrop(entry, level, pos, player);
            
        } catch (Exception e) {
            LOGGER.error("Failed to process block drop for entry: {}", entry.getItemId(), e);
        }
    }
    
    /**
     * Gives the item drop to the player or spawns it in the world.
     */
    private static void giveItemDrop(BlockDropEntry entry, ServerLevel level, BlockPos pos, ServerPlayer player) {
        try {
            // Get the item
            ResourceLocation itemId = new ResourceLocation(entry.getItemId());
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) {
                LOGGER.error("Invalid item ID in block drop: {}", entry.getItemId());
                return;
            }
            
            // Calculate amount
            int amount;
            if (entry.getMinAmount() == entry.getMaxAmount()) {
                amount = entry.getMinAmount();
            } else {
                amount = RANDOM.nextInt(entry.getMinAmount(), entry.getMaxAmount() + 1);
            }
            
            // Apply double drops event if active
            if (BlockConfig.isBlockDoubleDropsActive()) {
                amount *= 2;
                LOGGER.debug("Doubled drop amount for {} to {}", entry.getItemId(), amount);
            }
            
            // Create the item stack
            ItemStack stack = new ItemStack(item, amount);
            
            // Apply NBT if present
            if (entry.hasNbtData()) {
                try {
                    CompoundTag nbt = TagParser.parseTag(entry.getNbtData());
                    stack.setTag(nbt);
                } catch (Exception e) {
                    LOGGER.error("Invalid NBT data in block drop: {}", entry.getNbtData(), e);
                }
            }
            
            // Spawn the item in the world
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;
            
            ItemEntity itemEntity = new ItemEntity(level, x, y, z, stack);
            itemEntity.setPickUpDelay(10); // Short delay before pickup
            level.addFreshEntity(itemEntity);
            
            LOGGER.debug("Spawned {} x{} at {}", entry.getItemId(), amount, pos);
            
        } catch (Exception e) {
            LOGGER.error("Failed to give item drop", e);
        }
    }
    
    /**
     * Executes a command with player and position context.
     * This method is now only called AFTER all requirements have been validated.
     */
    private static void executeCommand(String command, ServerLevel level, BlockPos pos, ServerPlayer player) {
        MinecraftServer server = level.getServer();
        if (server == null) return;
        
        try {
            // Replace placeholders
            String processedCommand = command
                .replace("{player}", player.getGameProfile().getName())
                .replace("{uuid}", player.getStringUUID())
                .replace("{x}", String.valueOf(pos.getX()))
                .replace("{y}", String.valueOf(pos.getY()))
                .replace("{z}", String.valueOf(pos.getZ()))
                .replace("{block_x}", String.valueOf(pos.getX()))
                .replace("{block_y}", String.valueOf(pos.getY()))
                .replace("{block_z}", String.valueOf(pos.getZ()));
            
            // Execute command
            CommandSourceStack source = server.createCommandSourceStack()
                .withLevel(level)
                .withPosition(player.position())
                .withEntity(player)
                .withPermission(4);
            
            server.getCommands().performPrefixedCommand(source, processedCommand);
            
            LOGGER.debug("Executed command: {}", processedCommand);
            
        } catch (Exception e) {
            LOGGER.error("Failed to execute block drop command: {}", command, e);
        }
    }
    
    /**
     * Handles block regeneration by scheduling it with the regeneration manager.
     */
    private static void handleBlockRegeneration(ServerLevel level, BlockPos pos, BlockState originalState, BlockDropEntry entry) {
        try {
            LOGGER.debug("Scheduling regeneration for block {} at {} (replace with: {}, time: {}s)", 
                        ForgeRegistries.BLOCKS.getKey(originalState.getBlock()), 
                        pos, entry.getBrokenBlockReplace(), entry.getRespawnTime());
            
            // Schedule the regeneration
            BlockRegenerationManager.scheduleRegeneration(
                level,
                pos,
                originalState,
                entry.getBrokenBlockReplace(),
                entry.getRespawnTime()
            );
            
        } catch (Exception e) {
            LOGGER.error("Failed to handle block regeneration at {}", pos, e);
        }
    }
}
