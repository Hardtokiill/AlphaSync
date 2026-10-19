package com.example.alphasync.adapters;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alphasync.R;
import com.example.alphasync.databinding.ItemAlertBinding;
import com.example.alphasync.models.Alert;
import com.example.alphasync.utils.DateUtils;

import java.util.Objects;

/** Shows alerts with message, date/time, type and read/unread state. Tapping marks an alert as read. */
public class AlertsAdapter extends ListAdapter<Alert, AlertsAdapter.VH> {
    public interface OnAlertClick { void onClick(Alert alert); }

    private final OnAlertClick listener;

    public AlertsAdapter(OnAlertClick l) {
        super(new DiffUtil.ItemCallback<Alert>() {
            @Override public boolean areItemsTheSame(@NonNull Alert a, @NonNull Alert b) { return Objects.equals(a.id, b.id); }
            @Override public boolean areContentsTheSame(@NonNull Alert a, @NonNull Alert b) {
                return a.read == b.read && Objects.equals(a.message, b.message) && a.createdAt == b.createdAt;
            }
        });
        listener = l;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(ItemAlertBinding.inflate(LayoutInflater.from(p.getContext()), p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Alert a = getItem(pos);
        bind(h.b, a);
        h.b.getRoot().setOnClickListener(v -> listener.onClick(a));
    }

    /** Shared with the Home screen's "recent alerts" list. */
    public static void bind(ItemAlertBinding b, Alert a) {
        android.content.Context c = b.getRoot().getContext();
        b.tvAlertTitle.setText(a.title);
        b.tvAlertMessage.setText(a.message);
        b.tvAlertTime.setText(DateUtils.formatDateTime(a.createdAt) + (a.read ? "  •  Read" : "  •  Unread"));
        b.viewUnread.setVisibility(a.read ? View.INVISIBLE : View.VISIBLE);
        b.tvAlertTitle.setTypeface(null, a.read ? Typeface.NORMAL : Typeface.BOLD);

        String label; int color;
        if (Alert.TYPE_CRITICAL_STOCK.equals(a.type)) { label = "Critical stock"; color = R.color.status_critical; }
        else if (Alert.TYPE_LOW_STOCK.equals(a.type)) { label = "Low stock"; color = R.color.status_low; }
        else if (Alert.TYPE_DISEASE.equals(a.type)) { label = "Disease"; color = R.color.status_pending; }
        else { label = "System"; color = R.color.text_secondary; }
        b.chipType.setText(label);
        b.chipType.setTextColor(ContextCompat.getColor(c, R.color.white));
        b.chipType.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(c, color)));
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemAlertBinding b;
        VH(ItemAlertBinding b) { super(b.getRoot()); this.b = b; }
    }
}
