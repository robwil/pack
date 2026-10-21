package me.robwilliams.pack;

import org.junit.Test;
import static org.junit.Assert.*;

public class SearchLogicTest {

    @Test
    public void matches_partialWord() {
        assertTrue(SearchLogic.matches("shirt", "T-Shirts"));
    }

    @Test
    public void matches_spaceInsteadOfHyphen() {
        assertTrue(SearchLogic.matches("t shirt", "T-Shirts"));
    }

    @Test
    public void matches_noSeparator() {
        assertTrue(SearchLogic.matches("tshirt", "T-Shirts"));
    }

    @Test
    public void matches_wordsOutOfOrder() {
        assertTrue(SearchLogic.matches("charger phone", "Phone Charger"));
    }

    @Test
    public void matches_caseInsensitive() {
        assertTrue(SearchLogic.matches("SOCKS", "socks"));
    }

    @Test
    public void noMatch_missingWord() {
        assertFalse(SearchLogic.matches("t pants", "T-Shirts"));
    }

    @Test
    public void noMatch_nullName() {
        assertFalse(SearchLogic.matches("a", null));
    }

    @Test
    public void page_notSelectedGoesToShouldPack() {
        assertEquals(0, SearchLogic.resolvePageForStatus(0));
    }

    @Test
    public void page_shouldPackGoesToPack() {
        assertEquals(1, SearchLogic.resolvePageForStatus(1));
    }

    @Test
    public void page_packedGoesToRepack() {
        assertEquals(2, SearchLogic.resolvePageForStatus(2));
    }

    @Test
    public void page_repackedStaysOnRepack() {
        assertEquals(2, SearchLogic.resolvePageForStatus(3));
    }
}
