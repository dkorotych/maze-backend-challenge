package com.github.dkorotych.maze.backend.event;

import com.github.dkorotych.maze.backend.AbstractChangePortTest;
import org.junit.runners.Parameterized;

import java.net.UnknownHostException;
import java.util.Arrays;

public class EventSourceVerticleChangePortTest extends AbstractChangePortTest {

    @Parameterized.Parameters
    public static Iterable<Integer> data() {
        return Arrays.asList(9090, 9091, 9092, 12345);
    }

    public EventSourceVerticleChangePortTest(int port) throws UnknownHostException {
        super(port, EventSourceVerticle.class, "eventListenerPort");
    }
}