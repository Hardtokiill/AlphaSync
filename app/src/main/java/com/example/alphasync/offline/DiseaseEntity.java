package com.example.alphasync.offline;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Local copy of a disease record that has not (yet) reached Firestore. */
@Entity(tableName = "disease_pending")
public class DiseaseEntity {
    @PrimaryKey @NonNull public String id = "";
    public String laboratoryId;
    public String diseaseName;
    public String dateReported;
    public String severity;
    public int numberOfCases;
    public String notes;
    public String createdBy;
    public long createdAt;
    /** PENDING | FAILED | SYNCED */
    public String syncStatus;
}
