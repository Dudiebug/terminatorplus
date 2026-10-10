package net.nuggetmc.tplus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminatorPlusVersionTest {

    @Test
    void acceptsTheRequiredVersionAndItsHotfixes() {
        assertTrue(TerminatorPlus.isSupportedVersion(TerminatorPlus.REQUIRED_VERSION));
        assertTrue(TerminatorPlus.isSupportedVersion(TerminatorPlus.REQUIRED_VERSION + ".1"));
    }

    @Test
    void rejectsOtherVersions() {
        assertFalse(TerminatorPlus.isSupportedVersion(null));
        assertFalse(TerminatorPlus.isSupportedVersion("26.2"));
        assertFalse(TerminatorPlus.isSupportedVersion(TerminatorPlus.REQUIRED_VERSION + "0"));
    }
}
