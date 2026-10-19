package com.example.alphasync.models;

import com.example.alphasync.utils.DateUtils;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

/** Firestore document: inventory/{inventoryId}. One document per (laboratory, item name). */
public class Inventory {
    public static final String STATUS_AVAILABLE = "Available";
    public static final String STATUS_LOW = "Low Stock";
    public static final String STATUS_CRITICAL = "Critical";
    public static final String STATUS_EXPIRED = "Expired";

    @DocumentId public String id;
    public String laboratoryId;
    public String itemName;
    public int quantity;
    public int minimumThreshold;
    /** ISO date string yyyy-MM-dd (sortable and easy for the web dashboard to read). */
    public String expiryDate;
    public long updatedAt;
    public String updatedBy;

    /** UI-only: SYNCED / PENDING / FAILED. Never written to Firestore. */
    @Exclude public String syncStatus = SyncRecord.STATUS_SYNCED;

    public Inventory() { }

    /**
     * Derives the stock status shown to the user.
     * Expired -> past expiry date. Critical -> empty or at/below half of the minimum.
     * Low Stock -> below the minimum level. Otherwise Available.
     */
    @Exclude
    public String getStockStatus() {
        if (expiryDate != null && expiryDate.compareTo(DateUtils.today()) < 0) return STATUS_EXPIRED;
        if (quantity <= 0 || quantity * 2 <= minimumThreshold) return STATUS_CRITICAL;
        if (quantity < minimumThreshold) return STATUS_LOW;
        return STATUS_AVAILABLE;
    }
}
