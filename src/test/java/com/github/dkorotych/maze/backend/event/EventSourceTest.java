package com.github.dkorotych.maze.backend.event;

import com.github.dkorotych.maze.backend.MainVerticle;
import com.github.dkorotych.maze.backend.client.ClientsVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunnerWithParametersFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(VertxUnitRunnerWithParametersFactory.class)
public class EventSourceTest {

    @Rule
    public Timeout timeout = Timeout.seconds(5);

    @Parameterized.Parameters
    public static Iterable<Integer> data() {
        return Arrays.asList(10, 1, 100, 134, 500, 34);
    }

    private final int totalEvents;

    public EventSourceTest(int totalEvents) {
        this.totalEvents = totalEvents;
    }

    @Test
    public void calculateEvents(TestContext context) {
        StringBuilder builder = new StringBuilder(4096);
        for (int i = 0; i < totalEvents; i++) {
            builder.append(i).append('|').append('B').append('\n');
        }
        final Vertx vertx = Vertx.vertx();
        final Async async = context.async();
        final DeploymentOptions options = new DeploymentOptions();
        options.setConfig(new JsonObject()
                .put("totalEvents", totalEvents));
        vertx.eventBus().
                consumer(EventSourceVerticle.CLOSE_EVENT_SOURCE_ADDRESS, (Handler<Message<JsonObject>>) message -> {
                    context.assertEquals(message.body().getInteger("total"), totalEvents);
                    async.complete();
                });
        vertx.deployVerticle(EventSourceVerticle.class.getName(), options, asyncResult -> {
            final NetClient client = vertx.createNetClient();
            client.connect(EventSourceVerticle.DEFAULT_PORT, InetAddress.getLoopbackAddress().getHostAddress(), socketAsyncResult -> {
                if (socketAsyncResult.succeeded()) {
                    final NetSocket socket = socketAsyncResult.result();
                    socket.write(builder.toString()).end();
                } else {
                    context.fail(socketAsyncResult.cause());
                }
            });
        });
    }

    @Test
    public void sortingEventsTest(TestContext context) {
        StringBuilder builder = new StringBuilder(4096);
        Random random = new Random();
        for (int i = 0; i < totalEvents; i++) {
            builder.append(random.nextInt(totalEvents)).append('|').append('B').append('\n');
        }
        final Vertx vertx = Vertx.vertx();
        final Async async = context.async();
        final DeploymentOptions options = new DeploymentOptions();
        options.setConfig(new JsonObject()
                .put("totalEvents", totalEvents));
        vertx.eventBus().
                consumer(EventSourceVerticle.CLOSE_EVENT_SOURCE_ADDRESS, (Handler<Message<JsonObject>>) message -> {
                    async.complete();
                });
        final String host = InetAddress.getLoopbackAddress().getHostAddress();
        vertx.deployVerticle(MainVerticle.class.getName(), options, asyncResult -> {
            vertx.createNetClient()
                    .connect(EventSourceVerticle.DEFAULT_PORT, host, socketAsyncResult -> {
                        if (socketAsyncResult.succeeded()) {
                            final NetSocket socket = socketAsyncResult.result();
                            socket.write(builder.toString()).end();
                        } else {
                            context.fail(socketAsyncResult.cause());
                        }
                    });
            vertx.createNetClient().connect(ClientsVerticle.DEFAULT_PORT, host, socketAsyncResult -> {
                if (socketAsyncResult.succeeded()) {
                    final NetSocket socket = socketAsyncResult.result();
                    socket.write("1\n\r").end();
                } else {
                    context.fail(socketAsyncResult.cause());
                }
            });
            final AtomicInteger number = new AtomicInteger(0);
            new Event(EventType.BROADCAST, 0, null, null)
                    .toAddress()
                    .ifPresent(address -> {
                        vertx.eventBus().<Event>consumer(address, message -> {
                            final int sequenceNumber = message.body().getSequenceNumber();
                            context.assertTrue(number.get() <= sequenceNumber,
                                    number.get() + " <= " + sequenceNumber + ": false");
                            number.set(sequenceNumber);
                        });
                    });
        });
    }
}
