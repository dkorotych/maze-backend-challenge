package com.github.dkorotych.maze.backend.client;

import com.github.dkorotych.maze.backend.event.Event;
import io.vertx.core.eventbus.MessageConsumer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class FollowersKeeper {
    private final Map<Integer, Data> userToFollowerMapper;

    FollowersKeeper() {
        userToFollowerMapper = new ConcurrentHashMap<>();
    }

    protected Map<Integer, MessageConsumer<Event>> getFollowers(final int userId) {
        return getData(userId).followers;
    }

    protected String getWriteHandlerIdentifier(final int userId) {
        return getData(userId).writeHandlerId;
    }

    protected void setWriteHandlerIdentifier(final int userId, final String writeHandlerID) {
        getData(userId).writeHandlerId = writeHandlerID;
    }

    private Data getData(final int userId) {
        return userToFollowerMapper.computeIfAbsent(userId, key -> new Data());
    }

    private static final class Data {
        private final Map<Integer, MessageConsumer<Event>> followers = new ConcurrentHashMap<>();
        private String writeHandlerId;
    }
}
