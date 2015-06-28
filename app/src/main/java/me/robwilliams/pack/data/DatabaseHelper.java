package me.robwilliams.pack.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "pack";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("PRAGMA foreign_keys=ON");

        db.execSQL("CREATE TABLE `list` (" +
                "  `_id` INTEGER PRIMARY KEY," +
                "  `name` TEXT NOT NULL," +
                "  `weight` INTEGER NOT NULL DEFAULT '0'" +
                ");");

        db.execSQL("CREATE TABLE `item` (" +
                "  `_id` INTEGER PRIMARY KEY," +
                "  `list_id` INTEGER NOT NULL," +
                "  `name` TEXT NOT NULL," +
                "  `weight` INTEGER NOT NULL DEFAULT '0'," +
                "  FOREIGN KEY(list_id) REFERENCES list(_id) ON UPDATE CASCADE ON DELETE CASCADE" +
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

        db.execSQL("CREATE TABLE `trip_list` (" +
                "  `_id` INTEGER PRIMARY KEY," +
                "  `trip_id` INTEGER NOT NULL," +
                "  `list_id` INTEGER NOT NULL," +
                "  FOREIGN KEY(trip_id) REFERENCES trip(_id) ON UPDATE CASCADE ON DELETE CASCADE," +
                "  FOREIGN KEY(list_id) REFERENCES list(_id) ON UPDATE CASCADE ON DELETE CASCADE" +
                ");");


        db.execSQL("CREATE TABLE `trip_item` (" +
                "  `_id` INTEGER PRIMARY KEY," +
                "  `trip_id` INTEGER NOT NULL," +
                "  `item_id` TEXT NOT NULL," +
                "  `status` INTEGER NOT NULL," +
                "  FOREIGN KEY(trip_id) REFERENCES trip(_id) ON UPDATE CASCADE ON DELETE CASCADE," +
                "  FOREIGN KEY(item_id) REFERENCES item(_id) ON UPDATE CASCADE ON DELETE CASCADE" +
                ");");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Only one version for now, so just implement it as a DROP / recreate.
        db.execSQL("DROP TABLE IF EXISTS `list`;");
        db.execSQL("DROP TABLE IF EXISTS `item`;");
        db.execSQL("DROP TABLE IF EXISTS `set`;");
        db.execSQL("DROP TABLE IF EXISTS `set_list`;");
        db.execSQL("DROP TABLE IF EXISTS `trip`;");
        db.execSQL("DROP TABLE IF EXISTS `trip_list`;");
        db.execSQL("DROP TABLE IF EXISTS `trip_item`;");
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        // Must turn on foreign keys every time DB is opened or the foreign keys won't be enforced
        db.execSQL("PRAGMA foreign_keys=ON");
    }
}
