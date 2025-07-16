package net.poe.entitylootdrops.lootdrops.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

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

    // Command cooldown tracking - Maps player UUID to command hash to last execution time
    private static final Map<UUID, Map<String, Long>> commandCooldowns = new HashMap<>();

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
     * Checks if a command is on cooldown for a player.
     *
     * @param player the player
     * @param command the command to check
     * @param cooldownSeconds the cooldown duration in seconds
     * @return true if the command is on cooldown, false otherwise
     */
    private static boolean isCommandOnCooldown(Player player, String command, int cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            return false;
        }

        UUID playerId = player.getUUID();
        String commandHash = command.hashCode() + "";
        long currentTime = System.currentTimeMillis();

        Map<String, Long> playerCooldowns = commandCooldowns.computeIfAbsent(playerId, k -> new HashMap<>());
        Long lastExecution = playerCooldowns.get(commandHash);

        if (lastExecution == null) {
            return false;
        }

        long timeSinceLastExecution = currentTime - lastExecution;
        long cooldownMs = cooldownSeconds * 1000L;

        return timeSinceLastExecution < cooldownMs;
    }

    /**
     * Sets a command cooldown for a player.
     *
     * @param player the player
     * @param command the command
     */
    private static void setCommandCooldown(Player player, String command) {
        UUID playerId = player.getUUID();
        String commandHash = command.hashCode() + "";
        long currentTime = System.currentTimeMillis();

        Map<String, Long> playerCooldowns = commandCooldowns.computeIfAbsent(playerId, k -> new HashMap<>());
        playerCooldowns.put(commandHash, currentTime);
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

        // Phase 1: Handle vanilla drop modifications (only check applicable drops)
        handleVanillaDropModifications(event, entityIdStr, isHostile, player, playerKilled);

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
     * Handles vanilla drop modifications - only applies to entities with actual drop configurations.
     */
    private static void handleVanillaDropModifications(LivingDropsEvent event, String entityIdStr, boolean isHostile, Player player, boolean playerKilled) {
        boolean shouldCancelVanillaDrops = false;
        Set<String> allowedModIDs = new HashSet<>();

        // Check entity-specific drops first
        for (EntityDropEntry drop : LootConfig.getNormalDrops()) {
            if (drop.getEntityId().equals(entityIdStr)) {
                if (!drop.isAllowDefaultDrops()) {
                    shouldCancelVanillaDrops = true;
                }
                if (drop.getAllowModIDs() != null && !drop.getAllowModIDs().isEmpty()) {
                    allowedModIDs.addAll(drop.getAllowModIDs());
                }
            }
        }

        // Check hostile drops only if this is a hostile mob AND has applicable drops
        if (isHostile) {
            List<CustomDropEntry> applicableHostileDrops = getApplicableHostileDrops(player, playerKilled);
            if (!applicableHostileDrops.isEmpty()) {
                for (CustomDropEntry drop : applicableHostileDrops) {
                    if (!drop.isAllowDefaultDrops()) {
                        shouldCancelVanillaDrops = true;
                    }
                    if (drop.getAllowModIDs() != null && !drop.getAllowModIDs().isEmpty()) {
                        allowedModIDs.addAll(drop.getAllowModIDs());
                    }
                }
            }
        }

        // Check event-specific drops
        for (String eventName : LootConfig.getActiveEvents()) {
            String matchingEventName = findMatchingEventName(eventName);
            if (matchingEventName != null) {
                // Check event entity-specific drops
                List<EntityDropEntry> eventDropList = LootConfig.getEventDrops().get(matchingEventName);
                if (eventDropList != null) {
                    for (EntityDropEntry drop : eventDropList) {
                        if (drop.getEntityId().equals(entityIdStr)) {
                            if (!drop.isAllowDefaultDrops()) {
                                shouldCancelVanillaDrops = true;
                            }
                            if (drop.getAllowModIDs() != null && !drop.getAllowModIDs().isEmpty()) {
                                allowedModIDs.addAll(drop.getAllowModIDs());
                            }
                        }
                    }
                }

                // Check event hostile drops only if this is a hostile mob AND has applicable drops
                if (isHostile) {
                    List<CustomDropEntry> applicableEventHostileDrops = getApplicableEventHostileDrops(matchingEventName, player, playerKilled);
                    if (!applicableEventHostileDrops.isEmpty()) {
                        for (CustomDropEntry drop : applicableEventHostileDrops) {
                            if (!drop.isAllowDefaultDrops()) {
                                shouldCancelVanillaDrops = true;
                            }
                            if (drop.getAllowModIDs() != null && !drop.getAllowModIDs().isEmpty()) {
                                allowedModIDs.addAll(drop.getAllowModIDs());
                            }
                        }
                    }
                }
            }
        }

        // If no modifications needed, return early
        if (!shouldCancelVanillaDrops && allowedModIDs.isEmpty()) {
            return;
        }

        // Filter drops based on the configuration
        List<ItemEntity> filteredDrops = new ArrayList<>();
        for (ItemEntity itemEntity : event.getDrops()) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(itemEntity.getItem().getItem());
            if (itemId != null) {
                String modId = itemId.getNamespace();

                boolean keepDrop = false;

                // If vanilla drops are not cancelled, keep all drops
                if (!shouldCancelVanillaDrops) {
                    keepDrop = true;
                }
                // If there are allowed mod IDs, keep drops from those mods
                else if (!allowedModIDs.isEmpty() && allowedModIDs.contains(modId)) {
                    keepDrop = true;
                }

                if (keepDrop) {
                    filteredDrops.add(itemEntity);
                }
            }
        }

        // Update the drops list
        event.getDrops().clear();
        event.getDrops().addAll(filteredDrops);

        logDebug("Filtered vanilla drops: cancelled={}, allowed mods={}", shouldCancelVanillaDrops, allowedModIDs);
    }

    /**
     * Gets applicable hostile drops that meet requirements.
     */
    private static List<CustomDropEntry> getApplicableHostileDrops(Player player, boolean playerKilled) {
        List<CustomDropEntry> applicableDrops = new ArrayList<>();
        for (CustomDropEntry drop : LootConfig.getNormalHostileDrops()) {
            if (checkDropRequirements(drop, player, playerKilled)) {
                applicableDrops.add(drop);
            }
        }
        return applicableDrops;
    }

    /**
     * Gets applicable event hostile drops that meet requirements.
     */
    private static List<CustomDropEntry> getApplicableEventHostileDrops(String eventName, Player player, boolean playerKilled) {
        List<CustomDropEntry> applicableDrops = new ArrayList<>();
        for (CustomDropEntry drop : LootConfig.getEventHostileDrops(eventName)) {
            if (checkDropRequirements(drop, player, playerKilled)) {
                applicableDrops.add(drop);
            }
        }
        return applicableDrops;
    }

    /**
     * Applies drop events to existing drops.
     */
    private static void applyDropEvents(LivingDropsEvent event) {
        // If double drops is active, double all drops
        if (LootConfig.isDoubleDropsActive()) {
            List<ItemEntity> doubledDrops = new ArrayList<>();

            event.getDrops().forEach(itemEntity -> {
                ItemStack originalStack = itemEntity.getItem();
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(originalStack.getItem());
                if (itemId != null) {
                    ItemStack duplicateStack = new ItemStack(originalStack.getItem(), originalStack.getCount());
                    if (originalStack.getTag() != null) {
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
                    doubledDrops.add(duplicateEntity);
                }
            });

            event.getDrops().addAll(doubledDrops);
        }
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

        return duplicate;
    }

    /**
     * Processes custom drops for a specific entity.
     */
    private static void processCustomDrops(LivingDropsEvent event, String entityIdStr, boolean isHostile,
                                           Player player, boolean playerKilled) {
        // Process normal entity-specific drops
        processEntityDrops(event, entityIdStr, LootConfig.getNormalDrops(), player, playerKilled);

        // Process normal hostile drops
        if (isHostile) {
            processDrops(event, LootConfig.getNormalHostileDrops(), player, playerKilled);
        }

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
        // Process event entity-specific drops
        List<EntityDropEntry> eventDropList = LootConfig.getEventDrops().get(eventName);
        if (eventDropList != null) {
            processEntityDrops(event, entityIdStr, eventDropList, player, playerKilled);
        }

        // Process event hostile drops
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
            ServerPlayer serverPlayer = (ServerPlayer) player;

            // Check command cooldown
            logDebug("Checking cooldown for command: {} (cooldown: {})", drop.getCommand(), drop.getCommandCoolDown());
            if (isCommandOnCooldown(player, drop.getCommand(), drop.getCommandCoolDown())) {
                logDebug("Command on cooldown for player {}: {}", player.getName().getString(), drop.getCommand());
                return;
            }

            float commandChance = drop.getCommandChance();
            if (commandChance > 0 && RANDOM.nextFloat() * 100 <= commandChance) {
                logDebug("Executing command: {}", drop.getCommand());
                executeCommand(drop.getCommand(), serverPlayer, entity);

                // Set cooldown after successful execution
                if (drop.getCommandCoolDown() > 0) {
                    logDebug("Setting cooldown for {} seconds", drop.getCommandCoolDown());
                    setCommandCooldown(player, drop.getCommand());
                    logDebug("Cooldown set for player {}", player.getName().getString());
                }
            }
        }
    }

    /**
     * Handles the item drop logic.
     */
    private static void handleItemDrop(LivingDropsEvent event, CustomDropEntry drop, Player player) {
        // Skip item drop if itemId is null or empty
        if (drop.getItemId() == null || drop.getItemId().isEmpty()) {
            return;
        }

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
            ServerPlayer serverPlayer = (ServerPlayer) player;

            // Use dropCommand cooldown if available, otherwise no cooldown
            int cooldownSeconds = 0; // Default no cooldown for dropCommand

            float dropCommandChance = drop.getDropCommandChance();
            if (dropCommandChance > 0 && RANDOM.nextFloat() * 100 <= dropCommandChance) {
                logDebug("Executing drop command: {}", drop.getDropCommand());
                executeCommand(drop.getDropCommand(), serverPlayer, entity);

                // Set cooldown after successful execution if needed
                if (cooldownSeconds > 0) {
                    setCommandCooldown(player, drop.getDropCommand());
                }
            }
        }
    }

    /**
     * Executes a command with placeholder replacement.
     */
    private static void executeCommand(String command, ServerPlayer player, LivingEntity entity) {
        try {
            MinecraftServer server = player.getServer();
            if (server == null) {
                return;
            }

            // Replace placeholders
            String processedCommand = replacePlaceholders(command, player, entity);

            // Create command source
            CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withEntity(player)
                    .withLevel((ServerLevel) player.level())
                    .withPosition(player.position())
                    .withRotation(player.getRotationVector())
                    .withPermission(2);

            // Execute the command
            server.getCommands().performPrefixedCommand(commandSource, processedCommand);

        } catch (Exception e) {
            LOGGER.error("Error executing command '{}': {}", command, e.getMessage());
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

        return checkDropRequirements(drop, player);
    }

    /**
     * Checks if all drop requirements are met (overloaded version).
     */
    private static boolean checkDropRequirements(CustomDropEntry drop, Player player) {
        if (player == null) {
            return false;
        }

        // Check advancement requirement
        if (drop.getRequiredAdvancement() != null && !drop.getRequiredAdvancement().isEmpty()) {
            if (!hasAdvancement(player, drop.getRequiredAdvancement())) {
                return false;
            }
        }

        // Check effect requirement
        if (drop.getRequiredEffect() != null && !drop.getRequiredEffect().isEmpty()) {
            if (!hasEffect(player, drop.getRequiredEffect())) {
                return false;
            }
        }

        // Check equipment requirement
        if (drop.getRequiredEquipment() != null && !drop.getRequiredEquipment().isEmpty()) {
            if (!hasEquipment(player, drop.getRequiredEquipment())) {
                return false;
            }
        }

        // Check weather requirement
        if (drop.getRequiredWeather() != null && !drop.getRequiredWeather().isEmpty()) {
            if (!checkWeather(player, drop.getRequiredWeather())) {
                return false;
            }
        }

        // Check time requirement
        if (drop.getRequiredTime() != null && !drop.getRequiredTime().isEmpty()) {
            if (!checkTime(player, drop.getRequiredTime())) {
                return false;
            }
        }

        // Check dimension requirement
        if (drop.getRequiredDimension() != null && !drop.getRequiredDimension().isEmpty()) {
            if (!checkDimension(player, drop.getRequiredDimension())) {
                return false;
            }
        }

        // Check biome requirement
        if (drop.getRequiredBiome() != null && !drop.getRequiredBiome().isEmpty()) {
            if (!checkBiome(player, drop.getRequiredBiome())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if the player has the required advancement.
     */
    private static boolean hasAdvancement(Player player, String advancementId) {
        if (!(player instanceof ServerPlayer)) {
            return false;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        ResourceLocation advancementLocation = new ResourceLocation(advancementId);
        Advancement advancement = serverPlayer.getServer().getAdvancements().getAdvancement(advancementLocation);

        return advancement != null && serverPlayer.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    /**
     * Checks if the player has the required effect.
     */
    private static boolean hasEffect(Player player, String effectId) {
        ResourceLocation effectLocation = new ResourceLocation(effectId);
        Holder<MobEffect> effectHolder = ForgeRegistries.MOB_EFFECTS.getHolder(effectLocation).orElse(null);

        return effectHolder != null && player.hasEffect(effectHolder.value());
    }

    /**
     * Checks if the player has the required equipment.
     */
    private static boolean hasEquipment(Player player, String equipmentId) {
        ResourceLocation itemLocation = new ResourceLocation(equipmentId);
        Item requiredItem = ForgeRegistries.ITEMS.getValue(itemLocation);

        if (requiredItem == null) {
            return false;
        }

        // Check main hand
        if (player.getMainHandItem().getItem() == requiredItem) {
            return true;
        }

        // Check off hand
        if (player.getOffhandItem().getItem() == requiredItem) {
            return true;
        }

        // Check armor slots
        for (ItemStack armorStack : player.getArmorSlots()) {
            if (armorStack.getItem() == requiredItem) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the weather condition is met.
     */
    private static boolean checkWeather(Player player, String weatherCondition) {
        if (player.level().isClientSide) {
            return false;
        }

        switch (weatherCondition.toLowerCase()) {
            case "clear":
                return !player.level().isRaining() && !player.level().isThundering();
            case "rain":
                return player.level().isRaining() && !player.level().isThundering();
            case "thunder":
                return player.level().isThundering();
            default:
                return false;
        }
    }

    /**
     * Checks if the time condition is met.
     */
    private static boolean checkTime(Player player, String timeRequirement) {
        long time = player.level().getDayTime() % 24000;
        switch (timeRequirement.toLowerCase()) {
            case "day":
                return time >= 0 && time < 12000;
            case "night":
                return time >= 12000 && time < 24000;
            case "dawn":
                return time >= 23000 || time < 1000;
            case "dusk":
                return time >= 11000 && time < 13000;
            default:
                return false;
        }
    }

    /**
     * Checks if the dimension condition is met.
     */
    private static boolean checkDimension(Player player, String dimensionId) {
        ResourceLocation currentDimension = player.level().dimension().location();
        ResourceLocation requiredDimension = new ResourceLocation(dimensionId);
        return currentDimension.equals(requiredDimension);
    }

    /**
     * Checks if the biome condition is met.
     */
    private static boolean checkBiome(Player player, String biomeId) {
        ResourceLocation requiredBiome = new ResourceLocation(biomeId);
        Holder<Biome> currentBiome = player.level().getBiome(player.blockPosition());
        return currentBiome.is(requiredBiome);
    }

    /**
     * Replaces placeholders in commands.
     */
    private static String replacePlaceholders(String command, ServerPlayer player, LivingEntity entity) {
        String result = command;

        // Player placeholders
        result = result.replace("{player}", player.getName().getString());
        result = result.replace("{player_x}", String.valueOf((int) player.getX()));
        result = result.replace("{player_y}", String.valueOf((int) player.getY()));
        result = result.replace("{player_z}", String.valueOf((int) player.getZ()));
        result = result.replace("{player_uuid}", player.getUUID().toString());

        // Entity placeholders
        result = result.replace("{entity_x}", String.valueOf((int) entity.getX()));
        result = result.replace("{entity_y}", String.valueOf((int) entity.getY()));
        result = result.replace("{entity_z}", String.valueOf((int) entity.getZ()));
        result = result.replace("{entity_type}", ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString());
        result = result.replace("{entity_uuid}", entity.getUUID().toString());

        // Special placeholders
        result = result.replace("@killer", player.getName().getString());
        result = result.replace("@player", player.getName().getString());

        return result;
    }
}