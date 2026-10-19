package com.example.alphasync.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

/** Firestore document: diseaseRecords/{recordId}. The id is a client-generated UUID so re-uploads never duplicate. */
public class DiseaseRecord {
    public static final String[] SEVERITIES = {"Low", "Moderate", "High", "Critical"};

    @DocumentId public String id;
    public String laboratoryId;
    public String diseaseName;
    /** ISO date string yyyy-MM-dd. */
    public String dateReported;
    public String severity;
    public int numberOfCases;
    public String notes;
    public String createdBy;
    public long createdAt;

    /** UI-only: SYNCED / PENDING / FAILED. Never written to Firestore. */
    @Exclude public String syncStatus = SyncRecord.STATUS_SYNCED;

    public DiseaseRecord() { }
}
