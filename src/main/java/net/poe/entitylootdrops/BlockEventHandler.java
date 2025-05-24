package net.poe.entitylootdrops;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Handles block break events and manages custom block drops.
 */
@Mod.EventBusSubscriber(modid = EntityLootDrops.MOD_ID)
public class BlockEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random RANDOM = new Random();
    
    /**
     * Handles block break events and processes custom drops.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        // Get basic event information
        BlockState blockState = event.getState();
        Block block = blockState.getBlock();
        BlockPos pos = event.getPos();
        Player player = event.getPlayer();
        
        // Get the block ID
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
        if (blockId == null) return;
        
        String blockIdStr = blockId.toString();
        boolean isPlayerBreak = player != null;
        
        // Check for block drops that should be cancelled
        boolean cancelVanillaDrops = false;
        List<String> allowedModIDs = new ArrayList<>();
        
        // Process global block drops
        List<BlockConfig.CustomBlockDropEntry> globalDrops = BlockConfig.getGlobalBlockDrops();
        for (BlockConfig.CustomBlockDropEntry drop : globalDrops) {
            if (!drop.isAllowDefaultDrops()) {
                cancelVanillaDrops = true;
                if (drop.getAllowModIDs() != null) {
                    allowedModIDs.addAll(drop.getAllowModIDs());
                }
                break;
            }
        }
        
        // Process block-specific drops
        List<BlockConfig.BlockDropEntry> blockSpecificDrops = BlockConfig.getBlockDrops(blockIdStr);
        
        // Check for replacement drops first
        boolean hasReplacementDrop = false;
        for (BlockConfig.BlockDropEntry drop : blockSpecificDrops) {
            if (drop.isReplaceDefaultDrops() && checkToolRequirements(player instanceof ServerPlayer ? (ServerPlayer) player : null, drop)) {
                // Calculate drop chance
                float chance = drop.getDropChance();
                if (BlockConfig.isBlockDropChanceEventActive()) {
                    chance *= 2.0f;
                }
                
                // Roll for drop chance
                if (RANDOM.nextFloat() * 100 <= chance) {
                    // Cancel vanilla drops
                    event.setCanceled(true);
                    
                    // Create and spawn the replacement item
                    if (event.getLevel() instanceof ServerLevel) {
                        ServerLevel level = (ServerLevel) event.getLevel();
                        
                        // Create the item stack
                        ResourceLocation itemId = new ResourceLocation(drop.getItemId());
                        Item item = ForgeRegistries.ITEMS.getValue(itemId);
                        
                        if (item != null) {
                            int amount = drop.getMinAmount();
                            if (drop.getMaxAmount() > drop.getMinAmount()) {
                                amount += RANDOM.nextInt(drop.getMaxAmount() - drop.getMinAmount() + 1);
                            }
                            
                            // Apply double drops event if active
                            if (BlockConfig.isBlockDoubleDropsActive()) {
                                amount *= 2;
                            }
                            
                            ItemStack stack = new ItemStack(item, amount);
                            
                            // Apply NBT data if specified
                            if (drop.hasNbtData()) {
                                try {
                                    CompoundTag nbt = TagParser.parseTag(drop.getNbtData());
                                    stack.setTag(nbt);
                                } catch (CommandSyntaxException e) {
                                    LOGGER.error("Invalid NBT format for {}: {}", drop.getItemId(), e.getMessage());
                                }
                            }
                            
                            // Spawn the item in the world
                            ItemEntity itemEntity = new ItemEntity(level, 
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 
                                stack);
                            level.addFreshEntity(itemEntity);
                            
                            // Handle command execution
                            if (drop.hasCommand() && player != null) {
                                float cmdChance = drop.getCommandChance();
                                if (cmdChance > 0 && RANDOM.nextFloat() * 100 <= cmdChance) {
                                    executeCommand(drop.getCommand(), player instanceof ServerPlayer ? (ServerPlayer) player : null, pos);
                                }
                            }
                        }
                    }
                    
                    hasReplacementDrop = true;
                    break;
                }
            }
        }
        
        // If we already handled a replacement drop, return early
        if (hasReplacementDrop) {
            return;
        }
        
        // Continue with normal drop processing
        for (BlockConfig.BlockDropEntry drop : blockSpecificDrops) {
            if (!drop.isAllowDefaultDrops() && !drop.isReplaceDefaultDrops()) {
                cancelVanillaDrops = true;
                if (drop.getAllowModIDs() != null) {
                    allowedModIDs.addAll(drop.getAllowModIDs());
                }
                break;
            }
        }
        
        // Cancel vanilla drops if needed
        if (cancelVanillaDrops) {
            event.setCanceled(true);
        }
        
        // Process drops
        if (isPlayerBreak) {
            ServerPlayer serverPlayer = player instanceof ServerPlayer ? (ServerPlayer) player : null;
            
            // Process global drops
            processDrops(event, globalDrops, serverPlayer);
            
            // Process block-specific drops
            for (BlockConfig.BlockDropEntry drop : blockSpecificDrops) {
                if (drop.getBlockId().equals(blockIdStr)) {
                    processBlockDrop(event, drop, serverPlayer);
                }
            }
            
            // Process event drops if any events are active
            for (String eventName : BlockConfig.getActiveBlockEvents()) {
                List<BlockConfig.BlockDropEntry> eventDrops = BlockConfig.getEventBlockDrops(eventName);
                for (BlockConfig.BlockDropEntry drop : eventDrops) {
                    if (drop.getBlockId().equals(blockIdStr)) {
                        processBlockDrop(event, drop, serverPlayer);
                    }
                }
            }
        }
    }
    
    /**
     * Processes a list of drops for a block break event.
     */
    private static void processDrops(BlockEvent.BreakEvent event, List<BlockConfig.CustomBlockDropEntry> drops, ServerPlayer player) {
        for (BlockConfig.CustomBlockDropEntry drop : drops) {
            processBlockDrop(event, drop, player);
        }
    }
    
    /**
     * Processes a single drop entry for a block.
     */
    private static void processBlockDrop(BlockEvent.BreakEvent event, BlockConfig.CustomBlockDropEntry drop, ServerPlayer player) {
        try {
            // Skip if this is a replacement drop - those are handled separately
            if (drop instanceof BlockConfig.BlockDropEntry && ((BlockConfig.BlockDropEntry)drop).isReplaceDefaultDrops()) {
                return;
            }
            
            // Check if this drop requires a player break
            if (drop.isRequirePlayerBreak() && player == null) {
                return;
            }
            
            // Check tool requirements
            if (!checkToolRequirements(player, drop)) {
                return;
            }
            
            // Calculate drop chance
            float chance = drop.getDropChance();
            if (BlockConfig.isBlockDropChanceEventActive()) {
                chance *= 2.0f;
            }
            
            // Handle command execution
            if (drop.hasCommand() && player != null) {
                float cmdChance = drop.getCommandChance();
                if (cmdChance > 0 && RANDOM.nextFloat() * 100 <= cmdChance) {
                    executeCommand(drop.getCommand(), player, event.getPos());
                }
            }
            
            // Roll for drop chance
            if (RANDOM.nextFloat() * 100 <= chance) {
                ResourceLocation itemId = new ResourceLocation(drop.getItemId());
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                
                if (item != null) {
                    int amount = drop.getMinAmount();
                    if (drop.getMaxAmount() > drop.getMinAmount()) {
                        amount += RANDOM.nextInt(drop.getMaxAmount() - drop.getMinAmount() + 1);
                    }
                    
                    // Apply double drops event if active
                    if (BlockConfig.isBlockDoubleDropsActive()) {
                        amount *= 2;
                    }
                    
                    ItemStack stack = new ItemStack(item, amount);
                    
                    // Apply NBT data if specified
                    if (drop.hasNbtData()) {
                        try {
                            CompoundTag nbt = TagParser.parseTag(drop.getNbtData());
                            stack.setTag(nbt);
                        } catch (CommandSyntaxException e) {
                            LOGGER.error("Invalid NBT format for {}: {}", drop.getItemId(), e.getMessage());
                        }
                    }
                    
                    // Spawn the item in the world
                    if (event.getLevel() instanceof ServerLevel) {
                        ServerLevel level = (ServerLevel) event.getLevel();
                        BlockPos pos = event.getPos();
                        ItemEntity itemEntity = new ItemEntity(level, 
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 
                            stack);
                        level.addFreshEntity(itemEntity);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error processing block drop {}: {}", drop.getItemId(), e.getMessage());
        }
    }
    
    /**
     * Checks if the player's tool meets the requirements for the drop.
     */
    private static boolean checkToolRequirements(ServerPlayer player, BlockConfig.CustomBlockDropEntry drop) {
        if (player == null) return !drop.isRequirePlayerBreak();
        
        ItemStack tool = player.getMainHandItem();
        
        // Check required tool type
        if (drop.getRequiredTool() != null) {
            ResourceLocation toolId = ForgeRegistries.ITEMS.getKey(tool.getItem());
            if (toolId == null || !toolId.toString().equals(drop.getRequiredTool())) {
                return false;
            }
        }
        
        // Check required tool tier
        if (drop.getRequiredToolTier() != null && tool.getItem() instanceof TieredItem) {
            TieredItem tieredItem = (TieredItem) tool.getItem();
            if (!tieredItem.getTier().toString().equalsIgnoreCase(drop.getRequiredToolTier())) {
                return false;
            }
        }
        
        // Check required tool level
        if (drop.getRequiredToolLevel() > 0 && tool.getItem() instanceof TieredItem) {
            TieredItem tieredItem = (TieredItem) tool.getItem();
            if (tieredItem.getTier().getLevel() < drop.getRequiredToolLevel()) {
                return false;
            }
        }
        
        // Check required enchantment
        if (drop.getRequiredEnchantment() != null) {
            ResourceLocation enchId = new ResourceLocation(drop.getRequiredEnchantment());
            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchId);
            if (enchantment == null) return false;
            
            int level = EnchantmentHelper.getItemEnchantmentLevel(enchantment, tool);
            if (level < drop.getRequiredEnchantLevel()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Executes a command when a block is broken.
     */
    private static void executeCommand(String command, ServerPlayer player, BlockPos pos) {
        try {
            if (player == null) return;
            
            MinecraftServer server = player.getServer();
            if (server == null) return;
            
            ServerLevel level = player.serverLevel();
            String[] commands = command.replace("\\n", "\n").split("\n");
            
            for (String singleCommand : commands) {
                if (singleCommand.trim().isEmpty()) continue;
                
                // Replace placeholders in command
                String processedCommand = singleCommand
                    .replace("{player}", player.getName().getString())
                    .replace("{player_x}", String.format("%.2f", player.getX()))
                    .replace("{player_y}", String.format("%.2f", player.getY()))
                    .replace("{player_z}", String.format("%.2f", player.getZ()))
                    .replace("{block_x}", String.valueOf(pos.getX()))
                    .replace("{block_y}", String.valueOf(pos.getY()))
                    .replace("{block_z}", String.valueOf(pos.getZ()));
                
                // Create command source and execute
                server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack()
                        .withPosition(Vec3.atCenterOf(pos))
                        .withRotation(Vec2.ZERO)
                        .withLevel(level)
                        .withPermission(4)
                        .withEntity(player),
                    processedCommand
                );
            }
        } catch (Exception e) {
            LOGGER.error("Failed to execute command for block break: {}", e.getMessage());
        }
    }
}
