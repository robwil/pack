package me.robwilliams.pack;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {

    private static final String PREFS_NAME = "pack_preferences";
    private static final String KEY_THEME_MODE = "theme_mode";

    public static final int MODE_SYSTEM = 0;
    public static final int MODE_LIGHT = 1;
    public static final int MODE_DARK = 2;

    public static void applyTheme(Context context) {
        int mode = getSavedThemeMode(context);
        applyNightMode(mode);
    }

    public static int getSavedThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, MODE_SYSTEM);
    }

    public static void saveThemeMode(Context context, int mode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_THEME_MODE, mode)
                .apply();
    }

    public static int cycleTheme(Context context) {
        int current = getSavedThemeMode(context);
        int next = (current + 1) % 3;
        saveThemeMode(context, next);
        applyNightMode(next);
        return next;
    }

    private static void applyNightMode(int mode) {
        switch (mode) {
            case MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case MODE_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public static int getLabelResId(int mode) {
        switch (mode) {
            case MODE_LIGHT:
                return R.string.theme_light;
            case MODE_DARK:
                return R.string.theme_dark;
            case MODE_SYSTEM:
            default:
                return R.string.theme_auto;
        }
    }
}
