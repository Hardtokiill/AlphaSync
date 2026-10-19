package com.example.alphasync.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Pattern;

/** Pure-Java input validation (unit-testable without an emulator). */
public final class ValidationUtils {
    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private ValidationUtils() { }

    public static boolean isValidEmail(String s) {
        return s != null && EMAIL.matcher(s.trim()).matches();
    }

    /** @return the parsed number, or null if the text is not a valid non-negative whole number. */
    public static Integer parseNonNegativeInt(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty() || t.length() > 9) return null;
        try {
            int v = Integer.parseInt(t);
            return v >= 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Strict yyyy-MM-dd check (rejects e.g. 2026-02-31). */
    public static boolean isValidDate(String s) {
        if (s == null || s.trim().isEmpty()) return false;
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setLenient(false);
        try {
            f.parse(s.trim());
            return s.trim().matches("\\d{4}-\\d{2}-\\d{2}");
        } catch (ParseException e) {
            return false;
        }
    }

    public static boolean isFutureDate(String isoDate) {
        return isoDate.compareTo(DateUtils.today()) > 0;
    }

    public static boolean isNotBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /**
     * Deterministic inventory document id: "labId_item-name". Saving the same item name again
     * therefore UPDATES the item instead of creating a duplicate (and makes offline uploads idempotent).
     */
    public static String inventoryId(String laboratoryId, String itemName) {
        String slug = itemName.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return laboratoryId + "_" + slug;
    }
}
