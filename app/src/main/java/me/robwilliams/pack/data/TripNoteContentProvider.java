package me.robwilliams.pack.data;

import android.net.Uri;

public class TripNoteContentProvider extends AbstractSqliteContentProvider {

    public static Uri CONTENT_URI;
    public static String CONTENT_ITEM_TYPE;

    public TripNoteContentProvider() {
        super(/*tableName = */      "trip_note",
              /*validColumns = */   new String[]{"_id", "trip_id", "content"},
              /*authority = */      "me.robwilliams.pack.trip_notes.contentprovider",
              /*basePath = */       "trip_notes",
              /*allItemsId = */     190,
              /*singleItemId = */   200);

        CONTENT_URI = getContentUri();
        CONTENT_ITEM_TYPE = getContentItemType();
    }
}
