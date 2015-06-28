package me.robwilliams.pack.data;

import android.net.Uri;

public class SetContentProvider extends AbstractSqliteContentProvider {

    public static Uri CONTENT_URI;
    public static String CONTENT_ITEM_TYPE;

    public SetContentProvider() {
        super(/*tableName = */      "listset",
              /*validColumns = */   new String[]{"_id", "name", "weight"},
              /*authority = */      "me.robwilliams.pack.listsets.contentprovider",
              /*basePath = */       "listsets",
              /*allItemsId = */     50,
              /*singleItemId = */   60);

        CONTENT_URI = getContentUri();
        CONTENT_ITEM_TYPE = getContentItemType();
    }
}
