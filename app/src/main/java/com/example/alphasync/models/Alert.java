package com.example.alphasync.models;

import com.google.firebase.firestore.DocumentId;

/** Firestore document: alerts/{alertId}. */
public class Alert {
    public static final String TYPE_LOW_STOCK = "LOW_STOCK";
    public static final String TYPE_CRITICAL_STOCK = "CRITICAL_STOCK";
    public static final String TYPE_DISEASE = "DISEASE";
    public static final String TYPE_SYSTEM = "SYSTEM";

    @DocumentId public String id;
    public String title;
    public String message;
    public String type;
    public String laboratoryId;
    public long createdAt;
    public boolean read;

    public Alert() { }
}
