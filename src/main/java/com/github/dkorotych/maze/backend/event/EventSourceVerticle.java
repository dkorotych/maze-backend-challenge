package com.github.dkorotych.maze.backend.event;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.parsetools.RecordParser;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class EventSourceVerticle extends AbstractVerticle {
    public static final String CLOSE_EVENT_SOURCE_ADDRESS = "/event-source/close";
    public static final int DEFAULT_PORT = 9090;
    public static final int DEFAULT_TOTAL_EVENTS = 10000000;

    private final Logger logger = LoggerFactory.getLogger(EventSourceVerticle.class);

    @Override
    public void start(final Future<Void> startFuture) throws Exception {
        final NetServerOptions options = new NetServerOptions();
        options.setPort(getPort())
                .setLogActivity(true)
                .setTcpFastOpen(true)
                .setTcpKeepAlive(true);
        final int totalEvents = config().getInteger("totalEvents", DEFAULT_TOTAL_EVENTS);
        final EventBus eventBus = vertx.eventBus()
                .registerDefaultCodec(Event.class, new EventMessageCodec());
        eventBus.consumer(CLOSE_EVENT_SOURCE_ADDRESS, message -> vertx.close());
        final EventAggregator eventAggregator = new EventAggregator(totalEvents, eventBus);
        vertx.createNetServer(options)
                .connectHandler(socket -> socket
                        .handler(eventAggregator)
                        .endHandler(event -> {
                            eventAggregator.sendEvents();
                        }))
                .exceptionHandler(throwable -> logger.warn("Can't process one event buffer from socket", throwable))
                .listen(event -> {
                    if (event.succeeded()) {
                        startFuture.complete();
                    } else {
                        startFuture.fail(event.cause());
                    }
                });
        vertx.setPeriodic(config().getInteger("timeout", 10000), timerId -> {
            final Handler<Future<Object>> futureHandler = future -> {
                eventAggregator.sendEvents();
                future.complete();
            };
            final Handler<AsyncResult<Object>> resultHandler = result -> {
                if (result.failed()) {
                    logger.warn("Can't process events buffer", result.cause());
                }
            };
            vertx.executeBlocking(futureHandler, true, resultHandler);
        });
    }

    private int getPort() {
        return config().getInteger("eventListenerPort", DEFAULT_PORT);
    }

    private static class EventAggregator implements Handler<Buffer> {
        private final Logger logger = LoggerFactory.getLogger(EventAggregator.class);

        private final CopyOnWriteArrayList<Event> events;
        private final EventProcessor eventProcessor;
        private final RecordParser parser;
        private final ReentrantLock lock;

        EventAggregator(final int totalEvents, final EventBus eventBus) {
            events = new CopyOnWriteArrayList<>();
            lock = new ReentrantLock();
            eventProcessor = new EventProcessor(totalEvents, eventBus);
            parser = RecordParser.newDelimited("\n", buffer -> {
                final String eventAsString = buffer.toString(StandardCharsets.UTF_8).trim();
                logger.info("Receive new event: {0}", eventAsString);
                final Event event = Event.parse(eventAsString);
                transaction(tmp -> {
                    events.add(event);
                    return null;
                });
            });
        }

        @Override
        public void handle(final Buffer buffer) {
            parser.handle(buffer);
        }

        @SuppressWarnings("unchecked")
        private void sendEvents() {
            transaction(events -> {
                final List<Event> list = (List<Event>) events.clone();
                events.clear();
                return list;
            })
                    .stream()
                    .parallel()
                    .sorted()
                    .forEachOrdered(eventProcessor::handle);
        }

        private List<Event> transaction(final Function<CopyOnWriteArrayList<Event>, List<Event>> function) {
            lock.lock();
            try {
                return function.apply(events);
            } finally {
                lock.unlock();
            }
        }
    }
}
