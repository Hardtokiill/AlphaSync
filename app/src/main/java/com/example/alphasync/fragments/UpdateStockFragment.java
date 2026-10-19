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
import com.example.alphasync.databinding.FragmentUpdateStockBinding;
import com.example.alphasync.firebase.FirebaseManager;
import com.example.alphasync.models.Inventory;
import com.example.alphasync.models.User;
import com.example.alphasync.utils.DateUtils;
import com.example.alphasync.utils.Messages;
import com.example.alphasync.utils.ValidationUtils;
import com.example.alphasync.viewmodels.InventoryViewModel;
import com.example.alphasync.viewmodels.SessionViewModel;
import com.google.android.material.datepicker.MaterialDatePicker;

/** Screen 4 – Add or update an antivenom stock item (online -> Firestore, offline -> Pending Sync). */
public class UpdateStockFragment extends Fragment {
    private static final String ARG_NAME = "name", ARG_QTY = "qty", ARG_MIN = "min", ARG_EXPIRY = "expiry";
    private FragmentUpdateStockBinding b;

    /** @param item existing item to edit, or null to add a new one */
    public static UpdateStockFragment newInstance(@Nullable Inventory item) {
        Bundle a = new Bundle();
        if (item != null) {
            a.putString(ARG_NAME, item.itemName);
            a.putInt(ARG_QTY, item.quantity);
            a.putInt(ARG_MIN, item.minimumThreshold);
            a.putString(ARG_EXPIRY, item.expiryDate);
        }
        UpdateStockFragment f = new UpdateStockFragment();
        f.setArguments(a);
        return f;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentUpdateStockBinding.inflate(inf, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle saved) {
        ViewModelProvider vmp = new ViewModelProvider(requireActivity());
        InventoryViewModel vm = vmp.get(InventoryViewModel.class);
        SessionViewModel session = vmp.get(SessionViewModel.class);
        MainActivity act = (MainActivity) requireActivity();

        Bundle args = getArguments();
        if (saved == null && args != null && args.containsKey(ARG_NAME)) {
            b.tvTitle.setText("Update stock");
            b.etItemName.setText(args.getString(ARG_NAME));
            b.etItemName.setEnabled(false); // the item name identifies the record, so it is fixed when editing
            b.etQuantity.setText(String.valueOf(args.getInt(ARG_QTY)));
            b.etMinimum.setText(String.valueOf(args.getInt(ARG_MIN)));
            b.etExpiry.setText(args.getString(ARG_EXPIRY));
        }

        View.OnClickListener pick = x -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker().setTitleText("Expiry date").build();
            picker.addOnPositiveButtonClickListener(sel -> b.etExpiry.setText(DateUtils.fromPickerMillis(sel)));
            picker.show(getParentFragmentManager(), "expiryPicker");
        };
        b.etExpiry.setOnClickListener(pick);
        b.tilExpiry.setEndIconOnClickListener(pick);

        b.btnCancel.setOnClickListener(x -> act.closeScreen());
        b.btnSave.setOnClickListener(x -> save(vm, session, act));
    }

    private void save(InventoryViewModel vm, SessionViewModel session, MainActivity act) {
        b.tilItemName.setError(null); b.tilQuantity.setError(null);
        b.tilMinimum.setError(null); b.tilExpiry.setError(null);

        String name = txt(b.etItemName.getText());
        Integer qty = ValidationUtils.parseNonNegativeInt(txt(b.etQuantity.getText()));
        Integer min = ValidationUtils.parseNonNegativeInt(txt(b.etMinimum.getText()));
        String expiry = txt(b.etExpiry.getText());

        boolean ok = true;
        if (!ValidationUtils.isNotBlank(name)) { b.tilItemName.setError("Item name is required"); ok = false; }
        if (qty == null) { b.tilQuantity.setError("Enter a valid whole number (0 or more)"); ok = false; }
        if (min == null) { b.tilMinimum.setError("Enter a valid whole number (0 or more)"); ok = false; }
        if (!ValidationUtils.isValidDate(expiry)) { b.tilExpiry.setError("Select a valid expiry date"); ok = false; }
        if (!ok) return;

        User u = session.user.getValue();
        if (u == null || u.laboratoryId == null) {
            act.showMessage("Your profile is still loading. Please try again in a moment.");
            return;
        }

        Inventory inv = new Inventory();
        inv.laboratoryId = u.laboratoryId;
        inv.itemName = name;
        inv.id = ValidationUtils.inventoryId(u.laboratoryId, name);
        inv.quantity = qty;
        inv.minimumThreshold = min;
        inv.expiryDate = expiry;
        inv.updatedAt = System.currentTimeMillis();
        inv.updatedBy = FirebaseManager.uid();

        setBusy(true);
        vm.save(inv, outcome -> {
            act.showMessage(Messages.forOutcome(act, outcome));
            if (b != null) { setBusy(false); act.closeScreen(); }
        });
    }

    private void setBusy(boolean busy) {
        b.progress.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
        b.btnSave.setEnabled(!busy);
        b.btnCancel.setEnabled(!busy);
    }

    private static String txt(CharSequence c) { return c == null ? "" : c.toString().trim(); }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
