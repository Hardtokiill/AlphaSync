package com.example.alphasync.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.alphasync.databinding.ActivityLoginBinding;
import com.example.alphasync.firebase.FirebaseManager;
import com.example.alphasync.utils.NetworkUtils;
import com.example.alphasync.utils.ValidationUtils;
import com.google.firebase.auth.AuthResult;
import android.os.CancellationSignal;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.NoCredentialException;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import android.util.Log;
/** Screen 1 – Login with Firebase Authentication (email/password) + password reset. */
public class LoginActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "message";
    private ActivityLoginBinding b;

    private static final String TAG = "AlphaSync/Login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // "Remember authenticated session": Firebase persists the login, so skip straight to the app.
        if (FirebaseManager.auth().getCurrentUser() != null) { openMain(); return; }

        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(b.root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        String msg = getIntent().getStringExtra(EXTRA_MESSAGE);
        if (msg != null) {
            if (getIntent().getBooleanExtra("success", false)) info(msg); // green "Account created" message
            else showError(msg);                                           // red error message
        }
        String prefill = getIntent().getStringExtra("email");
        if (prefill != null && b.etEmail != null) b.etEmail.setText(prefill);

        b.btnLogin.setOnClickListener(v -> attemptLogin());
        b.btnForgot.setOnClickListener(v -> sendReset());
        b.btnGoogle.setOnClickListener(v -> googleSignIn());
        b.btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        b.etPassword.setOnEditorActionListener((tv, action, e) -> {
            if (action == EditorInfo.IME_ACTION_DONE) { attemptLogin(); return true; }
            return false;
        });
    }

    private void attemptLogin() {
        clearErrors();
        String email = text(b.etEmail.getText());
        String password = b.etPassword.getText() == null ? "" : b.etPassword.getText().toString();

        boolean ok = true;
        if (!ValidationUtils.isValidEmail(email)) { b.tilEmail.setError("Enter a valid email address"); ok = false; }
        if (password.isEmpty()) { b.tilPassword.setError("Password is required"); ok = false; }
        if (!ok) return;

        if (!NetworkUtils.isOnline(this)) {
            showError("No internet connection. You need a connection to log in.");
            return;
        }

        setBusy(true);
        FirebaseManager.call(FirebaseManager.auth().signInWithEmailAndPassword(email, password), 20000,
                new FirebaseManager.Callback<AuthResult>() {
                    @Override public void onSuccess(AuthResult r) { setBusy(false); openMain(); }
                    @Override public void onError(Exception e) { setBusy(false); showError(FirebaseManager.describe(e)); }
                });
    }

    private void sendReset() {
        clearErrors();
        String email = text(b.etEmail.getText());
        if (!ValidationUtils.isValidEmail(email)) { b.tilEmail.setError("Enter your email address first"); return; }
        if (!NetworkUtils.isOnline(this)) { showError("No internet connection. Password reset needs a connection."); return; }

        setBusy(true);
        FirebaseManager.call(FirebaseManager.auth().sendPasswordResetEmail(email), 20000,
                new FirebaseManager.Callback<Void>() {
                    // Same wording whether or not the account exists (prevents account enumeration).
                    @Override public void onSuccess(Void v) { setBusy(false); info("If an account exists for " + email + ", a password reset link has been sent."); }
                    @Override public void onError(Exception e) { setBusy(false); info("If an account exists for " + email + ", a password reset link has been sent."); }
                });
    }

    // ---------------- Google single sign-on ----------------
    private void googleSignIn() {
        clearErrors();
        if (!NetworkUtils.isOnline(this)) { showError("No internet connection. Google sign-in needs a connection."); return; }

        // "Sign in with Google" button flow: always shows the account chooser
        GetSignInWithGoogleOption option = new GetSignInWithGoogleOption.Builder(
                getString(com.example.alphasync.R.string.default_web_client_id)).build();
        GetCredentialRequest request = new GetCredentialRequest.Builder().addCredentialOption(option).build();

        setBusy(true);
        Log.i(TAG, "Starting Google sign-in");
        CredentialManager.create(this).getCredentialAsync(this, request, new CancellationSignal(),
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override public void onResult(GetCredentialResponse result) {
                        try {
                            Credential c = result.getCredential();
                            if (c instanceof CustomCredential
                                    && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(c.getType())) {
                                GoogleIdTokenCredential g = GoogleIdTokenCredential.createFrom(((CustomCredential) c).getData());
                                Log.i(TAG, "Google account chosen, signing in to Firebase");
                                firebaseAuthWithGoogle(g.getIdToken());
                            } else {
                                setBusy(false);
                                showError("Google sign-in returned an unexpected result.");
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, "Could not read Google credential", ex);
                            setBusy(false);
                            showError("Google sign-in failed. Please try again.");
                        }
                    }

                    @Override public void onError(GetCredentialException e) {
                        setBusy(false);
                        Log.e(TAG, "Google sign-in failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                        if (e instanceof GetCredentialCancellationException) showError("Google sign-in was cancelled.");
                        else if (e instanceof NoCredentialException) showError("No Google account found on this phone. Add one in Settings > Accounts, then try again.");
                        else showError("Google sign-in failed. Check your internet connection and update Google Play services.");
                    }
                });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential cred = GoogleAuthProvider.getCredential(idToken, null);
        FirebaseManager.call(FirebaseManager.auth().signInWithCredential(cred), 20000,
                new FirebaseManager.Callback<AuthResult>() {
                    @Override public void onSuccess(AuthResult r) { afterGoogleSignIn(r.getUser()); }
                    @Override public void onError(Exception e) { setBusy(false); showError(FirebaseManager.describe(e)); }
                });
    }

    /** First Google login has no staff profile yet, so ask for a laboratory and create it. */
    private void afterGoogleSignIn(FirebaseUser fu) {
        if (fu == null) { setBusy(false); showError("Something went wrong. Please try again."); return; }
        FirebaseManager.call(FirebaseManager.db().collection("users").document(fu.getUid()).get(), 15000,
                new FirebaseManager.Callback<DocumentSnapshot>() {
                    @Override public void onSuccess(DocumentSnapshot d) {
                        if (d.exists()) { setBusy(false); openMain(); } else chooseLaboratory(fu);
                    }
                    @Override public void onError(Exception e) { setBusy(false); showError(FirebaseManager.describe(e)); }
                });
    }

    private void chooseLaboratory(FirebaseUser fu) {
        FirebaseManager.call(FirebaseManager.db().collection("laboratories").get(), 15000,
                new FirebaseManager.Callback<QuerySnapshot>() {
                    @Override public void onSuccess(QuerySnapshot snap) {
                        List<String> ids = new ArrayList<>(), names = new ArrayList<>();
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            if (d.getString("name") != null) { ids.add(d.getId()); names.add(d.getString("name")); }
                        }
                        setBusy(false);
                        if (names.isEmpty()) {
                            FirebaseManager.auth().signOut();
                            showError("No laboratories have been set up yet.");
                            return;
                        }
                        new AlertDialog.Builder(LoginActivity.this).setTitle("Select your laboratory")
                                .setCancelable(false)
                                .setItems(names.toArray(new String[0]), (dlg, which) -> saveGoogleProfile(fu, ids.get(which)))
                                .setNegativeButton("Cancel", (dlg, w) -> FirebaseManager.auth().signOut())
                                .show();
                    }
                    @Override public void onError(Exception e) {
                        FirebaseManager.auth().signOut();
                        setBusy(false);
                        showError(FirebaseManager.describe(e));
                    }
                });
    }

    private void saveGoogleProfile(FirebaseUser fu, String labId) {
        String name = fu.getDisplayName() != null ? fu.getDisplayName() : fu.getEmail();
        Map<String, Object> profile = new HashMap<>();
        profile.put("name", name);
        profile.put("email", fu.getEmail());
        profile.put("role", "LAB_STAFF");
        profile.put("laboratoryId", labId);
        setBusy(true);
        FirebaseManager.call(FirebaseManager.db().collection("users").document(fu.getUid()).set(profile), 15000,
                new FirebaseManager.Callback<Void>() {
                    @Override public void onSuccess(Void v) { setBusy(false); openMain(); }
                    @Override public void onError(Exception e) {
                        FirebaseManager.auth().signOut();
                        setBusy(false);
                        showError("Could not save your profile. " + FirebaseManager.describe(e));
                    }
                });
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    private void setBusy(boolean busy) {
        b.progress.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
        b.btnLogin.setEnabled(!busy);
        b.btnForgot.setEnabled(!busy);
    }

    private void clearErrors() {
        b.tilEmail.setError(null);
        b.tilPassword.setError(null);
        b.tvError.setVisibility(View.GONE);
    }

    private void showError(String m) {
        b.tvError.setTextColor(getColor(com.example.alphasync.R.color.status_critical));
        b.tvError.setText(m);
        b.tvError.setVisibility(View.VISIBLE);
    }

    private void info(String m) {
        b.tvError.setTextColor(getColor(com.example.alphasync.R.color.status_available));
        b.tvError.setText(m);
        b.tvError.setVisibility(View.VISIBLE);
    }

    private static String text(CharSequence c) { return c == null ? "" : c.toString().trim(); }
}
