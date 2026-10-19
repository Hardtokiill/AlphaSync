package com.example.alphasync.repository;

import com.example.alphasync.firebase.FirebaseManager;
import com.example.alphasync.firebase.FirebaseManager.Callback;
import com.example.alphasync.models.Laboratory;
import com.example.alphasync.models.User;
import com.google.firebase.firestore.DocumentSnapshot;

/** Loads the signed-in user's profile and laboratory from Firestore (served from cache when offline). */
public class UserRepository {

    public void loadUser(String uid, Callback<User> cb) {
        FirebaseManager.call(FirebaseManager.db().collection("users").document(uid).get(), 15000,
                new Callback<DocumentSnapshot>() {
                    @Override public void onSuccess(DocumentSnapshot d) {
                        User u = d.exists() ? d.toObject(User.class) : null;
                        if (u == null) cb.onError(new IllegalStateException("NO_PROFILE"));
                        else cb.onSuccess(u);
                    }
                    @Override public void onError(Exception e) { cb.onError(e); }
                });
    }

    public void loadLaboratory(String labId, Callback<Laboratory> cb) {
        FirebaseManager.call(FirebaseManager.db().collection("laboratories").document(labId).get(), 15000,
                new Callback<DocumentSnapshot>() {
                    @Override public void onSuccess(DocumentSnapshot d) {
                        Laboratory l = d.exists() ? d.toObject(Laboratory.class) : null;
                        if (l == null) cb.onError(new IllegalStateException("NO_LAB"));
                        else cb.onSuccess(l);
                    }
                    @Override public void onError(Exception e) { cb.onError(e); }
                });
    }

    /** Stores the device's FCM token so the backend can target it. Security rules only allow this one field. */
    public void saveFcmToken(String uid, String token) {
        FirebaseManager.db().collection("users").document(uid).update("fcmToken", token);
    }
}
