package net.nuggetmc.tplus.bot.gui;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotManagementUITest {

    @Test
    void parentOpenContractStartsAtMainPage() {
        assertEquals(BotManagementUI.Page.MAIN, BotManagementUI.initialPage());
        assertTrue(java.util.Arrays.stream(BotManagementUI.class.getMethods())
                .anyMatch(method -> method.getName().equals("open")
                        && method.getParameterCount() == 1));
    }

    @Test
    void navigationHasBackAndForwardStateWithoutBukkit() {
        BotManagementUI.PageState state = new BotManagementUI.PageState();

        state.navigate(BotManagementUI.Page.BOTS);
        state.navigate(BotManagementUI.Page.BOT_DETAIL);
        assertEquals(BotManagementUI.Page.BOT_DETAIL, state.page());

        state.back();
        assertEquals(BotManagementUI.Page.BOTS, state.page());
        state.back();
        assertEquals(BotManagementUI.Page.MAIN, state.page());
    }

    @Test
    void commandMappingUsesExistingPlayerCommands() {
        assertEquals("bot create", BotManagementUI.commandFor(BotManagementUI.UiAction.BOT_CREATE));
        assertEquals("bot multi 3 bot", BotManagementUI.commandFor(
                BotManagementUI.UiAction.BOT_MULTI, "3 bot"));
        assertEquals("ai brain reset", BotManagementUI.commandFor(BotManagementUI.UiAction.AI_BRAIN_RESET));
        assertEquals("botenvironment movementV2Status", BotManagementUI.commandFor(
                BotManagementUI.UiAction.ENV_MOVEMENT_V2_STATUS));
        assertEquals("terminatorplus debuginfo", BotManagementUI.commandFor(
                BotManagementUI.UiAction.MAIN_DEBUG_INFO));
    }

    @Test
    void destructiveActionsAreConfirmationGated() {
        assertTrue(BotManagementUI.requiresConfirmation(BotManagementUI.UiAction.BOT_RESET));
        assertTrue(BotManagementUI.requiresConfirmation(BotManagementUI.UiAction.ENV_REMOVE_SOLID));
        assertTrue(BotManagementUI.requiresConfirmation(BotManagementUI.UiAction.ENV_REMOVE_CUSTOM_MOB));
        assertTrue(BotManagementUI.requiresConfirmation(BotManagementUI.UiAction.ENV_CLEAR_SOLIDS));
        assertTrue(BotManagementUI.requiresConfirmation(BotManagementUI.UiAction.BOT_PRESET_DELETE));
        assertFalse(BotManagementUI.requiresConfirmation(BotManagementUI.UiAction.BOT_GATHER));
        assertTrue(BotManagementUI.UiAction.BOT_RESET.requiresAdmin());
    }

    @Test
    void refreshIsExactlyFiveTicksAndOnlyChangesDifferentItems() {
        assertEquals(5L, BotManagementUI.refreshIntervalTicks());
        assertFalse(BotManagementUI.changedOnly("same", "same"));
        assertTrue(BotManagementUI.changedOnly("old", "new"));
        assertFalse(BotManagementUI.shouldUpdate(null, null));
    }

    @Test
    void cleanupStateStopsRefreshWhenLastSessionClosesOrShutdownRuns() {
        BotManagementUI.LifecycleState state = new BotManagementUI.LifecycleState();
        state.sessionOpened();
        state.sessionOpened();
        assertEquals(2, state.sessionCount());
        assertTrue(state.shouldRefresh());

        state.sessionClosed();
        assertEquals(1, state.sessionCount());
        assertTrue(state.shouldRefresh());
        state.sessionClosed();
        assertEquals(0, state.sessionCount());
        assertFalse(state.shouldRefresh());

        state.sessionOpened();
        state.shutdown();
        state.shutdown();
        assertEquals(0, state.sessionCount());
        assertFalse(state.shouldRefresh());
    }

    @Test
    void staleSelectedUuidIsInvalidated() {
        UUID selected = UUID.randomUUID();
        assertTrue(BotManagementUI.detailStillValid(selected, Set.of(selected)));
        assertFalse(BotManagementUI.detailStillValid(selected, Set.of(UUID.randomUUID())));
        assertFalse(BotManagementUI.detailStillValid(null, Set.of(selected)));
    }

    @Test
    void duplicateNamesCannotUseSelectedBotActions() {
        assertTrue(BotManagementUI.isUniqueBotName("bot", List.of("bot", "other")));
        assertFalse(BotManagementUI.isUniqueBotName("bot", List.of("bot", "BOT")));
        assertEquals("bot settings region 0 0 0 10 10 10", BotManagementUI.commandFor(
                BotManagementUI.UiAction.BOT_SETTINGS_REGION_INPUT, "0 0 0 10 10 10"));
    }

    @Test
    void promptBoundaryRejectsControlCharactersButLeavesParsingToCommands() {
        assertTrue(BotManagementUI.isSafePromptInput("12 bot-name 1 2 3"));
        assertFalse(BotManagementUI.isSafePromptInput("bot\nreset"));
        assertFalse(BotManagementUI.isSafePromptInput("x\u0000y"));
    }
}
