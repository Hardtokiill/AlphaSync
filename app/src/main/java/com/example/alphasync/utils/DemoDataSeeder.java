package com.example.alphasync.utils;

import com.example.alphasync.firebase.FirebaseManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * DEVELOPER TOOL: writes simulated demo records (10+ per collection) so the database can be demonstrated.
 * Document ids are fixed, so running it twice overwrites the same documents (no duplicates).
 * Needs the temporary seed rules (firestore.seed.rules) because staff are not allowed to create labs/users/alerts of every type.
 */
public final class DemoDataSeeder {
    public interface Callback { void done(boolean ok, String message); }

    private DemoDataSeeder() { }

    public static void seed(Callback cb) {
        String uid = FirebaseManager.uid();
        if (uid == null) { cb.done(false, "You are not signed in."); return; }
        WriteBatch b = FirebaseManager.db().batch();
        long now = System.currentTimeMillis();

        // ---- laboratories ----
        lab(b, "lab_jhb", "NHLS Johannesburg", "Braamfontein", "ONLINE");
        lab(b, "lab_cpt", "NHLS Cape Town", "Observatory", "ONLINE");
        lab(b, "lab_dbn", "NHLS Durban", "Mayville", "ONLINE");
        lab(b, "lab_bfn", "NHLS Bloemfontein", "Universitas", "DEGRADED");
        lab(b, "lab_pta", "NHLS Pretoria", "Pretoria West", "ONLINE");
        lab(b, "lab_plk", "NHLS Polokwane", "Polokwane", "OFFLINE");
        lab(b, "lab_nel", "NHLS Nelspruit", "Mbombela", "ONLINE");
        lab(b, "lab_kmb", "NHLS Kimberley", "Kimberley", "DEGRADED");
        lab(b, "lab_gqe", "NHLS Gqeberha", "Walmer", "ONLINE");
        lab(b, "lab_els", "NHLS East London", "Cecilia Makiwane", "ONLINE");
        // ---- users (simulated staff profiles) ----
        user(b, "demo_staff_01", "Thandi Mokoena", "demo.staff01@example.com", "lab_jhb");
        user(b, "demo_staff_02", "Sipho Dlamini", "demo.staff02@example.com", "lab_cpt");
        user(b, "demo_staff_03", "Naledi Khumalo", "demo.staff03@example.com", "lab_dbn");
        user(b, "demo_staff_04", "Pieter van Wyk", "demo.staff04@example.com", "lab_bfn");
        user(b, "demo_staff_05", "Ayesha Patel", "demo.staff05@example.com", "lab_pta");
        user(b, "demo_staff_06", "Lerato Molefe", "demo.staff06@example.com", "lab_plk");
        user(b, "demo_staff_07", "Johan Botha", "demo.staff07@example.com", "lab_nel");
        user(b, "demo_staff_08", "Zanele Nkosi", "demo.staff08@example.com", "lab_kmb");
        user(b, "demo_staff_09", "Kabelo Sithole", "demo.staff09@example.com", "lab_gqe");
        user(b, "demo_staff_10", "Fatima Hendricks", "demo.staff10@example.com", "lab_els");
        // ---- inventory (laboratory, item, quantity, minimum, expiry offset in days) ----
        stock(b, "lab_jhb", "SAIMR Polyvalent Snake Antivenom", 40, 20, 420, uid, now);
        stock(b, "lab_jhb", "SAIMR Boomslang Antivenom", 8, 10, 300, uid, now);
        stock(b, "lab_jhb", "SAIMR Scorpion Antivenom", 3, 10, 200, uid, now);
        stock(b, "lab_jhb", "SAIMR Spider (Latrodectus) Antivenom", 15, 10, 150, uid, now);
        stock(b, "lab_jhb", "Rabies Immunoglobulin", 25, 15, 180, uid, now);
        stock(b, "lab_jhb", "Tetanus Immunoglobulin", 12, 12, 90, uid, now);
        stock(b, "lab_jhb", "Snake Antivenom Diluent", 60, 30, 365, uid, now);
        stock(b, "lab_jhb", "Adrenaline 1mg/ml Ampoules", 5, 20, 60, uid, now);
        stock(b, "lab_jhb", "Hydrocortisone Injection", 30, 20, -15, uid, now);
        stock(b, "lab_jhb", "Cannulas 18G", 100, 50, 500, uid, now);
        stock(b, "lab_jhb", "Normal Saline 0.9% 1L", 9, 20, 45, uid, now);
        stock(b, "lab_jhb", "Antivenom Cooler Packs", 20, 10, 700, uid, now);
        stock(b, "lab_cpt", "SAIMR Polyvalent Snake Antivenom", 22, 15, 260, uid, now);
        stock(b, "lab_cpt", "Rabies Immunoglobulin", 6, 10, 120, uid, now);
        stock(b, "lab_dbn", "SAIMR Polyvalent Snake Antivenom", 30, 20, 330, uid, now);
        // ---- disease records ----
        disease(b, "demo_disease_01", "Cholera", -2, "High", 14, "Cluster linked to contaminated borehole water", uid, now);
        disease(b, "demo_disease_02", "Malaria", -3, "Moderate", 9, "Imported cases, travel history to Limpopo", uid, now);
        disease(b, "demo_disease_03", "Tuberculosis (MDR)", -5, "High", 6, "Drug-resistant strain confirmed by GeneXpert", uid, now);
        disease(b, "demo_disease_04", "Measles", -6, "Moderate", 11, "School outbreak, vaccination status unknown", uid, now);
        disease(b, "demo_disease_05", "Listeriosis", -8, "Critical", 4, "Neonatal cases, source investigation under way", uid, now);
        disease(b, "demo_disease_06", "Rabies", -9, "Critical", 1, "Dog bite, post-exposure prophylaxis given", uid, now);
        disease(b, "demo_disease_07", "Hepatitis A", -11, "Low", 7, "Food handler cluster", uid, now);
        disease(b, "demo_disease_08", "Typhoid fever", -13, "Moderate", 5, "Blood culture positive", uid, now);
        disease(b, "demo_disease_09", "Mpox", -15, "Moderate", 2, "Isolation advised, contacts traced", uid, now);
        disease(b, "demo_disease_10", "Meningococcal disease", -18, "Critical", 3, "Close contacts given prophylaxis", uid, now);
        disease(b, "demo_disease_11", "Influenza A (H1N1)", -21, "Low", 23, "Seasonal rise at sentinel clinic", uid, now);
        disease(b, "demo_disease_12", "Diphtheria", -30, "High", 2, "Toxin gene detected", uid, now);
        // ---- alerts ----
        alert(b, "demo_alert_01", "CRITICAL_STOCK", "Critical stock level", "SAIMR Scorpion Antivenom is critically low (3 in stock, minimum 10).", 2, false, now);
        alert(b, "demo_alert_02", "CRITICAL_STOCK", "Critical stock level", "Adrenaline 1mg/ml Ampoules is critically low (5 in stock, minimum 20).", 5, false, now);
        alert(b, "demo_alert_03", "LOW_STOCK", "Low stock", "SAIMR Boomslang Antivenom is below minimum level (8 in stock, minimum 10).", 9, false, now);
        alert(b, "demo_alert_04", "CRITICAL_STOCK", "Critical stock level", "Normal Saline 0.9% 1L is critically low (9 in stock, minimum 20).", 20, true, now);
        alert(b, "demo_alert_05", "DISEASE", "Disease alert", "Cholera cluster reported: 14 cases (High severity).", 30, false, now);
        alert(b, "demo_alert_06", "DISEASE", "Disease alert", "Meningococcal disease reported: 3 cases (Critical severity).", 50, true, now);
        alert(b, "demo_alert_07", "SYSTEM", "Sync complete", "12 records synchronised successfully.", 55, true, now);
        alert(b, "demo_alert_08", "SYSTEM", "Scheduled maintenance", "Dashboard maintenance on Sunday 02:00-03:00.", 72, true, now);
        alert(b, "demo_alert_09", "DISEASE", "Disease alert", "Listeriosis cases reported: 4 cases (Critical severity).", 96, true, now);
        alert(b, "demo_alert_10", "SYSTEM", "Welcome to AlphaSync", "Your laboratory account is active.", 120, true, now);
        // ---- sync records ----
        sync(b, "INVENTORY", "lab_jhb_saimr-polyvalent-snake-antivenom", uid, now);
        sync(b, "INVENTORY", "lab_jhb_saimr-boomslang-antivenom", uid, now);
        sync(b, "INVENTORY", "lab_jhb_saimr-scorpion-antivenom", uid, now);
        sync(b, "INVENTORY", "lab_jhb_saimr-spider-latrodectus-antivenom", uid, now);
        sync(b, "INVENTORY", "lab_jhb_rabies-immunoglobulin", uid, now);
        sync(b, "DISEASE", "demo_disease_01", uid, now);
        sync(b, "DISEASE", "demo_disease_02", uid, now);
        sync(b, "DISEASE", "demo_disease_03", uid, now);
        sync(b, "DISEASE", "demo_disease_04", uid, now);
        sync(b, "DISEASE", "demo_disease_05", uid, now);

        FirebaseManager.call(b.commit(), 30000, new FirebaseManager.Callback<Void>() {
            @Override public void onSuccess(Void v) { cb.done(true, "Demo data loaded: 10+ records in every collection."); }
            @Override public void onError(Exception e) {
                cb.done(false, "Could not load demo data. Publish firestore.seed.rules first. " + FirebaseManager.describe(e));
            }
        });
    }

    // ---------------- helpers ----------------
    private static DocumentReference ref(String collection, String id) {
        return FirebaseManager.db().collection(collection).document(id);
    }

    private static String day(int offsetDays) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, offsetDays);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }

    private static void lab(WriteBatch b, String id, String name, String location, String status) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name); m.put("location", location); m.put("networkStatus", status);
        b.set(ref("laboratories", id), m);
    }

    private static void user(WriteBatch b, String id, String name, String email, String labId) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name); m.put("email", email); m.put("role", "LAB_STAFF"); m.put("laboratoryId", labId);
        b.set(ref("users", id), m);
    }

    private static void stock(WriteBatch b, String labId, String item, int qty, int min, int expiryOffset, String uid, long now) {
        Map<String, Object> m = new HashMap<>();
        m.put("laboratoryId", labId); m.put("itemName", item); m.put("quantity", qty);
        m.put("minimumThreshold", min); m.put("expiryDate", day(expiryOffset));
        m.put("updatedAt", now); m.put("updatedBy", uid);
        b.set(ref("inventory", ValidationUtils.inventoryId(labId, item)), m);
    }

    private static void disease(WriteBatch b, String id, String name, int dateOffset, String severity, int cases,
                                String notes, String uid, long now) {
        Map<String, Object> m = new HashMap<>();
        m.put("laboratoryId", "lab_jhb"); m.put("diseaseName", name); m.put("dateReported", day(dateOffset));
        m.put("severity", severity); m.put("numberOfCases", cases); m.put("notes", notes);
        m.put("createdBy", uid); m.put("createdAt", now + dateOffset * 1000L);
        b.set(ref("diseaseRecords", id), m);
    }

    private static void alert(WriteBatch b, String id, String type, String title, String message,
                              int hoursAgo, boolean read, long now) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", type); m.put("title", title); m.put("message", message); m.put("laboratoryId", "lab_jhb");
        m.put("createdAt", now - hoursAgo * 3600000L); m.put("read", read);
        b.set(ref("alerts", id), m);
    }

    private static void sync(WriteBatch b, String type, String recordId, String uid, long now) {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", uid); m.put("laboratoryId", "lab_jhb"); m.put("recordType", type);
        m.put("recordId", recordId); m.put("status", "SYNCED"); m.put("timestamp", now);
        b.set(ref("syncRecords", type + "_" + recordId), m);
    }
}
