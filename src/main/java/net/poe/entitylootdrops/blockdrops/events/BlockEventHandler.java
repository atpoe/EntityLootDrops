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
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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

        // Check if this block is currently regenerating (i.e., is a replacement block)
        if (BlockRegenerationManager.isScheduledForRegeneration(level, pos)) {
            // This is a replacement block that's scheduled for regeneration
            if (event.getPlayer() instanceof ServerPlayer serverPlayer) {
                if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
                    // Creative mode player can break it - cancel regeneration
                    BlockRegenerationManager.cancelRegeneration(level, pos);
                    LOGGER.debug("Creative mode player {} broke regenerating block at {} - regeneration cancelled",
                            serverPlayer.getGameProfile().getName(), pos);
                    // Allow the break to proceed (do not cancel event)
                    return;
                } else {
                    // Non-creative player cannot break regenerating blocks
                    event.setCanceled(true);
                    serverPlayer.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§cThis block is regenerating and cannot be broken!"), true
                    );
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

        // Find applicable block drop entries using BlockConfig methods
        List<BlockDropEntry> applicableEntries = BlockConfig.getApplicableDrops(blockIdString);

        if (applicableEntries.isEmpty()) {
            return;
        }

        boolean shouldCancelDefaultDrops = false;
        boolean hasRegeneratingEntry = false;
        BlockDropEntry regeneratingEntry = null;
        boolean hasCustomDrops = false;

        // Process each applicable entry
        for (BlockDropEntry entry : applicableEntries) {
            // Check if this entry should apply to this specific block break
            if (!shouldProcessEntry(entry, event.getPlayer(), level, pos, state)) {
                continue;
            }

            // Check for regeneration BEFORE drop chance (regeneration should happen regardless of drops)
            if (entry.canRegenerate() && !hasRegeneratingEntry) {
                hasRegeneratingEntry = true;
                regeneratingEntry = entry;
            }

            // Check drop chance for actual drops
            if (entry.getDropChance() < 100.0f && RANDOM.nextFloat() * 100.0f > entry.getDropChance()) {
                continue; // Skip drops but regeneration still happens
            }

            // Handle item drops
            if (entry.getItemId() != null && !entry.getItemId().isEmpty()) {
                dropCustomItem(level, pos, entry);
                hasCustomDrops = true;
            }

            // Handle command execution
            if (entry.getCommand() != null && !entry.getCommand().isEmpty()) {
                executeCommand(level, pos, entry, event.getPlayer());
            }

            // Check for default drops handling - only cancel if we have custom drops and allowDefaultDrops is false
            if (entry.isReplaceDefaultDrops() && hasCustomDrops && !entry.isAllowDefaultDrops()) {
                shouldCancelDefaultDrops = true;
            }
        }

        // Handle block regeneration FIRST - this needs to happen before canceling the event
        if (hasRegeneratingEntry && regeneratingEntry != null) {
            handleBlockRegeneration(level, pos, state, regeneratingEntry, event);
        }

        // Handle default drops cancellation AFTER regeneration is set up
        if (shouldCancelDefaultDrops) {
            event.setCanceled(true);
        }
    }

    /**
     * Checks if a drop entry should be processed for this block break.
     */
    private static boolean shouldProcessEntry(BlockDropEntry entry, net.minecraft.world.entity.player.Player player,
                                              ServerLevel level, BlockPos pos, BlockState state) {
        // Check if player break is required
        if (entry.isRequirePlayerBreak() && player == null) {
            return false;
        }

        // Check required tool
        if (entry.getRequiredTool() != null && !entry.getRequiredTool().isEmpty() && player != null) {
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.isEmpty()) {
                return false;
            }

            ResourceLocation toolId = ForgeRegistries.ITEMS.getKey(heldItem.getItem());
            if (toolId == null || !toolId.toString().equals(entry.getRequiredTool())) {
                return false;
            }
        }

        // Check required enchantment
        if (entry.getRequiredEnchantment() != null && !entry.getRequiredEnchantment().isEmpty() && player != null) {
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.isEmpty()) {
                return false;
            }

            try {
                ResourceLocation enchantmentId = new ResourceLocation(entry.getRequiredEnchantment());
                Enchantment requiredEnchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
                if (requiredEnchantment == null) {
                    return false;
                }

                int enchantmentLevel = EnchantmentHelper.getItemEnchantmentLevel(requiredEnchantment, heldItem);
                if (enchantmentLevel < entry.getRequiredEnchantLevel()) {
                    return false;
                }
            } catch (Exception e) {
                LOGGER.error("Invalid required enchantment ID: {}", entry.getRequiredEnchantment(), e);
                return false;
            }
        }

        return true;
    }

    /**
     * Drops a custom item based on the drop entry configuration.
     */
    private static void dropCustomItem(ServerLevel level, BlockPos pos, BlockDropEntry entry) {
        try {
            ResourceLocation itemId = new ResourceLocation(entry.getItemId());
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) {
                LOGGER.error("Invalid item ID: {}", entry.getItemId());
                return;
            }

            // Calculate drop amount
            int amount = entry.getMinAmount();
            if (entry.getMaxAmount() > entry.getMinAmount()) {
                amount = RANDOM.nextInt(entry.getMaxAmount() - entry.getMinAmount() + 1) + entry.getMinAmount();
            }

            // Apply double drops if active
            if (BlockConfig.isBlockDoubleDropsActive()) {
                amount *= 2;
            }

            // Create item stack
            ItemStack itemStack = new ItemStack(item, amount);

            // Apply NBT data if specified (this is where enchantments can be added via NBT)
            if (entry.getNbtData() != null && !entry.getNbtData().isEmpty()) {
                try {
                    CompoundTag nbt = TagParser.parseTag(entry.getNbtData());
                    itemStack.setTag(nbt);
                } catch (Exception e) {
                    LOGGER.error("Invalid NBT data for item {}: {}", entry.getItemId(), entry.getNbtData(), e);
                }
            }

            // Drop the item
            Vec3 dropPos = Vec3.atCenterOf(pos);
            ItemEntity itemEntity = new ItemEntity(level, dropPos.x, dropPos.y + 0.5, dropPos.z, itemStack);
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);

        } catch (Exception e) {
            LOGGER.error("Failed to drop custom item: {}", entry.getItemId(), e);
        }
    }

    /**
     * Executes a command specified in the drop entry.
     */
    private static void executeCommand(ServerLevel level, BlockPos pos, BlockDropEntry entry,
                                       net.minecraft.world.entity.player.Player player) {
        try {
            // Check command chance
            if (entry.getCommandChance() < 100.0f && RANDOM.nextFloat() * 100.0f > entry.getCommandChance()) {
                return;
            }

            String command = entry.getCommand();
            if (command == null || command.isEmpty()) {
                return;
            }

            // Replace placeholders
            command = command.replace("{x}", String.valueOf(pos.getX()));
            command = command.replace("{y}", String.valueOf(pos.getY()));
            command = command.replace("{z}", String.valueOf(pos.getZ()));

            if (player != null) {
                command = command.replace("{player}", player.getGameProfile().getName());
            }

            // Execute command
            MinecraftServer server = level.getServer();
            if (server != null) {
                CommandSourceStack commandSource = server.createCommandSourceStack()
                        .withLevel(level)
                        .withPosition(Vec3.atCenterOf(pos))
                        .withSuppressedOutput();

                server.getCommands().performPrefixedCommand(commandSource, command);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to execute command: {}", entry.getCommand(), e);
        }
    }

    /**
     * Handles block regeneration by scheduling the replacement after allowing default drops.
     */
    private static void handleBlockRegeneration(ServerLevel level, BlockPos pos, BlockState originalState,
                                                BlockDropEntry entry, BlockEvent.BreakEvent event) {
        try {
            String replacementBlockId = entry.getBrokenBlockReplace();
            if (replacementBlockId == null || replacementBlockId.isEmpty()) {
                return;
            }

            // Get the replacement block
            ResourceLocation blockId = new ResourceLocation(replacementBlockId);
            Block replacementBlock = ForgeRegistries.BLOCKS.getValue(blockId);
            if (replacementBlock == null) {
                LOGGER.error("Invalid replacement block ID: {}", replacementBlockId);
                return;
            }

            // Schedule the regeneration with a 3-tick delay to allow the block to break and drop items first
            level.getServer().tell(new net.minecraft.server.TickTask(level.getServer().getTickCount() + 3, () -> {
                // Place the replacement block after default drops have occurred
                BlockState replacementState = replacementBlock.defaultBlockState();
                level.setBlock(pos, replacementState, 3);

                // Schedule regeneration
                int respawnTime = entry.getRespawnTime();
                if (respawnTime > 0) {
                    BlockRegenerationManager.scheduleRegeneration(level, pos, originalState, replacementBlockId, respawnTime);
                    LOGGER.debug("Scheduled regeneration for block at {} in {} seconds", pos, respawnTime);
                }
            }));

        } catch (Exception e) {
            LOGGER.error("Failed to handle block regeneration at {}: {}", pos, e.getMessage(), e);
        }
    }
}