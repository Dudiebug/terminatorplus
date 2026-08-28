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
    void botPagesClampAtBothBoundariesIncludingEmptyLists() {
        assertEquals(1, BotManagementUI.pageCount(0));
        assertEquals(1, BotManagementUI.pageCount(1));
        assertEquals(2, BotManagementUI.pageCount(BotManagementUI.BOT_PAGE_SIZE + 1));
        assertEquals(0, BotManagementUI.clampPageIndex(-1, 1));
        assertEquals(0, BotManagementUI.clampPageIndex(99, 1));
        assertEquals(1, BotManagementUI.clampPageIndex(1, 2));
        assertEquals(1, BotManagementUI.clampPageIndex(99, 2));

        BotManagementUI.PageState state = new BotManagementUI.PageState();
        state.navigate(BotManagementUI.Page.BOTS);
        state.setPageIndex(99, 2);
        assertEquals(1, state.pageIndex());
        state.setPageIndex(-1, 2);
        assertEquals(0, state.pageIndex());
    }

    @Test
    void commandMappingUsesExistingPlayerCommands() {
        assertEquals("bot spawn single", BotManagementUI.commandFor(BotManagementUI.UiAction.BOT_CREATE));
        assertEquals("bot spawn multiple 3 bot", BotManagementUI.commandFor(
                BotManagementUI.UiAction.BOT_MULTI, "3 bot"));
        assertEquals("ai brain reset", BotManagementUI.commandFor(BotManagementUI.UiAction.AI_BRAIN_RESET));
        assertEquals("bot debug movement", BotManagementUI.commandFor(
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
    void adminOnlyActionsAreHiddenWithoutAdminPermission() {
        assertTrue(BotManagementUI.visibleForPermission(BotManagementUI.UiAction.BOT_RESET, true));
        assertFalse(BotManagementUI.visibleForPermission(BotManagementUI.UiAction.BOT_RESET, false));
        assertTrue(BotManagementUI.visibleForPermission(BotManagementUI.UiAction.BOT_GATHER, false));
    }

    @Test
    void dispatchFeedbackDistinguishesAcceptedFromRejectedCommands() {
        assertEquals("Dispatched /bot move gather",
                BotManagementUI.dispatchStatus(true, "bot move gather"));
        assertEquals("Rejected /bot move gather",
                BotManagementUI.dispatchStatus(false, "bot move gather"));
        assertTrue(BotManagementUI.dispatchStatus(true, "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz")
                .endsWith("..."));
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
        assertEquals("bot settings target-region 0 0 0 10 10 10", BotManagementUI.commandFor(
                BotManagementUI.UiAction.BOT_SETTINGS_REGION_INPUT, "0 0 0 10 10 10"));
    }

    @Test
    void promptBoundaryRejectsControlCharactersButLeavesParsingToCommands() {
        assertTrue(BotManagementUI.isSafePromptInput("12 bot-name 1 2 3"));
        assertFalse(BotManagementUI.isSafePromptInput("bot\nreset"));
        assertFalse(BotManagementUI.isSafePromptInput("x\u0000y"));
    }

    @Test
    void promptsHaveARealTimeoutAndCancellationIsObservable() {
        assertEquals(1200L, BotManagementUI.promptTimeoutTicks());
        BotManagementUI.PendingPrompt pending = new BotManagementUI.PendingPrompt(
                BotManagementUI.UiAction.BOT_CREATE, "name");
        BotManagementUI.PendingPrompt replacement = new BotManagementUI.PendingPrompt(
                BotManagementUI.UiAction.BOT_MULTI, "amount name");
        assertEquals(BotManagementUI.UiAction.BOT_CREATE, pending.action());
        assertEquals("name", pending.hint());
        assertTrue(BotManagementUI.isCurrentPrompt(pending, pending));
        assertFalse(BotManagementUI.isCurrentPrompt(replacement, pending));
        assertFalse(pending.cancelled());
        pending.cancel();
        assertTrue(pending.cancelled());
        assertFalse(BotManagementUI.isCurrentPrompt(pending, pending));
    }
}
