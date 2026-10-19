package com.example.alphasync.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alphasync.R;
import com.example.alphasync.databinding.ItemInventoryBinding;
import com.example.alphasync.models.Inventory;
import com.example.alphasync.models.SyncRecord;

import java.util.Objects;

/** Shows inventory cards: name, laboratory, quantity, minimum, expiry and stock status. */
public class InventoryAdapter extends ListAdapter<Inventory, InventoryAdapter.VH> {
    public interface OnItemClick { void onClick(Inventory item); }

    private final OnItemClick listener;
    private String labName = "";

    public InventoryAdapter(OnItemClick l) {
        super(new DiffUtil.ItemCallback<Inventory>() {
            @Override public boolean areItemsTheSame(@NonNull Inventory a, @NonNull Inventory b) { return a.id.equals(b.id); }
            @Override public boolean areContentsTheSame(@NonNull Inventory a, @NonNull Inventory b) {
                return a.quantity == b.quantity && a.minimumThreshold == b.minimumThreshold
                        && Objects.equals(a.expiryDate, b.expiryDate) && Objects.equals(a.syncStatus, b.syncStatus)
                        && Objects.equals(a.itemName, b.itemName);
            }
        });
        listener = l;
    }

    public void setLabName(String name) { labName = name == null ? "" : name; notifyDataSetChanged(); }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(ItemInventoryBinding.inflate(LayoutInflater.from(p.getContext()), p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Inventory i = getItem(pos);
        ItemInventoryBinding b = h.b;
        b.tvItemName.setText(i.itemName);
        b.tvLab.setText("Laboratory: " + labName);
        b.tvQuantity.setText("Quantity: " + i.quantity + "   •   Minimum: " + i.minimumThreshold);
        b.tvExpiry.setText("Expiry: " + i.expiryDate);

        String status = i.getStockStatus();
        b.chipStatus.setText(status);
        int color;
        switch (status) {
            case Inventory.STATUS_LOW: color = R.color.status_low; break;
            case Inventory.STATUS_CRITICAL: color = R.color.status_critical; break;
            case Inventory.STATUS_EXPIRED: color = R.color.status_expired; break;
            default: color = R.color.status_available;
        }
        b.chipStatus.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(b.getRoot().getContext(), color)));

        boolean unsynced = !SyncRecord.STATUS_SYNCED.equals(i.syncStatus);
        b.tvSync.setVisibility(unsynced ? android.view.View.VISIBLE : android.view.View.GONE);
        b.tvSync.setText(SyncRecord.STATUS_FAILED.equals(i.syncStatus) ? "Sync failed — will retry" : "Pending Sync");
        b.getRoot().setOnClickListener(v -> listener.onClick(i));
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemInventoryBinding b;
        VH(ItemInventoryBinding b) { super(b.getRoot()); this.b = b; }
    }
}
