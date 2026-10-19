package com.example.alphasync.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.alphasync.firebase.FirebaseManager;
import com.example.alphasync.models.Inventory;
import com.example.alphasync.offline.OfflineRepository;
import com.example.alphasync.offline.SyncManager;
import com.example.alphasync.repository.AlertRepository;
import com.example.alphasync.repository.InventoryRepository;
import com.example.alphasync.repository.SaveOutcome;
import com.example.alphasync.utils.NetworkUtils;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Merges cloud inventory (Firestore) with local Pending-Sync updates, applies search/filter,
 * and decides online-vs-offline saving.
 */
public class InventoryViewModel extends AndroidViewModel {
    public interface SaveCallback { void onResult(SaveOutcome outcome); }

    /** Everything for the lab (unfiltered) — used by the Home summary. */
    public final MediatorLiveData<List<Inventory>> all = new MediatorLiveData<>();
    /** After search + status filter — used by the Inventory list. */
    public final MutableLiveData<List<Inventory>> filtered = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<String> error = new MutableLiveData<>();

    private final MutableLiveData<List<Inventory>> remote = new MutableLiveData<>(new ArrayList<>());
    private List<Inventory> local = new ArrayList<>();
    private String query = "";
    private String statusFilter = null; // null = all

    private final InventoryRepository repo = new InventoryRepository();
    private final AlertRepository alerts = new AlertRepository();
    private final OfflineRepository offline;
    private ListenerRegistration registration;
    private LiveData<List<Inventory>> localSource;
    private String startedFor;

    public InventoryViewModel(@NonNull Application app) {
        super(app);
        offline = new OfflineRepository(app);
        all.addSource(remote, r -> recompute());
    }

    /** Begin listening for a laboratory's inventory (safe to call repeatedly). */
    public void start(String labId) {
        if (labId.equals(startedFor)) return;
        startedFor = labId;
        if (registration != null) registration.remove();
        registration = repo.listen(labId, remote, error);
        if (localSource != null) all.removeSource(localSource);
        localSource = offline.observeInventory(labId);
        all.addSource(localSource, l -> { local = l == null ? new ArrayList<>() : l; recompute(); });
    }

    public void setQuery(String q) { query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT); recompute(); }
    public void setStatusFilter(String status) { statusFilter = status; recompute(); }

    private void recompute() {
        // Local Pending-Sync rows are newer than the cloud copy, so they override it.
        Map<String, Inventory> map = new TreeMap<>();
        List<Inventory> r = remote.getValue();
        if (r != null) for (Inventory i : r) map.put(i.id, i);
        for (Inventory i : local) map.put(i.id, i);

        List<Inventory> merged = new ArrayList<>(map.values());
        Collections.sort(merged, (a, b) -> a.itemName.compareToIgnoreCase(b.itemName));
        all.setValue(merged);

        List<Inventory> out = new ArrayList<>();
        for (Inventory i : merged) {
            if (!query.isEmpty() && !i.itemName.toLowerCase(Locale.ROOT).contains(query)) continue;
            if (statusFilter != null && !statusFilter.equals(i.getStockStatus())) continue;
            out.add(i);
        }
        filtered.setValue(out);
    }

    /** Online -> Firestore. Offline (or Firestore failure) -> local storage marked Pending Sync. */
    public void save(Inventory item, SaveCallback cb) {
        if (!NetworkUtils.isOnline(getApplication())) {
            saveLocally(item, SaveOutcome.SAVED_OFFLINE, cb);
            return;
        }
        repo.save(item, new FirebaseManager.Callback<Void>() {
            @Override public void onSuccess(Void v) {
                offline.deleteInventory(item.id); // drop any stale pending copy of the same item
                Task<?> t = alerts.createStockAlert(item); // low/critical -> alert document (best effort)
                cb.onResult(SaveOutcome.SYNCED);
            }
            @Override public void onError(Exception e) {
                saveLocally(item, SaveOutcome.SAVED_LOCALLY_AFTER_ERROR, cb);
            }
        });
    }

    private void saveLocally(Inventory item, SaveOutcome outcome, SaveCallback cb) {
        offline.saveInventory(item, () -> {
            SyncManager.enqueue(getApplication()); // uploads automatically when connectivity returns
            cb.onResult(outcome);
        });
    }

    @Override
    protected void onCleared() {
        if (registration != null) registration.remove();
    }
}
