package com.example.alphasync.models;

import com.google.firebase.firestore.DocumentId;

/** Firestore document: syncRecords/{syncId}. An audit trail of what each device has uploaded. */
public class SyncRecord {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SYNCED = "SYNCED";
    public static final String STATUS_FAILED = "FAILED";

    public static final String TYPE_INVENTORY = "INVENTORY";
    public static final String TYPE_DISEASE = "DISEASE";

    @DocumentId public String id;
    public String userId;
    public String laboratoryId;
    public String recordType;
    public String recordId;
    public String status;
    public long timestamp;

    public SyncRecord() { }
}
