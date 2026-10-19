package com.example.alphasync.repository;

import androidx.lifecycle.MutableLiveData;

import com.example.alphasync.firebase.FirebaseManager;
import com.example.alphasync.firebase.FirebaseManager.Callback;
import com.example.alphasync.models.DiseaseRecord;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/** Cloud Firestore access for the "diseaseRecords" collection. */
public class DiseaseRepository {

    public ListenerRegistration listen(String labId, MutableLiveData<List<DiseaseRecord>> out,
                                       MutableLiveData<String> error) {
        return FirebaseManager.db().collection("diseaseRecords").whereEqualTo("laboratoryId", labId)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) { error.setValue(FirebaseManager.describe(err)); return; }
                    List<DiseaseRecord> list = new ArrayList<>();
                    if (snap != null) for (DocumentSnapshot d : snap.getDocuments()) {
                        DiseaseRecord r = d.toObject(DiseaseRecord.class);
                        if (r != null) list.add(r);
                    }
                    out.setValue(list);
                });
    }

    /** The record id is a UUID created on the device, so retrying the upload can never create duplicates. */
    public void save(DiseaseRecord r, Callback<Void> cb) {
        FirebaseManager.call(FirebaseManager.db().collection("diseaseRecords").document(r.id).set(r), 15000, cb);
    }
}
