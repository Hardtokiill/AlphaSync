package com.example.alphasync.offline;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/** Background job: WorkManager runs it as soon as the device has a network connection. */
public class SyncWorker extends Worker {
    public SyncWorker(@NonNull Context ctx, @NonNull WorkerParameters params) { super(ctx, params); }

    @NonNull
    @Override
    public Result doWork() {
        SyncManager.Result r = SyncManager.syncAll(getApplicationContext());
        // Retry (with exponential back-off) if something failed or we were offline.
        return (r.failed > 0 || r.offline) ? Result.retry() : Result.success();
    }
}
