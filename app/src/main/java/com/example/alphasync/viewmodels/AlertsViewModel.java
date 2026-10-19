package com.example.alphasync.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.alphasync.models.Alert;
import com.example.alphasync.repository.AlertRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/** Live alerts for the laboratory (Alerts screen, Home "recent alerts", bottom-nav badge). */
public class AlertsViewModel extends ViewModel {
    public final MutableLiveData<List<Alert>> alerts = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<String> error = new MutableLiveData<>();

    private final AlertRepository repo = new AlertRepository();
    private ListenerRegistration registration;
    private String startedFor;

    public void start(String labId) {
        if (labId.equals(startedFor)) return;
        startedFor = labId;
        if (registration != null) registration.remove();
        registration = repo.listen(labId, alerts, error);
    }

    public void markRead(Alert a) {
        if (!a.read && a.id != null) repo.markRead(a.id);
    }

    public int unreadCount() {
        int n = 0;
        List<Alert> l = alerts.getValue();
        if (l != null) for (Alert a : l) if (!a.read) n++;
        return n;
    }

    @Override
    protected void onCleared() {
        if (registration != null) registration.remove();
    }
}
