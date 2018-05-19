package com.github.dkorotych.maze.backend.client;

import com.github.dkorotych.maze.backend.AbstractChangePortTest;
import org.junit.runners.Parameterized;

import java.net.UnknownHostException;
import java.util.Arrays;

public class ClientsVerticleChangePortTest extends AbstractChangePortTest {

    @Parameterized.Parameters
    public static Iterable<Integer> data() {
        return Arrays.asList(9099, 9010, 9020, 9030);
    }

    public ClientsVerticleChangePortTest(int port) throws UnknownHostException {
        super(port, ClientsVerticle.class, "clientListenerPort");
    }
}