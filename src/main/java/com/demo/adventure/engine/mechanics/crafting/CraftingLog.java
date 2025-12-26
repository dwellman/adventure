package com.demo.adventure.engine.mechanics.crafting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Simple file-backed logger for crafting and related flows.
 */
public final class CraftingLog {
    private static final Logger LOGGER = init();

    private CraftingLog() {
    }

    public static Logger get() {
        return LOGGER;
    }

    private static Logger init() {
        Logger logger = Logger.getLogger("CraftingFlow");
        logger.setUseParentHandlers(false);
        try {
            Files.createDirectories(Path.of("logs"));
            FileHandler handler = new FileHandler("logs/crafting-flow.log", true);
            handler.setFormatter(new SimpleFormatter());
            logger.addHandler(handler);
        } catch (IOException e) {
            // If logging setup fails, fallback silently to default handlers.
            logger.setUseParentHandlers(true);
        }
        return logger;
    }
}
