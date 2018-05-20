package com.github.dkorotych.maze.backend;

import com.github.dkorotych.maze.backend.client.ClientsVerticle;
import com.github.dkorotych.maze.backend.event.EventSourceVerticle;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Arrays;
import java.util.logging.Level;

public class MainVerticle extends AbstractVerticle {
    private final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(final Future<Void> startFuture) {
        logger.debug("Main verticle has started, let's deploy some others...");
        vertx.eventBus()
                .consumer(EventSourceVerticle.CLOSE_EVENT_SOURCE_ADDRESS, (Handler<Message<JsonObject>>) message -> {
                    final JsonObject object = message.body();
                    logger.info("{0} messages was received. Close all related servers", object.getInteger("total", 0));
                    vertx.close();
                });
        ConfigRetriever.create(vertx).getConfig(configAsyncResult -> {
            if (configAsyncResult.succeeded()) {
                final JsonObject config = configAsyncResult.result();
                initializeLogger(config);
                final Future<String> eventSourceVerticleFuture = deploy("Events source",
                        EventSourceVerticle.class, config);
                final Future<String> clientsVerticleFuture = deploy("Clients", ClientsVerticle.class, config);
                CompositeFuture.all(eventSourceVerticleFuture, clientsVerticleFuture).setHandler(event -> {
                    if (event.succeeded()) {
                        logger.info("Server started successfully. Try to receive {0} events",
                                config.getInteger("totalEvents"));
                        startFuture.complete();
                    } else {
                        startFuture.fail(event.cause());
                    }
                });
            } else {
                startFuture.fail(configAsyncResult.cause());
            }
        });
    }

    @SuppressWarnings("checkstyle:UnnecessaryParentheses")
    private void initializeLogger(final JsonObject config) {
        final String levelName = config.getString("logLevel", "info");
        final Level level = LoggingLevel.fromString(levelName).getLevel();
        Arrays.asList(java.util.logging.Logger.getGlobal(), ((java.util.logging.Logger) logger.getDelegate().unwrap()))
                .forEach(log -> {
                    log.setLevel(level);
                    for (java.util.logging.Handler handler : log.getHandlers()) {
                        handler.setLevel(level);
                    }
                });
    }

    private Future<String> deploy(final String name, final Class<? extends AbstractVerticle> aClass,
                                  final JsonObject config) {
        final Future<String> future = Future.future();
        future.setHandler(event -> {
            if (event.succeeded()) {
                logger.debug("{0} verticle deployed ok, deploymentID = {1}", name, event.result());
            }
        });
        final DeploymentOptions options = new DeploymentOptions();
        options.setConfig(config);
        vertx.deployVerticle(aClass.getName(), options, future.completer());
        return future;
    }
}
