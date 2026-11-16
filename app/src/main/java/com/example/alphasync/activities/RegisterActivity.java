package com.example.alphasync.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.alphasync.databinding.ActivityRegisterBinding;
import com.example.alphasync.firebase.FirebaseManager;
import com.example.alphasync.utils.NetworkUtils;
import com.example.alphasync.utils.ValidationUtils;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registration for laboratory staff.
 * 1) Firebase Authentication creates the account (password hashed by Firebase).
 * 2) A users/{uid} profile is written with role LAB_STAFF and the chosen laboratory.
 * 3) The new user is signed OUT again and sent to the login screen, so registering and logging in are separate steps.
 */
public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "AlphaSync/Register";

    private ActivityRegisterBinding b;
    private final List<String> labIds = new ArrayList<>();
    private final List<String> labNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(b.root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        b.btnBack.setOnClickListener(v -> finish());
        b.btnCreate.setOnClickListener(v -> register());
        loadLaboratories();
    }

    /** Fills the laboratory dropdown from the "laboratories" collection. */
    private void loadLaboratories() {
        if (!NetworkUtils.isOnline(this)) {
            showError("No internet connection. Registration needs a connection.");
            return;
        }
        FirebaseManager.call(FirebaseManager.db().collection("laboratories").get(), 15000,
                new FirebaseManager.Callback<QuerySnapshot>() {
                    @Override public void onSuccess(QuerySnapshot snap) {
                        labIds.clear();
                        labNames.clear();
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            String name = d.getString("name");
                            if (name != null) { labIds.add(d.getId()); labNames.add(name); }
                        }
                        Log.i(TAG, "Loaded " + labNames.size() + " laboratories");
                        b.actLab.setAdapter(new ArrayAdapter<>(RegisterActivity.this,
                                android.R.layout.simple_list_item_1, labNames));
                        if (labNames.isEmpty()) showError("No laboratories have been set up yet.");
                    }
                    @Override public void onError(Exception e) { showError(FirebaseManager.describe(e)); }
                });
    }

    private void register() {
        b.tilName.setError(null); b.tilEmail.setError(null); b.tilLab.setError(null);
        b.tilPassword.setError(null); b.tilConfirm.setError(null);
        b.tvError.setVisibility(View.GONE);

        String name = text(b.etName.getText());
        String email = text(b.etEmail.getText());
        String labName = text(b.actLab.getText());
        String pw = b.etPassword.getText() == null ? "" : b.etPassword.getText().toString();
        String confirm = b.etConfirm.getText() == null ? "" : b.etConfirm.getText().toString();

        int labIndex = labNames.indexOf(labName);

        boolean ok = true;
        if (!ValidationUtils.isNotBlank(name)) { b.tilName.setError("Full name is required"); ok = false; }
        if (!ValidationUtils.isValidEmail(email)) { b.tilEmail.setError("Enter a valid email address"); ok = false; }
        if (labIndex < 0) { b.tilLab.setError("Select your laboratory"); ok = false; }
        if (pw.length() < 8 || !pw.matches(".*[A-Za-z].*") || !pw.matches(".*[0-9].*")) {
            b.tilPassword.setError("Use at least 8 characters, with a letter and a number"); ok = false;
        }
        if (!pw.equals(confirm)) { b.tilConfirm.setError("Passwords do not match"); ok = false; }
        if (!ok) { Log.w(TAG, "Registration blocked by validation"); return; }

        if (!NetworkUtils.isOnline(this)) {
            showError("No internet connection. Registration needs a connection.");
            return;
        }

        final String labId = labIds.get(labIndex);
        setBusy(true);
        Log.i(TAG, "Creating account for laboratory " + labId);

        // Step 1: create the login (Firebase hashes the password; we never store it)
        FirebaseManager.call(FirebaseManager.auth().createUserWithEmailAndPassword(email, pw), 20000,
                new FirebaseManager.Callback<AuthResult>() {
                    @Override public void onSuccess(AuthResult r) {
                        FirebaseUser fu = r.getUser();
                        if (fu == null) { setBusy(false); showError("Something went wrong. Please try again."); return; }
                        Log.i(TAG, "Login account created, saving staff profile");

                        // Step 2: create the staff profile document
                        Map<String, Object> profile = new HashMap<>();
                        profile.put("name", name);
                        profile.put("email", email);
                        profile.put("role", "LAB_STAFF");
                        profile.put("laboratoryId", labId);

                        FirebaseManager.call(FirebaseManager.db().collection("users").document(fu.getUid()).set(profile),
                                15000, new FirebaseManager.Callback<Void>() {
                                    @Override public void onSuccess(Void v) {
                                        Log.i(TAG, "Registration complete - signing out so the user logs in");
                                        // Step 3: Firebase signed the new user in automatically. Sign out, then
                                        // return to the login screen so registering and logging in are separate steps.
                                        FirebaseManager.auth().signOut();
                                        setBusy(false);
                                        Intent i = new Intent(RegisterActivity.this, LoginActivity.class)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        i.putExtra(LoginActivity.EXTRA_MESSAGE, "Account created successfully. Please log in.");
                                        i.putExtra("success", true);
                                        i.putExtra("email", email);
                                        startActivity(i);
                                        finish();
                                    }
                                    @Override public void onError(Exception e) {
                                        Log.e(TAG, "Profile save failed, rolling back account: " + e.getMessage());
                                        fu.delete(); // roll back so no half-registered account is left
                                        FirebaseManager.auth().signOut();
                                        setBusy(false);
                                        showError("Could not save your profile. " + FirebaseManager.describe(e));
                                    }
                                });
                    }
                    @Override public void onError(Exception e) {
                        setBusy(false);
                        Log.w(TAG, "Account creation failed: " + e.getClass().getSimpleName());
                        if (e instanceof FirebaseAuthUserCollisionException)
                            b.tilEmail.setError("An account with this email already exists");
                        else if (e instanceof FirebaseAuthWeakPasswordException)
                            b.tilPassword.setError("This password is too weak");
                        else showError(FirebaseManager.describe(e));
                    }
                });
    }

    private void setBusy(boolean busy) {
        b.progress.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
        b.btnCreate.setEnabled(!busy);
        b.btnBack.setEnabled(!busy);
    }

    private void showError(String m) { b.tvError.setText(m); b.tvError.setVisibility(View.VISIBLE); }

    private static String text(CharSequence c) { return c == null ? "" : c.toString().trim(); }
}