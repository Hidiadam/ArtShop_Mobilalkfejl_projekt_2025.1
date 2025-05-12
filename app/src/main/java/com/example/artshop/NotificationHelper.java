package com.example.artshop;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.util.Log; // Logoláshoz

import androidx.core.app.ActivityCompat; // Jogosultság ellenőrzéshez
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat; // Jobb a Compat verzió használata

public class NotificationHelper {
    private static final String LOG_TAG = NotificationHelper.class.getName(); // Log tag
    private static final String CHANNEL_ID = "artshop_notification_channel";
    private static final int NOTIFICATION_ID = 1;

    private NotificationManagerCompat mNotifyManagerCompat;
    private Context mContext;

    public NotificationHelper(Context context) {
        this.mContext = context;
        this.mNotifyManagerCompat = NotificationManagerCompat.from(context);
        createChannel();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.d(LOG_TAG, "Notification channel creation skipped (API < 26).");
            return;
        }

        // Ellenőrizzük, hogy a csatorna már létezik-e
        if (mNotifyManagerCompat.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "ArtShop Értesítések", // Felhasználó által látható név
                    NotificationManager.IMPORTANCE_HIGH); // Fontosság beállítása

            channel.enableLights(true);
            channel.setLightColor(Color.CYAN);
            channel.enableVibration(true);
            channel.setDescription("Értesítések az ArtShop alkalmazásból.");
            channel.setShowBadge(true);
            mNotifyManagerCompat.createNotificationChannel(channel);
            Log.i(LOG_TAG, "Notification channel created: " + CHANNEL_ID);
        } else {
            Log.d(LOG_TAG, "Notification channel already exists: " + CHANNEL_ID);
        }
    }

    public void send(String message) {
        // 1. Jogosultság ellenőrzése (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(LOG_TAG, "POST_NOTIFICATIONS permission not granted. Cannot send notification.");
                return;
            }
        }

        // 2. Intent létrehozása, ami megnyílik kattintásra
        Intent intent = new Intent(mContext, ArtListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int flags = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ?
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE :
                PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, NOTIFICATION_ID, intent, flags);

        // 3. Értesítés összeállítása
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, CHANNEL_ID)
                .setContentTitle("ArtShop") // Értesítés címe
                .setContentText(message)
                .setSmallIcon(R.drawable.baseline_palette_24)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // 4. Értesítés megjelenítése
        mNotifyManagerCompat.notify(NOTIFICATION_ID, builder.build());
        Log.i(LOG_TAG, "Notification sent: " + message);
    }

    // Értesítés eltávolítása (ha szükséges)
    public void cancel() {
        mNotifyManagerCompat.cancel(NOTIFICATION_ID);
        Log.i(LOG_TAG, "Notification cancelled: " + NOTIFICATION_ID);
    }
}