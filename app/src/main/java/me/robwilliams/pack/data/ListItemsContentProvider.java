package me.robwilliams.pack.data;

public class ListItemsContentProvider extends AbstractSqliteContentProvider {
    public ListItemsContentProvider() {
        super(/*tableName = */      "list_items",
              /*validColumns = */   new String[]{"_id", "list_id", "name", "weight"},
              /*authority = */      "me.robwilliams.pack.list_items.contentprovider",
              /*basePath = */       "list_items",
              /*allItemsId = */     30,
              /*singleItemId = */   40);
    }
}
