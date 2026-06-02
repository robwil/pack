package me.robwilliams.pack.data;

import android.net.Uri;

public class TripBagContentProvider extends AbstractSqliteContentProvider {

    public static Uri CONTENT_URI;
    public static String CONTENT_ITEM_TYPE;

    public TripBagContentProvider() {
        super(/*tableName = */      "trip_bag",
              /*validColumns = */   new String[]{"_id", "trip_id", "bag_id"},
              /*authority = */      "me.robwilliams.pack.trip_bags.contentprovider",
              /*basePath = */       "trip_bags",
              /*allItemsId = */     170,
              /*singleItemId = */   180);

        CONTENT_URI = getContentUri();
        CONTENT_ITEM_TYPE = getContentItemType();
    }
}
