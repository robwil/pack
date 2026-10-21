package me.robwilliams.pack;

import java.util.Locale;

import me.robwilliams.pack.fragment.AbstractPackingFragment;

public class SearchLogic {

    /**
     * Fuzzy partial match: ignores case, spaces and punctuation, and requires every
     * query word to appear somewhere in the name. So "shirt", "t shirt" and "tshirt"
     * all match "T-Shirts".
     */
    public static boolean matches(String query, String name) {
        if (name == null) return false;
        String normalizedName = normalize(name);
        String[] tokens = query == null ? new String[0]
                : query.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+");
        for (String token : tokens) {
            if (!token.isEmpty() && !normalizedName.contains(token)) return false;
        }
        return true;
    }

    private static String normalize(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private static final int LAST_PACKING_PAGE = 2;

    /**
     * Picks the tab to show a search result on: the current tab if the item appears there,
     * otherwise the nearest previous tab (Repack → Pack → Should Pack). Tab N shows items
     * with status >= N, so Should Pack always contains it. From a non-packing tab (Notes),
     * the search starts at Repack.
     */
    public static int resolvePageForStatus(int status, int currentPage) {
        int start = Math.min(currentPage, LAST_PACKING_PAGE);
        for (int page = start; page > 0; page--) {
            if (status >= page) return page;
        }
        return 0;
    }
}
