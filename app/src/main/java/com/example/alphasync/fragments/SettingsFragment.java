package com.example.alphasync.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.alphasync.R;
import com.example.alphasync.activities.MainActivity;
import com.example.alphasync.databinding.FragmentSettingsBinding;
import com.example.alphasync.firebase.FirebaseManager;
import com.example.alphasync.models.User;
import com.example.alphasync.utils.NetworkUtils;
import com.example.alphasync.utils.ValidationUtils;
import com.example.alphasync.viewmodels.SessionViewModel;

import com.example.alphasync.utils.DemoDataSeeder;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Settings: display name, theme, notification switches, Wi-Fi-only sync and an About card. */
public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding b;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentSettingsBinding.inflate(inf, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        SessionViewModel session = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        MainActivity act = (MainActivity) requireActivity();
        SharedPreferences prefs = requireContext().getSharedPreferences("alphasync_prefs", Context.MODE_PRIVATE);

        // ---- Appearance (0 = system, 1 = light, 2 = dark) ----
        int mode = prefs.getInt("theme_mode", 0);
        b.rgTheme.check(mode == 1 ? R.id.rbLight : mode == 2 ? R.id.rbDark : R.id.rbSystem);
        b.rgTheme.setOnCheckedChangeListener((group, id) -> {
            int m = id == R.id.rbLight ? 1 : id == R.id.rbDark ? 2 : 0;
            prefs.edit().putInt("theme_mode", m).apply();
            AppCompatDelegate.setDefaultNightMode(m == 1 ? AppCompatDelegate.MODE_NIGHT_NO
                    : m == 2 ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        });

        // ---- Notifications ----
        boolean master = prefs.getBoolean("notifications_enabled", true);
        b.switchNotifications.setChecked(master);
        b.switchStock.setChecked(prefs.getBoolean("notify_stock", true));
        b.switchDisease.setChecked(prefs.getBoolean("notify_disease", true));
        b.switchStock.setEnabled(master);
        b.switchDisease.setEnabled(master);
        b.switchNotifications.setOnCheckedChangeListener((btn, on) -> {
            prefs.edit().putBoolean("notifications_enabled", on).apply();
            b.switchStock.setEnabled(on);
            b.switchDisease.setEnabled(on);
        });
        b.switchStock.setOnCheckedChangeListener((btn, on) -> prefs.edit().putBoolean("notify_stock", on).apply());
        b.switchDisease.setOnCheckedChangeListener((btn, on) -> prefs.edit().putBoolean("notify_disease", on).apply());

        // ---- Data and sync ----
        b.switchWifiOnly.setChecked(prefs.getBoolean("sync_wifi_only", false));
        b.switchWifiOnly.setOnCheckedChangeListener((btn, on) -> {
            prefs.edit().putBoolean("sync_wifi_only", on).apply();
            act.showMessage(on ? "Background sync will use Wi-Fi only" : "Background sync can use any connection");
        });

        // ---- Developer tool: load simulated demo data ----
        b.btnSeed.setOnClickListener(x -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Load demo data")
                .setMessage("This writes simulated records to the database. Continue?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Load", (d, w) -> {
                    b.btnSeed.setEnabled(false);
                    DemoDataSeeder.seed((ok, msg) -> {
                        act.showMessage(msg);
                        if (b != null) b.btnSeed.setEnabled(true);
                    });
                }).show());

        // ---- Display name ----
        session.user.observe(getViewLifecycleOwner(), u -> {
            if (u != null && b.etName.getText() != null && b.etName.getText().length() == 0) b.etName.setText(u.name);
            renderAbout(session);
        });
        session.lab.observe(getViewLifecycleOwner(), l -> renderAbout(session));

        b.btnSaveName.setOnClickListener(x -> {
            b.tilName.setError(null);
            String name = b.etName.getText() == null ? "" : b.etName.getText().toString().trim();
            if (!ValidationUtils.isNotBlank(name)) { b.tilName.setError("Name is required"); return; }
            String uid = FirebaseManager.uid();
            if (uid == null) return;
            if (!NetworkUtils.isOnline(requireContext())) {
                act.showMessage("No internet connection. Changing your name needs a connection.");
                return;
            }
            b.btnSaveName.setEnabled(false);
            FirebaseManager.call(FirebaseManager.db().collection("users").document(uid).update("name", name), 15000,
                    new FirebaseManager.Callback<Void>() {
                        @Override public void onSuccess(Void r) {
                            User u = session.user.getValue();
                            if (u != null) { u.name = name; session.user.setValue(u); }
                            act.showMessage("Name updated");
                            if (b != null) b.btnSaveName.setEnabled(true);
                        }
                        @Override public void onError(Exception e) {
                            act.showMessage(FirebaseManager.describe(e));
                            if (b != null) b.btnSaveName.setEnabled(true);
                        }
                    });
        });
    }

    private void renderAbout(SessionViewModel session) {
        if (b == null) return;
        String version = "1.0";
        try {
            PackageInfo pi = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
            version = pi.versionName;
        } catch (Exception ignored) { }
        User u = session.user.getValue();
        String lab = session.lab.getValue() == null ? "—" : session.lab.getValue().name;
        String role = u != null && User.ROLE_LAB_STAFF.equals(u.role) ? "Laboratory Staff" : "—";
        b.tvAbout.setText("AlphaSync v" + version + "\nLaboratory: " + lab + "\nRole: " + role
                + "\nNHLS laboratory staff app");
    }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}