package com.example.alphasync.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.alphasync.firebase.FirebaseManager;
import com.example.alphasync.models.Laboratory;
import com.example.alphasync.models.User;
import com.example.alphasync.offline.OfflineRepository;
import com.example.alphasync.repository.UserRepository;

/** Holds the signed-in user, their laboratory and the pending-record count (shared by all screens). */
public class SessionViewModel extends AndroidViewModel {
    public final MutableLiveData<User> user = new MutableLiveData<>();
    public final MutableLiveData<Laboratory> lab = new MutableLiveData<>();
    /** Non-null when loading the profile failed; value is "NO_PROFILE", "NO_LAB" or a friendly message. */
    public final MutableLiveData<String> loadError = new MutableLiveData<>();
    public final MediatorLiveData<Integer> pendingCount = new MediatorLiveData<>();

    private final UserRepository users = new UserRepository();
    private final OfflineRepository offline;
    private LiveData<Integer> pendingSource;

    public SessionViewModel(@NonNull Application app) {
        super(app);
        offline = new OfflineRepository(app);
        pendingCount.setValue(0);
    }

    /** Loads users/{uid} and then laboratories/{laboratoryId}. */
    public void load() {
        String uid = FirebaseManager.uid();
        if (uid == null) { loadError.setValue("NOT_SIGNED_IN"); return; }
        loadError.setValue(null);
        users.loadUser(uid, new FirebaseManager.Callback<User>() {
            @Override public void onSuccess(User u) {
                user.setValue(u);
                if (u.laboratoryId == null) { loadError.setValue("NO_LAB"); return; }
                bindPending(u.laboratoryId);
                users.loadLaboratory(u.laboratoryId, new FirebaseManager.Callback<Laboratory>() {
                    @Override public void onSuccess(Laboratory l) { lab.setValue(l); }
                    @Override public void onError(Exception e) { loadError.setValue(messageFor(e)); }
                });
            }
            @Override public void onError(Exception e) { loadError.setValue(messageFor(e)); }
        });
    }

    private static String messageFor(Exception e) {
        String m = e.getMessage();
        if ("NO_PROFILE".equals(m) || "NO_LAB".equals(m)) return m;
        return FirebaseManager.describe(e);
    }

    private void bindPending(String labId) {
        if (pendingSource != null) pendingCount.removeSource(pendingSource);
        pendingSource = offline.observePendingCount(labId);
        pendingCount.addSource(pendingSource, pendingCount::setValue);
    }
}
