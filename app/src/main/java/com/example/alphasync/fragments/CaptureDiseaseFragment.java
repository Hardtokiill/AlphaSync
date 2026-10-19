package com.example.alphasync.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.alphasync.activities.MainActivity;
import com.example.alphasync.databinding.FragmentCaptureDiseaseBinding;
import com.example.alphasync.firebase.FirebaseManager;
import com.example.alphasync.models.DiseaseRecord;
import com.example.alphasync.models.User;
import com.example.alphasync.utils.DateUtils;
import com.example.alphasync.utils.Messages;
import com.example.alphasync.utils.ValidationUtils;
import com.example.alphasync.viewmodels.DiseaseViewModel;
import com.example.alphasync.viewmodels.SessionViewModel;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.Arrays;
import java.util.UUID;

/** Screen 6 – Capture a disease surveillance record (online -> Firestore, offline -> Pending Sync). */
public class CaptureDiseaseFragment extends Fragment {
    private FragmentCaptureDiseaseBinding b;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentCaptureDiseaseBinding.inflate(inf, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle saved) {
        ViewModelProvider vmp = new ViewModelProvider(requireActivity());
        DiseaseViewModel vm = vmp.get(DiseaseViewModel.class);
        SessionViewModel session = vmp.get(SessionViewModel.class);
        MainActivity act = (MainActivity) requireActivity();

        b.actSeverity.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, DiseaseRecord.SEVERITIES));
        if (saved == null) b.etDate.setText(DateUtils.today()); // convenient default: report date = today

        View.OnClickListener pick = x -> {
            // Reports cannot be dated in the future.
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker().setTitleText("Date reported")
                    .setCalendarConstraints(new CalendarConstraints.Builder()
                            .setValidator(DateValidatorPointBackward.now()).build()).build();
            picker.addOnPositiveButtonClickListener(sel -> b.etDate.setText(DateUtils.fromPickerMillis(sel)));
            picker.show(getParentFragmentManager(), "datePicker");
        };
        b.etDate.setOnClickListener(pick);
        b.tilDate.setEndIconOnClickListener(pick);

        b.btnCancel.setOnClickListener(x -> act.closeScreen());
        b.btnSubmit.setOnClickListener(x -> submit(vm, session, act));
    }

    private void submit(DiseaseViewModel vm, SessionViewModel session, MainActivity act) {
        b.tilDisease.setError(null); b.tilDate.setError(null);
        b.tilSeverity.setError(null); b.tilCases.setError(null);

        String disease = txt(b.etDisease.getText());
        String date = txt(b.etDate.getText());
        String severity = txt(b.actSeverity.getText());
        Integer cases = ValidationUtils.parseNonNegativeInt(txt(b.etCases.getText()));

        boolean ok = true;
        if (!ValidationUtils.isNotBlank(disease)) { b.tilDisease.setError("Disease name is required"); ok = false; }
        if (!ValidationUtils.isValidDate(date)) { b.tilDate.setError("Select a valid date"); ok = false; }
        else if (ValidationUtils.isFutureDate(date)) { b.tilDate.setError("Date cannot be in the future"); ok = false; }
        if (!Arrays.asList(DiseaseRecord.SEVERITIES).contains(severity)) { b.tilSeverity.setError("Select a severity"); ok = false; }
        if (cases == null) { b.tilCases.setError("Enter a valid whole number (0 or more)"); ok = false; }
        if (!ok) return;

        User u = session.user.getValue();
        if (u == null || u.laboratoryId == null) {
            act.showMessage("Your profile is still loading. Please try again in a moment.");
            return;
        }

        DiseaseRecord r = new DiseaseRecord();
        r.id = UUID.randomUUID().toString(); // stable id => re-uploading can never create a duplicate
        r.laboratoryId = u.laboratoryId;
        r.diseaseName = disease;
        r.dateReported = date;
        r.severity = severity;
        r.numberOfCases = cases;
        r.notes = txt(b.etNotes.getText());
        r.createdBy = FirebaseManager.uid();
        r.createdAt = System.currentTimeMillis();

        setBusy(true);
        vm.save(r, outcome -> {
            act.showMessage(Messages.forOutcome(act, outcome));
            if (b != null) { setBusy(false); act.closeScreen(); }
        });
    }

    private void setBusy(boolean busy) {
        b.progress.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
        b.btnSubmit.setEnabled(!busy);
        b.btnCancel.setEnabled(!busy);
    }

    private static String txt(CharSequence c) { return c == null ? "" : c.toString().trim(); }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
