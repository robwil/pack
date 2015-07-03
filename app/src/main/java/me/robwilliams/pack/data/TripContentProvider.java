package me.robwilliams.pack.data;

import android.net.Uri;

public class TripContentProvider extends AbstractSqliteContentProvider {

    public static Uri CONTENT_URI;
    public static String CONTENT_ITEM_TYPE;

    public TripContentProvider() {
        super(/*tableName = */      "trip",
              /*validColumns = */   new String[]{"_id", "name", "timestamp"},
              /*authority = */      "me.robwilliams.pack.trips.contentprovider",
              /*basePath = */       "trips",
              /*allItemsId = */     90,
              /*singleItemId = */   100);

        CONTENT_URI = getContentUri();
        CONTENT_ITEM_TYPE = getContentItemType();
    }
}
