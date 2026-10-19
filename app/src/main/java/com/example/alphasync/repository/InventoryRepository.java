package com.example.alphasync.repository;

import androidx.lifecycle.MutableLiveData;

import com.example.alphasync.firebase.FirebaseManager;
import com.example.alphasync.firebase.FirebaseManager.Callback;
import com.example.alphasync.models.Inventory;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/** Cloud Firestore access for the "inventory" collection. */
public class InventoryRepository {

    /** Live-updates {@code out} with this laboratory's inventory. Caller must remove the registration. */
    public ListenerRegistration listen(String labId, MutableLiveData<List<Inventory>> out,
                                       MutableLiveData<String> error) {
        // The laboratoryId filter is required: security rules only allow reading your own lab's data.
        return FirebaseManager.db().collection("inventory").whereEqualTo("laboratoryId", labId)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) { error.setValue(FirebaseManager.describe(err)); return; }
                    List<Inventory> list = new ArrayList<>();
                    if (snap != null) for (DocumentSnapshot d : snap.getDocuments()) {
                        Inventory i = d.toObject(Inventory.class);
                        if (i != null) list.add(i);
                    }
                    out.setValue(list);
                });
    }

    /** Upserts the item (document id is deterministic, so this is idempotent). */
    public void save(Inventory item, Callback<Void> cb) {
        FirebaseManager.call(FirebaseManager.db().collection("inventory").document(item.id).set(item), 15000, cb);
    }
}
