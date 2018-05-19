package com.github.dkorotych.maze.backend;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunnerWithParametersFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.InetAddress;
import java.net.UnknownHostException;

@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(VertxUnitRunnerWithParametersFactory.class)
public abstract class AbstractChangePortTest {
    private final SocketAddress socketAddress;
    private final Class<? extends AbstractVerticle> verticleClass;
    private final String parameterName;

    public AbstractChangePortTest(final int port,
                                  final Class<? extends AbstractVerticle> aClass,
                                  final String parameterName) throws UnknownHostException {
        socketAddress = SocketAddress.inetSocketAddress(port, InetAddress.getLocalHost().getHostAddress());
        this.verticleClass = aClass;
        this.parameterName = parameterName;
    }

    @Test
    public void changePort(TestContext context) {
        final Vertx vertx = Vertx.vertx();
        final Async async = context.async();
        final DeploymentOptions options = new DeploymentOptions();
        options.setConfig(new JsonObject()
                .put(parameterName, socketAddress.port()));
        vertx.deployVerticle(verticleClass.getName(), options, asyncResult -> {
            final NetClient client = vertx.createNetClient();
            client.connect(socketAddress, event -> {
                if (event.succeeded()) {
                    context.assertEquals(socketAddress, event.result().remoteAddress());
                } else {
                    context.fail(event.cause());
                }
                client.close();
                vertx.close(context.asyncAssertSuccess());
                async.complete();
            });
        });
    }
}
