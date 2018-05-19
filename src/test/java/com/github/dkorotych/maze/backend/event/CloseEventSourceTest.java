package com.github.dkorotych.maze.backend.event;

import com.github.dkorotych.maze.backend.Addresses;
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

@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(VertxUnitRunnerWithParametersFactory.class)
public class CloseEventSourceTest {

    @Rule
    public Timeout timeout = Timeout.seconds(5);

    @Parameterized.Parameters
    public static Iterable<Integer> data() {
        return Arrays.asList(10, 1, 100, 134);
    }

    private final int totalEvents;

    public CloseEventSourceTest(int totalEvents) {
        this.totalEvents = totalEvents;
    }

    @Test
    public void calculateEvents(TestContext context) {
        final Vertx vertx = Vertx.vertx();
        final Async async = context.async(totalEvents);
        final DeploymentOptions options = new DeploymentOptions();
        options.setConfig(new JsonObject()
                .put("totalEvents", totalEvents));
        vertx.deployVerticle(EventSourceVerticle.class.getName(), options, asyncResult -> {
            final NetClient client = vertx.createNetClient();
            client.connect(EventSourceVerticle.DEFAULT_PORT, InetAddress.getLoopbackAddress().getHostAddress(), socketAsyncResult -> {
                if (socketAsyncResult.succeeded()) {
                    final NetSocket socket = socketAsyncResult.result();
                    final Event event = Event.parse("1|B");
                    for (int i = 0; i <= totalEvents; i++) {
                        socket.write(event.toString()).end();
                    }
//                    vertx.timerStream(10).handler(timerId -> socket.write(event.toString()).end());
                } else {
                    context.fail(socketAsyncResult.cause());
                }
            });
        });
        vertx.eventBus().
                consumer(Addresses.Events.CLOSE, (Handler<Message<JsonObject>>) message -> {
                    context.assertTrue(message.body().getInteger("total").equals(totalEvents));
                    async.complete();
                });
    }
}
