package com.example.alphasync.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/** Detects connectivity with the Android ConnectivityManager APIs. */
public final class NetworkUtils {
    private static final MutableLiveData<Boolean> ONLINE = new MutableLiveData<>(false);

    private NetworkUtils() { }

    /** Observable online/offline state (true = device has validated internet access). */
    public static LiveData<Boolean> online() { return ONLINE; }

    /** One-off check used right before saving data. */
    public static boolean isOnline(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network n = cm.getActiveNetwork();
        if (n == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(n);
        return caps != null
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    /** Call once from Application.onCreate. {@code onReconnected} runs when connectivity returns. */
    public static void register(Context appCtx, Runnable onReconnected) {
        ConnectivityManager cm = (ConnectivityManager) appCtx.getSystemService(Context.CONNECTIVITY_SERVICE);
        ONLINE.setValue(isOnline(appCtx));
        if (cm == null) return;
        cm.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities caps) {
                boolean validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                Boolean was = ONLINE.getValue();
                ONLINE.postValue(validated);
                if (validated && (was == null || !was)) onReconnected.run();
            }

            @Override
            public void onLost(@NonNull Network network) { ONLINE.postValue(false); }
        });
    }
}
