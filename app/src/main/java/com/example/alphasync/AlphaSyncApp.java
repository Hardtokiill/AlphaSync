package com.example.alphasync;

import android.app.Application;

import com.example.alphasync.firebase.NotificationHelper;
import com.example.alphasync.offline.SyncManager;
import com.example.alphasync.utils.NetworkUtils;

/** Application entry point: sets up notifications, connectivity monitoring and the sync engine. */
public class AlphaSyncApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannel(this);
        SyncManager.init(this);
        // Whenever connectivity returns, push any Pending Sync records to Firestore.
        NetworkUtils.register(this, () -> SyncManager.enqueue(this));
        SyncManager.enqueue(this); // also try once at start-up

        int themeMode = getSharedPreferences("alphasync_prefs", MODE_PRIVATE).getInt("theme_mode", 0);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                themeMode == 1 ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                        : themeMode == 2 ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                        : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }
}
