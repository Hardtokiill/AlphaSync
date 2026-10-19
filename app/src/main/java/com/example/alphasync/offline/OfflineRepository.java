package com.example.alphasync.offline;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.alphasync.models.DiseaseRecord;
import com.example.alphasync.models.Inventory;
import com.example.alphasync.models.SyncRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Reads/writes locally stored records and converts between Room entities and UI models. */
public class OfflineRepository {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private final PendingDao dao;

    public OfflineRepository(Context ctx) { dao = AppDatabase.get(ctx).dao(); }

    // ---------------- writes (async) ----------------
    /** Saves an inventory update locally and marks it PENDING. {@code done} runs on the main thread. */
    public void saveInventory(Inventory i, Runnable done) {
        IO.execute(() -> {
            InventoryEntity e = new InventoryEntity();
            e.id = i.id; e.laboratoryId = i.laboratoryId; e.itemName = i.itemName;
            e.quantity = i.quantity; e.minimumThreshold = i.minimumThreshold; e.expiryDate = i.expiryDate;
            e.updatedAt = i.updatedAt; e.updatedBy = i.updatedBy;
            e.syncStatus = SyncRecord.STATUS_PENDING;
            dao.upsertInventory(e);
            MAIN.post(done);
        });
    }

    public void saveDisease(DiseaseRecord d, Runnable done) {
        IO.execute(() -> {
            DiseaseEntity e = new DiseaseEntity();
            e.id = d.id; e.laboratoryId = d.laboratoryId; e.diseaseName = d.diseaseName;
            e.dateReported = d.dateReported; e.severity = d.severity; e.numberOfCases = d.numberOfCases;
            e.notes = d.notes; e.createdBy = d.createdBy; e.createdAt = d.createdAt;
            e.syncStatus = SyncRecord.STATUS_PENDING;
            dao.upsertDisease(e);
            MAIN.post(done);
        });
    }

    /** Removes a stale local copy once the same item has been saved online. */
    public void deleteInventory(String id) { IO.execute(() -> dao.deleteInventory(id)); }

    // ---------------- observers ----------------
    public LiveData<List<Inventory>> observeInventory(String labId) {
        MediatorLiveData<List<Inventory>> out = new MediatorLiveData<>();
        out.addSource(dao.observeInventory(labId), list -> {
            List<Inventory> r = new ArrayList<>();
            for (InventoryEntity e : list) r.add(toModel(e));
            out.setValue(r);
        });
        return out;
    }

    public LiveData<List<DiseaseRecord>> observeDisease(String labId) {
        MediatorLiveData<List<DiseaseRecord>> out = new MediatorLiveData<>();
        out.addSource(dao.observeDisease(labId), list -> {
            List<DiseaseRecord> r = new ArrayList<>();
            for (DiseaseEntity e : list) r.add(toModel(e));
            out.setValue(r);
        });
        return out;
    }

    /** Total number of records waiting to be synchronised for a laboratory. */
    public LiveData<Integer> observePendingCount(String labId) {
        MediatorLiveData<Integer> out = new MediatorLiveData<>();
        MutableLiveData<Integer> a = new MutableLiveData<>(0), b = new MutableLiveData<>(0);
        out.addSource(dao.countInventory(labId), v -> { a.setValue(v == null ? 0 : v); out.setValue(a.getValue() + b.getValue()); });
        out.addSource(dao.countDisease(labId), v -> { b.setValue(v == null ? 0 : v); out.setValue(a.getValue() + b.getValue()); });
        return out;
    }

    // ---------------- mapping ----------------
    public static Inventory toModel(InventoryEntity e) {
        Inventory i = new Inventory();
        i.id = e.id; i.laboratoryId = e.laboratoryId; i.itemName = e.itemName; i.quantity = e.quantity;
        i.minimumThreshold = e.minimumThreshold; i.expiryDate = e.expiryDate;
        i.updatedAt = e.updatedAt; i.updatedBy = e.updatedBy; i.syncStatus = e.syncStatus;
        return i;
    }

    public static DiseaseRecord toModel(DiseaseEntity e) {
        DiseaseRecord d = new DiseaseRecord();
        d.id = e.id; d.laboratoryId = e.laboratoryId; d.diseaseName = e.diseaseName;
        d.dateReported = e.dateReported; d.severity = e.severity; d.numberOfCases = e.numberOfCases;
        d.notes = e.notes; d.createdBy = e.createdBy; d.createdAt = e.createdAt; d.syncStatus = e.syncStatus;
        return d;
    }
}
