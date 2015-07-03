package me.robwilliams.pack.data;

import android.net.Uri;

public class ListsetListContentProvider extends AbstractSqliteContentProvider {

    public static Uri CONTENT_URI;
    public static String CONTENT_ITEM_TYPE;

    public ListsetListContentProvider() {
        super(/*tableName = */      "listset_list",
              /*validColumns = */   new String[]{"_id", "listset_id", "list_id"},
              /*authority = */      "me.robwilliams.pack.listset_lists.contentprovider",
              /*basePath = */       "listset_lists",
              /*allItemsId = */     70,
              /*singleItemId = */   80);

        CONTENT_URI = getContentUri();
        CONTENT_ITEM_TYPE = getContentItemType();
    }
}
