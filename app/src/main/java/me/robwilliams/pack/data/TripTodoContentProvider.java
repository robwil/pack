package me.robwilliams.pack.data;

import android.net.Uri;

public class TripTodoContentProvider extends AbstractSqliteContentProvider {

    public static Uri CONTENT_URI;
    public static String CONTENT_ITEM_TYPE;

    public TripTodoContentProvider() {
        super(/*tableName = */      "trip_todo",
              /*validColumns = */   new String[]{"_id", "trip_id", "text", "done", "sort_order"},
              /*authority = */      "me.robwilliams.pack.trip_todos.contentprovider",
              /*basePath = */       "trip_todos",
              /*allItemsId = */     210,
              /*singleItemId = */   220);

        CONTENT_URI = getContentUri();
        CONTENT_ITEM_TYPE = getContentItemType();
    }
}
