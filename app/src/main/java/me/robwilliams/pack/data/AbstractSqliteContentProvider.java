package me.robwilliams.pack.data;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Implementing abstract ContentProvider wrapper for Sqlite table,
 * so that my Sqlite interaction code can be as DRY as possible.
 *
 * Reference: http://www.vogella.com/tutorials/AndroidSQLite/article.html
 */
public abstract class AbstractSqliteContentProvider extends ContentProvider {
    private DatabaseHelper databaseHelper;

    private final String tableName;
    private final String[] validColumns;
    private final String basePath;
    private final int allItemsId;
    private final int singleItemId;
    private final UriMatcher sURIMatcher;


    public static Uri CONTENT_URI;
    public static String CONTENT_ITEM_TYPE;

    public AbstractSqliteContentProvider(String tableName,
                                         String[] validColumns,
                                         String authority,
                                         String basePath,
                                         int allItemsId,
                                         int singleItemId) {
        this.tableName = tableName;
        this.allItemsId = allItemsId;
        this.singleItemId = singleItemId;
        this.basePath = basePath;
        this.validColumns = validColumns;

        sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sURIMatcher.addURI(authority, basePath, allItemsId);
        sURIMatcher.addURI(authority, basePath + "/#", singleItemId);

        // This is kind of hacky, but since these Uris are essentially constants,
        // we expose them via a static context
        CONTENT_URI = Uri.parse("content://" + authority + "/" + basePath);
        CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + tableName;

    }

    @Override
    public boolean onCreate() {
        databaseHelper = new DatabaseHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        checkColumns(projection);
        queryBuilder.setTables(tableName);

        int uriType = sURIMatcher.match(uri);
        if (uriType == allItemsId) {
            // do nothing, leave query without WHERE clause
        } else if (uriType == singleItemId) {
            queryBuilder.appendWhere("_id=" + uri.getLastPathSegment());
        } else {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = databaseHelper.getWritableDatabase();
        long id;
        if (uriType == allItemsId) {
            id = sqlDB.insert(tableName, null, values);
        } else {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(basePath + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = databaseHelper.getWritableDatabase();
        int rowsDeleted;
        if (uriType == allItemsId) {
            rowsDeleted = sqlDB.delete(tableName, selection, selectionArgs);
        } else if (uriType == singleItemId) {
            String id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsDeleted = sqlDB.delete(tableName, "_id=" + id, null);
            } else {
                rowsDeleted = sqlDB.delete(tableName,
                        "_id=" + id + " and " + selection,
                        selectionArgs);
            }
        } else {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = databaseHelper.getWritableDatabase();
        int rowsUpdated;
        if (uriType == allItemsId) {
            rowsUpdated = sqlDB.update(tableName,
                    values,
                    selection,
                    selectionArgs);
        } else if (uriType == singleItemId) {
            String id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsUpdated = sqlDB.update(tableName,
                        values,
                        "_id=" + id,
                        null);
            } else {
                rowsUpdated = sqlDB.update(tableName,
                        values,
                        "_id=" + id + " and " + selection,
                        selectionArgs);
            }
        } else {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    private void checkColumns(String[] projection) {
        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<>(Arrays.asList(validColumns));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }
}
