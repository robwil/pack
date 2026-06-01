package me.robwilliams.pack.data;

import android.os.Parcel;
import android.os.Parcelable;

public class Bag implements Parcelable {
    private int id;
    private String name;
    private String color;

    public static final Parcelable.Creator<Bag> CREATOR = new Parcelable.Creator<Bag>() {
        public Bag createFromParcel(Parcel in) {
            return new Bag(in);
        }

        public Bag[] newArray(int size) {
            return new Bag[size];
        }
    };

    public Bag(int id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public Bag(Parcel parcel) {
        this.id = parcel.readInt();
        this.name = parcel.readString();
        this.color = parcel.readString();
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getColor() { return color; }

    public void setName(String name) { this.name = name; }
    public void setColor(String color) { this.color = color; }

    @Override
    public String toString() { return name; }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(color);
    }
}
