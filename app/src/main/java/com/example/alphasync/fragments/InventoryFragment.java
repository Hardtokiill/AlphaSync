package com.example.alphasync.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.alphasync.R;
import com.example.alphasync.activities.MainActivity;
import com.example.alphasync.adapters.InventoryAdapter;
import com.example.alphasync.databinding.FragmentInventoryBinding;
import com.example.alphasync.models.Inventory;
import com.example.alphasync.viewmodels.InventoryViewModel;
import com.example.alphasync.viewmodels.SessionViewModel;

/** Screen 3 – Inventory list with search, status filter and Add/Update Stock. */
public class InventoryFragment extends Fragment {
    private FragmentInventoryBinding b;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentInventoryBinding.inflate(inf, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        ViewModelProvider vmp = new ViewModelProvider(requireActivity());
        InventoryViewModel vm = vmp.get(InventoryViewModel.class);
        SessionViewModel session = vmp.get(SessionViewModel.class);
        MainActivity act = (MainActivity) requireActivity();

        InventoryAdapter adapter = new InventoryAdapter(item -> act.openScreen(UpdateStockFragment.newInstance(item)));
        b.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recycler.setAdapter(adapter);

        session.lab.observe(getViewLifecycleOwner(), l -> adapter.setLabName(l == null ? "" : l.name));
        vm.filtered.observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            b.tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
        vm.error.observe(getViewLifecycleOwner(), e -> { if (e != null) act.showMessage(e); });

        b.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b2, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b2, int c) { vm.setQuery(s.toString()); }
            @Override public void afterTextChanged(Editable s) { }
        });

        b.chipFilter.setOnCheckedStateChangeListener((group, ids) -> {
            int id = ids.isEmpty() ? R.id.chipAll : ids.get(0);
            String status = null;
            if (id == R.id.chipAvailable) status = Inventory.STATUS_AVAILABLE;
            else if (id == R.id.chipLow) status = Inventory.STATUS_LOW;
            else if (id == R.id.chipCritical) status = Inventory.STATUS_CRITICAL;
            else if (id == R.id.chipExpired) status = Inventory.STATUS_EXPIRED;
            vm.setStatusFilter(status);
        });

        b.btnAddStock.setOnClickListener(x -> act.openScreen(UpdateStockFragment.newInstance(null)));
    }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
