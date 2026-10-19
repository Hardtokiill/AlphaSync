package com.example.alphasync.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.alphasync.R;
import com.example.alphasync.databinding.FragmentNetworkStatusBinding;
import com.example.alphasync.offline.SyncManager;
import com.example.alphasync.utils.DateUtils;
import com.example.alphasync.utils.NetworkUtils;
import com.example.alphasync.viewmodels.SessionViewModel;

/** Screen 7 – Network status: connection, last sync, pending records, sync state and "Synchronise Now". */
public class NetworkStatusFragment extends Fragment {
    private FragmentNetworkStatusBinding b;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentNetworkStatusBinding.inflate(inf, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        SessionViewModel session = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);

        NetworkUtils.online().observe(getViewLifecycleOwner(), online -> {
            boolean on = Boolean.TRUE.equals(online);
            b.tvConnection.setText(on ? "Connection: Online" : "Connection: Offline");
            b.tvConnection.setTextColor(requireContext().getColor(on ? R.color.status_available : R.color.status_critical));
        });
        SyncManager.lastSync.observe(getViewLifecycleOwner(), t ->
                b.tvLastSync.setText("Last successful synchronisation: " + (t == null || t == 0 ? "Never" : DateUtils.formatDateTime(t))));
        session.pendingCount.observe(getViewLifecycleOwner(), n ->
                b.tvPendingCount.setText("Pending records: " + (n == null ? 0 : n)));
        SyncManager.message.observe(getViewLifecycleOwner(), m -> b.tvSyncState.setText("Synchronisation: " + m));
        SyncManager.syncing.observe(getViewLifecycleOwner(), busy -> {
            boolean on = Boolean.TRUE.equals(busy);
            b.progress.setVisibility(on ? View.VISIBLE : View.INVISIBLE);
            b.btnSyncNow.setEnabled(!on);
        });

        b.btnSyncNow.setOnClickListener(x -> {
            if (!NetworkUtils.isOnline(requireContext())) {
                SyncManager.message.setValue("Offline — records will synchronise when connection is restored");
                return;
            }
            SyncManager.syncNowAsync(requireContext(), r -> { });
        });
    }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
