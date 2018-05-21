package com.github.dkorotych.maze.backend.event;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

class EventProcessor implements Handler<Event> {
    private final Logger logger = LoggerFactory.getLogger(EventProcessor.class);
    private final int totalEvents;
    private final EventBus eventBus;
    private final AtomicInteger counter = new AtomicInteger(0);

    EventProcessor(final int totalEvents, final EventBus eventBus) {
        this.totalEvents = totalEvents;
        this.eventBus = eventBus;
    }

    @Override
    public void handle(final Event event) {
        event.toAddress().ifPresent(address -> {
            logger.debug("Publish event {0} to address {1}", event, address);
            eventBus.publish(address, event);
            final int count = counter.incrementAndGet();
            if (count >= totalEvents) {
                logger.info("Receive maximum events. Send close event source server event");
                eventBus.publish(EventSourceVerticle.CLOSE_EVENT_SOURCE_ADDRESS, new JsonObject()
                        .put("total", counter.get()));
            }
        });
    }
}
