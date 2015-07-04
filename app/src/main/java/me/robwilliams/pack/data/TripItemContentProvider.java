package me.robwilliams.pack.data;

import android.net.Uri;

public class TripItemContentProvider extends AbstractSqliteContentProvider {

    public static Uri CONTENT_URI;
    public static String CONTENT_ITEM_TYPE;

    public TripItemContentProvider() {
        super(/*tableName = */      "trip_item",
              /*validColumns = */   new String[]{"_id", "trip_id", "item_id", "status"},
              /*authority = */      "me.robwilliams.pack.trip_items.contentprovider",
              /*basePath = */       "trip_items",
              /*allItemsId = */     130,
              /*singleItemId = */   140);

        CONTENT_URI = getContentUri();
        CONTENT_ITEM_TYPE = getContentItemType();
    }
}
