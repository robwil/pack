package me.robwilliams.pack.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DatabaseMigrationTest {

    private static final String TEST_DB = "pack_migration_test";
    private Context context;
    private DatabaseHelper helper;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(TEST_DB);
        helper = new DatabaseHelper(context);
    }

    @After
    public void tearDown() {
        context.deleteDatabase(TEST_DB);
    }

    private SQLiteDatabase createV1Database() {
        SQLiteDatabase db = context.openOrCreateDatabase(TEST_DB, Context.MODE_PRIVATE, null);
        db.execSQL("PRAGMA foreign_keys=ON");
        db.execSQL("CREATE TABLE `list` (`_id` INTEGER PRIMARY KEY, `name` TEXT NOT NULL, `weight` INTEGER NOT NULL DEFAULT '0')");
        db.execSQL("CREATE TABLE `item` (`_id` INTEGER PRIMARY KEY, `list_id` INTEGER NOT NULL, `name` TEXT NOT NULL, `weight` INTEGER NOT NULL DEFAULT '0', FOREIGN KEY(list_id) REFERENCES list(_id) ON UPDATE CASCADE ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE `listset` (`_id` INTEGER PRIMARY KEY, `name` TEXT NOT NULL, `weight` INTEGER NOT NULL DEFAULT '0')");
        db.execSQL("CREATE TABLE `listset_list` (`_id` INTEGER PRIMARY KEY, `listset_id` INTEGER NOT NULL, `list_id` INTEGER NOT NULL, FOREIGN KEY(listset_id) REFERENCES listset(_id) ON UPDATE CASCADE ON DELETE CASCADE, FOREIGN KEY(list_id) REFERENCES list(_id) ON UPDATE CASCADE ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE `trip` (`_id` INTEGER PRIMARY KEY, `name` TEXT NOT NULL, `timestamp` DATE DEFAULT (datetime('now','localtime')))");
        db.execSQL("CREATE TABLE `trip_listset` (`_id` INTEGER PRIMARY KEY, `trip_id` INTEGER NOT NULL, `listset_id` INTEGER NOT NULL, FOREIGN KEY(trip_id) REFERENCES trip(_id) ON UPDATE CASCADE ON DELETE CASCADE, FOREIGN KEY(listset_id) REFERENCES listset(_id) ON UPDATE CASCADE ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE `trip_item` (`_id` INTEGER PRIMARY KEY, `trip_id` INTEGER NOT NULL, `item_id` TEXT NOT NULL, `status` INTEGER NOT NULL, FOREIGN KEY(trip_id) REFERENCES trip(_id) ON UPDATE CASCADE ON DELETE CASCADE, FOREIGN KEY(item_id) REFERENCES item(_id) ON UPDATE CASCADE ON DELETE CASCADE)");
        return db;
    }

    private SQLiteDatabase createV2Database() {
        SQLiteDatabase db = createV1Database();
        db.execSQL("CREATE TABLE IF NOT EXISTS `bag` (`_id` INTEGER PRIMARY KEY, `name` TEXT NOT NULL, `color` TEXT NOT NULL DEFAULT '#808080')");
        db.execSQL("CREATE TABLE IF NOT EXISTS `trip_bag` (`_id` INTEGER PRIMARY KEY, `trip_id` INTEGER NOT NULL, `bag_id` INTEGER NOT NULL, FOREIGN KEY(trip_id) REFERENCES trip(_id) ON UPDATE CASCADE ON DELETE CASCADE, FOREIGN KEY(bag_id) REFERENCES bag(_id) ON UPDATE CASCADE ON DELETE CASCADE)");
        db.execSQL("ALTER TABLE item ADD COLUMN bag_hint_id INTEGER REFERENCES bag(_id) ON DELETE SET NULL");
        db.execSQL("ALTER TABLE trip_item ADD COLUMN quantity INTEGER NOT NULL DEFAULT 1");
        db.execSQL("ALTER TABLE trip_item ADD COLUMN bag_id INTEGER REFERENCES bag(_id) ON DELETE SET NULL");
        return db;
    }

    private boolean columnExists(SQLiteDatabase db, String table, String column) {
        Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null);
        boolean found = false;
        while (cursor.moveToNext()) {
            if (cursor.getString(cursor.getColumnIndexOrThrow("name")).equals(column)) {
                found = true;
                break;
            }
        }
        cursor.close();
        return found;
    }

    private boolean tableExists(SQLiteDatabase db, String table) {
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{table});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // --- V1 -> current: simulates restoring a v1 backup ---

    @Test
    public void migrateV1ToCurrent_createsNewTables() {
        SQLiteDatabase db = createV1Database();
        helper.onUpgrade(db, 1, 3);

        assertTrue("bag table should exist", tableExists(db, "bag"));
        assertTrue("trip_bag table should exist", tableExists(db, "trip_bag"));
        db.close();
    }

    @Test
    public void migrateV1ToCurrent_addsAllNewColumns() {
        SQLiteDatabase db = createV1Database();
        helper.onUpgrade(db, 1, 3);

        assertTrue("item.bag_hint_id should exist", columnExists(db, "item", "bag_hint_id"));
        assertTrue("trip_item.quantity should exist", columnExists(db, "trip_item", "quantity"));
        assertTrue("trip_item.bag_id should exist", columnExists(db, "trip_item", "bag_id"));
        assertTrue("list.default_bag_id should exist", columnExists(db, "list", "default_bag_id"));
        db.close();
    }

    @Test
    public void migrateV1ToCurrent_preservesExistingData() {
        SQLiteDatabase db = createV1Database();

        ContentValues listValues = new ContentValues();
        listValues.put("name", "Clothes");
        listValues.put("weight", 5);
        long listId = db.insert("list", null, listValues);

        ContentValues itemValues = new ContentValues();
        itemValues.put("list_id", listId);
        itemValues.put("name", "Socks");
        itemValues.put("weight", 1);
        db.insert("item", null, itemValues);

        helper.onUpgrade(db, 1, 3);

        Cursor cursor = db.rawQuery("SELECT name, weight FROM list WHERE _id=?", new String[]{String.valueOf(listId)});
        assertTrue(cursor.moveToFirst());
        assertEquals("Clothes", cursor.getString(0));
        assertEquals(5, cursor.getInt(1));
        cursor.close();

        cursor = db.rawQuery("SELECT name, bag_hint_id FROM item WHERE list_id=?", new String[]{String.valueOf(listId)});
        assertTrue(cursor.moveToFirst());
        assertEquals("Socks", cursor.getString(0));
        assertTrue("bag_hint_id should be null for existing items", cursor.isNull(1));
        cursor.close();

        db.close();
    }

    @Test
    public void migrateV1ToCurrent_quantityDefaultsToOne() {
        SQLiteDatabase db = createV1Database();

        db.execSQL("INSERT INTO list (name) VALUES ('Test')");
        db.execSQL("INSERT INTO item (list_id, name) VALUES (1, 'Shirt')");
        db.execSQL("INSERT INTO trip (name) VALUES ('Trip1')");
        db.execSQL("INSERT INTO trip_item (trip_id, item_id, status) VALUES (1, 1, 1)");

        helper.onUpgrade(db, 1, 3);

        Cursor cursor = db.rawQuery("SELECT quantity FROM trip_item WHERE _id=1", null);
        assertTrue(cursor.moveToFirst());
        assertEquals(1, cursor.getInt(0));
        cursor.close();

        db.close();
    }

    @Test
    public void migrateV1ToCurrent_fullDataSurvival() {
        SQLiteDatabase db = createV1Database();

        db.execSQL("INSERT INTO list (name, weight) VALUES ('Toiletries', 3)");
        db.execSQL("INSERT INTO item (list_id, name, weight) VALUES (1, 'Toothbrush', 1)");
        db.execSQL("INSERT INTO item (list_id, name, weight) VALUES (1, 'Soap', 2)");
        db.execSQL("INSERT INTO trip (name) VALUES ('Beach Trip')");
        db.execSQL("INSERT INTO trip_item (trip_id, item_id, status) VALUES (1, 1, 2)");

        helper.onUpgrade(db, 1, 3);

        for (String table : new String[]{"list", "item", "listset", "listset_list", "trip", "trip_listset", "trip_item", "bag", "trip_bag"}) {
            assertTrue(table + " should exist", tableExists(db, table));
        }

        assertTrue(columnExists(db, "item", "bag_hint_id"));
        assertTrue(columnExists(db, "trip_item", "quantity"));
        assertTrue(columnExists(db, "trip_item", "bag_id"));
        assertTrue(columnExists(db, "list", "default_bag_id"));

        Cursor cursor = db.rawQuery("SELECT name FROM list", null);
        assertTrue(cursor.moveToFirst());
        assertEquals("Toiletries", cursor.getString(0));
        cursor.close();

        cursor = db.rawQuery("SELECT COUNT(*) FROM item WHERE list_id=1", null);
        assertTrue(cursor.moveToFirst());
        assertEquals(2, cursor.getInt(0));
        cursor.close();

        cursor = db.rawQuery("SELECT status, quantity FROM trip_item WHERE trip_id=1 AND item_id=1", null);
        assertTrue(cursor.moveToFirst());
        assertEquals(2, cursor.getInt(0));
        assertEquals(1, cursor.getInt(1));
        cursor.close();

        db.close();
    }

    // --- V2 -> current: simulates a v2 database upgrading ---

    @Test
    public void migrateV2ToCurrent_addsDefaultBagIdColumn() {
        SQLiteDatabase db = createV2Database();
        helper.onUpgrade(db, 2, 3);

        assertTrue("list.default_bag_id should exist", columnExists(db, "list", "default_bag_id"));
        db.close();
    }

    @Test
    public void migrateV2ToCurrent_defaultBagIdIsNullable() {
        SQLiteDatabase db = createV2Database();
        helper.onUpgrade(db, 2, 3);

        db.execSQL("INSERT INTO list (name) VALUES ('Test')");
        Cursor cursor = db.rawQuery("SELECT default_bag_id FROM list WHERE name='Test'", null);
        assertTrue(cursor.moveToFirst());
        assertTrue("default_bag_id should be null by default", cursor.isNull(0));
        cursor.close();

        db.close();
    }

    // --- Functional tests on fully migrated schema ---

    @Test
    public void migratedSchema_bagHintForeignKeyWorks() {
        SQLiteDatabase db = createV1Database();
        helper.onUpgrade(db, 1, 3);
        db.execSQL("PRAGMA foreign_keys=ON");

        db.execSQL("INSERT INTO bag (name, color) VALUES ('Backpack', '#4CAF50')");
        db.execSQL("INSERT INTO list (name) VALUES ('Clothes')");
        db.execSQL("INSERT INTO item (list_id, name, bag_hint_id) VALUES (1, 'Shirt', 1)");

        Cursor cursor = db.rawQuery("SELECT bag_hint_id FROM item WHERE name='Shirt'", null);
        assertTrue(cursor.moveToFirst());
        assertEquals(1, cursor.getInt(0));
        cursor.close();

        db.delete("bag", "_id=1", null);
        cursor = db.rawQuery("SELECT bag_hint_id FROM item WHERE name='Shirt'", null);
        assertTrue(cursor.moveToFirst());
        assertTrue("bag_hint_id should be null after bag deletion", cursor.isNull(0));
        cursor.close();

        db.close();
    }

    @Test
    public void migratedSchema_tripBagCascadeDelete() {
        SQLiteDatabase db = createV1Database();
        helper.onUpgrade(db, 1, 3);
        db.execSQL("PRAGMA foreign_keys=ON");

        db.execSQL("INSERT INTO bag (name, color) VALUES ('Carry-on', '#2196F3')");
        db.execSQL("INSERT INTO trip (name) VALUES ('Flight')");
        db.execSQL("INSERT INTO trip_bag (trip_id, bag_id) VALUES (1, 1)");

        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM trip_bag WHERE trip_id=1", null);
        assertTrue(cursor.moveToFirst());
        assertEquals(1, cursor.getInt(0));
        cursor.close();

        db.delete("trip", "_id=1", null);
        cursor = db.rawQuery("SELECT COUNT(*) FROM trip_bag", null);
        assertTrue(cursor.moveToFirst());
        assertEquals(0, cursor.getInt(0));
        cursor.close();

        db.close();
    }

    @Test
    public void migratedSchema_quantityAndBagOnTripItem() {
        SQLiteDatabase db = createV1Database();
        helper.onUpgrade(db, 1, 3);

        db.execSQL("INSERT INTO bag (name, color) VALUES ('Suitcase', '#F44336')");
        db.execSQL("INSERT INTO list (name) VALUES ('Clothes')");
        db.execSQL("INSERT INTO item (list_id, name) VALUES (1, 'Pants')");
        db.execSQL("INSERT INTO trip (name) VALUES ('Vacation')");

        ContentValues values = new ContentValues();
        values.put("trip_id", 1);
        values.put("item_id", 1);
        values.put("status", 2);
        values.put("quantity", 5);
        values.put("bag_id", 1);
        db.insert("trip_item", null, values);

        Cursor cursor = db.rawQuery("SELECT quantity, bag_id FROM trip_item WHERE trip_id=1 AND item_id=1", null);
        assertTrue(cursor.moveToFirst());
        assertEquals(5, cursor.getInt(0));
        assertEquals(1, cursor.getInt(1));
        cursor.close();

        db.close();
    }

    @Test
    public void migratedSchema_defaultBagIdOnList() {
        SQLiteDatabase db = createV1Database();
        helper.onUpgrade(db, 1, 3);
        db.execSQL("PRAGMA foreign_keys=ON");

        db.execSQL("INSERT INTO bag (name, color) VALUES ('Daypack', '#FF9800')");
        db.execSQL("INSERT INTO list (name, default_bag_id) VALUES ('Hiking Gear', 1)");

        Cursor cursor = db.rawQuery("SELECT default_bag_id FROM list WHERE name='Hiking Gear'", null);
        assertTrue(cursor.moveToFirst());
        assertEquals(1, cursor.getInt(0));
        cursor.close();

        db.delete("bag", "_id=1", null);
        cursor = db.rawQuery("SELECT default_bag_id FROM list WHERE name='Hiking Gear'", null);
        assertTrue(cursor.moveToFirst());
        assertTrue("default_bag_id should be null after bag deletion", cursor.isNull(0));
        cursor.close();

        db.close();
    }

    // --- Fresh install (onCreate) test ---

    @Test
    public void freshInstall_allTablesAndColumnsExist() {
        SQLiteDatabase db = context.openOrCreateDatabase(TEST_DB, Context.MODE_PRIVATE, null);
        helper.onCreate(db);

        for (String table : new String[]{"list", "item", "listset", "listset_list", "trip", "trip_listset", "trip_item", "bag", "trip_bag"}) {
            assertTrue(table + " should exist", tableExists(db, table));
        }

        assertTrue(columnExists(db, "item", "bag_hint_id"));
        assertTrue(columnExists(db, "trip_item", "quantity"));
        assertTrue(columnExists(db, "trip_item", "bag_id"));
        assertTrue(columnExists(db, "list", "default_bag_id"));
        assertTrue(columnExists(db, "bag", "name"));
        assertTrue(columnExists(db, "bag", "color"));
        assertTrue(columnExists(db, "trip_bag", "trip_id"));
        assertTrue(columnExists(db, "trip_bag", "bag_id"));

        db.close();
    }
}
