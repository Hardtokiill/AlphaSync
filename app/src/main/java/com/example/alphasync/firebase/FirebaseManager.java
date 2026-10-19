package com.example.alphasync.firebase;

import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/** Central access point for Firebase services plus shared helpers (timeouts, friendly errors). */
public final class FirebaseManager {
    private FirebaseManager() { }

    /** Simple success/error callback (always invoked on the main thread). */
    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public static FirebaseAuth auth() { return FirebaseAuth.getInstance(); }
    public static FirebaseFirestore db() { return FirebaseFirestore.getInstance(); }

    /** @return the signed-in user's uid, or null when nobody is signed in. */
    public static String uid() {
        return auth().getCurrentUser() == null ? null : auth().getCurrentUser().getUid();
    }

    /**
     * Wraps a Firebase Task with a timeout. Firestore writes queue forever while the SDK is offline,
     * so without this a "save" could hang the UI. On timeout the caller falls back to local storage.
     */
    public static <T> void call(Task<T> task, long timeoutMs, Callback<T> cb) {
        Handler main = new Handler(Looper.getMainLooper());
        AtomicBoolean done = new AtomicBoolean(false);
        Runnable timeout = () -> {
            if (done.compareAndSet(false, true)) cb.onError(new TimeoutException("Request timed out"));
        };
        main.postDelayed(timeout, timeoutMs);
        task.addOnCompleteListener(t -> {
            if (!done.compareAndSet(false, true)) return;
            main.removeCallbacks(timeout);
            if (t.isSuccessful()) cb.onSuccess(t.getResult());
            else cb.onError(t.getException() != null ? t.getException() : new Exception("Unknown error"));
        });
    }

    /** Converts low-level exceptions into messages a laboratory user can understand. */
    public static String describe(Exception e) {
        if (e instanceof FirebaseAuthInvalidCredentialsException || e instanceof FirebaseAuthInvalidUserException)
            return "Incorrect email or password.";
        if (e instanceof FirebaseTooManyRequestsException)
            return "Too many attempts. Please wait a moment and try again.";
        if (e instanceof FirebaseNetworkException)
            return "No internet connection. Please check your network and try again.";
        if (e instanceof TimeoutException)
            return "The server took too long to respond. Please try again.";
        if (e instanceof FirebaseFirestoreException) {
            switch (((FirebaseFirestoreException) e).getCode()) {
                case PERMISSION_DENIED: return "You do not have permission to perform this action.";
                case UNAVAILABLE: return "The service is temporarily unavailable. Please try again shortly.";
                default: break;
            }
        }
        return "Something went wrong. Please try again.";
    }
}
