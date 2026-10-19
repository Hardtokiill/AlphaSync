package com.example.alphasync.offline;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.MutableLiveData;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.alphasync.firebase.FirebaseManager;
import com.example.alphasync.models.DiseaseRecord;
import com.example.alphasync.models.Inventory;
import com.example.alphasync.models.SyncRecord;
import com.example.alphasync.repository.AlertRepository;
import com.example.alphasync.utils.NetworkUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Uploads locally stored (Pending Sync) records to Firestore.
 *
 * Duplicate protection (three layers):
 *  1. {@link #syncAll} is synchronized, so only one sync runs at a time.
 *  2. Each record has a stable document id (UUID / lab+item slug), so re-sending overwrites, never duplicates.
 *  3. A row is only marked SYNCED if it was not edited again during the upload.
 */
public final class SyncManager {
    private static final String PREFS = "alphasync_prefs";
    private static final String KEY_LAST_SYNC = "last_sync";
    private static final long KEEP_SYNCED_MS = TimeUnit.DAYS.toMillis(7);

    /** Observable state for the Network Status screen. */
    public static final MutableLiveData<Boolean> syncing = new MutableLiveData<>(false);
    public static final MutableLiveData<Long> lastSync = new MutableLiveData<>(0L);
    public static final MutableLiveData<String> message = new MutableLiveData<>("Idle");

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private SyncManager() { }

    public static final class Result {
        public int synced;
        public int failed;
        public boolean offline;
    }

    public interface Listener { void onDone(Result result); }

    /** Call once at startup (main thread) to restore the last-sync timestamp. */
    public static void init(Context ctx) {
        lastSync.setValue(prefs(ctx).getLong(KEY_LAST_SYNC, 0L));
    }

    /**
     * Schedules a sync that WorkManager runs as soon as a connection exists — even if the app was closed
     * in the meantime. This is what makes "synchronise automatically when connectivity returns" work.
     */
    public static void enqueue(Context ctx) {
        boolean wifiOnly = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean("sync_wifi_only", false);
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(wifiOnly ? NetworkType.UNMETERED : NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(ctx.getApplicationContext())
                .enqueueUniqueWork("alphasync_sync", ExistingWorkPolicy.APPEND_OR_REPLACE, req);
    }

    /** "Synchronise Now" button. Runs on a background thread and reports back on the main thread. */
    public static void syncNowAsync(Context ctx, Listener l) {
        Context app = ctx.getApplicationContext();
        EXEC.execute(() -> {
            Result r = syncAll(app);
            MAIN.post(() -> l.onDone(r));
        });
    }

    /** BLOCKING. Never call on the main thread. */
    public static synchronized Result syncAll(Context ctx) {
        Result result = new Result();
        String uid = FirebaseManager.uid();
        if (uid == null) return result; // signed out: nothing to do

        if (!NetworkUtils.isOnline(ctx)) {
            result.offline = true;
            message.postValue("Offline — waiting for connection");
            return result;
        }

        syncing.postValue(true);
        message.postValue("Synchronising…");
        PendingDao dao = AppDatabase.get(ctx).dao();
        AlertRepository alerts = new AlertRepository();

        // ---------- inventory ----------
        for (InventoryEntity e : dao.unsyncedInventory(uid)) {
            Inventory inv = OfflineRepository.toModel(e);
            try {
                await(FirebaseManager.db().collection("inventory").document(e.id).set(inv));
                dao.setInventoryStatus(e.id, e.updatedAt, SyncRecord.STATUS_SYNCED);
                result.synced++;
                writeSyncRecord(uid, e.laboratoryId, SyncRecord.TYPE_INVENTORY, e.id);
                try { Task<?> t = alerts.createStockAlert(inv); if (t != null) await(t); } catch (Exception ignored) { }
            } catch (Exception ex) {
                dao.setInventoryStatus(e.id, e.updatedAt, SyncRecord.STATUS_FAILED);
                result.failed++;
            }
        }

        // ---------- disease records ----------
        for (DiseaseEntity e : dao.unsyncedDisease(uid)) {
            DiseaseRecord rec = OfflineRepository.toModel(e);
            try {
                await(FirebaseManager.db().collection("diseaseRecords").document(e.id).set(rec));
                dao.setDiseaseStatus(e.id, e.createdAt, SyncRecord.STATUS_SYNCED);
                result.synced++;
                writeSyncRecord(uid, e.laboratoryId, SyncRecord.TYPE_DISEASE, e.id);
            } catch (Exception ex) {
                dao.setDiseaseStatus(e.id, e.createdAt, SyncRecord.STATUS_FAILED);
                result.failed++;
            }
        }

        // Housekeeping: drop local copies that were synced more than a week ago.
        long cutoff = System.currentTimeMillis() - KEEP_SYNCED_MS;
        dao.purgeSyncedInventory(cutoff);
        dao.purgeSyncedDisease(cutoff);

        if (result.failed == 0) {
            long now = System.currentTimeMillis();
            prefs(ctx).edit().putLong(KEY_LAST_SYNC, now).apply();
            lastSync.postValue(now);
            message.postValue(result.synced == 0 ? "Everything is up to date"
                    : "Synchronised " + result.synced + " record(s) successfully");
        } else {
            message.postValue(result.failed + " record(s) failed to synchronise — will retry automatically");
        }
        syncing.postValue(false);
        return result;
    }

    private static void writeSyncRecord(String uid, String labId, String type, String recordId) {
        try {
            SyncRecord s = new SyncRecord();
            s.userId = uid; s.laboratoryId = labId; s.recordType = type; s.recordId = recordId;
            s.status = SyncRecord.STATUS_SYNCED; s.timestamp = System.currentTimeMillis();
            await(FirebaseManager.db().collection("syncRecords").document(type + "_" + recordId).set(s));
        } catch (Exception ignored) {
            // The audit entry is best-effort; the data itself was already uploaded.
        }
    }

    private static <T> T await(Task<T> t) throws Exception {
        return Tasks.await(t, 20, TimeUnit.SECONDS);
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
