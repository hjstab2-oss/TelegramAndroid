package org.telegram.messenger;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MersalAntiDeleteHelper {

    private static final String DB_NAME = "mersal_antidelete.db";
    private static final int DB_VERSION = 1;

    private static volatile MersalDbHelper dbHelper;
    private static final ConcurrentHashMap<Long, Set<Integer>> cache = new ConcurrentHashMap<>();

    private static MersalDbHelper getDbHelper() {
        if (dbHelper == null) {
            synchronized (MersalAntiDeleteHelper.class) {
                if (dbHelper == null) {
                    dbHelper = new MersalDbHelper(ApplicationLoader.applicationContext);
                }
            }
        }
        return dbHelper;
    }

    private static Set<Integer> getOrLoadDialogSet(long dialogId) {
        Set<Integer> set = cache.get(dialogId);
        if (set != null) {
            return set;
        }
        Set<Integer> loaded = Collections.synchronizedSet(new HashSet<>());
        try {
            SQLiteDatabase db = getDbHelper().getReadableDatabase();
            try (Cursor cursor = db.rawQuery(
                    "SELECT msg_id FROM deleted_messages WHERE dialog_id = ?",
                    new String[]{String.valueOf(dialogId)})) {
                while (cursor.moveToNext()) {
                    loaded.add(cursor.getInt(0));
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        Set<Integer> existing = cache.putIfAbsent(dialogId, loaded);
        return existing != null ? existing : loaded;
    }

    public static void markDeleted(long dialogId, Collection<Integer> msgIds) {
        if (msgIds == null || msgIds.isEmpty()) {
            return;
        }
        Set<Integer> set = getOrLoadDialogSet(dialogId);
        set.addAll(msgIds);
        try {
            SQLiteDatabase db = getDbHelper().getWritableDatabase();
            db.beginTransaction();
            try {
                long now = System.currentTimeMillis();
                for (Integer msgId : msgIds) {
                    if (msgId != null) {
                        db.execSQL(
                                "INSERT OR IGNORE INTO deleted_messages (dialog_id, msg_id, deleted_at) VALUES (?, ?, ?)",
                                new Object[]{dialogId, msgId, now}
                        );
                    }
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void unmarkDeleted(long dialogId, Collection<Integer> msgIds) {
        if (msgIds == null || msgIds.isEmpty()) {
            return;
        }
        Set<Integer> set = cache.get(dialogId);
        if (set != null) {
            set.removeAll(msgIds);
        }
        try {
            SQLiteDatabase db = getDbHelper().getWritableDatabase();
            db.beginTransaction();
            try {
                for (Integer msgId : msgIds) {
                    if (msgId != null) {
                        if (dialogId != 0) {
                            db.execSQL(
                                    "DELETE FROM deleted_messages WHERE dialog_id = ? AND msg_id = ?",
                                    new Object[]{dialogId, msgId}
                            );
                        } else {
                            db.execSQL(
                                    "DELETE FROM deleted_messages WHERE msg_id = ?",
                                    new Object[]{msgId}
                            );
                        }
                    }
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static boolean isDeleted(long dialogId, int msgId) {
        if (msgId == 0) {
            return false;
        }
        if (getOrLoadDialogSet(dialogId).contains(msgId)) {
            return true;
        }
        // Fallback if stored under dialogId=0 (legacy or unresolved private chat)
        return dialogId != 0 && getOrLoadDialogSet(0L).contains(msgId);
    }

    private static class MersalDbHelper extends SQLiteOpenHelper {
        MersalDbHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS deleted_messages (" +
                    "dialog_id INTEGER NOT NULL, " +
                    "msg_id INTEGER NOT NULL, " +
                    "deleted_at INTEGER NOT NULL, " +
                    "PRIMARY KEY (dialog_id, msg_id))");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_deleted_messages_dialog ON deleted_messages(dialog_id)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onCreate(db);
        }
    }
}
