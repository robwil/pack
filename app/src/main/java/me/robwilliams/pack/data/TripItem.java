package me.robwilliams.pack.data;

import android.os.Parcel;
import android.os.Parcelable;

public class TripItem implements Parcelable {
    final private int itemId;
    final private int listsetId;
    final private String listName;
    final private String itemName;
    private int status;

    public static final Parcelable.Creator<TripItem> CREATOR = new Parcelable.Creator<TripItem>() {
        public TripItem createFromParcel(Parcel in) {
            return new TripItem(in);
        }

        public TripItem[] newArray(int size) {
            return new TripItem[size];
        }
    };

    public TripItem(int itemId, int listsetId, String listName, String itemName, int status) {
        this.itemId = itemId;
        this.listsetId = listsetId;
        this.listName = listName;
        this.itemName = itemName;
        this.status = status;
    }

    public TripItem(Parcel parcel) {
        this.itemId = parcel.readInt();
        this.listsetId = parcel.readInt();
        this.listName = parcel.readString();
        this.itemName = parcel.readString();
        this.status = parcel.readInt();
    }

    public int getItemId() {
        return itemId;
    }

    public int getListsetId() {
        return listsetId;
    }

    public String getListName() {
        return listName;
    }

    public String getItemName() {
        return itemName;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(itemId);
        dest.writeInt(listsetId);
        dest.writeString(listName);
        dest.writeString(itemName);
        dest.writeInt(status);
    }
}
