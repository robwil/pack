package me.robwilliams.pack.data;

import android.net.Uri;

public class ListContentProvider extends AbstractSqliteContentProvider {

    public static Uri CONTENT_URI;
    public static String CONTENT_ITEM_TYPE;
    public static String CONTENT_ID_TYPE;

    public ListContentProvider() {
        super(/*tableName = */      "list",
              /*validColumns = */   new String[]{"_id","name", "weight"},
              /*authority = */      "me.robwilliams.pack.lists.contentprovider",
              /*basePath = */       "lists",
              /*allItemsId = */     10,
              /*singleItemId = */   20);

        CONTENT_URI = getContentUri();
        CONTENT_ITEM_TYPE = getContentItemType();
        CONTENT_ID_TYPE = getContentIdType();
    }
}
