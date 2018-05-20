package com.github.dkorotych.maze.backend.event;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.assertj.core.api.Assertions;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class EventTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    @Parameters
    public void parse(@Nullable String event, Event expected) {
        Assertions.assertThat(Event.parse(event)).isEqualTo(expected);
    }

    Object[][] parametersForParse() {
        return new Object[][]{
                {"666|F|60|50", new Event(EventType.FOLLOW, 666, 60, 50)},
                {"1|U|12|9", new Event(EventType.UNFOLLOW, 1, 12, 9)},
                {"542532|B", new Event(EventType.BROADCAST, 542532, null, null)},
                {"43|P|32|56", new Event(EventType.PRIVATE_MESSAGE, 43, 32, 56)},
                {"634|S|32", new Event(EventType.STATUS_UPDATE, 634, 32, null)}
        };
    }

    @Test
    @Parameters
    public void parseError(@Nullable String event) {
        expectedException.expect(ParseEventException.class);
        expectedException.expectMessage(CoreMatchers.equalTo(String.format("Can not parse %s as event", event)));
        Event.parse(event);
    }

    Object[][] parametersForParseError() {
        return new Object[][]{
                {null},
                {""},
                {"\t\t\n\n\n   "},
                {"634|s|32"},
                {"634||32"},
                {"| | | | | | "},
                {"6-4|S|32"},
        };
    }

    @Test
    @Parameters
    public void testToString(Event event, String expected) {
        Assertions.assertThat(event.toString()).isEqualTo(expected);
    }

    Object[][] parametersForTestToString() {
        return new Object[][]{
                {new Event(null, 0, null, null), ""},
                {new Event(null, 340, 4, 5), ""},
                {new Event(EventType.FOLLOW, 666, 60, 50), "666|F|60|50"},
                {new Event(EventType.UNFOLLOW, 1, 12, 9), "1|U|12|9"},
                {new Event(EventType.BROADCAST, 542532, null, null), "542532|B"},
                {new Event(EventType.PRIVATE_MESSAGE, 43, 32, 56), "43|P|32|56"},
                {new Event(EventType.STATUS_UPDATE, 634, 32, null), "634|S|32"}
        };
    }

    @Test
    @Parameters
    public void toAddress(Event event, String expected) {
        Assertions.assertThat(event.toAddress()).isPresent().hasValue(expected);
    }

    Object[][] parametersForToAddress() {
        return new Object[][]{
                {new Event(EventType.FOLLOW, 666, 60, 50), "/event-source/follow/50"},
                {new Event(EventType.UNFOLLOW, 1, 12, 9), "/event-source/unfollow/12"},
                {new Event(EventType.BROADCAST, 542532, null, null), "/event-source/broadcast"},
                {new Event(EventType.PRIVATE_MESSAGE, 43, 32, 56), "/event-source/message/56"},
                {new Event(EventType.STATUS_UPDATE, 634, 32, null), "/event-source/status/32"}
        };
    }

    @Test
    @Parameters
    public void toAddressEmpty(Event event) {
        Assertions.assertThat(event.toAddress()).isNotPresent();
    }

    Object[][] parametersForToAddressEmpty() {
        return new Object[][]{
                {new Event(null, 0, null, null)},
                {new Event(null, 340, 4, 5)}
        };
    }

    @Test
    public void compareTo() {
    }
}