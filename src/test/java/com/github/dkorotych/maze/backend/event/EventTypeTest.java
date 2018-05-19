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
public class EventTypeTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    @Parameters
    public void fromCode(char code, EventType expected) {
        Assertions.assertThat(EventType.fromCode(code)).
                isEqualTo(expected);
    }

    Object[][] parametersForFromCode() {
        return new Object[][]{
                {'F', EventType.FOLLOW},
                {'U', EventType.UNFOLLOW},
                {'B', EventType.BROADCAST},
                {'P', EventType.PRIVATE_MESSAGE},
                {'S', EventType.STATUS_UPDATE}
        };
    }

    @Test
    @Parameters({
            "null",
            "a",
            "f",
            "d",
            "-",
            "b",
            "s"
    })
    public void fromCodeError(@Nullable Character code) {
        expectedException.expect(ParseEventException.class);
        expectedException.expectMessage(CoreMatchers.equalTo(String.format("Can not parse %c as event type", code)));
        EventType.fromCode(code);
    }

    @Test
    @Parameters
    public void fromCodeAsString(String code, EventType expected) {
        Assertions.assertThat(EventType.fromCode(code)).
                isEqualTo(expected);
    }

    Object[][] parametersForFromCodeAsString() {
        return new Object[][]{
                {"F", EventType.FOLLOW},
                {"U", EventType.UNFOLLOW},
                {"B", EventType.BROADCAST},
                {"P", EventType.PRIVATE_MESSAGE},
                {"S", EventType.STATUS_UPDATE}
        };
    }

    @Test
    @Parameters({
            "null",
            "a",
            "f",
            "d",
            "-",
            "b",
            "s"
    })
    public void fromCodeAsStringError(@Nullable String code) {
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage(CoreMatchers.equalTo(String.format("Can not parse %s as event type", code)));
        EventType.fromCode(code);
    }

    @Test
    public void getCode() {
    }
}