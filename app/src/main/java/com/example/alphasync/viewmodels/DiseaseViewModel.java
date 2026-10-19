package com.example.alphasync.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.alphasync.firebase.FirebaseManager;
import com.example.alphasync.models.DiseaseRecord;
import com.example.alphasync.offline.OfflineRepository;
import com.example.alphasync.offline.SyncManager;
import com.example.alphasync.repository.DiseaseRepository;
import com.example.alphasync.repository.SaveOutcome;
import com.example.alphasync.utils.NetworkUtils;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Merges cloud disease records with local Pending-Sync records and handles saving. */
public class DiseaseViewModel extends AndroidViewModel {
    public interface SaveCallback { void onResult(SaveOutcome outcome); }

    public final MediatorLiveData<List<DiseaseRecord>> records = new MediatorLiveData<>();
    public final MutableLiveData<String> error = new MutableLiveData<>();

    private final MutableLiveData<List<DiseaseRecord>> remote = new MutableLiveData<>(new ArrayList<>());
    private List<DiseaseRecord> local = new ArrayList<>();
    private final DiseaseRepository repo = new DiseaseRepository();
    private final OfflineRepository offline;
    private ListenerRegistration registration;
    private LiveData<List<DiseaseRecord>> localSource;
    private String startedFor;

    public DiseaseViewModel(@NonNull Application app) {
        super(app);
        offline = new OfflineRepository(app);
        records.addSource(remote, r -> recompute());
    }

    public void start(String labId) {
        if (labId.equals(startedFor)) return;
        startedFor = labId;
        if (registration != null) registration.remove();
        registration = repo.listen(labId, remote, error);
        if (localSource != null) records.removeSource(localSource);
        localSource = offline.observeDisease(labId);
        records.addSource(localSource, l -> { local = l == null ? new ArrayList<>() : l; recompute(); });
    }

    private void recompute() {
        Map<String, DiseaseRecord> map = new TreeMap<>();
        List<DiseaseRecord> r = remote.getValue();
        if (r != null) for (DiseaseRecord d : r) map.put(d.id, d);
        for (DiseaseRecord d : local) map.put(d.id, d); // unsynced copies take priority (shows Pending Sync)
        List<DiseaseRecord> merged = new ArrayList<>(map.values());
        // Newest report date first, then newest capture time.
        Collections.sort(merged, (a, b) -> {
            int c = b.dateReported.compareTo(a.dateReported);
            return c != 0 ? c : Long.compare(b.createdAt, a.createdAt);
        });
        records.setValue(merged);
    }

    public void save(DiseaseRecord rec, SaveCallback cb) {
        if (!NetworkUtils.isOnline(getApplication())) {
            saveLocally(rec, SaveOutcome.SAVED_OFFLINE, cb);
            return;
        }
        repo.save(rec, new FirebaseManager.Callback<Void>() {
            @Override public void onSuccess(Void v) { cb.onResult(SaveOutcome.SYNCED); }
            @Override public void onError(Exception e) { saveLocally(rec, SaveOutcome.SAVED_LOCALLY_AFTER_ERROR, cb); }
        });
    }

    private void saveLocally(DiseaseRecord rec, SaveOutcome outcome, SaveCallback cb) {
        offline.saveDisease(rec, () -> {
            SyncManager.enqueue(getApplication());
            cb.onResult(outcome);
        });
    }

    @Override
    protected void onCleared() {
        if (registration != null) registration.remove();
    }
}
