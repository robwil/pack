package me.robwilliams.pack;

import org.junit.Test;
import static org.junit.Assert.*;

public class BagSelectionLogicTest {

    // --- resolvePreselectedBagId ---

    @Test
    public void preselect_itemBagTakesPriority() {
        assertEquals(10, BagSelectionLogic.resolvePreselectedBagId(10, 20, 30));
    }

    @Test
    public void preselect_bagHintWhenNoItemBag() {
        assertEquals(20, BagSelectionLogic.resolvePreselectedBagId(0, 20, 30));
    }

    @Test
    public void preselect_lastSelectedWhenNoItemBagOrHint() {
        assertEquals(30, BagSelectionLogic.resolvePreselectedBagId(0, 0, 30));
    }

    @Test
    public void preselect_zeroWhenNothingSet() {
        assertEquals(0, BagSelectionLogic.resolvePreselectedBagId(0, 0, 0));
    }

    @Test
    public void preselect_itemBagOverridesAll() {
        assertEquals(5, BagSelectionLogic.resolvePreselectedBagId(5, 10, 15));
    }

    @Test
    public void preselect_hintOverridesLastSelected() {
        assertEquals(10, BagSelectionLogic.resolvePreselectedBagId(0, 10, 15));
    }

    @Test
    public void preselect_repackUsesItemBag() {
        // Repack scenario: item was packed into bag 7, hint is 3, last selected is 5
        assertEquals(7, BagSelectionLogic.resolvePreselectedBagId(7, 3, 5));
    }

    @Test
    public void preselect_packWithOnlyHint() {
        // Pack scenario: no current bag, hint is set
        assertEquals(3, BagSelectionLogic.resolvePreselectedBagId(0, 3, 0));
    }

    // --- shouldClearBagOnUncheck ---

    @Test
    public void clearBag_onPackTab() {
        int STATUS_PACKED = 2;
        assertTrue(BagSelectionLogic.shouldClearBagOnUncheck(STATUS_PACKED, STATUS_PACKED));
    }

    @Test
    public void clearBag_notOnRepackTab() {
        int STATUS_PACKED = 2;
        int STATUS_REPACKED = 3;
        assertFalse(BagSelectionLogic.shouldClearBagOnUncheck(STATUS_REPACKED, STATUS_PACKED));
    }

    @Test
    public void clearBag_notOnShouldPackTab() {
        int STATUS_PACKED = 2;
        int STATUS_SHOULD_PACK = 1;
        assertFalse(BagSelectionLogic.shouldClearBagOnUncheck(STATUS_SHOULD_PACK, STATUS_PACKED));
    }

    // --- resolveDefaultPage ---

    @Test
    public void defaultPage_noItems() {
        assertEquals(0, BagSelectionLogic.resolveDefaultPage(0, false, true));
    }

    @Test
    public void defaultPage_shouldPackOnly() {
        assertEquals(1, BagSelectionLogic.resolveDefaultPage(1, true, false));
    }

    @Test
    public void defaultPage_somePackedNotAll() {
        // Max status is PACKED(2) but not all items are packed -> stay on Pack tab
        assertEquals(1, BagSelectionLogic.resolveDefaultPage(2, true, false));
    }

    @Test
    public void defaultPage_allPacked() {
        // All items are at least packed -> Repack tab
        assertEquals(2, BagSelectionLogic.resolveDefaultPage(2, true, true));
    }

    @Test
    public void defaultPage_someRepackedNotAllPacked() {
        // Max status is REPACKED(3) but not all items are packed -> stay on Pack tab
        assertEquals(1, BagSelectionLogic.resolveDefaultPage(3, true, false));
    }

    @Test
    public void defaultPage_allPackedSomeRepacked() {
        // All items are at least packed, some repacked -> Repack tab
        assertEquals(2, BagSelectionLogic.resolveDefaultPage(3, true, true));
    }

    @Test
    public void defaultPage_maxStatusCappedAtTwo() {
        // Status 3 should still map to page 2 max
        assertEquals(2, BagSelectionLogic.resolveDefaultPage(3, true, true));
    }
}
