package me.robwilliams.pack.data;

import android.os.Parcel;
import android.os.Parcelable;

public class TripItem implements Parcelable {
    final private int itemId;
    final private int listsetId;
    final private String listName;
    final private String itemName;
    private int status;
    private int quantity;
    private int bagId;
    private String bagName;
    private String bagColor;
    private int bagHintId;

    public static final Parcelable.Creator<TripItem> CREATOR = new Parcelable.Creator<TripItem>() {
        public TripItem createFromParcel(Parcel in) {
            return new TripItem(in);
        }

        public TripItem[] newArray(int size) {
            return new TripItem[size];
        }
    };

    public TripItem(int itemId, int listsetId, String listName, String itemName, int status) {
        this(itemId, listsetId, listName, itemName, status, 1, 0, null, null, 0);
    }

    public TripItem(int itemId, int listsetId, String listName, String itemName, int status,
                    int quantity, int bagId, String bagName, String bagColor, int bagHintId) {
        this.itemId = itemId;
        this.listsetId = listsetId;
        this.listName = listName;
        this.itemName = itemName;
        this.status = status;
        this.quantity = quantity;
        this.bagId = bagId;
        this.bagName = bagName;
        this.bagColor = bagColor;
        this.bagHintId = bagHintId;
    }

    public TripItem(Parcel parcel) {
        this.itemId = parcel.readInt();
        this.listsetId = parcel.readInt();
        this.listName = parcel.readString();
        this.itemName = parcel.readString();
        this.status = parcel.readInt();
        this.quantity = parcel.readInt();
        this.bagId = parcel.readInt();
        this.bagName = parcel.readString();
        this.bagColor = parcel.readString();
        this.bagHintId = parcel.readInt();
    }

    public int getItemId() { return itemId; }
    public int getListsetId() { return listsetId; }
    public String getListName() { return listName; }
    public String getItemName() { return itemName; }
    public int getStatus() { return status; }
    public int getQuantity() { return quantity; }
    public int getBagId() { return bagId; }
    public String getBagName() { return bagName; }
    public String getBagColor() { return bagColor; }
    public int getBagHintId() { return bagHintId; }

    public void setStatus(int status) { this.status = status; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setBagId(int bagId) { this.bagId = bagId; }
    public void setBagName(String bagName) { this.bagName = bagName; }
    public void setBagColor(String bagColor) { this.bagColor = bagColor; }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(itemId);
        dest.writeInt(listsetId);
        dest.writeString(listName);
        dest.writeString(itemName);
        dest.writeInt(status);
        dest.writeInt(quantity);
        dest.writeInt(bagId);
        dest.writeString(bagName);
        dest.writeString(bagColor);
        dest.writeInt(bagHintId);
    }
}
