package me.robwilliams.pack.data;

public class ListsContentProvider extends AbstractSqliteContentProvider {
    public ListsContentProvider() {
        super(/*tableName = */      "list",
              /*validColumns = */   new String[]{"_id","name", "weight"},
              /*authority = */      "me.robwilliams.pack.lists.contentprovider",
              /*basePath = */       "lists",
              /*allItemsId = */     10,
              /*singleItemId = */   20);
    }
}
