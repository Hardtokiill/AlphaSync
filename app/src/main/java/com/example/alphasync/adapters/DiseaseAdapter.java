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
import com.example.alphasync.databinding.ItemDiseaseBinding;
import com.example.alphasync.models.DiseaseRecord;
import com.example.alphasync.models.SyncRecord;

import java.util.Objects;

/** Shows disease records: name, date, severity, cases, laboratory and synchronisation status. */
public class DiseaseAdapter extends ListAdapter<DiseaseRecord, DiseaseAdapter.VH> {
    private String labName = "";

    public DiseaseAdapter() {
        super(new DiffUtil.ItemCallback<DiseaseRecord>() {
            @Override public boolean areItemsTheSame(@NonNull DiseaseRecord a, @NonNull DiseaseRecord b) { return a.id.equals(b.id); }
            @Override public boolean areContentsTheSame(@NonNull DiseaseRecord a, @NonNull DiseaseRecord b) {
                return Objects.equals(a.syncStatus, b.syncStatus) && a.numberOfCases == b.numberOfCases
                        && Objects.equals(a.severity, b.severity) && Objects.equals(a.diseaseName, b.diseaseName);
            }
        });
    }

    public void setLabName(String name) { labName = name == null ? "" : name; notifyDataSetChanged(); }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(ItemDiseaseBinding.inflate(LayoutInflater.from(p.getContext()), p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        DiseaseRecord d = getItem(pos);
        ItemDiseaseBinding b = h.b;
        android.content.Context c = b.getRoot().getContext();
        b.tvDisease.setText(d.diseaseName);
        b.tvDate.setText("Reported: " + d.dateReported);
        b.tvCases.setText("Cases: " + d.numberOfCases);
        b.tvLab.setText("Laboratory: " + labName);

        b.chipSeverity.setText(d.severity);
        int sev;
        if ("Critical".equals(d.severity)) sev = R.color.status_critical;
        else if ("High".equals(d.severity)) sev = R.color.status_low;
        else if ("Moderate".equals(d.severity)) sev = R.color.status_pending;
        else sev = R.color.status_available;
        b.chipSeverity.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(c, sev)));

        if (SyncRecord.STATUS_SYNCED.equals(d.syncStatus)) {
            b.tvSync.setText("✔ Synced");
            b.tvSync.setTextColor(ContextCompat.getColor(c, R.color.status_synced));
        } else if (SyncRecord.STATUS_FAILED.equals(d.syncStatus)) {
            b.tvSync.setText("⚠ Sync failed — will retry");
            b.tvSync.setTextColor(ContextCompat.getColor(c, R.color.status_critical));
        } else {
            b.tvSync.setText("⏳ Pending Sync");
            b.tvSync.setTextColor(ContextCompat.getColor(c, R.color.status_pending));
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemDiseaseBinding b;
        VH(ItemDiseaseBinding b) { super(b.getRoot()); this.b = b; }
    }
}
