package com.example.alphasync.offline;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/** Data-access object for locally stored (offline) records. */
@Dao
public interface PendingDao {
    // ---- writes ----
    @Insert(onConflict = OnConflictStrategy.REPLACE) void upsertInventory(InventoryEntity e);
    @Insert(onConflict = OnConflictStrategy.REPLACE) void upsertDisease(DiseaseEntity e);

    @Query("DELETE FROM inventory_pending WHERE id = :id") void deleteInventory(String id);

    // ---- UI observers (anything not yet synced) ----
    @Query("SELECT * FROM inventory_pending WHERE laboratoryId = :labId AND syncStatus != 'SYNCED' ORDER BY updatedAt DESC")
    LiveData<List<InventoryEntity>> observeInventory(String labId);

    @Query("SELECT * FROM disease_pending WHERE laboratoryId = :labId AND syncStatus != 'SYNCED' ORDER BY createdAt DESC")
    LiveData<List<DiseaseEntity>> observeDisease(String labId);

    @Query("SELECT COUNT(*) FROM inventory_pending WHERE laboratoryId = :labId AND syncStatus != 'SYNCED'")
    LiveData<Integer> countInventory(String labId);

    @Query("SELECT COUNT(*) FROM disease_pending WHERE laboratoryId = :labId AND syncStatus != 'SYNCED'")
    LiveData<Integer> countDisease(String labId);

    // ---- sync engine (blocking; call from a background thread) ----
    @Query("SELECT * FROM inventory_pending WHERE syncStatus != 'SYNCED' AND updatedBy = :uid")
    List<InventoryEntity> unsyncedInventory(String uid);

    @Query("SELECT * FROM disease_pending WHERE syncStatus != 'SYNCED' AND createdBy = :uid")
    List<DiseaseEntity> unsyncedDisease(String uid);

    /**
     * Only changes the row if it has not been edited since it was uploaded (updatedAt still matches),
     * so a newer offline edit made during a sync is never wrongly marked as synced.
     */
    @Query("UPDATE inventory_pending SET syncStatus = :status WHERE id = :id AND updatedAt = :updatedAt")
    void setInventoryStatus(String id, long updatedAt, String status);

    @Query("UPDATE disease_pending SET syncStatus = :status WHERE id = :id AND createdAt = :createdAt")
    void setDiseaseStatus(String id, long createdAt, String status);

    @Query("DELETE FROM inventory_pending WHERE syncStatus = 'SYNCED' AND updatedAt < :before")
    void purgeSyncedInventory(long before);

    @Query("DELETE FROM disease_pending WHERE syncStatus = 'SYNCED' AND createdAt < :before")
    void purgeSyncedDisease(long before);
}
