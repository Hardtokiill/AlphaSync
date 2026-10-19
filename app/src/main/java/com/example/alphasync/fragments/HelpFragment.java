package com.example.alphasync.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.alphasync.databinding.FragmentHelpBinding;
import com.example.alphasync.databinding.ItemFaqBinding;

/** In-app help: a list of tap-to-expand questions and answers. */
public class HelpFragment extends Fragment {
    private FragmentHelpBinding b;

    private static final String[][] FAQ = {
            {"How do I log in or create an account?",
                    "Enter your email and password and tap Log in. New users tap Create an account, enter their name, email, laboratory and a password (8+ characters with a letter and a number). Logging in needs an internet connection."},
            {"I forgot my password",
                    "On the login screen enter your email and tap Forgot password?. A reset link is emailed to you."},
            {"How do I add or update antivenom stock?",
                    "Open the Inventory tab and tap Add / Update Stock. Enter the item name, quantity, minimum threshold and expiry date, then tap Save. To change an existing item, tap it in the list. Saving an item name that already exists updates that item."},
            {"What do Available, Low Stock, Critical and Expired mean?",
                    "Available: stock is at or above the minimum level. Low Stock: below the minimum level. Critical: empty, or at or below half of the minimum. Expired: the expiry date has passed. Expired stock is not counted as usable on the Home screen."},
            {"How do I record a disease case?",
                    "Open the Disease tab and tap Capture Disease Data. Enter the disease name, date reported, severity, number of cases and optional notes, then tap Submit. The date cannot be in the future."},
            {"What happens if there is no internet?",
                    "You can keep working. Stock updates and disease records are saved on the phone and marked Pending Sync. When the connection returns they upload automatically and change to Synced."},
            {"How do I check or force a synchronisation?",
                    "On the Home screen tap the Online or Offline banner. The Network Status screen shows your connection, the last successful sync and the number of pending records. Tap Synchronise Now to upload immediately."},
            {"How do alerts work?",
                    "Low and critical stock levels create alerts for your laboratory, and you can also receive push notifications. Open the Alerts tab and tap an alert to mark it as read. The red number on the Alerts tab shows unread alerts."},
            {"How do I change my name, theme or notifications?",
                    "Open Profile and tap Settings. You can change your display name, choose Light, Dark or System theme, switch alert types on or off, and limit background sync to Wi-Fi."},
            {"How do I change my password?",
                    "Open Profile and tap Change password. A secure reset link is emailed to you, and you choose the new password from that email."},
            {"Who can see my data?",
                    "You can only see and change data for your own laboratory. Your password is handled by Firebase Authentication and is never stored in the app or the database."},
            {"What happens to unsynced records if I log out?",
                    "They stay safely on the phone. Log in again with an internet connection and they will upload."}
    };

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentHelpBinding.inflate(inf, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        for (String[] qa : FAQ) {
            ItemFaqBinding item = ItemFaqBinding.inflate(getLayoutInflater(), b.faqContainer, false);
            item.tvQuestion.setText(qa[0]);
            item.tvAnswer.setText(qa[1]);
            item.getRoot().setOnClickListener(x -> item.tvAnswer.setVisibility(
                    item.tvAnswer.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
            b.faqContainer.addView(item.getRoot());
        }
    }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
