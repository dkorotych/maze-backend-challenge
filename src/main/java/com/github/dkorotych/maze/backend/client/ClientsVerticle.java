package com.github.dkorotych.maze.backend.client;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;

import java.nio.charset.StandardCharsets;

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
        final NetServer server = vertx.createNetServer(options);
        server.connectHandler(socket ->
                socket.handler(buffer -> {
                    final String event = buffer.toString(StandardCharsets.UTF_8);
                    logger.debug("Receive new event: {0}", event);
                }))
                .exceptionHandler(throwable -> logger.warn("Can't process event", throwable))
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
}
