package com.github.dkorotych.maze.backend.event;

import com.github.dkorotych.maze.backend.Addresses;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.dkorotych.maze.backend.Addresses.Events.CLOSE;

public class EventProcessor implements Handler<Buffer> {
    private final Logger logger = LoggerFactory.getLogger(EventProcessor.class);
    private final int totalEvents;
    private final EventBus eventBus;
    private final AtomicInteger counter = new AtomicInteger(0);

    EventProcessor(final int totalEvents, final EventBus eventBus) {
        this.totalEvents = totalEvents;
        this.eventBus = eventBus;
    }

    @Override
    public void handle(final Buffer buffer) {
        final String eventAsString = buffer.toString(StandardCharsets.UTF_8).trim();
        logger.debug("Receive new event: {0}", eventAsString);
        final Event event = Event.parse(eventAsString);
        Addresses.Events.createAddress(event).ifPresent(address -> {
            eventBus.publish(address, event);
            final int count = counter.incrementAndGet();
            if (count >= totalEvents) {
                logger.info("Receive maximum events. Send close event source server event");
                eventBus.publish(CLOSE, new JsonObject()
                        .put("total", counter.get()));
            }
        });
    }
}
