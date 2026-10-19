package com.example.alphasync.offline;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/** Local SQLite database (Room) used for offline capture. Lives in the app-private sandbox. */
@Database(entities = {InventoryEntity.class, DiseaseEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public abstract PendingDao dao();

    public static AppDatabase get(Context ctx) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(ctx.getApplicationContext(), AppDatabase.class, "alphasync.db").build();
                }
            }
        }
        return instance;
    }
}
