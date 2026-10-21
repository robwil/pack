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

    /**
     * Picks the latest tab on which the item still appears unchecked (Should Pack → Pack →
     * Repack). Fully repacked items go to the Repack tab.
     */
    public static int resolvePageForStatus(int status) {
        if (status >= AbstractPackingFragment.STATUS_PACKED) return 2;
        if (status >= AbstractPackingFragment.STATUS_SHOULD_PACK) return 1;
        return 0;
    }
}
