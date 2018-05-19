package com.github.dkorotych.maze.backend.event;

import com.github.dkorotych.maze.backend.Addresses;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.parsetools.RecordParser;

public class EventSourceVerticle extends AbstractVerticle {
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
        eventBus.consumer(Addresses.Events.CLOSE, message -> vertx.close());
        final EventProcessor eventProcessor = new EventProcessor(totalEvents, eventBus);
        final RecordParser parser = RecordParser.newDelimited("\n", eventProcessor);
        vertx.createNetServer(options)
                .connectHandler(socket -> socket.handler(parser))
                .exceptionHandler(throwable -> logger.warn("Can't process one event buffer from socket", throwable))
                .listen(event -> {
                    if (event.succeeded()) {
                        startFuture.complete();
                    } else {
                        startFuture.fail(event.cause());
                    }
                });
    }

    private int getPort() {
        return config().getInteger("eventListenerPort", DEFAULT_PORT);
    }
}
