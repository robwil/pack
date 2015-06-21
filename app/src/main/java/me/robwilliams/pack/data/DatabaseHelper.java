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

        db.execSQL("CREATE TABLE `instance` (" +
                "  `_id` INTEGER PRIMARY KEY," +
                "  `name` TEXT NOT NULL," +
                "  `weight` INTEGER NOT NULL DEFAULT '0'" +
                ");");

        db.execSQL("CREATE TABLE `instance_item` (" +
                "  `_id` INTEGER PRIMARY KEY," +
                "  `instance_id` INTEGER NOT NULL," +
                "  `name` TEXT NOT NULL," +
                "  `weight` INTEGER NOT NULL DEFAULT '0'," +
                "  `from_list` TEXT NOT NULL," +
                "  `should_pack` INTEGER NOT NULL DEFAULT '0'," +
                "  `packed` INTEGER DEFAULT NULL," +
//                "  `unpacked_location` TEXT DEFAULT NULL," +
                "  `repacked` INTEGER DEFAULT NULL" +
//                "  KEY `instance_link` (`instance_id`)" +
                ");");

        db.execSQL("CREATE TABLE `list` (" +
                "  `_id` INTEGER PRIMARY KEY," +
                "  `name` TEXT NOT NULL," +
                "  `weight` INTEGER NOT NULL DEFAULT '0'" +
                ");");

        db.execSQL("CREATE TABLE `list_item` (" +
                "  `_id` INTEGER PRIMARY KEY," +
                "  `list_id` INTEGER NOT NULL," +
                "  `name` TEXT NOT NULL," +
                "  `weight` INTEGER NOT NULL DEFAULT '0'" +
//                "  KEY `list_item_link` (`list_id`)" +
                ");");

        db.execSQL("CREATE TABLE `trip` (" +
                "  `_id` INTEGER PRIMARY KEY," +
                "  `name` TEXT NOT NULL," +
                "  `weight` INTEGER NOT NULL DEFAULT '0'" +
                ");");

        db.execSQL("CREATE TABLE `trip_list` (" +
                "  `_id` INTEGER PRIMARY KEY," +
                "  `trip_id` INTEGER NOT NULL," +
                "  `list_id` INTEGER NOT NULL" +
//                "  UNIQUE KEY `no_dupe_links` (`trip_id`,`list_id`)," +
//                "  KEY `trip_link` (`trip_id`)," +
//                "  KEY `list_link` (`list_id`)" +
                ");");

//        db.execSQL("ALTER TABLE `instance_item`" +
//                "  ADD CONSTRAINT `instance_link` FOREIGN KEY (`instance_id`) REFERENCES `instance` (`instance_id`) ON DELETE CASCADE ON UPDATE NO ACTION;");
//
//        db.execSQL("ALTER TABLE `list_item`" +
//                "  ADD CONSTRAINT `list_item_link` FOREIGN KEY (`list_id`) REFERENCES `list` (`list_id`) ON DELETE CASCADE ON UPDATE NO ACTION;");
//
//        db.execSQL("ALTER TABLE `trip_list`" +
//                "  ADD CONSTRAINT `list_link` FOREIGN KEY (`list_id`) REFERENCES `list` (`list_id`) ON DELETE CASCADE ON UPDATE NO ACTION," +
//                "  ADD CONSTRAINT `trip_link` FOREIGN KEY (`trip_id`) REFERENCES `trip` (`trip_id`) ON DELETE CASCADE ON UPDATE NO ACTION;");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Only one version for now, so just implement it as a DROP / recreate.
        db.execSQL("DROP TABLE IF EXISTS `instance`;");
        db.execSQL("DROP TABLE IF EXISTS `instance_item`;");
        db.execSQL("DROP TABLE IF EXISTS `list`;");
        db.execSQL("DROP TABLE IF EXISTS `list_item`;");
        db.execSQL("DROP TABLE IF EXISTS `trip`;");
        db.execSQL("DROP TABLE IF EXISTS `trip_list`;");
        onCreate(db);
    }
}
