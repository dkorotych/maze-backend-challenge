package com.github.dkorotych.maze.backend;

import com.github.dkorotych.maze.backend.client.ClientsVerticle;
import com.github.dkorotych.maze.backend.event.EventSourceVerticle;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MainVerticle extends AbstractVerticle {
    private final io.vertx.core.logging.Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void init(final Vertx vertx, final Context context) {
        initializeLogger(context.config());
        super.init(vertx, context);
    }

    @Override
    public void start(final Future<Void> startFuture) {
        logger.debug("Main verticle has started, let's deploy some others...");
        vertx.eventBus()
                .consumer(Addresses.Events.CLOSE, (Handler<Message<JsonObject>>) message -> {
                    final JsonObject object = message.body();
                    logger.info("{0} messages was received. Close all related servers", object.getInteger("total", 0));
                    vertx.close();
                });
        ConfigRetriever.create(vertx).getConfig(configAsyncResult -> {
            if (configAsyncResult.succeeded()) {
                final JsonObject config = configAsyncResult.result();
                final Future<String> eventSourceVerticleFuture = deploy("Events source",
                        EventSourceVerticle.class, config);
                final Future<String> clientsVerticleFuture = deploy("Clients", ClientsVerticle.class, config);
                CompositeFuture.all(eventSourceVerticleFuture, clientsVerticleFuture).setHandler(event -> {
                    if (event.succeeded()) {
                        logger.info("Server started successfully");
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

    private void initializeLogger(final JsonObject config) {
        final String levelName = config.getString("logLevel", "info");
        final Level level = LoggingLevel.fromString(levelName).getLevel();
        Logger.getGlobal().setLevel(level);
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
