package com.github.dkorotych.maze.backend.event;

public final class ParseEventException extends RuntimeException {
    private ParseEventException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

    protected static ParseEventException create(final String event) {
        return create(event, null);
    }

    protected static ParseEventException create(final String event, final Throwable throwable) {
        return create(event, "", throwable);
    }

    protected static ParseEventException create(final Character eventType) {
        return create(String.valueOf(eventType), " type", null);
    }

    private static ParseEventException create(final String event, final String typePart, final Throwable throwable) {
        return new ParseEventException(String.format("Can not parse %s as event%s", event, typePart), throwable);
    }
}
