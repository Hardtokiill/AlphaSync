package com.example.alphasync.firebase;

import androidx.annotation.NonNull;

import com.example.alphasync.repository.UserRepository;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Receives Firebase Cloud Messaging pushes.
 * Expected payload (data and/or notification):
 *   title, body, destination (home | inventory | disease | alerts | profile)
 */
public class AlphaSyncMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Map<String, String> data = message.getData();
        String title = "AlphaSync";
        String body = "";
        if (message.getNotification() != null) {
            if (message.getNotification().getTitle() != null) title = message.getNotification().getTitle();
            if (message.getNotification().getBody() != null) body = message.getNotification().getBody();
        }
        if (data.get("title") != null) title = data.get("title");
        if (data.get("body") != null) body = data.get("body");
        String dest = data.get("destination") != null ? data.get("destination") : "alerts";
        NotificationHelper.show(this, title, body, dest);
    }

    /** The token changes occasionally; keep it on the user's profile so the backend can target this device. */
    @Override
    public void onNewToken(@NonNull String token) {
        String uid = FirebaseManager.uid();
        if (uid != null) new UserRepository().saveFcmToken(uid, token);
    }
}
