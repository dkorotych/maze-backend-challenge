package com.github.dkorotych.maze.backend.event;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class Event implements Comparable<Event> {
    private static final char DELIMITER = '|';
    private static final Pattern PATTERN = Pattern.compile("\\|");

    private final EventType type;
    private final int sequenceNumber;
    private final Integer fromUser;
    private final Integer toUser;

    Event(final EventType type, final int sequenceNumber, final Integer fromUser, final Integer toUser) {
        this.type = type;
        this.sequenceNumber = sequenceNumber;
        this.fromUser = fromUser;
        this.toUser = toUser;
    }

    public static Event parse(final String eventAsString) throws ParseEventException {
        final String[] strings = Optional.ofNullable(eventAsString)
                .map(String::trim)
                .map(event -> PATTERN.split(eventAsString))
                .filter(parts -> parts.length >= 2)
                .filter(parts -> {
                    try {
                        EventType.fromCode(parts[1]);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .orElseThrow(() -> ParseEventException.create(eventAsString));
        try {
            final int number = Integer.parseInt(strings[0]);
            final EventType eventType = EventType.fromCode(strings[1]);
            Integer from = null;
            Integer to = null;
            switch (eventType) {
                case FOLLOW:
                case UNFOLLOW:
                case PRIVATE_MESSAGE:
                    from = Integer.parseInt(strings[2]);
                    to = Integer.parseInt(strings[3]);
                    break;
                case STATUS_UPDATE:
                    from = Integer.parseInt(strings[2]);
                    break;
                default:
            }
            return new Event(eventType, number, from, to);
        } catch (Exception e) {
            throw ParseEventException.create(eventAsString, e);
        }
    }

    public EventType getType() {
        return type;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public Integer getFromUser() {
        return fromUser;
    }

    public Integer getToUser() {
        return toUser;
    }

    @Override
    public String toString() {
        if (type == null) {
            return "";
        }
        final StringBuilder builder = new StringBuilder(16);
        builder.append(sequenceNumber)
                .append(DELIMITER)
                .append(type.getCode());
        if (fromUser != null) {
            builder.append(DELIMITER)
                    .append(fromUser);
        }
        if (toUser != null) {
            builder.append(DELIMITER)
                    .append(toUser);
        }
        return builder.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Event event = (Event) o;
        return sequenceNumber == event.sequenceNumber
                && type == event.type
                && Objects.equals(fromUser, event.fromUser)
                && Objects.equals(toUser, event.toUser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, sequenceNumber, fromUser, toUser);
    }

    @Override
    public int compareTo(final Event event) {
        return Comparator
                .comparingInt(Event::getSequenceNumber)
                .thenComparing(Event::getType)
                .thenComparingInt(Event::getToUser)
                .thenComparingInt(Event::getFromUser)
                .compare(this, event);
    }
}
