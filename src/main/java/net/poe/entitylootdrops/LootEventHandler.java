package net.poe.entitylootdrops;

import java.util.ArrayList;
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
import net.minecraft.world.entity.item.ItemEntity;
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

/**
 * Main event handler for the EntityLootDrops mod.
 * Listens for entity death events and processes custom drops based on configuration.
 */
@Mod.EventBusSubscriber(modid = EntityLootDrops.MOD_ID)
public class LootEventHandler {
    // Logger for this class
    private static final Logger LOGGER = LogManager.getLogger();
    // Random number generator for drop chances and amounts
    private static final Random RANDOM = new Random();
    
    /**
     * Main event handler for entity drops.
     * This is called whenever an entity dies and drops items.
     * 
     * @param event The LivingDropsEvent containing information about the entity and its drops
     */
    @SubscribeEvent
    public static void onEntityDrop(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        
        // Only process drops if a player killed the entity
        if (!(event.getSource().getEntity() instanceof Player)) {
            return;
        }
        
        // If double drops is active, double the amount of vanilla drops
        if (LootConfig.isDoubleDropsActive()) {
            List<ItemEntity> doubledDrops = new ArrayList<>();
            
            // Process each vanilla drop and double its amount
            event.getDrops().forEach(itemEntity -> {
                ItemStack originalStack = itemEntity.getItem();
                
                // Create a new stack with double the amount
                ItemStack doubledStack = new ItemStack(
                    originalStack.getItem(),
                    originalStack.getCount() * 2
                );
                
                // Copy NBT data if present
                if (originalStack.hasTag()) {
                    doubledStack.setTag(originalStack.getTag().copy());
                }
                
                // Create a new item entity with the doubled stack
                ItemEntity doubledEntity = new ItemEntity(
                    itemEntity.level(),
                    itemEntity.getX(),
                    itemEntity.getY(),
                    itemEntity.getZ(),
                    doubledStack
                );
                
                // Copy motion from original
                doubledEntity.setDeltaMovement(itemEntity.getDeltaMovement());
                
                // Set pickup delay using the correct method
                doubledEntity.setDefaultPickUpDelay();
                
                doubledDrops.add(doubledEntity);
                
                LOGGER.debug("Doubled vanilla drop amount for {} to {}", 
                    ForgeRegistries.ITEMS.getKey(originalStack.getItem()), 
                    doubledStack.getCount());
            });
            
            // Replace original drops with doubled drops
            event.getDrops().clear();
            event.getDrops().addAll(doubledDrops);
        }
        
        Player player = (Player) event.getSource().getEntity();
        
        // Get the entity ID as a string (e.g., "minecraft:zombie")
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityId == null) return;
        
        String entityIdStr = entityId.toString();
        // Check if the entity is hostile (implements the Enemy interface)
        boolean isHostile = entity instanceof Enemy;
        
        // Process normal (always active) entity-specific drops
        processEntityDrops(event, entityIdStr, LootConfig.getNormalDrops(), player);
        
        // Process normal (always active) hostile mob drops if this is a hostile entity
        if (isHostile) {
            processDrops(event, LootConfig.getNormalHostileDrops(), player);
        }
        
        // Process event-specific drops for all active events
        for (String eventName : LootConfig.getActiveEvents()) {
            // Process entity-specific drops for this event
            List<LootConfig.EntityDropEntry> eventDropList = LootConfig.getEventDrops().get(eventName);
            if (eventDropList != null) {
                processEntityDrops(event, entityIdStr, eventDropList, player);
            }
            
            // Process hostile mob drops for this event if this is a hostile entity
            if (isHostile) {
                processDrops(event, LootConfig.getEventHostileDrops(eventName), player);
            }
        }
    }
    
    /**
     * Processes entity-specific drops for a given entity.
     * Only processes drops that match the entity ID.
     * 
     * @param event The LivingDropsEvent
     * @param entityIdStr The entity ID as a string
     * @param dropsList The list of entity drop entries to process
     * @param player The player who killed the entity
     */
    private static void processEntityDrops(LivingDropsEvent event, String entityIdStr, 
                                     List<LootConfig.EntityDropEntry> dropsList, Player player) {
        for (LootConfig.EntityDropEntry drop : dropsList) {
            // Only process drops for this specific entity type
            if (drop.getEntityId().equals(entityIdStr)) {
                processDropEntry(event, drop, player);
            }
        }
    }
    
    /**
     * Processes a list of custom drops.
     * Used for hostile mob drops that apply to all hostile entities.
     * 
     * @param event The LivingDropsEvent
     * @param drops The list of custom drop entries to process
     * @param player The player who killed the entity
     */
    private static void processDrops(LivingDropsEvent event, List<LootConfig.CustomDropEntry> drops, Player player) {
        for (LootConfig.CustomDropEntry drop : drops) {
            processDropEntry(event, drop, player);
        }
    }
    
    /**
     * Processes a single drop entry.
     * Checks all requirements, calculates drop chance, and spawns the item if successful.
     * Also handles command execution if specified.
     * 
     * @param event The LivingDropsEvent
     * @param drop The drop entry to process
     * @param player The player who killed the entity
     */
    private static void processDropEntry(LivingDropsEvent event, LootConfig.CustomDropEntry drop, Player player) {
        try {
            // Check dimension requirement
            if (drop.hasRequiredDimension()) {
                ResourceLocation playerDimension = player.level().dimension().location();
                String requiredDimension = drop.getRequiredDimension();
                
                // Skip if player is not in the required dimension
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
            
            // Check potion effect requirement
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
            
            // Calculate drop chance, applying double chance if the event is active
            float chance = drop.getDropChance();
            if (LootConfig.isDropChanceEventActive()) {
                chance *= 2.0f;
                LOGGER.debug("Drop chance doubled for {}: {}% -> {}%", 
                    drop.getItemId(), drop.getDropChance(), chance);
            }
            
            // Execute command if specified and chance check passes
            if (drop.hasCommand() && player instanceof ServerPlayer) {
                float cmdChance = drop.getCommandChance();
                if (RANDOM.nextFloat() * 100 <= cmdChance) {
                    executeCommand(drop.getCommand(), (ServerPlayer) player, event.getEntity());
                    LOGGER.debug("Executed command for {} with {}% chance", drop.getItemId(), cmdChance);
                }
            }
            
            // Roll for drop chance
            if (RANDOM.nextFloat() * 100 <= chance) {
                ResourceLocation itemId = new ResourceLocation(drop.getItemId());
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                
                if (item != null) {
                    // Calculate drop amount
                    int amount = drop.getMinAmount();
                    if (drop.getMaxAmount() > drop.getMinAmount()) {
                        amount += RANDOM.nextInt(drop.getMaxAmount() - drop.getMinAmount() + 1);
                    }
                    
                    // Double the amount if double drops is active
                    if (LootConfig.isDoubleDropsActive()) {
                        amount *= 2;
                        LOGGER.debug("Doubled drop amount for {} to {}", drop.getItemId(), amount);
                    }
                    
                    // Create the item stack
                    ItemStack stack = new ItemStack(item, amount);
                    
                    // Apply NBT data if specified
                    if (drop.hasNbtData()) {
                        try {
                            CompoundTag nbt = TagParser.parseTag(drop.getNbtData());
                            stack.setTag(nbt);
                            LOGGER.debug("Applied NBT data to {}: {}", drop.getItemId(), drop.getNbtData());
                        } catch (CommandSyntaxException e) {
                            LOGGER.error("Invalid NBT format for {}: {}", drop.getItemId(), e.getMessage());
                        }
                    }
                    
                    // Spawn the item in the world
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
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected error processing drop {}: {}", 
                drop.getItemId(), e.getMessage(), e);
        }
    }
    
    /**
     * Checks if a player has completed an advancement.
     * 
     * @param player The player to check
     * @param advancement The advancement ID
     * @return True if the player has completed the advancement
     */
    private static boolean hasAdvancement(ServerPlayer player, ResourceLocation advancement) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;
        
        // Get the advancement from the server
        Advancement adv = server.getAdvancements().getAdvancement(advancement);
        if (adv == null) return false;
        
        // Check if the player has completed it
        return player.getAdvancements().getOrStartProgress(adv).isDone();
    }
    
    /**
     * Checks if a player has an active potion effect.
     * 
     * @param player The player to check
     * @param effectId The effect ID
     * @return True if the player has the effect
     */
    private static boolean hasEffect(Player player, String effectId) {
        ResourceLocation effectResource = new ResourceLocation(effectId);
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectResource);
        if (effect == null) return false;
        
        return player.hasEffect(effect);
    }
    
    /**
     * Checks if a player has a specific item equipped.
     * Checks both main hand and offhand.
     * 
     * @param player The player to check
     * @param itemId The item ID
     * @return True if the player has the item equipped
     */
    private static boolean hasEquipment(Player player, String itemId) {
        ResourceLocation itemResource = new ResourceLocation(itemId);
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        
        return (ForgeRegistries.ITEMS.getKey(mainHand.getItem()).equals(itemResource) ||
                ForgeRegistries.ITEMS.getKey(offHand.getItem()).equals(itemResource));
    }
    
    /**
     * Executes a command when a drop occurs.
     * Replaces placeholders in the command with actual values.
     * 
     * @param command The command to execute
     * @param player The player who killed the entity
     * @param killedEntity The entity that was killed
     */
    private static void executeCommand(String command, ServerPlayer player, LivingEntity killedEntity) {
        try {
            MinecraftServer server = player.getServer();
            if (server == null) return;
            
            ServerLevel level = (ServerLevel) player.level();
            
            // Replace placeholders in the command with actual values
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
            
            // Create a command source with the player's position and permissions
            CommandSourceStack source = server.createCommandSourceStack()
                .withPosition(Vec3.atCenterOf(killedEntity.blockPosition()))
                .withRotation(Vec2.ZERO)
                .withLevel(level)
                .withPermission(4)  // Op level 4 (highest) to ensure command can execute
                .withEntity(player);
            
            // Execute the command
            server.getCommands().performPrefixedCommand(source, processedCommand);
            LOGGER.debug("Executed command on mob death: {}", processedCommand);
        } catch (Exception e) {
            LOGGER.error("Failed to execute command: {} - {}", command, e.getMessage());
        }
    }
}
