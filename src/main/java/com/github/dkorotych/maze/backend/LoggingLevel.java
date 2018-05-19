package com.github.dkorotych.maze.backend;

import java.util.Optional;
import java.util.logging.Level;

public enum LoggingLevel {
    INFO(Level.INFO),
    DEBUG(Level.FINE);

    private final Level level;

    LoggingLevel(final Level level) {
        this.level = level;
    }

    public static LoggingLevel fromString(final String name) {
        final String safeName = Optional.ofNullable(name)
                .map(String::trim)
                .orElse("");
        for (LoggingLevel level : values()) {
            if (level.name().equalsIgnoreCase(safeName)) {
                return level;
            }
        }
        return LoggingLevel.INFO;
    }

    public Level getLevel() {
        return level;
    }
}
