package com.github.dkorotych.maze.backend.event;

import java.util.Optional;

public enum EventType {
    FOLLOW('F'),
    UNFOLLOW('U'),
    BROADCAST('B'),
    PRIVATE_MESSAGE('P'),
    STATUS_UPDATE('S');

    private final char code;

    EventType(final char code) {
        this.code = code;
    }

    public static EventType fromCode(final String code) throws ParseEventException {
        final Character character = Optional.ofNullable(code)
                .filter(string -> string.length() == 1)
                .map(String::trim)
                .filter(string -> !string.isEmpty())
                .map(string -> string.charAt(0))
                .orElse(null);
        return fromCode(character);
    }

    public static EventType fromCode(final Character code) throws ParseEventException {
        if (code != null) {
            for (EventType type : values()) {
                if (code.equals(type.code)) {
                    return type;
                }
            }
        }
        throw ParseEventException.create(code);
    }

    public char getCode() {
        return code;
    }
}
