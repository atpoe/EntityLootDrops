package net.poe.entitylootdrops.lootdrops.handlers;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.poe.entitylootdrops.lootdrops.LootConfig;
import net.poe.entitylootdrops.lootdrops.model.CustomDropEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles NBT-based conditional drops for entities.
 * Supports nested paths, array indexing, and various comparison operators.
 */
public class NBTDropHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random RANDOM = new Random();

    // Pattern to match array indices like [0], [5], etc.
    private static final Pattern ARRAY_INDEX_PATTERN = Pattern.compile("(.+?)\\[(\\d+)\\]");

    /**
     * Checks if the entity meets the NBT conditions specified in the drop entry.
     */
    public static boolean checkNBTConditions(CustomDropEntry drop, LivingEntity entity) {
        if (!drop.hasNbtEntityCondition()) {
            return true;
        }

        boolean debugLoggingEnabled = LootConfig.isDebugLoggingEnabled();

        try {
            CompoundTag entityData = new CompoundTag();
            entity.saveWithoutId(entityData);

            if (debugLoggingEnabled) {
                LOGGER.info("[NBT Debug] Checking NBT condition for entity: {}",
                        ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
                LOGGER.info("[NBT Debug] NBT Path: {}", drop.getNbtEntityData());
                LOGGER.info("[NBT Debug] Condition: {}", drop.getNbtDataCondition());
                LOGGER.info("[NBT Debug] Expected Value: {}", drop.getNbtDataValue());
                LOGGER.info("[NBT Debug] Full entity NBT: {}", entityData.toString());
            }

            Tag nbtValue = navigateNBTPath(entityData, drop.getNbtEntityData());

            if (nbtValue == null) {
                if (debugLoggingEnabled) {
                    LOGGER.info("[NBT Debug] Failed to find NBT value at path: {}", drop.getNbtEntityData());
                }
                return false;
            }

            boolean result = compareValues(nbtValue, drop.getNbtDataCondition(), drop.getNbtDataValue());

            if (debugLoggingEnabled) {
                LOGGER.info("[NBT Debug] Found value: {}", nbtValue.getAsString());
                LOGGER.info("[NBT Debug] Condition result: {}", result);
            }

            return result;

        } catch (Exception e) {
            LOGGER.error("Error checking NBT conditions: {}", e.getMessage());
            if (debugLoggingEnabled) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Navigates through NBT path with support for:
     * - Nested compound tags (ForgeCaps.mmorpg:entity_data.level)
     * - Array indexing (HandItems[0].id)
     * - Mixed paths (Inventory[5].tag.display.Name)
     */
    private static Tag navigateNBTPath(CompoundTag nbt, String path) {
        boolean debugLoggingEnabled = LootConfig.isDebugLoggingEnabled();

        String[] parts = path.split("\\.");
        Tag current = nbt;

        if (debugLoggingEnabled) {
            LOGGER.info("[NBT Debug] Navigating path: {}", path);
            LOGGER.info("[NBT Debug] Path parts: {}", String.join(" -> ", parts));
        }

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            if (debugLoggingEnabled) {
                LOGGER.info("[NBT Debug] Processing part [{}]: {}", i, part);
            }

            // Check if this part has an array index
            Matcher matcher = ARRAY_INDEX_PATTERN.matcher(part);

            if (matcher.matches()) {
                // Handle array indexing like HandItems[0]
                String arrayName = matcher.group(1);
                int index = Integer.parseInt(matcher.group(2));

                if (debugLoggingEnabled) {
                    LOGGER.info("[NBT Debug] Array access: {}[{}]", arrayName, index);
                }

                if (!(current instanceof CompoundTag)) {
                    if (debugLoggingEnabled) {
                        LOGGER.info("[NBT Debug] Current tag is not CompoundTag, cannot access array");
                    }
                    return null;
                }

                CompoundTag compound = (CompoundTag) current;
                if (!compound.contains(arrayName)) {
                    if (debugLoggingEnabled) {
                        LOGGER.info("[NBT Debug] Array '{}' not found in compound", arrayName);
                    }
                    return null;
                }

                Tag arrayTag = compound.get(arrayName);

                // Handle ListTag (most common for arrays)
                if (arrayTag instanceof ListTag) {
                    ListTag list = (ListTag) arrayTag;
                    if (debugLoggingEnabled) {
                        LOGGER.info("[NBT Debug] Found ListTag with {} elements", list.size());
                    }

                    if (index >= 0 && index < list.size()) {
                        current = list.get(index);
                        if (debugLoggingEnabled) {
                            LOGGER.info("[NBT Debug] Retrieved element at index {}: {}", index, current);
                        }
                    } else {
                        if (debugLoggingEnabled) {
                            LOGGER.info("[NBT Debug] Index {} out of bounds (size: {})", index, list.size());
                        }
                        return null;
                    }
                } else {
                    if (debugLoggingEnabled) {
                        LOGGER.info("[NBT Debug] Tag '{}' is not a ListTag: {}", arrayName, arrayTag.getClass().getSimpleName());
                    }
                    return null;
                }
            } else {
                // Regular compound tag navigation
                if (!(current instanceof CompoundTag)) {
                    if (debugLoggingEnabled) {
                        LOGGER.info("[NBT Debug] Current tag is not CompoundTag: {}", current.getClass().getSimpleName());
                    }
                    return null;
                }

                CompoundTag compound = (CompoundTag) current;
                if (!compound.contains(part)) {
                    if (debugLoggingEnabled) {
                        LOGGER.info("[NBT Debug] Key '{}' not found in compound", part);
                        LOGGER.info("[NBT Debug] Available keys: {}", compound.getAllKeys());
                    }
                    return null;
                }

                current = compound.get(part);
                if (debugLoggingEnabled) {
                    LOGGER.info("[NBT Debug] Found key '{}': {}", part, current);
                }
            }
        }

        if (debugLoggingEnabled) {
            LOGGER.info("[NBT Debug] Navigation complete. Final value: {}", current);
        }

        return current;
    }

    /**
     * Compares NBT values with support for:
     * - Numeric comparisons (<, >, <=, >=, ==, !=)
     * - String comparisons (==, !=, contains, startsWith, endsWith)
     * - Boolean comparisons (==, !=)
     */
    private static boolean compareValues(Tag nbtValue, String operator, String expectedValue) {
        boolean debugLoggingEnabled = LootConfig.isDebugLoggingEnabled();

        try {
            // Handle numeric comparisons
            if (isNumericTag(nbtValue)) {
                return compareNumeric(nbtValue, operator, expectedValue);
            }

            // Handle string comparisons
            if (nbtValue instanceof StringTag) {
                return compareString(nbtValue.getAsString(), operator, expectedValue);
            }

            // Handle boolean comparisons (stored as bytes in NBT)
            if (nbtValue.getId() == Tag.TAG_BYTE && (expectedValue.equalsIgnoreCase("true") || expectedValue.equalsIgnoreCase("false"))) {
                return compareBoolean(nbtValue, operator, expectedValue);
            }

            LOGGER.warn("Unsupported NBT type for comparison: {}", nbtValue.getClass().getSimpleName());
            return false;

        } catch (Exception e) {
            LOGGER.error("Error comparing values: {}", e.getMessage());
            if (debugLoggingEnabled) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Checks if an NBT tag is numeric.
     */
    private static boolean isNumericTag(Tag tag) {
        int id = tag.getId();
        return id == Tag.TAG_BYTE || id == Tag.TAG_SHORT || id == Tag.TAG_INT ||
                id == Tag.TAG_LONG || id == Tag.TAG_FLOAT || id == Tag.TAG_DOUBLE;
    }

    /**
     * Compares numeric NBT values.
     */
    private static boolean compareNumeric(Tag nbtValue, String operator, String expectedValue) {
        boolean debugLoggingEnabled = LootConfig.isDebugLoggingEnabled();

        try {
            double actual = getNumericValue(nbtValue);
            double expected = Double.parseDouble(expectedValue);

            if (debugLoggingEnabled) {
                LOGGER.info("[NBT Debug] Numeric comparison: {} {} {}", actual, operator, expected);
            }

            switch (operator) {
                case "<": return actual < expected;
                case ">": return actual > expected;
                case "<=": return actual <= expected;
                case ">=": return actual >= expected;
                case "==": return Math.abs(actual - expected) < 0.0001;
                case "!=": return Math.abs(actual - expected) >= 0.0001;
                default:
                    LOGGER.warn("Unknown numeric operator: {}", operator);
                    return false;
            }
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid numeric value: {}", expectedValue);
            return false;
        }
    }

    /**
     * Gets numeric value from NBT tag.
     */
    private static double getNumericValue(Tag tag) {
        switch (tag.getId()) {
            case Tag.TAG_BYTE: return ((ByteTag) tag).getAsByte();
            case Tag.TAG_SHORT: return ((ShortTag) tag).getAsShort();
            case Tag.TAG_INT: return ((IntTag) tag).getAsInt();
            case Tag.TAG_LONG: return ((LongTag) tag).getAsLong();
            case Tag.TAG_FLOAT: return ((FloatTag) tag).getAsFloat();
            case Tag.TAG_DOUBLE: return ((DoubleTag) tag).getAsDouble();
            default: throw new IllegalArgumentException("Not a numeric tag");
        }
    }

    /**
     * Compares string NBT values with extended operators.
     * Supports: ==, !=, contains, startsWith, endsWith
     */
    private static boolean compareString(String actual, String operator, String expected) {
        boolean debugLoggingEnabled = LootConfig.isDebugLoggingEnabled();

        if (debugLoggingEnabled) {
            LOGGER.info("[NBT Debug] String comparison: '{}' {} '{}'", actual, operator, expected);
        }

        switch (operator) {
            case "==": return actual.equals(expected);
            case "!=": return !actual.equals(expected);
            case "contains": return actual.contains(expected);
            case "startsWith": return actual.startsWith(expected);
            case "endsWith": return actual.endsWith(expected);
            default:
                LOGGER.warn("Unknown string operator: {}", operator);
                return false;
        }
    }

    /**
     * Compares boolean NBT values (stored as bytes: 0 = false, 1 = true).
     */
    private static boolean compareBoolean(Tag nbtValue, String operator, String expectedValue) {
        boolean debugLoggingEnabled = LootConfig.isDebugLoggingEnabled();

        boolean actual = ((ByteTag) nbtValue).getAsByte() != 0;
        boolean expected = Boolean.parseBoolean(expectedValue);

        if (debugLoggingEnabled) {
            LOGGER.info("[NBT Debug] Boolean comparison: {} {} {}", actual, operator, expected);
        }

        switch (operator) {
            case "==": return actual == expected;
            case "!=": return actual != expected;
            default:
                LOGGER.warn("Unknown boolean operator: {}", operator);
                return false;
        }
    }

    /**
     * Processes NBT-based drops when conditions are met.
     */
    public static void processNBTDrop(LivingDropsEvent event, CustomDropEntry drop, Player player, String eventName) {
        boolean debugLoggingEnabled = LootConfig.isDebugLoggingEnabled();

        String itemId = drop.getNbtEntityDrop();
        float dropChance = drop.getNbtEntityDropChance();
        int minAmount = drop.getNbtEntityDropMin();
        int maxAmount = drop.getNbtEntityDropMax();

        if (debugLoggingEnabled) {
            LOGGER.info("[NBT Drop] Processing NBT drop for event: {}", eventName != null ? eventName : "normal");
            LOGGER.info("[NBT Drop] Item: {}, Chance: {}%, Amount: {}-{}",
                    itemId, dropChance, minAmount, maxAmount);
        }

        if (RANDOM.nextFloat() * 100 <= dropChance) {
            int amount = calculateAmount(minAmount, maxAmount);
            ItemStack stack = createItemStack(itemId, amount, drop);

            if (stack != null && !stack.isEmpty()) {
                event.getEntity().spawnAtLocation(stack);

                if (debugLoggingEnabled) {
                    LOGGER.info("[NBT Drop] Dropped {} x{} at {}", itemId, amount, event.getEntity().position());
                }
            }
        } else {
            if (debugLoggingEnabled) {
                LOGGER.info("[NBT Drop] Drop chance failed for {}", itemId);
            }
        }
    }

    /**
     * Calculates random amount between min and max.
     */
    private static int calculateAmount(int min, int max) {
        if (min >= max) {
            return min;
        }
        return min + RANDOM.nextInt(max - min + 1);
    }

    /**
     * Creates an ItemStack with NBT data if specified.
     */
    private static ItemStack createItemStack(String itemId, int amount, CustomDropEntry drop) {
        boolean debugLoggingEnabled = LootConfig.isDebugLoggingEnabled();

        try {
            ResourceLocation itemLocation = new ResourceLocation(itemId);
            Item item = ForgeRegistries.ITEMS.getValue(itemLocation);

            if (item == null) {
                LOGGER.error("Invalid item ID: {}", itemId);
                return null;
            }

            ItemStack stack = new ItemStack(item, amount);

            // Apply NBT data if specified
            if (drop.getNbtData() != null && !drop.getNbtData().isEmpty()) {
                try {
                    CompoundTag nbt = net.minecraft.nbt.TagParser.parseTag(drop.getNbtData());
                    stack.setTag(nbt);

                    if (debugLoggingEnabled) {
                        LOGGER.info("[NBT Drop] Applied NBT data to item: {}", drop.getNbtData());
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to parse NBT data for item {}: {}", itemId, e.getMessage());
                }
            }

            return stack;

        } catch (Exception e) {
            LOGGER.error("Error creating item stack for {}: {}", itemId, e.getMessage());
            if (debugLoggingEnabled) {
                e.printStackTrace();
            }
            return null;
        }
    }
}