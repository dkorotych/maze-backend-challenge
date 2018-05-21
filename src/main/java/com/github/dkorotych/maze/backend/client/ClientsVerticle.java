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
import java.util.Optional;

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
        private final FollowersKeeper followersKeeper;

        private String writeHandlerID;

        ConnectHandler(final EventBus eventBus) {
            this.eventBus = eventBus;
            followersKeeper = new FollowersKeeper();
            parser = RecordParser.newDelimited("\n", buffer -> {
                final int userId = Integer.parseInt(buffer.toString(ENCODING));
                logger.info("User {0} connected", userId);
                final Map<Integer, MessageConsumer<Event>> followers = followersKeeper.getFollowers(userId);
                followersKeeper.setWriteHandlerIdentifier(userId, writeHandlerID);
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
                final Buffer buffer = toBuffer(event);
                final Integer toUser = event.getToUser();
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
                                event.getSequenceNumber(), toUser, null)
                                .toAddress()
                                .ifPresent(address -> {
                                    if (!followers.containsKey(toUser)) {
                                        final MessageConsumer<Event> consumer = eventBus.consumer(address, handler -> {
                                            sendToSocket(handler.body(), userId);
                                        });
                                        followers.put(toUser, consumer);
                                    }
                                });
                        sendToSocket(buffer, toUser);
                        break;
                    case UNFOLLOW:
                        final MessageConsumer<Event> consumer = followers.remove(toUser);
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

        private Buffer toBuffer(final Event event) {
            return Buffer.buffer(event.toString() + "\r\n", ENCODING);
        }

        private void sendToSocket(final Event event, final int userId) {
            sendToSocket(toBuffer(event), userId);
        }

        private void sendToSocket(final Buffer buffer, final int userId) {
            final String command = buffer.toString().trim();
            logger.info("Try to send {0} event to user {1} socket", command, userId);
            Optional.ofNullable(followersKeeper.getWriteHandlerIdentifier(userId))
                    .map(String::trim)
                    .filter(String::isEmpty)
                    .ifPresent(writeHandlerId -> {
                        eventBus.publish(writeHandlerId, buffer);
                        logger.info("Sent {0} to user {1}", command, userId);
                    });
        }
    }
}
