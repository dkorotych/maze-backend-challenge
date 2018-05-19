package com.github.dkorotych.maze.backend;

import com.github.dkorotych.maze.backend.event.Event;

import java.util.Optional;

public interface Addresses {
    interface Events {
        String FOLLOW = "/event-source/follow";
        String UNFOLLOW = "/event-source/unfollow";
        String BROADCAST = "/event-source/broadcast";
        String PRIVATE_MESSAGE = "/event-source/message";
        String STATUS_UPDATE = "/event-source/status";
        String CLOSE = "/event-source/close";

        static Optional<String> createAddress(final Event event) {
            final String address;
            switch (event.getType()) {
                case FOLLOW:
                    address = FOLLOW + '/' + event.getToUser();
                    break;
                case UNFOLLOW:
                    address = UNFOLLOW + '/' + event.getToUser();
                    break;
                case BROADCAST:
                    address = BROADCAST;
                    break;
                case PRIVATE_MESSAGE:
                    address = PRIVATE_MESSAGE + '/' + event.getToUser();
                    break;
                case STATUS_UPDATE:
                    address = STATUS_UPDATE;
                    break;
                default:
                    address = null;
                    break;
            }
            return Optional.ofNullable(address);
        }
    }
}
