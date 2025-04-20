package net.poe.entitylootdrops;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

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
    // Flag to control debug logging
    private static boolean debugLoggingEnabled = false;
    
    /**
     * Enables or disables debug logging.
     * 
     * @param enabled True to enable logging, false to disable
     */
    public static void setDebugLogging(boolean enabled) {
        debugLoggingEnabled = enabled;
        LOGGER.info("Debug logging has been {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Checks if debug logging is enabled.
     * 
     * @return True if debug logging is enabled
     */
    public static boolean isDebugLoggingEnabled() {
        return debugLoggingEnabled;
    }
    
    /**
     * Logs a debug message if debug logging is enabled.
     * 
     * @param message The message format string
     * @param params The message parameters
     */
    private static void logDebug(String message, Object... params) {
        if (debugLoggingEnabled) {
            LOGGER.debug(message, params);
        }
    }
    
    /**
     * Main event handler for entity drops.
     * This is called whenever an entity dies and drops items.
     * 
     * @param event The LivingDropsEvent containing information about the entity and its drops
     */

@SubscribeEvent
public static void onEntityDrop(LivingDropsEvent event) {
    LivingEntity entity = event.getEntity();
    
    // Check if a player killed the entity (directly or indirectly)
    boolean playerKilled = false;
    Player player = null;

    // Check for direct player kill
    if (event.getSource().getEntity() instanceof Player) {
        playerKilled = true;
        player = (Player) event.getSource().getEntity();
    } 
    // Check for indirect player kill (fire, etc.)
    else if (event.getSource().getEntity() == null) {
        // Get the last damage source if it exists
        if (entity.getLastHurtByMob() instanceof Player) {
            playerKilled = true;
            player = (Player) entity.getLastHurtByMob();
            logDebug("Indirect player kill detected from: {}", player.getName().getString());
        }
    }

    // Get the entity ID as a string (e.g., "minecraft:zombie")
    ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
    if (entityId == null) return;
    
    String entityIdStr = entityId.toString();
    boolean isHostile = entity instanceof Enemy;

    // Check if we should disable all default drops (vanilla and modded)
    boolean disableDefaultDrops = false;
    Set<String> allowedModIDs = new HashSet<>();
    Set<String> allowedItemIDs = new HashSet<>();
    
    if (isHostile) {
        // Get the normal hostile drops configuration
        List<LootConfig.CustomDropEntry> hostileDrops = LootConfig.getNormalHostileDrops();
        
        // Check if any entry has allowDefaultDrops set to false
        for (LootConfig.CustomDropEntry drop : hostileDrops) {
            if (!drop.isAllowDefaultDrops()) {
                disableDefaultDrops = true;
                // Add the drop's itemId to allowed items
                if (drop.getItemId() != null) {
                    allowedItemIDs.add(drop.getItemId());
                }
                // Add allowed mod IDs
                if (drop.getAllowModIDs() != null) {
                    allowedModIDs.addAll(drop.getAllowModIDs());
                }
            }
        }
        
        // Also check event-specific hostile drops if any events are active
        for (String eventName : LootConfig.getActiveEvents()) {
            String matchingEventName = null;
            for (String key : LootConfig.getEventDrops().keySet()) {
                if (key.equalsIgnoreCase(eventName)) {
                    matchingEventName = key;
                    break;
                }
            }
            
            if (matchingEventName != null) {
                List<LootConfig.CustomDropEntry> eventHostileDrops = LootConfig.getEventHostileDrops(matchingEventName);
                for (LootConfig.CustomDropEntry drop : eventHostileDrops) {
                    if (!drop.isAllowDefaultDrops()) {
                        disableDefaultDrops = true;
                        // Add the drop's itemId to allowed items
                        if (drop.getItemId() != null) {
                            allowedItemIDs.add(drop.getItemId());
                        }
                        // Add allowed mod IDs
                        if (drop.getAllowModIDs() != null) {
                            allowedModIDs.addAll(drop.getAllowModIDs());
                        }
                    }
                }
            }
        }
    }
    
    // Handle vanilla drop modifications if player killed the entity
    if (playerKilled && player != null) {
        // If drop chance event is active, potentially duplicate vanilla drops
        if (LootConfig.isDropChanceEventActive()) {
            List<ItemEntity> additionalDrops = new ArrayList<>();
            
            event.getDrops().forEach(itemEntity -> {
                if (RANDOM.nextFloat() < 0.5f) {
                    ItemStack originalStack = itemEntity.getItem();
                    ItemStack duplicateStack = new ItemStack(
                        originalStack.getItem(),
                        originalStack.getCount()
                    );
                    
                    if (originalStack.hasTag()) {
                        duplicateStack.setTag(originalStack.getTag().copy());
                    }
                    
                    ItemEntity duplicateEntity = new ItemEntity(
                        itemEntity.level(),
                        itemEntity.getX(),
                        itemEntity.getY(),
                        itemEntity.getZ(),
                        duplicateStack
                    );
                    
                    duplicateEntity.setDeltaMovement(itemEntity.getDeltaMovement());
                    duplicateEntity.setDefaultPickUpDelay();
                    additionalDrops.add(duplicateEntity);
                    
                    logDebug("Added extra vanilla drop for {} due to drop chance event", 
                        ForgeRegistries.ITEMS.getKey(originalStack.getItem()));
                }
            });
            
            event.getDrops().addAll(additionalDrops);
        }
        
        // If double drops is active, double the amount of vanilla drops
        if (LootConfig.isDoubleDropsActive()) {
            List<ItemEntity> doubledDrops = new ArrayList<>();
            
            event.getDrops().forEach(itemEntity -> {
                ItemStack originalStack = itemEntity.getItem();
                ItemStack doubledStack = new ItemStack(
                    originalStack.getItem(),
                    originalStack.getCount() * 2
                );
                
                if (originalStack.hasTag()) {
                    doubledStack.setTag(originalStack.getTag().copy());
                }
                
                ItemEntity doubledEntity = new ItemEntity(
                    itemEntity.level(),
                    itemEntity.getX(),
                    itemEntity.getY(),
                    itemEntity.getZ(),
                    doubledStack
                );
                
                doubledEntity.setDeltaMovement(itemEntity.getDeltaMovement());
                doubledEntity.setDefaultPickUpDelay();
                doubledDrops.add(doubledEntity);
                
                logDebug("Doubled vanilla drop amount for {} to {}", 
                    ForgeRegistries.ITEMS.getKey(originalStack.getItem()), 
                    doubledStack.getCount());
            });
            
            event.getDrops().clear();
            event.getDrops().addAll(doubledDrops);
        }
    }
    
    // Filter drops if needed (after applying drop modifications)
    if (disableDefaultDrops) {
        List<ItemEntity> filteredDrops = new ArrayList<>();
        
        for (ItemEntity itemEntity : event.getDrops()) {
            ItemStack stack = itemEntity.getItem();
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            
            if (itemId != null) {
                String itemIdStr = itemId.toString();
                String modId = itemId.getNamespace();
                
                // Keep the drop if it's specifically allowed by itemId or from an allowed mod
                if (allowedItemIDs.contains(itemIdStr) || allowedModIDs.contains(modId)) {
                    filteredDrops.add(itemEntity);
                    logDebug("Keeping drop {} (allowed by configuration)", itemId);
                } else {
                    logDebug("Removing drop {} (not in allowed list)", itemId);
                }
            }
        }
        
        // Replace the drops with our filtered list
        event.getDrops().clear();
        event.getDrops().addAll(filteredDrops);
    }

    // Log kill information if debug is enabled
    if (debugLoggingEnabled) {
        logDebug("Entity death: {} ({})", entityIdStr, 
            playerKilled ? "Player kill by " + player.getName().getString() : "Non-player kill");
        logDebug("Death source: {}", event.getSource().getMsgId());
        if (event.getSource().getEntity() != null) {
            logDebug("Direct killer: {}", event.getSource().getEntity().getName().getString());
        }
        if (entity.getLastHurtByMob() != null) {
            logDebug("Last attacker: {}", entity.getLastHurtByMob().getName().getString());
        }
    }
    
    // Process normal (always active) entity-specific drops
    processEntityDrops(event, entityIdStr, LootConfig.getNormalDrops(), player, playerKilled);
    
    // Process normal (always active) hostile mob drops if this is a hostile entity
    if (isHostile) {
        processDrops(event, LootConfig.getNormalHostileDrops(), player, playerKilled);
    }
    
    // Process event-specific drops for all active events
    for (String eventName : LootConfig.getActiveEvents()) {
        String matchingEventName = null;
        for (String key : LootConfig.getEventDrops().keySet()) {
            if (key.equalsIgnoreCase(eventName)) {
                matchingEventName = key;
                break;
            }
        }
        
        if (matchingEventName != null) {
            List<LootConfig.EntityDropEntry> eventDropList = LootConfig.getEventDrops().get(matchingEventName);
            if (eventDropList != null) {
                processEntityDrops(event, entityIdStr, eventDropList, player, playerKilled);
            }
            
            if (isHostile) {
                processDrops(event, LootConfig.getEventHostileDrops(matchingEventName), player, playerKilled);
            }
        }
    }

    // Log final drop information if debug is enabled
    if (debugLoggingEnabled && !event.getDrops().isEmpty()) {
        logDebug("Final drops for {}: ", entityIdStr);
        event.getDrops().forEach(drop -> 
            logDebug("- {} x{}", 
                ForgeRegistries.ITEMS.getKey(drop.getItem().getItem()), 
                drop.getItem().getCount())
        );
    }
}

    
    /**
     * Processes entity-specific drops for a given entity.
     */
    private static void processEntityDrops(LivingDropsEvent event, String entityIdStr, 
        List<LootConfig.EntityDropEntry> dropsList, Player player, boolean playerKilled) {
        for (LootConfig.EntityDropEntry drop : dropsList) {
            if (drop.getEntityId().equals(entityIdStr)) {
                processDropEntry(event, drop, player, playerKilled);
            }
        }
    }
    
    /**
     * Processes a list of custom drops.
     */
    private static void processDrops(LivingDropsEvent event, List<LootConfig.CustomDropEntry> drops, 
        Player player, boolean playerKilled) {
        for (LootConfig.CustomDropEntry drop : drops) {
            processDropEntry(event, drop, player, playerKilled);
        }
    }
    
/**
 * Processes a single drop entry.
 */
private static void processDropEntry(LivingDropsEvent event, LootConfig.CustomDropEntry drop, 
    Player player, boolean playerKilled) {
    try {
        // Check if this drop requires a player kill
        if (drop.isRequirePlayerKill() && !playerKilled) {
            logDebug("Skipping drop {} - requires player kill but entity was not killed by a player", 
                drop.getItemId());
            return;
        }
        
        // Skip if player is null but we need to check player-specific requirements
        if (player == null && (drop.hasRequiredDimension() || drop.hasRequiredAdvancement() || 
            drop.hasRequiredEffect() || drop.hasRequiredEquipment())) {
            logDebug("Skipping drop {} - player-specific requirements but no player killed the entity", 
                drop.getItemId());
            return;
        }

        // Check dimension requirement
        if (drop.hasRequiredDimension() && player != null) {
            ResourceLocation playerDimension = player.level().dimension().location();
            String requiredDimension = drop.getRequiredDimension();
            
            if (!playerDimension.toString().equals(requiredDimension)) {
                logDebug("Skipping drop {} - wrong dimension (player in {}, required {})", 
                    drop.getItemId(), playerDimension, requiredDimension);
                return;
            }
            logDebug("Dimension requirement met for {}: {}", drop.getItemId(), requiredDimension);
        }
        
        // Check advancement requirement
        if (drop.hasRequiredAdvancement() && player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            ResourceLocation advancement = new ResourceLocation(drop.getRequiredAdvancement());
            if (!hasAdvancement(serverPlayer, advancement)) {
                logDebug("Skipping drop {} - missing advancement {}", 
                    drop.getItemId(), advancement);
                return;
            }
            logDebug("Advancement requirement met for {}: {}", drop.getItemId(), advancement);
        }
        
        // Check potion effect requirement
        if (drop.hasRequiredEffect() && player != null) {
            if (!hasEffect(player, drop.getRequiredEffect())) {
                logDebug("Skipping drop {} - missing effect {}", 
                    drop.getItemId(), drop.getRequiredEffect());
                return;
            }
            logDebug("Effect requirement met for {}: {}", drop.getItemId(), drop.getRequiredEffect());
        }
        
        // Check equipment requirement
        if (drop.hasRequiredEquipment() && player != null) {
            if (!hasEquipment(player, drop.getRequiredEquipment())) {
                logDebug("Skipping drop {} - missing equipment {}", 
                    drop.getItemId(), drop.getRequiredEquipment());
                return;
            }
            logDebug("Equipment requirement met for {}: {}", drop.getItemId(), drop.getRequiredEquipment());
        }
        
        // Calculate drop chance
        float chance = drop.getDropChance();
        if (LootConfig.isDropChanceEventActive() && playerKilled) {
            chance *= 2.0f;
            logDebug("Drop chance doubled for {}: {}% -> {}%", 
                drop.getItemId(), drop.getDropChance(), chance);
        }
        
        // Handle command execution
        if (drop.hasCommand() && player instanceof ServerPlayer) {
            float cmdChance = drop.getCommandChance();
            
            if (cmdChance <= 0) {
                logDebug("Skipping command execution for {} - command chance is {}%", 
                    drop.getItemId(), cmdChance);
            } else {
                boolean executeCmd = RANDOM.nextFloat() * 100 <= cmdChance;
                logDebug("Command chance roll for {}: {}% - Result: {}", 
                    drop.getItemId(), cmdChance, executeCmd ? "EXECUTE" : "SKIP");
        
                if (executeCmd) {
                    executeCommand(drop.getCommand(), (ServerPlayer) player, event.getEntity());
                }
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
                if (LootConfig.isDoubleDropsActive() && playerKilled) {
                    amount *= 2;
                    logDebug("Doubled drop amount for {} to {}", drop.getItemId(), amount);
                }
                
                ItemStack stack = new ItemStack(item, amount);
                
                if (drop.hasNbtData()) {
                    try {
                        CompoundTag nbt = TagParser.parseTag(drop.getNbtData());
                        stack.setTag(nbt);
                        logDebug("Applied NBT data to {}: {}", drop.getItemId(), drop.getNbtData());
                    } catch (CommandSyntaxException e) {
                        LOGGER.error("Invalid NBT format for {}: {}", drop.getItemId(), e.getMessage());
                    }
                }
                
                event.getEntity().spawnAtLocation(stack);
                
                if (LootConfig.isDropChanceEventActive() && playerKilled) {
                    logDebug("Dropped {} x{} (doubled chance: {}%)", 
                        drop.getItemId(), amount, chance);
                } else {
                    logDebug("Dropped {} x{} (chance: {}%)", 
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
            if (server == null) {
                LOGGER.error("Failed to execute command: server is null");
                return;
            }
            
            ServerLevel level = (ServerLevel) player.level();
            String[] commands = command.replace("\\n", "\n").split("\n");
            
            for (String singleCommand : commands) {
                if (singleCommand.trim().isEmpty()) {
                    continue;
                }
                
                String processedCommand = singleCommand
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
                
                try {
                    logDebug("Attempting to execute command: {}", processedCommand);
                    server.getCommands().performPrefixedCommand(source, processedCommand);
                    logDebug("Successfully executed command: {}", processedCommand);
                } catch (Exception e) {
                    LOGGER.error("Error executing command '{}': {}", processedCommand, e.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to execute command: {} - {}", command, e.getMessage(), e);
        }
    }
}
