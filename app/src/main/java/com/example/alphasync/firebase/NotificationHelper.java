package com.example.alphasync.firebase;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import android.content.SharedPreferences;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.alphasync.R;
import com.example.alphasync.activities.MainActivity;

/** Creates the notification channel and displays alert notifications. */
public final class NotificationHelper {
    public static final String CHANNEL_ID = "alphasync_alerts";
    public static final String EXTRA_DESTINATION = "destination";

    private NotificationHelper() { }

    public static void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT < 26) return; // channels don't exist before Android 8
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "AlphaSync alerts",
                NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Low-stock, disease surveillance and system alerts");
        ctx.getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }

    /** @param destination which screen to open when tapped: home | inventory | disease | alerts | profile */
    public static void show(Context ctx, String title, String body, String destination) {
        SharedPreferences p = ctx.getSharedPreferences("alphasync_prefs", Context.MODE_PRIVATE);
        if (!p.getBoolean("notifications_enabled", true)) return;
        if ("inventory".equals(destination) && !p.getBoolean("notify_stock", true)) return;
        if ("disease".equals(destination) && !p.getBoolean("notify_disease", true)) return;
        // On Android 13+ the user must have granted POST_NOTIFICATIONS; otherwise fail silently.
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;

        Intent i = new Intent(ctx, MainActivity.class)
                .putExtra(EXTRA_DESTINATION, destination)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, (int) System.currentTimeMillis(), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_nav_alerts)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);
        try {
            NotificationManagerCompat.from(ctx).notify((int) System.currentTimeMillis(), b.build());
        } catch (SecurityException ignored) {
            // Notification failure must never crash the app.
        }
    }
}
