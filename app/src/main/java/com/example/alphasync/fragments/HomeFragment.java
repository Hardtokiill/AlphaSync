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
import com.example.alphasync.activities.MainActivity;
import com.example.alphasync.adapters.AlertsAdapter;
import com.example.alphasync.databinding.FragmentHomeBinding;
import com.example.alphasync.databinding.ItemAlertBinding;
import com.example.alphasync.models.Alert;
import com.example.alphasync.models.Inventory;
import com.example.alphasync.utils.NetworkUtils;
import com.example.alphasync.viewmodels.AlertsViewModel;
import com.example.alphasync.viewmodels.InventoryViewModel;
import com.example.alphasync.viewmodels.SessionViewModel;

import java.util.List;

/** Screen 2 – Home dashboard: staff, laboratory, network, stock summary, low-stock warning, pending, alerts. */
public class HomeFragment extends Fragment {
    private FragmentHomeBinding b;
    private SessionViewModel session;
    private AlertsViewModel alerts;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentHomeBinding.inflate(inf, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        ViewModelProvider vmp = new ViewModelProvider(requireActivity());
        session = vmp.get(SessionViewModel.class);
        alerts = vmp.get(AlertsViewModel.class);
        InventoryViewModel inventory = vmp.get(InventoryViewModel.class);

        session.user.observe(getViewLifecycleOwner(), u ->
                b.tvGreeting.setText(u == null || u.name == null ? "Welcome" : "Welcome, " + u.name));
        session.lab.observe(getViewLifecycleOwner(), l -> {
            if (l == null) { b.tvLabName.setText("Loading laboratory…"); return; }
            b.tvLabName.setText(l.location == null ? l.name : l.name + " • " + l.location);
        });
        NetworkUtils.online().observe(getViewLifecycleOwner(), o -> renderNetwork());
        session.pendingCount.observe(getViewLifecycleOwner(), n -> renderNetwork());
        inventory.all.observe(getViewLifecycleOwner(), this::renderStock);
        alerts.alerts.observe(getViewLifecycleOwner(), this::renderAlerts);

        b.cardNetwork.setOnClickListener(x -> ((MainActivity) requireActivity()).openScreen(new NetworkStatusFragment()));
    }

    private void renderNetwork() {
        boolean online = Boolean.TRUE.equals(NetworkUtils.online().getValue());
        Integer pending = session.pendingCount.getValue();
        int p = pending == null ? 0 : pending;
        b.cardNetwork.setBackgroundResource(online ? R.drawable.bg_banner_online : R.drawable.bg_banner_offline);
        b.tvNetworkStatus.setText(online ? "● Online" : "● Offline — you can keep working");
        b.tvPending.setText(p + " record(s) pending synchronisation  •  tap for details");
    }

    private void renderStock(List<Inventory> items) {
        int units = 0, available = 0, low = 0, critical = 0, expired = 0;
        StringBuilder names = new StringBuilder();
        int named = 0;
        for (Inventory i : items) {
            String st = i.getStockStatus();
            if (Inventory.STATUS_EXPIRED.equals(st)) { expired++; continue; } // expired stock is not usable
            units += i.quantity;
            if (Inventory.STATUS_AVAILABLE.equals(st)) available++;
            else {
                if (Inventory.STATUS_LOW.equals(st)) low++; else critical++;
                if (named++ < 3) names.append(named > 1 ? ", " : "").append(i.itemName);
            }
        }
        if (items.isEmpty()) {
            b.tvTotalUnits.setText("No stock recorded");
            b.tvStockBreakdown.setText("Open the Inventory tab to add antivenom stock.");
        } else {
            b.tvTotalUnits.setText(units + " usable units");
            b.tvStockBreakdown.setText("Item types tracked: " + items.size() + "\nAvailable: " + available
                    + "   Low: " + low + "   Critical: " + critical + "   Expired: " + expired);
        }
        int bad = low + critical;
        b.cardLowStock.setVisibility(bad > 0 ? View.VISIBLE : View.GONE);
        if (bad > 0) {
            String more = bad > 3 ? " and " + (bad - 3) + " more" : "";
            b.tvLowStock.setText("⚠ " + bad + " item(s) below minimum stock: " + names + more);
        }
    }

    private void renderAlerts(List<Alert> list) {
        b.recentAlertsContainer.removeAllViews();
        b.tvNoAlerts.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        for (int i = 0; i < Math.min(3, list.size()); i++) {
            Alert a = list.get(i);
            ItemAlertBinding row = ItemAlertBinding.inflate(getLayoutInflater(), b.recentAlertsContainer, false);
            // item_alert has 16dp side margins meant for full-width lists; remove them inside the padded Home page
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) row.getRoot().getLayoutParams();
            lp.setMargins(0, lp.topMargin, 0, lp.bottomMargin);
            AlertsAdapter.bind(row, a);
            row.getRoot().setOnClickListener(x -> alerts.markRead(a));
            b.recentAlertsContainer.addView(row.getRoot());
        }
    }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
