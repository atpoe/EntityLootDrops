package net.poe.entitylootdrops.lootdrops.events;

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
import net.poe.entitylootdrops.EntityLootDrops;
import net.poe.entitylootdrops.lootdrops.LootConfig;
import net.poe.entitylootdrops.lootdrops.model.CustomDropEntry;
import net.poe.entitylootdrops.lootdrops.model.EntityDropEntry;

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

        // Check if we should disable vanilla drops for hostile mobs
        boolean cancelVanillaDrops = false;
        List<String> allowedModIDs = new ArrayList<>();

        if (isHostile) {
            // Get the normal hostile drops configuration
            List<CustomDropEntry> hostileDrops = LootConfig.getNormalHostileDrops();
            
            // Check if any entry has allowDefaultDrops set to false
            for (CustomDropEntry drop : hostileDrops) {
                if (!drop.isAllowDefaultDrops()) {
                    // Check dimension requirement if specified
                    if (drop.hasRequiredDimension() && player != null) {
                        ResourceLocation playerDimension = player.level().dimension().location();
                        String requiredDimension = drop.getRequiredDimension();
                        
                        if (!playerDimension.toString().equals(requiredDimension)) {
                            // Skip this entry if dimension doesn't match
                            logDebug("Skipping allowDefaultDrops=false - wrong dimension (player in {}, required {})", 
                                playerDimension, requiredDimension);
                            continue;
                        }
                    }
                    
                    cancelVanillaDrops = true;
                    if (drop.getAllowModIDs() != null) {
                        allowedModIDs.addAll(drop.getAllowModIDs());
                    }
                    break;
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
                    List<CustomDropEntry> eventHostileDrops = LootConfig.getEventHostileDrops(matchingEventName);
                    for (CustomDropEntry drop : eventHostileDrops) {
                        if (!drop.isAllowDefaultDrops()) {
                            // Check dimension requirement if specified
                            if (drop.hasRequiredDimension() && player != null) {
                                ResourceLocation playerDimension = player.level().dimension().location();
                                String requiredDimension = drop.getRequiredDimension();
                                
                                if (!playerDimension.toString().equals(requiredDimension)) {
                                    // Skip this entry if dimension doesn't match
                                    logDebug("Skipping allowDefaultDrops=false - wrong dimension (player in {}, required {})", 
                                        playerDimension, requiredDimension);
                                    continue;
                                }
                            }
                            
                            cancelVanillaDrops = true;
                            if (drop.getAllowModIDs() != null) {
                                allowedModIDs.addAll(drop.getAllowModIDs());
                            }
                            break;
                        }
                    }
                }
            }
        }
        
        // Filter vanilla drops if needed
        if (cancelVanillaDrops) {
            List<ItemEntity> filteredDrops = new ArrayList<>();
            
            // First, collect all itemIds from drop entries
            Set<String> configuredItemIds = new HashSet<>();
            for (CustomDropEntry drop : LootConfig.getNormalHostileDrops()) {
                if (drop.getItemId() != null) {
                    configuredItemIds.add(drop.getItemId());
                }
            }
            
            // Also check event drops
            for (String eventName : LootConfig.getActiveEvents()) {
                List<CustomDropEntry> eventDrops = LootConfig.getEventHostileDrops(eventName);
                for (CustomDropEntry drop : eventDrops) {
                    if (drop.getItemId() != null) {
                        configuredItemIds.add(drop.getItemId());
                    }
                }
            }
            
            for (ItemEntity itemEntity : event.getDrops()) {
                ItemStack stack = itemEntity.getItem();
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
                
                if (itemId != null) {
                    String modId = itemId.getNamespace();
                    String itemIdStr = itemId.toString();
                    
                    // Keep the drop if:
                    // 1. It's from an allowed mod OR
                    // 2. It matches any itemId in the drop entries
                    boolean keepDrop = allowedModIDs.contains(modId) || configuredItemIds.contains(itemIdStr);
                    
                    if (keepDrop) {
                        filteredDrops.add(itemEntity);
                        logDebug("Keeping drop {} (allowed mod or specified in drop entries)", itemId);
                    } else {
                        logDebug("Removing drop {} (not from allowed mod and not specified in drop entries)", itemId);
                    }
                }
            }
            
            // Replace the drops with our filtered list
            event.getDrops().clear();
            event.getDrops().addAll(filteredDrops);
        }

        // Handle vanilla drop modifications only if player killed the entity and vanilla drops aren't cancelled
        if (playerKilled && player != null && !cancelVanillaDrops) {
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
                List<EntityDropEntry> eventDropList = LootConfig.getEventDrops().get(matchingEventName);
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
        List<EntityDropEntry> dropsList, Player player, boolean playerKilled) {
        for (EntityDropEntry drop : dropsList) {
            if (drop.getEntityId().equals(entityIdStr)) {
                processDropEntry(event, drop, player, playerKilled);
            }
        }
    }
    
    /**
     * Processes a list of custom drops.
     */
    private static void processDrops(LivingDropsEvent event, List<CustomDropEntry> drops, 
        Player player, boolean playerKilled) {
        for (CustomDropEntry drop : drops) {
            processDropEntry(event, drop, player, playerKilled);
        }
    }
    
    /**
     * Processes a single drop entry.
     */
    private static void processDropEntry(LivingDropsEvent event, CustomDropEntry drop, 
            Player player, boolean playerKilled) {
        try {
            // Step 1: Check player kill requirement FIRST
            if (drop.isRequirePlayerKill()) {
                if (!playerKilled) {
                    return; // STOP - requirement not met
                }
            }
            
            // Step 2: If we need player-specific checks, ensure player exists
            boolean needsPlayer = drop.hasRequiredDimension() || 
                                 drop.hasRequiredAdvancement() || 
                                 drop.hasRequiredEffect() || 
                                 drop.hasRequiredEquipment() ||
                                 drop.hasCommand();
            
            if (needsPlayer && player == null) {
                return; // STOP - need player but don't have one
            }
            
            // Step 3: Check dimension requirement
            if (drop.hasRequiredDimension()) {
                if (player == null) {
                    return; // STOP - need dimension check but no player
                }
                
                String playerDim = player.level().dimension().location().toString();
                String requiredDim = drop.getRequiredDimension();
                
                if (!playerDim.equals(requiredDim)) {
                    return; // STOP - wrong dimension
                }
            }
            
            // Step 4: Check advancement requirement
            if (drop.hasRequiredAdvancement()) {
                if (!(player instanceof ServerPlayer)) {
                    return; // STOP - need ServerPlayer for advancement check
                }
                
                ServerPlayer serverPlayer = (ServerPlayer) player;
                ResourceLocation advancementId = new ResourceLocation(drop.getRequiredAdvancement());
                
                MinecraftServer server = serverPlayer.getServer();
                if (server == null) {
                    return; // STOP - no server
                }
                
                Advancement advancement = server.getAdvancements().getAdvancement(advancementId);
                if (advancement == null) {
                    return; // STOP - advancement doesn't exist
                }
                
                boolean hasAdvancement = serverPlayer.getAdvancements().getOrStartProgress(advancement).isDone();
                if (!hasAdvancement) {
                    return; // STOP - player doesn't have advancement
                }
            }
            
            // Step 5: Check effect requirement
            if (drop.hasRequiredEffect()) {
                if (player == null) {
                    return; // STOP - need player for effect check
                }
                
                ResourceLocation effectId = new ResourceLocation(drop.getRequiredEffect());
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);
                
                if (effect == null || !player.hasEffect(effect)) {
                    return; // STOP - player doesn't have required effect
                }
            }
            
            // Step 6: Check equipment requirement
            if (drop.hasRequiredEquipment()) {
                if (player == null) {
                    return; // STOP - need player for equipment check
                }
                
                ResourceLocation equipmentId = new ResourceLocation(drop.getRequiredEquipment());
                ItemStack mainHand = player.getMainHandItem();
                ItemStack offHand = player.getOffhandItem();
                
                boolean hasEquipment = ForgeRegistries.ITEMS.getKey(mainHand.getItem()).equals(equipmentId) ||
                                      ForgeRegistries.ITEMS.getKey(offHand.getItem()).equals(equipmentId);
                
                if (!hasEquipment) {
                    return; // STOP - player doesn't have required equipment
                }
            }
            
            // ALL REQUIREMENTS PASSED - Now we can execute command and handle drops
            
            // Execute command if present
            if (drop.hasCommand() && player instanceof ServerPlayer) {
                float commandChance = drop.getCommandChance();
                if (commandChance > 0) {
                    float commandRoll = RANDOM.nextFloat() * 100;
                    if (commandRoll <= commandChance) {
                        executeCommand(drop.getCommand(), (ServerPlayer) player, event.getEntity());
                    }
                }
            }
            
            // Handle item drop
            float dropChance = drop.getDropChance();
            if (LootConfig.isDropChanceEventActive() && playerKilled) {
                dropChance *= 2.0f;
            }
            
            float dropRoll = RANDOM.nextFloat() * 100;
            if (dropRoll <= dropChance) {
                ResourceLocation itemId = new ResourceLocation(drop.getItemId());
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                
                if (item != null) {
                    int amount = drop.getMinAmount();
                    if (drop.getMaxAmount() > drop.getMinAmount()) {
                        amount += RANDOM.nextInt(drop.getMaxAmount() - drop.getMinAmount() + 1);
                    }
                    
                    if (LootConfig.isDoubleDropsActive() && playerKilled) {
                        amount *= 2;
                    }
                    
                    ItemStack stack = new ItemStack(item, amount);
                    
                    if (drop.hasNbtData()) {
                        try {
                            CompoundTag nbt = TagParser.parseTag(drop.getNbtData());
                            stack.setTag(nbt);
                        } catch (CommandSyntaxException e) {
                            LOGGER.error("Invalid NBT format for {}: {}", drop.getItemId(), e.getMessage());
                        }
                    }
                    
                    event.getEntity().spawnAtLocation(stack);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error processing drop {}: {}", drop.getItemId(), e.getMessage());
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
                    server.getCommands().performPrefixedCommand(source, processedCommand);
                } catch (Exception e) {
                    LOGGER.error("Error executing command '{}': {}", processedCommand, e.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to execute command: {} - {}", command, e.getMessage(), e);
        }
    }
}
