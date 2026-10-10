package net.nuggetmc.tplus.command.commands;

import net.nuggetmc.tplus.bot.SpawnCheck;
import net.nuggetmc.tplus.command.annotation.Require;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotCommandRespawnTest {

    @Test
    void acceptsOnlyExplicitBooleanValues() {
        assertEquals(Boolean.TRUE, BotCommand.parseBoolean("TRUE"));
        assertEquals(Boolean.FALSE, BotCommand.parseBoolean("false"));
        assertNull(BotCommand.parseBoolean("on"));
        assertNull(BotCommand.parseBoolean(""));
    }

    @Test
    void setSpawnUsesAdminPermissionAndCanonicalName() throws NoSuchMethodException {
        Require require = BotCommand.class
                .getMethod("setSpawn", CommandSender.class, String.class)
                .getAnnotation(Require.class);

        assertEquals("terminatorplus.admin", require.value());
        assertEquals("set-spawn", BotCommand.canonicalSettingsAction("setspawn"));
    }

    @Test
    void setSpawnRefusesVoidAndBlockedSpotsButAcceptsMidAir() {
        assertNull(BotCommand.setSpawnRefusal(SpawnCheck.Problem.NONE));
        for (SpawnCheck.Problem problem : SpawnCheck.Problem.values()) {
            if (problem != SpawnCheck.Problem.NONE) assertNotNull(BotCommand.setSpawnRefusal(problem));
        }
        assertTrue(BotCommand.setSpawnRefusal(SpawnCheck.Problem.NO_GROUND).contains("fall out of the world"));
        assertTrue(new SpawnCheck(SpawnCheck.Problem.NONE, 12.0).accepted());
    }
}
