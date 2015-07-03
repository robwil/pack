package me.robwilliams.pack.data;

import android.net.Uri;

public class TripSetContentProvider extends AbstractSqliteContentProvider {

    public static Uri CONTENT_URI;
    public static String CONTENT_ITEM_TYPE;

    public TripSetContentProvider() {
        super(/*tableName = */      "trip_listset",
              /*validColumns = */   new String[]{"_id", "trip_id", "listset_id"},
              /*authority = */      "me.robwilliams.pack.trip_listsets.contentprovider",
              /*basePath = */       "trip_listsets",
              /*allItemsId = */     110,
              /*singleItemId = */   120);

        CONTENT_URI = getContentUri();
        CONTENT_ITEM_TYPE = getContentItemType();
    }
}
