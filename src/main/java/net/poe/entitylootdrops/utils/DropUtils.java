package net.poe.entitylootdrops.utils;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.poe.entitylootdrops.blockdrops.model.CustomBlockDropEntry;

/**
 * Utility class for handling drops and related functionality.
 */
public class DropUtils {
    private static final Logger LOGGER = LogManager.getLogger();
    
    /**
     * Creates an ItemStack from an item ID, amount, and optional NBT data.
     */
    public static ItemStack createItemStack(String itemId, int amount, String nbtData) {
        try {
            // Get the item from the registry
            ResourceLocation itemLocation = new ResourceLocation(itemId);
            Item item = ForgeRegistries.ITEMS.getValue(itemLocation);
            
            if (item == null) {
                LOGGER.warn("Unknown item ID: {}", itemId);
                return ItemStack.EMPTY;
            }
            
            // Create the item stack
            ItemStack itemStack = new ItemStack(item, amount);
            
            // Apply NBT data if provided
            if (nbtData != null && !nbtData.trim().isEmpty()) {
                try {
                    CompoundTag nbt = TagParser.parseTag(nbtData);
                    itemStack.setTag(nbt);
                } catch (CommandSyntaxException e) {
                    LOGGER.error("Failed to parse NBT data for item {}: {}", itemId, nbtData, e);
                }
            }
            
            return itemStack;
            
        } catch (Exception e) {
            LOGGER.error("Failed to create item stack for: {}", itemId, e);
            return ItemStack.EMPTY;
        }
    }
    
    /**
     * Checks if the player meets the tool requirements for a drop.
     */
    public static boolean checkToolRequirements(CustomBlockDropEntry drop, Player player) {
        if (player == null) {
            return true; // No player means no tool requirements
        }
        
        ItemStack heldItem = player.getMainHandItem();
        
        // Check required tool
        if (drop.getRequiredTool() != null && !drop.getRequiredTool().isEmpty()) {
            String heldItemId = ForgeRegistries.ITEMS.getKey(heldItem.getItem()).toString();
            if (!heldItemId.equals(drop.getRequiredTool())) {
                return false;
            }
        }
        
        // Check required tool tier (simplified check) - REMOVED
//        if (drop.getRequiredToolTier() != null && !drop.getRequiredToolTier().isEmpty()) {
//            // This is a simplified check - you might want to implement more sophisticated tier checking
//            String heldItemId = ForgeRegistries.ITEMS.getKey(heldItem.getItem()).toString();
//            if (!heldItemId.contains(drop.getRequiredToolTier().toLowerCase())) {
//                return false;
//            }
//        }
        
        // Check required enchantment
        if (drop.getRequiredEnchantment() != null && !drop.getRequiredEnchantment().isEmpty()) {
            ResourceLocation enchantmentLocation = new ResourceLocation(drop.getRequiredEnchantment());
            Enchantment requiredEnchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentLocation);
            
            if (requiredEnchantment != null) {
                int enchantmentLevel = EnchantmentHelper.getItemEnchantmentLevel(requiredEnchantment, heldItem);
                if (enchantmentLevel < drop.getRequiredEnchantLevel()) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Executes a command with placeholder replacement.
     */
    public static void executeCommand(ServerLevel level, BlockPos pos, Player player, String command) {
        if (command == null || command.trim().isEmpty()) {
            return;
        }
        
        try {
            // Replace placeholders in the command
            String processedCommand = replacePlaceholders(command, pos, player);
            
            // Split multiple commands by newline
            String[] commands = processedCommand.split("\\n");
            
            for (String cmd : commands) {
                cmd = cmd.trim();
                if (!cmd.isEmpty()) {
                    executeServerCommand(level, pos, cmd);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to execute command: {}", command, e);
        }
    }
    
    /**
     * Replaces placeholders in a command string.
     */
    private static String replacePlaceholders(String command, BlockPos pos, Player player) {
        String result = command;
        
        // Block position placeholders
        result = result.replace("{block_x}", String.valueOf(pos.getX()));
        result = result.replace("{block_y}", String.valueOf(pos.getY()));
        result = result.replace("{block_z}", String.valueOf(pos.getZ()));
        
        // Player placeholders
        if (player != null) {
            result = result.replace("{player}", player.getName().getString());
            result = result.replace("{player_x}", String.valueOf((int) player.getX()));
            result = result.replace("{player_y}", String.valueOf((int) player.getY()));
            result = result.replace("{player_z}", String.valueOf((int) player.getZ()));
        }
        
        return result;
    }
    
    /**
     * Executes a server command.
     */
    private static void executeServerCommand(ServerLevel level, BlockPos pos, String command) {
        try {
            // Create a command source at the block position
            CommandSourceStack commandSource = new CommandSourceStack(
                CommandSource.NULL,
                Vec3.atCenterOf(pos),
                Vec2.ZERO,
                level,
                4, // Permission level
                "BlockDrop",
                net.minecraft.network.chat.Component.literal("BlockDrop"),
                level.getServer(),
                null
            );
            
            // Execute the command
            level.getServer().getCommands().performPrefixedCommand(commandSource, command);
            
        } catch (Exception e) {
            LOGGER.error("Failed to execute server command: {}", command, e);
        }
    }
}
