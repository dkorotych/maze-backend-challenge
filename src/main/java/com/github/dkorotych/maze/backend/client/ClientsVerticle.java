package com.github.dkorotych.maze.backend.client;

import com.github.dkorotych.maze.backend.event.Event;
import com.github.dkorotych.maze.backend.event.EventType;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.parsetools.RecordParser;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientsVerticle extends AbstractVerticle {
    public static final int DEFAULT_PORT = 9099;

    private final Logger logger = LoggerFactory.getLogger(ClientsVerticle.class);

    @Override
    public void start(final Future<Void> startFuture) throws Exception {
        final NetServerOptions options = new NetServerOptions();
        options.setPort(getPort())
                .setLogActivity(true)
                .setTcpFastOpen(true)
                .setTcpKeepAlive(true);
        final EventBus eventBus = vertx.eventBus();
        final ConnectHandler parser = new ConnectHandler(eventBus);
        vertx.createNetServer(options)
                .connectHandler(socket -> {
                    parser.writeHandlerID = socket.writeHandlerID();
                    socket.handler(parser);
                })
                .exceptionHandler(throwable -> logger.warn("Can't process client connection", throwable))
                .listen(event -> {
                    if (event.succeeded()) {
                        startFuture.complete();
                    } else {
                        startFuture.fail(event.cause());
                    }
                });
    }

    private int getPort() {
        return config().getInteger("clientListenerPort", DEFAULT_PORT);
    }

    private static final class ConnectHandler implements RecordParser {
        private static final String ENCODING = StandardCharsets.UTF_8.name();

        private final Logger logger = LoggerFactory.getLogger(ConnectHandler.class);
        private final RecordParser parser;
        private final EventBus eventBus;

        private String writeHandlerID;

        ConnectHandler(final EventBus eventBus) {
            this.eventBus = eventBus;
            parser = RecordParser.newDelimited("\n", buffer -> {
                final int userId = Integer.parseInt(buffer.toString(ENCODING));
                logger.debug("User {0} connected", userId);
                final Map<Integer, MessageConsumer<Event>> followers = new ConcurrentHashMap<>();
                final Event broadcastEvent = new Event(EventType.BROADCAST, 0, null, null);
                final Event privateMessageEvent = new Event(EventType.PRIVATE_MESSAGE, 0, null, userId);
                final Event statusUpdateEvent = new Event(EventType.STATUS_UPDATE, 0, userId, null);
                final Event followEvent = new Event(EventType.FOLLOW, 0, userId, userId);
                final Event unfollowEvent = new Event(EventType.UNFOLLOW, 0, userId, null);
                for (Event event : Arrays.asList(
                        broadcastEvent,
                        privateMessageEvent,
                        statusUpdateEvent,
                        followEvent,
                        unfollowEvent)) {
                    event.toAddress()
                            .ifPresent(address -> {
                                logger.debug("Register user {0} as consumer for events {1}", userId, address);
                                this.eventBus.consumer(address, eventHandler(userId, followers));
                            });
                }
            })
                    .exceptionHandler(throwable -> logger.warn("Can't process batch client events", throwable));
        }

        @Override
        public void setOutput(final Handler<Buffer> output) {
            parser.setOutput(output);
        }

        @Override
        public void delimitedMode(final String delim) {
            parser.delimitedMode(delim);
        }

        @Override
        public void delimitedMode(final Buffer delim) {
            parser.delimitedMode(delim);
        }

        @Override
        public void fixedSizeMode(final int size) {
            parser.fixedSizeMode(size);
        }

        @Override
        public void handle(final Buffer buffer) {
            parser.handle(buffer);
        }

        @Override
        public RecordParser exceptionHandler(final Handler<Throwable> handler) {
            return parser.exceptionHandler(handler);
        }

        @Override
        public RecordParser handler(final Handler<Buffer> handler) {
            return parser.handler(handler);
        }

        @Override
        public RecordParser pause() {
            return parser.pause();
        }

        @Override
        public RecordParser resume() {
            return parser.resume();
        }

        @Override
        public RecordParser endHandler(final Handler<Void> endHandler) {
            return parser.endHandler(endHandler);
        }

        private Handler<Message<Event>> eventHandler(final int userId,
                                                     final Map<Integer, MessageConsumer<Event>> followers) {
            return message -> {
                final Event event = message.body();
                logger.debug("User {0} receive event {1}", userId, event);
                final Buffer buffer = Buffer.buffer(event.toString() + "\r\n", ENCODING);
                switch (event.getType()) {
                    case BROADCAST:
                        sendToSocket(buffer);
                        break;
                    case PRIVATE_MESSAGE:
                        sendToSocket(buffer);
                        break;
                    case STATUS_UPDATE:
                        if (!followers.isEmpty()) {
                            sendToSocket(buffer);
                        }
                        break;
                    case FOLLOW:
                        new Event(EventType.STATUS_UPDATE,
                                event.getSequenceNumber(), event.getToUser(), null)
                                .toAddress()
                                .ifPresent(address -> followers.put(event.getToUser(),
                                        eventBus.consumer(address, eventHandler(userId, followers))));
                        // sendToSocket(buffer);
                        break;
                    case UNFOLLOW:
                        final MessageConsumer<Event> consumer = followers.remove(event.getToUser());
                        if (consumer != null) {
                            consumer.unregister();
                        }
                        break;
                    default:
                        logger.warn("Type {0} can not be processing", event.getType());
                        break;
                }
            };
        }

        private void sendToSocket(final Buffer buffer) {
            logger.debug("Send {0} to socket", buffer.toString().trim());
            eventBus.publish(writeHandlerID, buffer);
        }
    }
}
