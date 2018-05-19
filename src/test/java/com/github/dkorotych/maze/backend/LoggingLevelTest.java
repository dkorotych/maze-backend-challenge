package com.github.dkorotych.maze.backend;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.logging.Level;

@RunWith(JUnitParamsRunner.class)
public class LoggingLevelTest {

    @Test
    @Parameters
    public void fromString(@Nullable String level, Level expected) {
        Assertions.assertThat(LoggingLevel.fromString(level).getLevel()).
                isEqualTo(expected);
    }

    Object[][] parametersForFromString() {
        return new Object[][]{
                {null, Level.INFO},
                {"null", Level.INFO},
                {"", Level.INFO},
                {"\t\t    \t\n", Level.INFO},
                {"DEBUG", Level.FINE},
                {"deBUg", Level.FINE},
                {" debug      ", Level.FINE},
                {"fine", Level.INFO}
        };
    }
}