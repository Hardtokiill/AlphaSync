package com.example.alphasync.utils;

import android.content.Context;

import com.example.alphasync.R;
import com.example.alphasync.repository.SaveOutcome;

/** User-facing confirmation text for save results. */
public final class Messages {
    private Messages() { }

    public static String forOutcome(Context ctx, SaveOutcome outcome) {
        switch (outcome) {
            case SYNCED: return ctx.getString(R.string.online_saved);
            case SAVED_OFFLINE: return ctx.getString(R.string.offline_saved);
            default: return ctx.getString(R.string.firebase_error_saved_locally);
        }
    }
}
