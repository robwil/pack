package me.robwilliams.pack;

import android.app.Application;

public class PackApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeHelper.applyTheme(this);
    }
}
