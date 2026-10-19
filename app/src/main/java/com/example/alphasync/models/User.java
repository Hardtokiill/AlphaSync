package com.example.alphasync.models;

import com.google.firebase.firestore.DocumentId;

/** Firestore document: users/{userId}. Passwords are NEVER stored here (Firebase Authentication owns them). */
public class User {
    /** Role string used by this mobile app. Admin / Healthcare Manager roles belong to the web dashboard. */
    public static final String ROLE_LAB_STAFF = "LAB_STAFF";

    @DocumentId public String id;
    public String name;
    public String email;
    public String role;
    public String laboratoryId;
    public String fcmToken;

    public User() { } // required by Firestore
}
