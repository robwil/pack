package me.robwilliams.pack.data;

import android.net.Uri;

public class BagContentProvider extends AbstractSqliteContentProvider {

    public static Uri CONTENT_URI;
    public static String CONTENT_ITEM_TYPE;

    public BagContentProvider() {
        super(/*tableName = */      "bag",
              /*validColumns = */   new String[]{"_id", "name", "color"},
              /*authority = */      "me.robwilliams.pack.bags.contentprovider",
              /*basePath = */       "bags",
              /*allItemsId = */     150,
              /*singleItemId = */   160);

        CONTENT_URI = getContentUri();
        CONTENT_ITEM_TYPE = getContentItemType();
    }
}
