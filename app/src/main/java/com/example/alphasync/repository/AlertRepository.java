package com.example.alphasync.repository;

import androidx.lifecycle.MutableLiveData;

import com.example.alphasync.firebase.FirebaseManager;
import com.example.alphasync.models.Alert;
import com.example.alphasync.models.Inventory;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Cloud Firestore access for the "alerts" collection. */
public class AlertRepository {

    public ListenerRegistration listen(String labId, MutableLiveData<List<Alert>> out, MutableLiveData<String> error) {
        return FirebaseManager.db().collection("alerts").whereEqualTo("laboratoryId", labId)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) { error.setValue(FirebaseManager.describe(err)); return; }
                    List<Alert> list = new ArrayList<>();
                    if (snap != null) for (DocumentSnapshot d : snap.getDocuments()) {
                        Alert a = d.toObject(Alert.class);
                        if (a != null) list.add(a);
                    }
                    Collections.sort(list, (x, y) -> Long.compare(y.createdAt, x.createdAt)); // newest first
                    out.setValue(list);
                });
    }

    /** Security rules let staff change only the "read" field of an alert. */
    public void markRead(String alertId) {
        FirebaseManager.db().collection("alerts").document(alertId).update("read", true);
    }

    /**
     * Creates an alert document when saved stock is low/critical. The optional Cloud Function in /functions
     * turns every new alert document into an FCM push for the laboratory topic.
     * @return the Task, or null when the stock level does not need an alert
     */
    public Task<DocumentReference> createStockAlert(Inventory inv) {
        String status = inv.getStockStatus();
        boolean critical = Inventory.STATUS_CRITICAL.equals(status);
        if (!critical && !Inventory.STATUS_LOW.equals(status)) return null;

        Alert a = new Alert();
        a.type = critical ? Alert.TYPE_CRITICAL_STOCK : Alert.TYPE_LOW_STOCK;
        a.title = critical ? "Critical stock level" : "Low stock";
        a.message = inv.itemName + (critical ? " is critically low" : " is below minimum level")
                + " (" + inv.quantity + " in stock, minimum " + inv.minimumThreshold + ").";
        a.laboratoryId = inv.laboratoryId;
        a.createdAt = System.currentTimeMillis();
        a.read = false;
        return FirebaseManager.db().collection("alerts").add(a);
    }
}
