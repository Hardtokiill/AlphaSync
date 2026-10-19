package com.example.alphasync.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.alphasync.adapters.AlertsAdapter;
import com.example.alphasync.databinding.FragmentAlertsBinding;
import com.example.alphasync.viewmodels.AlertsViewModel;

/** Screen 8 – Alerts list. Tap an unread alert to mark it as read. */
public class AlertsFragment extends Fragment {
    private FragmentAlertsBinding b;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentAlertsBinding.inflate(inf, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        AlertsViewModel vm = new ViewModelProvider(requireActivity()).get(AlertsViewModel.class);
        AlertsAdapter adapter = new AlertsAdapter(vm::markRead);
        b.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recycler.setAdapter(adapter);
        vm.alerts.observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            b.tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
