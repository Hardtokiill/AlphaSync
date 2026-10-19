package com.example.alphasync.repository;

/** What happened when the user pressed Save/Submit. Used to show the right confirmation message. */
public enum SaveOutcome {
    /** Device was online and Firestore accepted the data. */
    SYNCED,
    /** Device was offline; data stored locally and marked Pending Sync. */
    SAVED_OFFLINE,
    /** Device looked online but Firestore failed/timed out; data stored locally and will retry. */
    SAVED_LOCALLY_AFTER_ERROR
}
