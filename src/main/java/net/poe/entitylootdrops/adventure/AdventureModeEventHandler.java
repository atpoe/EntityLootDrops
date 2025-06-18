package net.poe.entitylootdrops.adventure;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class AdventureModeEventHandler {

    /**
     * Registers this event handler to the Forge event bus.
     */
    public static void register() {
        MinecraftForge.EVENT_BUS.register(new AdventureModeEventHandler());
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        // Only restrict Survival and Adventure mode players
        if (player.gameMode.isCreative()) return;
        if (!(event.getLevel() instanceof Level level)) return;
        ResourceLocation dimension = level.dimension().location();

        AdventureModeRule rule = AdventureModeConfigLoader.getActiveRuleForDimension(dimension);
        if (rule == null) return;

        BlockState state = event.getState();
        Block block = state.getBlock();
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);

        if (!isAllowed(blockId, block, rule.getAllowedBlockBreakIDs())) {
            event.setCanceled(true);
            if (rule.getBreakMessage() != null && !rule.getBreakMessage().isEmpty()) {
                player.displayClientMessage(Component.literal(rule.getBreakMessage()).withStyle(ChatFormatting.RED), true);
            }
        }
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Only restrict Survival and Adventure mode players
        if (player.gameMode.isCreative()) return;
        if (!(event.getLevel() instanceof Level level)) return;
        ResourceLocation dimension = level.dimension().location();

        AdventureModeRule rule = AdventureModeConfigLoader.getActiveRuleForDimension(dimension);
        if (rule == null || !rule.isPreventBlockPlacement()) return;

        BlockState state = event.getPlacedBlock();
        Block block = state.getBlock();
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);

        if (!isAllowed(blockId, block, rule.getAllowedPlacementIDs())) {
            event.setCanceled(true);
            if (rule.getPlaceMessage() != null && !rule.getPlaceMessage().isEmpty()) {
                player.displayClientMessage(Component.literal(rule.getPlaceMessage()).withStyle(ChatFormatting.RED), true);
            }
        }
    }

    @SubscribeEvent
    public void onBlockModify(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Only restrict Survival and Adventure mode players
        if (player.gameMode.isCreative()) return;
        Level level = event.getLevel();
        ResourceLocation dimension = level.dimension().location();

        AdventureModeRule rule = AdventureModeConfigLoader.getActiveRuleForDimension(dimension);
        if (rule == null || !rule.isPreventBlockModification()) return;

        BlockState state = level.getBlockState(event.getPos());
        Block block = state.getBlock();
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);

        if (!isAllowed(blockId, block, rule.getAllowedModificationIDs())) {
            event.setCanceled(true);
            if (rule.getModifyMessage() != null && !rule.getModifyMessage().isEmpty()) {
                player.displayClientMessage(Component.literal(rule.getModifyMessage()).withStyle(ChatFormatting.RED), true);
            }
        }
    }

    /**
     * Checks if the given block is allowed by the provided list of IDs or tags.
     */
    private boolean isAllowed(ResourceLocation blockId, Block block, List<String> allowedIDs) {
        if (allowedIDs == null) return false;
        String idString = blockId.toString();
        if (allowedIDs.contains(idString)) return true;
        // Tag support: "tag:namespace:tagname"
        for (String allowed : allowedIDs) {
            if (allowed.startsWith("tag:")) {
                String tagName = allowed.substring(4);
                ResourceLocation tagLoc = ResourceLocation.tryParse(tagName);
                if (tagLoc != null && block.builtInRegistryHolder().is(BlockTags.create(tagLoc))) {
                    return true;
                }
            }
        }
        return false;
    }
}