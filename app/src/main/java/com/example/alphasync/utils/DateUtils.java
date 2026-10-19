package com.example.alphasync.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Small date helpers. Dates are stored as ISO strings (yyyy-MM-dd). */
public final class DateUtils {
    private DateUtils() { }

    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    /** Converts the UTC-midnight millis returned by MaterialDatePicker into yyyy-MM-dd. */
    public static String fromPickerMillis(long utcMillis) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date(utcMillis));
    }

    public static String formatDateTime(long millis) {
        if (millis <= 0) return "—";
        return new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date(millis));
    }
}
