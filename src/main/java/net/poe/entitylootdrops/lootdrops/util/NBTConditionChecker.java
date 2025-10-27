package net.poe.entitylootdrops.lootdrops.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class for checking NBT data conditions on entities.
 * Supports reading nested NBT paths and comparing values with various operators.
 */
public class NBTConditionChecker {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Checks if an entity's NBT data meets the specified condition.
     *
     * @param entity The entity to check
     * @param nbtPath The path to the NBT data (e.g., "ForgeCaps.mmorpg:entity_data.health")
     * @param condition The comparison operator ("<", ">", "<=", ">=", "==", "!=")
     * @param valueStr The value to compare against
     * @return true if the condition is met, false otherwise
     */
    public static boolean checkCondition(LivingEntity entity, String nbtPath, String condition, String valueStr) {
        if (entity == null || nbtPath == null || condition == null || valueStr == null) {
            return false;
        }

        try {
            // Get entity's NBT data
            CompoundTag entityNbt = new CompoundTag();
            entity.saveWithoutId(entityNbt);

            // Navigate to the specified NBT path
            Tag nbtValue = getNbtValueAtPath(entityNbt, nbtPath);

            if (nbtValue == null) {
                LOGGER.debug("NBT path not found: {} for entity {}", nbtPath, entity.getType().getDescriptionId());
                return false;
            }

            // Compare the values based on the condition
            return compareValues(nbtValue, condition, valueStr);

        } catch (Exception e) {
            LOGGER.error("Error checking NBT condition for path '{}': {}", nbtPath, e.getMessage());
            return false;
        }
    }

    /**
     * Navigates through NBT compound tags to find the value at the specified path.
     *
     * @param nbt The root NBT compound
     * @param path The dot-separated path (e.g., "ForgeCaps.mmorpg:entity_data.health")
     * @return The NBT tag at the path, or null if not found
     */
    private static Tag getNbtValueAtPath(CompoundTag nbt, String path) {
        String[] parts = path.split("\\.");
        Tag current = nbt;

        for (String part : parts) {
            if (!(current instanceof CompoundTag)) {
                return null;
            }

            CompoundTag compound = (CompoundTag) current;
            if (!compound.contains(part)) {
                return null;
            }

            current = compound.get(part);
        }

        return current;
    }

    /**
     * Compares an NBT value against a string value using the specified operator.
     *
     * @param nbtValue The NBT tag to compare
     * @param operator The comparison operator
     * @param valueStr The value to compare against
     * @return true if the comparison is true, false otherwise
     */
    private static boolean compareValues(Tag nbtValue, String operator, String valueStr) {
        try {
            // Determine the type of NBT value and compare accordingly
            switch (nbtValue.getId()) {
                case Tag.TAG_BYTE:
                case Tag.TAG_SHORT:
                case Tag.TAG_INT:
                case Tag.TAG_LONG:
                case Tag.TAG_FLOAT:
                case Tag.TAG_DOUBLE:
                    return compareNumeric(getNumericValue(nbtValue), operator, parseNumeric(valueStr));

                case Tag.TAG_STRING:
                    return compareString(nbtValue.getAsString(), operator, valueStr);

                case Tag.TAG_BYTE_ARRAY:
                case Tag.TAG_INT_ARRAY:
                case Tag.TAG_LONG_ARRAY:
                    LOGGER.warn("Array comparison not supported for NBT condition");
                    return false;

                default:
                    LOGGER.warn("Unsupported NBT type for comparison: {}", nbtValue.getId());
                    return false;
            }
        } catch (Exception e) {
            LOGGER.error("Error comparing values: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extracts a numeric value from an NBT tag.
     */
    private static double getNumericValue(Tag tag) {
        switch (tag.getId()) {
            case Tag.TAG_BYTE:
            case Tag.TAG_SHORT:
            case Tag.TAG_INT:
            case Tag.TAG_LONG:
            case Tag.TAG_FLOAT:
            case Tag.TAG_DOUBLE:
                return Double.parseDouble(tag.getAsString());
            default:
                throw new IllegalArgumentException("Not a numeric NBT tag");
        }
    }

    /**
     * Parses a string value as a number.
     */
    private static double parseNumeric(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric value: " + value);
        }
    }

    /**
     * Compares two numeric values using the specified operator.
     */
    private static boolean compareNumeric(double nbtValue, String operator, double compareValue) {
        switch (operator) {
            case "<":
                return nbtValue < compareValue;
            case ">":
                return nbtValue > compareValue;
            case "<=":
                return nbtValue <= compareValue;
            case ">=":
                return nbtValue >= compareValue;
            case "==":
                return Math.abs(nbtValue - compareValue) < 0.0001; // Float comparison tolerance
            case "!=":
                return Math.abs(nbtValue - compareValue) >= 0.0001;
            default:
                LOGGER.warn("Unknown numeric operator: {}", operator);
                return false;
        }
    }

    /**
     * Compares two string values using the specified operator.
     */
    private static boolean compareString(String nbtValue, String operator, String compareValue) {
        switch (operator) {
            case "==":
                return nbtValue.equals(compareValue);
            case "!=":
                return !nbtValue.equals(compareValue);
            default:
                LOGGER.warn("Unsupported string operator: {}. Only == and != are supported for strings.", operator);
                return false;
        }
    }

    /**
     * Validates that an operator is supported.
     */
    public static boolean isValidOperator(String operator) {
        return operator != null && (
                operator.equals("<") || operator.equals(">") ||
                        operator.equals("<=") || operator.equals(">=") ||
                        operator.equals("==") || operator.equals("!=")
        );
    }
}