package net.nuggetmc.tplus.command.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BotCommandRespawnTest {

    @Test
    void acceptsOnlyExplicitBooleanValues() {
        assertEquals(Boolean.TRUE, BotCommand.parseBoolean("TRUE"));
        assertEquals(Boolean.FALSE, BotCommand.parseBoolean("false"));
        assertNull(BotCommand.parseBoolean("on"));
        assertNull(BotCommand.parseBoolean(""));
    }
}
