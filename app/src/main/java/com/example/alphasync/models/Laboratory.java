package com.example.alphasync.models;

import com.google.firebase.firestore.DocumentId;

/** Firestore document: laboratories/{laboratoryId}. */
public class Laboratory {
    @DocumentId public String id;
    public String name;
    public String location;
    public String networkStatus;

    public Laboratory() { }
}
