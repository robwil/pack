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
}
