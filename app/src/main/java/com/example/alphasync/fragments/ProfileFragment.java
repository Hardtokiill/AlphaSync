package com.example.alphasync.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.alphasync.activities.MainActivity;
import com.example.alphasync.databinding.FragmentProfileBinding;
import com.example.alphasync.firebase.FirebaseManager;
import com.example.alphasync.models.User;
import com.example.alphasync.utils.DateUtils;
import com.example.alphasync.utils.NetworkUtils;
import com.example.alphasync.viewmodels.SessionViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseUser;

/** Screen 9 – Profile, change password (via emailed reset link) and logout. */
public class ProfileFragment extends Fragment {
    private FragmentProfileBinding b;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentProfileBinding.inflate(inf, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        SessionViewModel session = new ViewModelProvider(requireActivity()).get(SessionViewModel.class);
        MainActivity act = (MainActivity) requireActivity();
        FirebaseUser fu = FirebaseManager.auth().getCurrentUser();

        session.user.observe(getViewLifecycleOwner(), u -> {
            if (u == null) return;
            b.tvName.setText(u.name);
            b.tvRole.setText(User.ROLE_LAB_STAFF.equals(u.role) ? "Laboratory Staff" : u.role);
            b.tvEmail.setText("Email: " + (u.email != null ? u.email : fu != null ? fu.getEmail() : "—"));
        });
        session.lab.observe(getViewLifecycleOwner(), l -> b.tvLab.setText("Laboratory: " + (l == null ? "—" : l.name)));
        if (fu != null && fu.getMetadata() != null) {
            b.tvAccount.setText("Account created: " + DateUtils.formatDateTime(fu.getMetadata().getCreationTimestamp())
                    + "\nLast sign-in: " + DateUtils.formatDateTime(fu.getMetadata().getLastSignInTimestamp()));
        }

        b.btnChangePassword.setOnClickListener(x -> {
            String email = fu == null ? null : fu.getEmail();
            if (email == null) return;
            if (!NetworkUtils.isOnline(requireContext())) {
                act.showMessage("No internet connection. Changing your password needs a connection.");
                return;
            }
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Change password")
                    .setMessage("We will email a secure password reset link to " + email + ".")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Send link", (d, w) -> FirebaseManager.call(
                            FirebaseManager.auth().sendPasswordResetEmail(email), 20000,
                            new FirebaseManager.Callback<Void>() {
                                @Override public void onSuccess(Void r) { act.showMessage("Password reset link sent to " + email); }
                                @Override public void onError(Exception e) { act.showMessage(FirebaseManager.describe(e)); }
                            }))
                    .show();
        });

        b.btnSettings.setOnClickListener(x -> act.openScreen(new SettingsFragment()));

        b.btnHelp.setOnClickListener(x -> act.openScreen(new HelpFragment()));

        b.btnLogout.setOnClickListener(x -> {
            Integer pending = session.pendingCount.getValue();
            String msg = (pending != null && pending > 0)
                    ? pending + " record(s) have not been synchronised yet. They stay safely on this device and will upload after you log in again with an internet connection."
                    : "Are you sure you want to log out?";
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Log out")
                    .setMessage(msg)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Log out", (d, w) -> act.logout())
                    .show();
        });
    }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
