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
                "  `instance_id` int(11) NOT NULL AUTO_INCREMENT," +
                "  `name` varchar(255) NOT NULL," +
                "  `weight` int(11) NOT NULL DEFAULT '0'," +
                "  PRIMARY KEY (`instance_id`)" +
                ") ENGINE=InnoDB  DEFAULT CHARSET=latin1;");

        db.execSQL("CREATE TABLE `instance_item` (" +
                "  `item_id` int(11) NOT NULL AUTO_INCREMENT," +
                "  `instance_id` int(11) NOT NULL," +
                "  `name` varchar(255) NOT NULL," +
                "  `weight` int(11) NOT NULL DEFAULT '0'," +
                "  `from_list` varchar(255) NOT NULL," +
                "  `should_pack` tinyint(1) NOT NULL DEFAULT '0'," +
                "  `packed` tinyint(1) DEFAULT NULL," +
                "  `unpacked_location` varchar(255) DEFAULT NULL," +
                "  `repacked` tinyint(1) DEFAULT NULL," +
                "  PRIMARY KEY (`item_id`)," +
                "  KEY `instance_link` (`instance_id`)" +
                ") ENGINE=InnoDB  DEFAULT CHARSET=latin1;");

        db.execSQL("CREATE TABLE `list` (" +
                "  `list_id` int(11) NOT NULL AUTO_INCREMENT," +
                "  `name` varchar(255) NOT NULL," +
                "  `weight` int(11) NOT NULL DEFAULT '0'," +
                "  PRIMARY KEY (`list_id`)" +
                ") ENGINE=InnoDB  DEFAULT CHARSET=latin1;");

        db.execSQL("CREATE TABLE `list_item` (" +
                "  `item_id` int(11) NOT NULL AUTO_INCREMENT," +
                "  `list_id` int(11) NOT NULL," +
                "  `name` varchar(255) NOT NULL," +
                "  `weight` int(11) NOT NULL DEFAULT '0'," +
                "  PRIMARY KEY (`item_id`)," +
                "  KEY `list_item_link` (`list_id`)" +
                ") ENGINE=InnoDB  DEFAULT CHARSET=latin1;");

        db.execSQL("CREATE TABLE `trip` (" +
                "  `trip_id` int(11) NOT NULL AUTO_INCREMENT," +
                "  `name` varchar(255) NOT NULL," +
                "  `weight` int(11) NOT NULL DEFAULT '0'," +
                "  PRIMARY KEY (`trip_id`)" +
                ") ENGINE=InnoDB  DEFAULT CHARSET=latin1;");

        db.execSQL("CREATE TABLE `trip_list` (" +
                "  `item_id` int(11) NOT NULL AUTO_INCREMENT," +
                "  `trip_id` int(11) NOT NULL," +
                "  `list_id` int(11) NOT NULL," +
                "  PRIMARY KEY (`item_id`)," +
                "  UNIQUE KEY `no_dupe_links` (`trip_id`,`list_id`)," +
                "  KEY `trip_link` (`trip_id`)," +
                "  KEY `list_link` (`list_id`)" +
                ") ENGINE=InnoDB  DEFAULT CHARSET=latin1;");

        db.execSQL("ALTER TABLE `instance_item`" +
                "  ADD CONSTRAINT `instance_link` FOREIGN KEY (`instance_id`) REFERENCES `instance` (`instance_id`) ON DELETE CASCADE ON UPDATE NO ACTION;");

        db.execSQL("ALTER TABLE `list_item`" +
                "  ADD CONSTRAINT `list_item_link` FOREIGN KEY (`list_id`) REFERENCES `list` (`list_id`) ON DELETE CASCADE ON UPDATE NO ACTION;");

        db.execSQL("ALTER TABLE `trip_list`" +
                "  ADD CONSTRAINT `list_link` FOREIGN KEY (`list_id`) REFERENCES `list` (`list_id`) ON DELETE CASCADE ON UPDATE NO ACTION," +
                "  ADD CONSTRAINT `trip_link` FOREIGN KEY (`trip_id`) REFERENCES `trip` (`trip_id`) ON DELETE CASCADE ON UPDATE NO ACTION;");
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
