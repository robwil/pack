package me.robwilliams.pack.data;

import android.net.Uri;

public class ItemContentProvider extends AbstractSqliteContentProvider {

    public static Uri CONTENT_URI;
    public static String CONTENT_ITEM_TYPE;

    public ItemContentProvider() {
        super(/*tableName = */      "item",
              /*validColumns = */   new String[]{"_id", "list_id", "name", "weight"},
              /*authority = */      "me.robwilliams.pack.list_items.contentprovider",
              /*basePath = */       "items",
              /*allItemsId = */     30,
              /*singleItemId = */   40);

        CONTENT_URI = getContentUri();
        CONTENT_ITEM_TYPE = getContentItemType();
    }
}
