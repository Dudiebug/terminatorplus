package net.nuggetmc.tplus.bot.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BotInspectionPaginationTest {

    @Test
    void paginatesFortyFiveBotsPerPage() {
        assertEquals(1, BotInspectionListGUI.pageCount(0));
        assertEquals(1, BotInspectionListGUI.pageCount(45));
        assertEquals(2, BotInspectionListGUI.pageCount(46));
        assertEquals(45, BotInspectionListGUI.startIndex(1));
        assertEquals(1, BotInspectionListGUI.clampPage(99, 46));
        assertEquals(0, BotInspectionListGUI.clampPage(-3, 46));
    }
}
