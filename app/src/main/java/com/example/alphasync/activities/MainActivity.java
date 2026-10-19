package com.example.alphasync.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.alphasync.R;
import com.example.alphasync.databinding.ActivityMainBinding;
import com.example.alphasync.firebase.FirebaseManager;
import com.example.alphasync.firebase.NotificationHelper;
import com.example.alphasync.fragments.AlertsFragment;
import com.example.alphasync.fragments.DiseaseFragment;
import com.example.alphasync.fragments.HomeFragment;
import com.example.alphasync.fragments.InventoryFragment;
import com.example.alphasync.fragments.ProfileFragment;
import com.example.alphasync.models.User;
import com.example.alphasync.offline.SyncManager;
import com.example.alphasync.repository.UserRepository;
import com.example.alphasync.viewmodels.AlertsViewModel;
import com.example.alphasync.viewmodels.DiseaseViewModel;
import com.example.alphasync.viewmodels.InventoryViewModel;
import com.example.alphasync.viewmodels.SessionViewModel;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.messaging.FirebaseMessaging;

/** Hosts the bottom navigation (Home | Inventory | Disease | Alerts | Profile) and shared ViewModels. */
public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding b;
    private SessionViewModel session;
    private InventoryViewModel inventory;
    private DiseaseViewModel disease;
    private AlertsViewModel alerts;
    private boolean dataStarted = false;
    private AlertDialog errorDialog;
    private ActivityResultLauncher<String> notificationPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Validate authentication state: protected screens are never shown to signed-out users.
        if (FirebaseManager.auth().getCurrentUser() == null) { goToLogin(null); return; }

        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(b.root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        ViewModelProvider vmp = new ViewModelProvider(this);
        session = vmp.get(SessionViewModel.class);
        inventory = vmp.get(InventoryViewModel.class);
        disease = vmp.get(DiseaseViewModel.class);
        alerts = vmp.get(AlertsViewModel.class);

        // Keeps the merged inventory list live even when the Inventory tab is not on screen (Home needs it).
        inventory.all.observe(this, l -> { });
        disease.records.observe(this, l -> { });

        notificationPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

        setupNavigation();

        session.user.observe(this, this::onUserLoaded);
        session.loadError.observe(this, this::onLoadError);
        alerts.alerts.observe(this, l -> updateAlertBadge());
        if (session.user.getValue() == null) session.load();

        if (savedInstanceState == null) navigateTo(getIntent().getStringExtra(NotificationHelper.EXTRA_DESTINATION));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (b != null) navigateTo(intent.getStringExtra(NotificationHelper.EXTRA_DESTINATION));
    }

    // ------------------------------------------------------------------ navigation
    private void setupNavigation() {
        // Safety net: make sure the five tabs exist even if the XML menu did not load
        android.view.Menu menu = b.bottomNav.getMenu();
        if (menu.size() == 0) {
            menu.add(android.view.Menu.NONE, R.id.nav_home, 0, R.string.nav_home).setIcon(R.drawable.ic_nav_home);
            menu.add(android.view.Menu.NONE, R.id.nav_inventory, 1, R.string.nav_inventory).setIcon(R.drawable.ic_nav_inventory);
            menu.add(android.view.Menu.NONE, R.id.nav_disease, 2, R.string.nav_disease).setIcon(R.drawable.ic_nav_disease);
            menu.add(android.view.Menu.NONE, R.id.nav_alerts, 3, R.string.nav_alerts).setIcon(R.drawable.ic_nav_alerts);
            menu.add(android.view.Menu.NONE, R.id.nav_profile, 4, R.string.nav_profile).setIcon(R.drawable.ic_nav_profile);
        }
        b.bottomNav.setOnItemSelectedListener(item -> {
            showTopLevel(fragmentFor(item.getItemId()));
            return true;
        });
        // Re-tapping the current tab returns to that tab's main screen.
        b.bottomNav.setOnItemReselectedListener(item ->
                getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE));
    }

    private Fragment fragmentFor(int id) {
        if (id == R.id.nav_inventory) return new InventoryFragment();
        if (id == R.id.nav_disease) return new DiseaseFragment();
        if (id == R.id.nav_alerts) return new AlertsFragment();
        if (id == R.id.nav_profile) return new ProfileFragment();
        return new HomeFragment();
    }

    private void showTopLevel(Fragment f) {
        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fm.beginTransaction().replace(R.id.fragmentContainer, f).commit();
    }

    /** Opens a sub-screen (Update Stock, Capture Disease Data, Network Status) with Back support. */
    public void openScreen(Fragment f) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, f).addToBackStack(null).commit();
    }

    public void closeScreen() { getSupportFragmentManager().popBackStack(); }

    private void navigateTo(String destination) {
        int id = R.id.nav_home;
        if ("inventory".equals(destination)) id = R.id.nav_inventory;
        else if ("disease".equals(destination)) id = R.id.nav_disease;
        else if ("alerts".equals(destination)) id = R.id.nav_alerts;
        else if ("profile".equals(destination)) id = R.id.nav_profile;
        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentById(R.id.fragmentContainer) == null) {
            // First launch: nothing is showing yet, so show the tab explicitly.
            showTopLevel(fragmentFor(id));
            android.view.MenuItem item = b.bottomNav.getMenu().findItem(id);
            if (item != null) item.setChecked(true);
        } else if (b.bottomNav.getSelectedItemId() == id) {
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        } else {
            b.bottomNav.setSelectedItemId(id);
        }
    }

    /** Confirmation / status message that survives screen changes (shown above the bottom bar). */
    public void showMessage(String message) {
        Snackbar s = Snackbar.make(b.fragmentContainer, message, Snackbar.LENGTH_LONG);
        s.setTextMaxLines(4);
        s.show();
    }

    // ------------------------------------------------------------------ session
    private void onUserLoaded(User user) {
        if (user == null) return;
        // The mobile app is for laboratory staff only; admins / managers use the web dashboard.
        if (!User.ROLE_LAB_STAFF.equals(user.role)) {
            FirebaseManager.auth().signOut();
            goToLogin("This account is not a laboratory staff account. Please use the AlphaSync web dashboard.");
            return;
        }
        if (user.laboratoryId == null || dataStarted) return;
        dataStarted = true;

        inventory.start(user.laboratoryId);
        disease.start(user.laboratoryId);
        alerts.start(user.laboratoryId);

        // Push notifications: topic per laboratory + store this device's token on the profile.
        FirebaseMessaging.getInstance().subscribeToTopic(topicFor(user.laboratoryId));
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(
                token -> new UserRepository().saveFcmToken(user.id, token));

        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
        SyncManager.enqueue(this); // push anything captured earlier while offline
    }

    private void onLoadError(String err) {
        if (err == null) return;
        if (errorDialog != null && errorDialog.isShowing()) return;
        boolean fatal = "NO_PROFILE".equals(err) || "NO_LAB".equals(err);
        String msg = "NO_PROFILE".equals(err)
                ? "Your account has no AlphaSync staff profile yet. Please contact your administrator."
                : "NO_LAB".equals(err) ? "Your account is not linked to a laboratory. Please contact your administrator."
                : err;
        AlertDialog.Builder d = new AlertDialog.Builder(this)
                .setTitle("Could not load your profile").setMessage(msg).setCancelable(false)
                .setNegativeButton("Log out", (x, w) -> logout());
        if (!fatal) d.setPositiveButton("Retry", (x, w) -> session.load());
        errorDialog = d.show();
    }

    private void updateAlertBadge() {
        int unread = alerts.unreadCount();
        if (unread > 0) {
            BadgeDrawable badge = b.bottomNav.getOrCreateBadge(R.id.nav_alerts);
            badge.setNumber(unread);
        } else {
            b.bottomNav.removeBadge(R.id.nav_alerts);
        }
    }

    /** Signs out. Records still Pending Sync stay safely on the device until the user signs in again. */
    public void logout() {
        User u = session.user.getValue();
        if (u != null && u.laboratoryId != null)
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topicFor(u.laboratoryId));
        FirebaseManager.auth().signOut();
        goToLogin(null);
    }

    private void goToLogin(String message) {
        Intent i = new Intent(this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (message != null) i.putExtra(LoginActivity.EXTRA_MESSAGE, message);
        startActivity(i);
        finish();
    }

    /** FCM topic names may only contain [a-zA-Z0-9-_.~%]. */
    public static String topicFor(String labId) {
        return "lab_" + labId.replaceAll("[^a-zA-Z0-9\\-_.~%]", "_");
    }
}
