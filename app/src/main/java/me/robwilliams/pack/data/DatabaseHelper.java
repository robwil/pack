package me.robwilliams.pack.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "pack";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("PRAGMA foreign_keys=ON");

        db.execSQL("CREATE TABLE `list` (" +
                "  `_id` INTEGER PRIMARY KEY," +
                "  `name` TEXT NOT NULL," +
                "  `weight` INTEGER NOT NULL DEFAULT '0'" +
                ");");

        db.execSQL("CREATE TABLE `bag` (" +
                "  `_id` INTEGER PRIMARY KEY," +
                "  `name` TEXT NOT NULL," +
                "  `color` TEXT NOT NULL DEFAULT '#808080'" +
                ");");

        db.execSQL("CREATE TABLE `item` (" +
                "  `_id` INTEGER PRIMARY KEY," +
                "  `list_id` INTEGER NOT NULL," +
                "  `name` TEXT NOT NULL," +
                "  `weight` INTEGER NOT NULL DEFAULT '0'," +
                "  `bag_hint_id` INTEGER," +
                "  FOREIGN KEY(list_id) REFERENCES list(_id) ON UPDATE CASCADE ON DELETE CASCADE," +
                "  FOREIGN KEY(bag_hint_id) REFERENCES bag(_id) ON DELETE SET NULL" +
                ");");

        db.execSQL("CREATE TABLE `listset` (" +
                "  `_id` INTEGER PRIMARY KEY," +
                "  `name` TEXT NOT NULL," +
                "  `weight` INTEGER NOT NULL DEFAULT '0'" +
                ");");

        db.execSQL("CREATE TABLE `listset_list` (" +
                "  `_id` INTEGER PRIMARY KEY," +
                "  `listset_id` INTEGER NOT NULL," +
                "  `list_id` INTEGER NOT NULL," +
                "  FOREIGN KEY(listset_id)  REFERENCES listset(_id) ON UPDATE CASCADE ON DELETE CASCADE," +
                "  FOREIGN KEY(list_id) REFERENCES list(_id) ON UPDATE CASCADE ON DELETE CASCADE" +
                ");");

        db.execSQL("CREATE TABLE `trip` (" +
                "  `_id` INTEGER PRIMARY KEY," +
                "  `name` TEXT NOT NULL," +
                "  `timestamp` DATE DEFAULT (datetime('now','localtime'))" +
                ");");

        db.execSQL("CREATE TABLE `trip_listset` (" +
                "  `_id` INTEGER PRIMARY KEY," +
                "  `trip_id` INTEGER NOT NULL," +
                "  `listset_id` INTEGER NOT NULL," +
                "  FOREIGN KEY(trip_id) REFERENCES trip(_id) ON UPDATE CASCADE ON DELETE CASCADE," +
                "  FOREIGN KEY(listset_id) REFERENCES listset(_id) ON UPDATE CASCADE ON DELETE CASCADE" +
                ");");

        db.execSQL("CREATE TABLE `trip_item` (" +
                "  `_id` INTEGER PRIMARY KEY," +
                "  `trip_id` INTEGER NOT NULL," +
                "  `item_id` TEXT NOT NULL," +
                "  `status` INTEGER NOT NULL," +
                "  `quantity` INTEGER NOT NULL DEFAULT 1," +
                "  `bag_id` INTEGER," +
                "  FOREIGN KEY(trip_id) REFERENCES trip(_id) ON UPDATE CASCADE ON DELETE CASCADE," +
                "  FOREIGN KEY(item_id) REFERENCES item(_id) ON UPDATE CASCADE ON DELETE CASCADE," +
                "  FOREIGN KEY(bag_id) REFERENCES bag(_id) ON DELETE SET NULL" +
                ");");

        db.execSQL("CREATE TABLE `trip_bag` (" +
                "  `_id` INTEGER PRIMARY KEY," +
                "  `trip_id` INTEGER NOT NULL," +
                "  `bag_id` INTEGER NOT NULL," +
                "  FOREIGN KEY(trip_id) REFERENCES trip(_id) ON UPDATE CASCADE ON DELETE CASCADE," +
                "  FOREIGN KEY(bag_id) REFERENCES bag(_id) ON UPDATE CASCADE ON DELETE CASCADE" +
                ");");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `bag` (" +
                    "  `_id` INTEGER PRIMARY KEY," +
                    "  `name` TEXT NOT NULL," +
                    "  `color` TEXT NOT NULL DEFAULT '#808080'" +
                    ");");
            db.execSQL("CREATE TABLE IF NOT EXISTS `trip_bag` (" +
                    "  `_id` INTEGER PRIMARY KEY," +
                    "  `trip_id` INTEGER NOT NULL," +
                    "  `bag_id` INTEGER NOT NULL," +
                    "  FOREIGN KEY(trip_id) REFERENCES trip(_id) ON UPDATE CASCADE ON DELETE CASCADE," +
                    "  FOREIGN KEY(bag_id) REFERENCES bag(_id) ON UPDATE CASCADE ON DELETE CASCADE" +
                    ");");
            db.execSQL("ALTER TABLE item ADD COLUMN bag_hint_id INTEGER REFERENCES bag(_id) ON DELETE SET NULL");
            db.execSQL("ALTER TABLE trip_item ADD COLUMN quantity INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE trip_item ADD COLUMN bag_id INTEGER REFERENCES bag(_id) ON DELETE SET NULL");
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys=ON");
    }
}
