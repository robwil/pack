package me.robwilliams.pack;

public class BagSelectionLogic {

    public static int resolvePreselectedBagId(int itemBagId, int bagHintId, int lastSelectedBagId) {
        if (itemBagId > 0) return itemBagId;
        if (bagHintId > 0) return bagHintId;
        if (lastSelectedBagId > 0) return lastSelectedBagId;
        return 0;
    }

    public static boolean shouldClearBagOnUncheck(int currentPageStatus, int statusPacked) {
        return currentPageStatus == statusPacked;
    }

    /**
     * @param maximumStatus highest status of any trip item
     * @param hasActiveItems whether any items have status >= SHOULD_PACK
     * @param allPacked whether all active items have status >= PACKED
     * @return the page index (0=ShouldPack, 1=Pack, 2=Repack) to default to
     */
    public static int resolveDefaultPage(int maximumStatus, boolean hasActiveItems, boolean allPacked) {
        int page = Math.min(maximumStatus, 2);
        if (page == 2 && !(hasActiveItems && allPacked)) {
            page = 1;
        }
        return page;
    }
}
