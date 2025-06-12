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
import net.minecraft.core.Holder;
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
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.poe.entitylootdrops.EntityLootDrops;
import net.poe.entitylootdrops.lootdrops.LootConfig;
import net.poe.entitylootdrops.lootdrops.config.EventConfig;
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
     * @param enabled true to enable debug logging, false to disable
     */
    public static void setDebugLogging(boolean enabled) {
        debugLoggingEnabled = enabled;
        if (enabled) {
            LOGGER.info("Debug logging enabled for EntityLootDrops");
        } else {
            LOGGER.info("Debug logging disabled for EntityLootDrops");
        }
    }

    /**
     * Logs a debug message if debug logging is enabled.
     *
     * @param message the message to log
     * @param args    arguments for the message
     */
    private static void logDebug(String message, Object... args) {
        if (debugLoggingEnabled) {
            LOGGER.info("[DEBUG] " + message, args);
        }
    }

    /**
     * Main event handler for entity deaths.
     * Processes custom drops based on configuration.
     */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || entity.level().isClientSide) {
            return;
        }

        // Get entity information
        ResourceLocation entityType = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityType == null) {
            return;
        }

        String entityIdStr = entityType.toString();
        boolean isHostile = entity instanceof Enemy;
        boolean playerKilled = event.getSource().getEntity() instanceof Player;
        Player player = playerKilled ? (Player) event.getSource().getEntity() : null;

        logDebug("Processing drops for {} (hostile: {}, player killed: {})", entityIdStr, isHostile, playerKilled);

        // Phase 1: Handle vanilla drop modifications
        handleVanillaDropModifications(event, isHostile);

        // Phase 2: Apply drop events to all drops (vanilla and modded)
        if (playerKilled && player != null) {
            applyDropEvents(event);
        }

        // Phase 3: Process extra vanilla drops
        processExtraVanillaDrops(event, entity, player, entityIdStr, isHostile);

        // Phase 4: Process custom drops
        processCustomDrops(event, entityIdStr, isHostile, player, playerKilled);
    }

    /**
     * Handles vanilla drop modifications for hostile mobs.
     */
    private static void handleVanillaDropModifications(LivingDropsEvent event, boolean isHostile) {
        if (!isHostile) {
            return;
        }

        boolean cancelVanillaDrops = false;
        Set<String> allowedModIDs = new HashSet<>();
        Set<String> configuredItemIds = collectConfiguredItemIds();

        // Check if any drop entry cancels vanilla drops or specifies allowed mods
        for (CustomDropEntry drop : LootConfig.getNormalHostileDrops()) {
            if (!drop.isAllowDefaultDrops()) {
                cancelVanillaDrops = true;
            }
            if (drop.getAllowModIDs() != null) {
                allowedModIDs.addAll(drop.getAllowModIDs());
            }
        }

        // Check event drops too
        for (String eventName : LootConfig.getActiveEvents()) {
            for (CustomDropEntry drop : LootConfig.getEventHostileDrops(eventName)) {
                if (!drop.isAllowDefaultDrops()) {
                    cancelVanillaDrops = true;
                }
                if (drop.getAllowModIDs() != null) {
                    allowedModIDs.addAll(drop.getAllowModIDs());
                }
            }
        }

        if (!cancelVanillaDrops && allowedModIDs.isEmpty()) {
            return;
        }

        // Filter drops
        List<ItemEntity> filteredDrops = new ArrayList<>();
        for (ItemEntity itemEntity : event.getDrops()) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(itemEntity.getItem().getItem());
            if (itemId != null) {
                String modId = itemId.getNamespace();
                String itemIdStr = itemId.toString();

                // Keep the drop if it's from an allowed mod or specified in drop entries
                boolean keepDrop = allowedModIDs.contains(modId) || configuredItemIds.contains(itemIdStr);

                if (keepDrop) {
                    filteredDrops.add(itemEntity);
                    logDebug("Keeping drop {} (allowed mod or specified in drop entries)", itemId);
                } else {
                    logDebug("Removing drop {} (not from allowed mod and not specified in drop entries)", itemId);
                }
            }
        }

        // Replace drops with filtered list
        event.getDrops().clear();
        event.getDrops().addAll(filteredDrops);
    }

    /**
     * Applies drop chance and double drops events to all drops (vanilla and modded).
     */
    private static void applyDropEvents(LivingDropsEvent event) {
        // If drop chance event is active, potentially duplicate drops
        if (LootConfig.isDropChanceEventActive()) {
            List<ItemEntity> additionalDrops = new ArrayList<>();

            event.getDrops().forEach(itemEntity -> {
                if (RANDOM.nextFloat() < 0.5f) {
                    ItemStack originalStack = itemEntity.getItem();
                    ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(originalStack.getItem());

                    // Check if this mod should be affected by events
                    if (itemId != null && EventConfig.isModAllowedForDropEvents(itemId.getNamespace())) {
                        ItemEntity duplicate = createDuplicateItemEntity(itemEntity);
                        additionalDrops.add(duplicate);
                        logDebug("Drop chance event: Duplicated {} from mod {}", itemId, itemId.getNamespace());
                    }
                }
            });

            event.getDrops().addAll(additionalDrops);
        }

        // If double drops is active, double all drops
        if (LootConfig.isDoubleDropsActive()) {
            List<ItemEntity> doubledDrops = new ArrayList<>();

            event.getDrops().forEach(itemEntity -> {
                ItemStack originalStack = itemEntity.getItem();
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(originalStack.getItem());

                // Check if this mod should be affected by events
                if (itemId != null && EventConfig.isModAllowedForDropEvents(itemId.getNamespace())) {
                    ItemEntity duplicate = createDuplicateItemEntity(itemEntity);
                    doubledDrops.add(duplicate);
                    logDebug("Double drops event: Doubled {} from mod {}", itemId, itemId.getNamespace());
                }
            });

            event.getDrops().addAll(doubledDrops);
        }
    }

    /**
     * Collects all configured item IDs from drop entries.
     */
    private static Set<String> collectConfiguredItemIds() {
        Set<String> configuredItemIds = new HashSet<>();

        // Add from normal hostile drops
        for (CustomDropEntry drop : LootConfig.getNormalHostileDrops()) {
            if (drop.getItemId() != null) {
                configuredItemIds.add(drop.getItemId());
            }
        }

        // Add from event drops
        for (String eventName : LootConfig.getActiveEvents()) {
            List<CustomDropEntry> eventDrops = LootConfig.getEventHostileDrops(eventName);
            for (CustomDropEntry drop : eventDrops) {
                if (drop.getItemId() != null) {
                    configuredItemIds.add(drop.getItemId());
                }
            }
        }

        return configuredItemIds;
    }

    /**
     * Processes extra vanilla drops for all applicable drop entries.
     */
    private static void processExtraVanillaDrops(LivingDropsEvent event, LivingEntity entity,
                                                 Player player, String entityIdStr, boolean isHostile) {
        // Process normal entity-specific extra drops
        for (EntityDropEntry drop : LootConfig.getNormalDrops()) {
            if (drop.getEntityId().equals(entityIdStr)) {
                addExtraVanillaDrops(event, drop, player, entity);
            }
        }

        // Process normal hostile extra drops
        if (isHostile) {
            for (CustomDropEntry drop : LootConfig.getNormalHostileDrops()) {
                addExtraVanillaDrops(event, drop, player, entity);
            }
        }

        // Process event-specific extra drops
        for (String eventName : LootConfig.getActiveEvents()) {
            String matchingEventName = findMatchingEventName(eventName);
            if (matchingEventName != null) {
                processEventExtraDrops(event, entityIdStr, matchingEventName, player, entity, isHostile);
            }
        }
    }

    /**
     * Processes event-specific extra drops.
     */
    private static void processEventExtraDrops(LivingDropsEvent event, String entityIdStr, String eventName,
                                               Player player, LivingEntity entity, boolean isHostile) {
        // Process event entity-specific extra drops
        List<EntityDropEntry> eventDropList = LootConfig.getEventDrops().get(eventName);
        if (eventDropList != null) {
            for (EntityDropEntry drop : eventDropList) {
                if (drop.getEntityId().equals(entityIdStr)) {
                    addExtraVanillaDrops(event, drop, player, entity);
                }
            }
        }

        // Process event hostile extra drops
        if (isHostile) {
            for (CustomDropEntry drop : LootConfig.getEventHostileDrops(eventName)) {
                addExtraVanillaDrops(event, drop, player, entity);
            }
        }
    }

    /**
     * Adds extra vanilla drops based on the extraDropChance setting.
     */
    private static void addExtraVanillaDrops(LivingDropsEvent event, CustomDropEntry drop, Player player, LivingEntity entity) {
        if (drop.getExtraDropChance() <= 0 || !checkDropRequirements(drop, player)) {
            return;
        }

        float extraDropRoll = RANDOM.nextFloat() * 100;
        if (extraDropRoll <= drop.getExtraDropChance()) {
            int extraMultiplier = calculateAmount(drop.getExtraAmountMin(), drop.getExtraAmountMax());
            List<ItemEntity> extraDrops = duplicateVanillaDrops(event, extraMultiplier);

            event.getDrops().addAll(extraDrops);
            logDebug("Added {} extra vanilla drops ({}x multiplier)", extraDrops.size(), extraMultiplier);
        }
    }

    /**
     * Duplicates vanilla drops with a multiplier.
     */
    private static List<ItemEntity> duplicateVanillaDrops(LivingDropsEvent event, int multiplier) {
        List<ItemEntity> duplicates = new ArrayList<>();

        for (ItemEntity itemEntity : event.getDrops()) {
            for (int i = 0; i < multiplier; i++) {
                ItemEntity duplicate = createDuplicateItemEntity(itemEntity);
                duplicates.add(duplicate);
            }
        }

        return duplicates;
    }

    /**
     * Creates a duplicate of an ItemEntity.
     */
    private static ItemEntity createDuplicateItemEntity(ItemEntity original) {
        ItemStack originalStack = original.getItem();
        ItemStack duplicateStack = new ItemStack(originalStack.getItem(), originalStack.getCount());

        if (originalStack.hasTag()) {
            duplicateStack.setTag(originalStack.getTag().copy());
        }

        ItemEntity duplicate = new ItemEntity(
                original.level(),
                original.getX(),
                original.getY(),
                original.getZ(),
                duplicateStack
        );

        duplicate.setDeltaMovement(original.getDeltaMovement());
        duplicate.setDefaultPickUpDelay();

        return duplicate;
    }

    /**
     * Processes all custom drops.
     */
    private static void processCustomDrops(LivingDropsEvent event, String entityIdStr, boolean isHostile,
                                           Player player, boolean playerKilled) {
        // Process normal drops
        if (isHostile) {
            processDrops(event, LootConfig.getNormalHostileDrops(), player, playerKilled);
        }

        // Process entity-specific drops
        processEntityDrops(event, entityIdStr, LootConfig.getNormalDrops(), player, playerKilled);

        // Process event-specific drops
        for (String eventName : LootConfig.getActiveEvents()) {
            String matchingEventName = findMatchingEventName(eventName);
            if (matchingEventName != null) {
                processEventDrops(event, entityIdStr, matchingEventName, player, playerKilled, isHostile);
            }
        }
    }

    /**
     * Processes event-specific drops.
     */
    private static void processEventDrops(LivingDropsEvent event, String entityIdStr, String eventName,
                                          Player player, boolean playerKilled, boolean isHostile) {
        List<EntityDropEntry> eventDropList = LootConfig.getEventDrops().get(eventName);
        if (eventDropList != null) {
            processEntityDrops(event, entityIdStr, eventDropList, player, playerKilled);
        }

        if (isHostile) {
            processDrops(event, LootConfig.getEventHostileDrops(eventName), player, playerKilled);
        }
    }

    /**
     * Finds a matching event name (case-insensitive).
     */
    private static String findMatchingEventName(String eventName) {
        for (String key : LootConfig.getEventDrops().keySet()) {
            if (key.equalsIgnoreCase(eventName)) {
                return key;
            }
        }
        return null;
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
            // Check all requirements
            if (!checkDropRequirements(drop, player, playerKilled)) {
                return;
            }

            // Execute command if present
            executeDropCommand(drop, player, event.getEntity());

            // Handle item drop
            handleItemDrop(event, drop, player);

        } catch (Exception e) {
            LOGGER.error("Error processing drop {}: {}", drop.getItemId(), e.getMessage());
        }
    }

    /**
     * Executes the drop command if present and conditions are met.
     */
    private static void executeDropCommand(CustomDropEntry drop, Player player, LivingEntity entity) {
        if (drop.hasCommand() && player instanceof ServerPlayer) {
            float commandChance = drop.getCommandChance();
            if (commandChance > 0 && RANDOM.nextFloat() * 100 <= commandChance) {
                executeCommand(drop.getCommand(), (ServerPlayer) player, entity);
            }
        }
    }

    /**
     * Handles the item drop logic.
     */
    private static void handleItemDrop(LivingDropsEvent event, CustomDropEntry drop, Player player) {
        float dropChance = drop.getDropChance();
        if (LootConfig.isDropChanceEventActive() && event.getSource().getEntity() instanceof Player) {
            dropChance *= 2.0f;
        }

        if (RANDOM.nextFloat() * 100 <= dropChance) {
            int amount = calculateAmount(drop.getMinAmount(), drop.getMaxAmount());
            ItemStack stack = createItemStack(drop, amount);

            if (stack != null) {
                event.getEntity().spawnAtLocation(stack);
                logDebug("Dropped {} x{} from {}", drop.getItemId(), amount,
                        ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType()));

                // Execute drop command if present
                executeDropCommandOnDrop(drop, player, event.getEntity(), amount);
            }
        }
    }

    /**
     * Executes the drop command when an item actually drops.
     */
    private static void executeDropCommandOnDrop(CustomDropEntry drop, Player player, LivingEntity entity, int amount) {
        if (drop.hasDropCommand() && player instanceof ServerPlayer) {
            float dropCommandChance = drop.getDropCommandChance();
            if (dropCommandChance > 0 && RANDOM.nextFloat() * 100 <= dropCommandChance) {
                executeDropCommand(drop.getDropCommand(), (ServerPlayer) player, entity, drop, amount);
            }
        }
    }

    /**
     * Calculates a random amount between min and max (inclusive).
     */
    private static int calculateAmount(int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + RANDOM.nextInt(max - min + 1);
    }

    /**
     * Creates an ItemStack from a drop entry.
     */
    private static ItemStack createItemStack(CustomDropEntry drop, int amount) {
        try {
            ResourceLocation itemId = new ResourceLocation(drop.getItemId());
            Item item = ForgeRegistries.ITEMS.getValue(itemId);

            if (item == null) {
                LOGGER.warn("Unknown item: {}", drop.getItemId());
                return null;
            }

            ItemStack stack = new ItemStack(item, amount);

            // Apply NBT if present
            if (drop.getNbtData() != null && !drop.getNbtData().isEmpty()) {
                try {
                    CompoundTag nbt = TagParser.parseTag(drop.getNbtData());
                    stack.setTag(nbt);
                } catch (CommandSyntaxException e) {
                    LOGGER.error("Invalid NBT for item {}: {}", drop.getItemId(), e.getMessage());
                }
            }

            return stack;
        } catch (Exception e) {
            LOGGER.error("Error creating item stack for {}: {}", drop.getItemId(), e.getMessage());
            return null;
        }
    }

    /**
     * Checks if all drop requirements are met.
     */
    private static boolean checkDropRequirements(CustomDropEntry drop, Player player, boolean playerKilled) {
        // Check player kill requirement
        if (drop.isRequirePlayerKill() && !playerKilled) {
            return false;
        }

        // Check advancement requirement
        if (drop.hasRequiredAdvancement()) {
            if (!checkAdvancementRequirement(drop, player)) {
                return false;
            }
        }

        // Check effect requirement
        if (drop.hasRequiredEffect()) {
            if (!checkEffectRequirement(drop, player)) {
                return false;
            }
        }

        // Check equipment requirement
        if (drop.hasRequiredEquipment()) {
            if (!checkEquipmentRequirement(drop, player)) {
                return false;
            }
        }

        // Check dimension requirement
        if (drop.hasRequiredDimension()) {
            if (!checkDimensionRequirement(drop, player)) {
                return false;
            }
        }

        // Check biome requirement
        if (drop.hasRequiredBiome()) {
            if (!checkBiomeRequirement(drop, player)) {
                return false;
            }
        }

        // Check weather requirement
        if (drop.hasRequiredWeather()) {
            if (!checkWeatherRequirement(drop, player)) {
                return false;
            }
        }

        // Check time requirement
        if (drop.hasRequiredTime()) {
            if (!checkTimeRequirement(drop, player)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if all drop requirements are met (overloaded for single player parameter).
     */
    private static boolean checkDropRequirements(CustomDropEntry drop, Player player) {
        return checkDropRequirements(drop, player, true);
    }

    /**
     * Checks if the player has the required advancement.
     */
    private static boolean checkAdvancementRequirement(CustomDropEntry drop, Player player) {
        if (!(player instanceof ServerPlayer)) {
            return false;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        ResourceLocation advancementId = new ResourceLocation(drop.getRequiredAdvancement());
        Advancement advancement = serverPlayer.server.getAdvancements().getAdvancement(advancementId);

        return advancement != null && serverPlayer.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    /**
     * Checks if the player has the required effect.
     */
    private static boolean checkEffectRequirement(CustomDropEntry drop, Player player) {
        if (player == null) {
            return false;
        }

        ResourceLocation effectId = new ResourceLocation(drop.getRequiredEffect());
        Holder<MobEffect> effectHolder = ForgeRegistries.MOB_EFFECTS.getHolder(effectId).orElse(null);

        return effectHolder != null && player.hasEffect(effectHolder.value());
    }

    /**
     * Checks if the player has the required equipment.
     */
    private static boolean checkEquipmentRequirement(CustomDropEntry drop, Player player) {
        if (player == null) {
            return false;
        }

        ResourceLocation itemId = new ResourceLocation(drop.getRequiredEquipment());
        Item requiredItem = ForgeRegistries.ITEMS.getValue(itemId);

        if (requiredItem == null) {
            return false;
        }

        // Check all equipment slots
        for (ItemStack stack : player.getArmorSlots()) {
            if (stack.getItem() == requiredItem) {
                return true;
            }
        }

        // Check main hand and offhand
        return player.getMainHandItem().getItem() == requiredItem ||
                player.getOffhandItem().getItem() == requiredItem;
    }

    /**
     * Checks if the player is in the required dimension.
     */
    private static boolean checkDimensionRequirement(CustomDropEntry drop, Player player) {
        if (player == null) {
            return false;
        }

        ResourceLocation dimensionId = new ResourceLocation(drop.getRequiredDimension());
        ResourceLocation playerDimension = player.level().dimension().location();

        return dimensionId.equals(playerDimension);
    }

    /**
     * Checks if the player is in the required biome.
     */
    private static boolean checkBiomeRequirement(CustomDropEntry drop, Player player) {
        if (player == null) {
            return false;
        }

        ResourceLocation biomeId = new ResourceLocation(drop.getRequiredBiome());
        Holder<Biome> playerBiome = player.level().getBiome(player.blockPosition());

        return playerBiome.is(biomeId);
    }

    /**
     * Checks if the weather requirement is met.
     */
    private static boolean checkWeatherRequirement(CustomDropEntry drop, Player player) {
        if (player == null) {
            return false;
        }

        String requiredWeather = drop.getRequiredWeather().toLowerCase();
        boolean isRaining = player.level().isRaining();
        boolean isThundering = player.level().isThundering();

        switch (requiredWeather) {
            case "clear":
                return !isRaining && !isThundering;
            case "rain":
                return isRaining && !isThundering;
            case "thunder":
                return isThundering;
            default:
                return true; // Invalid weather requirement, allow drop
        }
    }

    /**
     * Checks if the time requirement is met.
     */
    private static boolean checkTimeRequirement(CustomDropEntry drop, Player player) {
        if (player == null) {
            return false;
        }

        String requiredTime = drop.getRequiredTime().toLowerCase();
        long dayTime = player.level().getDayTime() % 24000;

        switch (requiredTime) {
            case "day":
                return dayTime >= 1000 && dayTime < 13000;
            case "night":
                return dayTime >= 13000 || dayTime < 1000;
            case "dawn":
                return dayTime >= 23000 || dayTime < 1000;
            case "dusk":
                return dayTime >= 12000 && dayTime < 14000;
            default:
                return true; // Invalid time requirement, allow drop
        }
    }

    /**
     * Executes a command with player and entity context.
     */
    private static void executeCommand(String command, ServerPlayer player, LivingEntity entity) {
        executeDropCommand(command, player, entity, null, 1);
    }

    /**
     * Executes a drop command with full context including item and amount.
     */
    private static void executeDropCommand(String command, ServerPlayer player, LivingEntity entity, CustomDropEntry drop, int amount) {
        try {
            MinecraftServer server = player.getServer();
            if (server == null) {
                return;
            }

            ServerLevel level = (ServerLevel) entity.level();
            Vec3 entityPos = entity.position();
            Vec2 entityRotation = new Vec2(entity.getXRot(), entity.getYRot());

            CommandSourceStack commandSource = new CommandSourceStack(
                    server,
                    entityPos,
                    entityRotation,
                    level,
                    4,
                    entity.getName().getString(),
                    entity.getDisplayName(),
                    server,
                    entity
            );

            // Replace placeholders
            String entityName = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
            String itemName = "unknown_item";
            if (drop != null && drop.getItemId() != null) {
                // Extract just the item name from the full ID (e.g., "minecraft:diamond" -> "diamond")
                String fullId = drop.getItemId();
                itemName = fullId.contains(":") ? fullId.substring(fullId.lastIndexOf(":") + 1) : fullId;
            }

            String processedCommand = command
                    .replace("%player%", player.getName().getString())
                    .replace("@killer", player.getName().getString())
                    .replace("%entity%", entityName)
                    .replace("@entity", entityName)
                    .replace("@item", itemName)
                    .replace("%item%", itemName)
                    .replace("@amount", String.valueOf(amount))
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%x%", String.valueOf((int) entity.getX()))
                    .replace("%y%", String.valueOf((int) entity.getY()))
                    .replace("%z%", String.valueOf((int) entity.getZ()));

            server.getCommands().performPrefixedCommand(commandSource, processedCommand);
            logDebug("Executed command: {}", processedCommand);

        } catch (Exception e) {
            LOGGER.error("Failed to execute command '{}': {}", command, e.getMessage());
        }
    }
}