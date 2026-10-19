package com.example.alphasync.offline;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Local copy of an inventory update that has not (yet) reached Firestore. */
@Entity(tableName = "inventory_pending")
public class InventoryEntity {
    @PrimaryKey @NonNull public String id = "";
    public String laboratoryId;
    public String itemName;
    public int quantity;
    public int minimumThreshold;
    public String expiryDate;
    public long updatedAt;
    public String updatedBy;
    /** PENDING | FAILED | SYNCED */
    public String syncStatus;
}
