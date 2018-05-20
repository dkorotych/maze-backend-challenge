package com.github.dkorotych.maze.backend.event;

import java.util.Optional;

public enum EventType {
    FOLLOW('F', "/event-source/follow"),
    UNFOLLOW('U', "/event-source/unfollow"),
    BROADCAST('B', "/event-source/broadcast"),
    PRIVATE_MESSAGE('P', "/event-source/message"),
    STATUS_UPDATE('S', "/event-source/status");

    private final char code;
    private final String address;

    EventType(final char code, final String address) {
        this.code = code;
        this.address = address;
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

    public String getAddress() {
        return address;
    }
}
