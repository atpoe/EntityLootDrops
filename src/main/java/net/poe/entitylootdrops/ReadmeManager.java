package net.poe.entitylootdrops;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.poe.entitylootdrops.readme.*;

/**
 * Handles the creation and management of README files for the EntityLootDrops mod.
 * This centralizes all documentation to prevent conflicts between configuration classes.
 */
public class ReadmeManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String CONFIG_DIR = "config/EntityLootDrops";

    /**
     * Creates all README files for the mod.
     * This should be called during mod initialization.
     */
    public static void createAllReadmeFiles() {
        try {
            // Create main config directory if it doesn't exist
            Path configDir = Paths.get(CONFIG_DIR);
            Files.createDirectories(configDir);

            // Create loot drops READMEs
            Path lootDropsDir = configDir.resolve("Loot Drops");
            if (Files.exists(lootDropsDir)) {
                MainReadme.create(lootDropsDir);

                Path normalDropsDir = lootDropsDir.resolve("Normal Drops");
                if (Files.exists(normalDropsDir)) {
                    NormalDropsReadme.create(normalDropsDir);
                }

                Path eventDropsDir = lootDropsDir.resolve("Event Drops");
                if (Files.exists(eventDropsDir)) {
                    EventDropsReadme.create(eventDropsDir);
                }
            }

            LOGGER.info("Created all README files");
        } catch (IOException e) {
            LOGGER.error("Failed to create README files", e);
        }
    }
}