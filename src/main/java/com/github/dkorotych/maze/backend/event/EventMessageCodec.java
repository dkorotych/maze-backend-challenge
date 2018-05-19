package com.github.dkorotych.maze.backend.event;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.eventbus.impl.codecs.StringMessageCodec;

public class EventMessageCodec implements MessageCodec<Event, Event> {

    private final StringMessageCodec codec = new StringMessageCodec();

    @Override
    public void encodeToWire(final Buffer buffer, final Event event) {
        codec.encodeToWire(buffer, event.toString());
    }

    @Override
    public Event decodeFromWire(final int pos, final Buffer buffer) {
        return Event.parse(codec.decodeFromWire(pos, buffer));
    }

    @Override
    public Event transform(final Event event) {
        return event;
    }

    @Override
    public String name() {
        return "event";
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }
}
