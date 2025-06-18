package net.poe.entitylootdrops.adventure;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class AdventureModeConfigLoader {
    private static final Logger LOGGER = LogManager.getLogger();
    private static List<AdventureModeRule> rules = Collections.emptyList();

    /**
     * Loads the Adventure Mode configuration from the specified config directory.
     * @param configDir The config directory path (should be config/EntityLootDrops).
     */
    public static void loadConfig(Path configDir) {
        // Look for Adventure_Mode.json inside Blocks/
        Path blocksDir = configDir.resolve("Blocks");
        Path adventureModePath = blocksDir.resolve("Adventure_Mode.json");

        // If the file does not exist, create it with an example configuration
        if (!java.nio.file.Files.exists(adventureModePath)) {
            try {
                java.nio.file.Files.createDirectories(blocksDir);
                String exampleJson = "[\n" +
                        "  {\n" +
                        "    \"enabled\": false,\n" +
                        "    \"dimension\": \"minecraft:overworld\",\n" +
                        "    \"allowedBlockBreakIDs\": [\n" +
                        "      \"minecraft:oak_log\",\n" +
                        "      \"minecraft:coal_ore\",\n" +
                        "      \"tag:forge:ores\",\n" +
                        "      \"tag:minecraft:logs\",\n" +
                        "      \"minecraft:stone\"\n" +
                        "    ],\n" +
                        "    \"preventBlockPlacement\": true,\n" +
                        "    \"allowedPlacementIDs\": [\n" +
                        "      \"minecraft:torch\",\n" +
                        "      \"minecraft:ladder\",\n" +
                        "      \"tag:minecraft:signs\",\n" +
                        "      \"tag:minecraft:beds\"\n" +
                        "    ],\n" +
                        "    \"preventBlockModification\": true,\n" +
                        "    \"allowedModificationIDs\": [\n" +
                        "      \"minecraft:dirt\",\n" +
                        "      \"minecraft:grass_block\",\n" +
                        "      \"tag:minecraft:logs\"\n" +
                        "    ],\n" +
                        "    \"breakMessage\": \"This block is protected and cannot be broken!\",\n" +
                        "    \"placeMessage\": \"You cannot place blocks here!\",\n" +
                        "    \"modifyMessage\": \"You cannot modify blocks here!\"\n" +
                        "  }\n" +
                        "]";
                java.nio.file.Files.write(adventureModePath, exampleJson.getBytes());
                LOGGER.info("Created example Adventure_Mode.json at {}", adventureModePath);

                // Create the Adventure_Mode-README.txt file
                try {
                    net.poe.entitylootdrops.readme.AdventureModeReadmeCreator.createReadme();
                    LOGGER.info("Created Adventure_Mode-README.txt at {}", blocksDir.resolve("Adventure_Mode-README.txt"));
                } catch (Exception e) {
                    LOGGER.error("Failed to create Adventure_Mode-README.txt", e);
                }
            } catch (IOException e) {
                LOGGER.error("Failed to create example Adventure_Mode.json", e);
            }
        }

        // Now load the config as normal
        try (FileReader reader = new FileReader(adventureModePath.toFile())) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<AdventureModeRule>>(){}.getType();
            rules = gson.fromJson(reader, listType);
            if (rules == null) rules = Collections.emptyList();
        } catch (IOException e) {
            LOGGER.error("Failed to load Adventure_Mode.json from Blocks folder", e);
            rules = Collections.emptyList();
        }
    }

    /**
     * Returns the list of loaded Adventure Mode rules.
     */
    public static List<AdventureModeRule> getRules() {
        return rules;
    }

    /**
     * Returns the active rule for the given dimension, or null if none is enabled.
     */
    public static AdventureModeRule getActiveRuleForDimension(ResourceLocation dimension) {
        for (AdventureModeRule rule : rules) {
            if (rule.isEnabled() && rule.getDimension().equals(dimension.toString())) {
                return rule;
            }
        }
        return null;
    }
}