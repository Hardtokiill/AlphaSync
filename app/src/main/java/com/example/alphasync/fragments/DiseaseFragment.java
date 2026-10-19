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

import com.example.alphasync.activities.MainActivity;
import com.example.alphasync.adapters.DiseaseAdapter;
import com.example.alphasync.databinding.FragmentDiseaseBinding;
import com.example.alphasync.viewmodels.DiseaseViewModel;
import com.example.alphasync.viewmodels.SessionViewModel;

/** Screen 5 – Disease surveillance records (with sync status) and the Capture button. */
public class DiseaseFragment extends Fragment {
    private FragmentDiseaseBinding b;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentDiseaseBinding.inflate(inf, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        ViewModelProvider vmp = new ViewModelProvider(requireActivity());
        DiseaseViewModel vm = vmp.get(DiseaseViewModel.class);
        SessionViewModel session = vmp.get(SessionViewModel.class);
        MainActivity act = (MainActivity) requireActivity();

        DiseaseAdapter adapter = new DiseaseAdapter();
        b.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recycler.setAdapter(adapter);

        session.lab.observe(getViewLifecycleOwner(), l -> adapter.setLabName(l == null ? "" : l.name));
        vm.records.observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            b.tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
        vm.error.observe(getViewLifecycleOwner(), e -> { if (e != null) act.showMessage(e); });

        b.btnCapture.setOnClickListener(x -> act.openScreen(new CaptureDiseaseFragment()));
    }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
