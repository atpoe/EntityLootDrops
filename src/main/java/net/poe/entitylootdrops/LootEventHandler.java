package net.poe.entitylootdrops;

import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.advancements.Advancement;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = EntityLootDrops.MOD_ID)
public class LootEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random RANDOM = new Random();
    
    @SubscribeEvent
    public static void onEntityDrop(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        
        if (!(event.getSource().getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getSource().getEntity();
        
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityId == null) return;
        
        String entityIdStr = entityId.toString();
        boolean isHostile = entity instanceof Enemy;
        
        processEntityDrops(event, entityIdStr, LootConfig.getNormalDrops(), player);
        
        if (isHostile) {
            processDrops(event, LootConfig.getNormalHostileDrops(), player);
        }
        
        for (String eventName : LootConfig.getActiveEvents()) {
            List<LootConfig.EntityDropEntry> eventDropList = LootConfig.getEventDrops().get(eventName);
            if (eventDropList != null) {
                processEntityDrops(event, entityIdStr, eventDropList, player);
            }
            
            if (isHostile) {
                processDrops(event, LootConfig.getEventHostileDrops(eventName), player);
            }
        }
    }
    
    private static void processEntityDrops(LivingDropsEvent event, String entityIdStr, 
                                         List<LootConfig.EntityDropEntry> dropsList, Player player) {
        for (LootConfig.EntityDropEntry drop : dropsList) {
            if (drop.getEntityId().equals(entityIdStr)) {
                processDropEntry(event, drop, player);
            }
        }
    }
    
    private static void processDrops(LivingDropsEvent event, List<LootConfig.CustomDropEntry> drops, Player player) {
        for (LootConfig.CustomDropEntry drop : drops) {
            processDropEntry(event, drop, player);
        }
    }
    
    private static void processDropEntry(LivingDropsEvent event, LootConfig.CustomDropEntry drop, Player player) {
        try {
            // Check dimension requirement first
            if (drop.hasRequiredDimension()) {
                ResourceLocation playerDimension = player.level().dimension().location();
                String requiredDimension = drop.getRequiredDimension();
                
                if (!playerDimension.toString().equals(requiredDimension)) {
                    LOGGER.debug("Skipping drop {} - wrong dimension (player in {}, required {})", 
                        drop.getItemId(), playerDimension, requiredDimension);
                    return;
                }
                LOGGER.debug("Dimension requirement met for {}: {}", drop.getItemId(), requiredDimension);
            }
            
            // Check advancement requirement
            if (drop.hasRequiredAdvancement() && player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                ResourceLocation advancement = new ResourceLocation(drop.getRequiredAdvancement());
                if (!hasAdvancement(serverPlayer, advancement)) {
                    LOGGER.debug("Skipping drop {} - missing advancement {}", 
                        drop.getItemId(), advancement);
                    return;
                }
                LOGGER.debug("Advancement requirement met for {}: {}", drop.getItemId(), advancement);
            }
            
            // Check effect requirement
            if (drop.hasRequiredEffect()) {
                if (!hasEffect(player, drop.getRequiredEffect())) {
                    LOGGER.debug("Skipping drop {} - missing effect {}", 
                        drop.getItemId(), drop.getRequiredEffect());
                    return;
                }
                LOGGER.debug("Effect requirement met for {}: {}", drop.getItemId(), drop.getRequiredEffect());
            }
            
            // Check equipment requirement
            if (drop.hasRequiredEquipment()) {
                if (!hasEquipment(player, drop.getRequiredEquipment())) {
                    LOGGER.debug("Skipping drop {} - missing equipment {}", 
                        drop.getItemId(), drop.getRequiredEquipment());
                    return;
                }
                LOGGER.debug("Equipment requirement met for {}: {}", drop.getItemId(), drop.getRequiredEquipment());
            }
            
            // Calculate drop chance
            float chance = drop.getDropChance();
            if (LootConfig.isDropChanceEventActive()) {
                chance *= 2.0f;
                LOGGER.debug("Drop chance doubled for {}: {}% -> {}%", 
                    drop.getItemId(), drop.getDropChance(), chance);
            }
            
            // Process command if present
            if (drop.hasCommand() && player instanceof ServerPlayer) {
                float cmdChance = drop.getCommandChance();
                if (RANDOM.nextFloat() * 100 <= cmdChance) {
                    executeCommand(drop.getCommand(), (ServerPlayer) player, event.getEntity());
                    LOGGER.debug("Executed command for {} with {}% chance", drop.getItemId(), cmdChance);
                }
            }
            
            // Roll for item drop
            if (RANDOM.nextFloat() * 100 <= chance) {
                try {
                    ResourceLocation itemId = new ResourceLocation(drop.getItemId());
                    Item item = ForgeRegistries.ITEMS.getValue(itemId);
                    
                    if (item != null) {
                        // Calculate amount
                        int amount = drop.getMinAmount();
                        if (drop.getMaxAmount() > drop.getMinAmount()) {
                            amount += RANDOM.nextInt(drop.getMaxAmount() - drop.getMinAmount() + 1);
                        }
                        
                        // Create item stack
                        ItemStack stack = new ItemStack(item, amount);
                        
                        // Apply NBT data if present
                        if (drop.hasNbtData()) {
                            try {
                                CompoundTag nbt = TagParser.parseTag(drop.getNbtData());
                                stack.setTag(nbt);
                                LOGGER.debug("Applied NBT data to {}: {}", drop.getItemId(), drop.getNbtData());
                            } catch (CommandSyntaxException e) {
                                LOGGER.error("Invalid NBT format for {}: {}", drop.getItemId(), e.getMessage());
                            }
                        }
                        
                        // Spawn the item
                        event.getEntity().spawnAtLocation(stack);
                        
                        // Log the drop
                        if (LootConfig.isDropChanceEventActive()) {
                            LOGGER.debug("Dropped {} x{} (doubled chance: {}%)", 
                                drop.getItemId(), amount, chance);
                        } else {
                            LOGGER.debug("Dropped {} x{} (chance: {}%)", 
                                drop.getItemId(), amount, chance);
                        }
                    } else {
                        LOGGER.error("Invalid item ID for drop: {}", drop.getItemId());
                    }
                } catch (Exception e) {
                    LOGGER.error("Error processing drop {}: {}", drop.getItemId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected error processing drop {}: {}", 
                drop.getItemId(), e.getMessage(), e);
        }
    }
    
    private static boolean hasAdvancement(ServerPlayer player, ResourceLocation advancement) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;
        
        Advancement adv = server.getAdvancements().getAdvancement(advancement);
        if (adv == null) return false;
        
        return player.getAdvancements().getOrStartProgress(adv).isDone();
    }
    
    private static boolean hasEffect(Player player, String effectId) {
        ResourceLocation effectResource = new ResourceLocation(effectId);
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectResource);
        if (effect == null) return false;
        
        return player.hasEffect(effect);
    }
    
    private static boolean hasEquipment(Player player, String itemId) {
        ResourceLocation itemResource = new ResourceLocation(itemId);
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        
        return (ForgeRegistries.ITEMS.getKey(mainHand.getItem()).equals(itemResource) ||
                ForgeRegistries.ITEMS.getKey(offHand.getItem()).equals(itemResource));
    }
    
    private static void executeCommand(String command, ServerPlayer player, LivingEntity killedEntity) {
        try {
            MinecraftServer server = player.getServer();
            if (server == null) return;
            
            ServerLevel level = (ServerLevel) player.level();
            
            String processedCommand = command
                .replace("{player}", player.getName().getString())
                .replace("{player_x}", String.format("%.2f", player.getX()))
                .replace("{player_y}", String.format("%.2f", player.getY()))
                .replace("{player_z}", String.format("%.2f", player.getZ()))
                .replace("{entity}", killedEntity.getName().getString())
                .replace("{entity_id}", ForgeRegistries.ENTITY_TYPES.getKey(killedEntity.getType()).toString())
                .replace("{entity_x}", String.format("%.2f", killedEntity.getX()))
                .replace("{entity_y}", String.format("%.2f", killedEntity.getY()))
                .replace("{entity_z}", String.format("%.2f", killedEntity.getZ()));
            
            CommandSourceStack source = server.createCommandSourceStack()
                .withPosition(Vec3.atCenterOf(killedEntity.blockPosition()))
                .withRotation(Vec2.ZERO)
                .withLevel(level)
                .withPermission(4)
                .withEntity(player);
            
            server.getCommands().performPrefixedCommand(source, processedCommand);
            LOGGER.debug("Executed command on mob death: {}", processedCommand);
        } catch (Exception e) {
            LOGGER.error("Failed to execute command: {} - {}", command, e.getMessage());
        }
    }
}
