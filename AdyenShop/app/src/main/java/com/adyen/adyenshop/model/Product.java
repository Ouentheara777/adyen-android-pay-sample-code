package com.adyen.adyenshop.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by andrei on 3/8/16.
 */
public class Product  implements Parcelable {

    private String name;
    private double price;
    private int photoId;

    public Product(String name, double price) {
        this.name = name;
        this.price = price;
    }

    public Product(String name, double price, int photoId) {
        this.name = name;
        this.price = price;
        this.photoId = photoId;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(name);
        out.writeDouble(price);
        out.writeInt(photoId);
    }

    private Product(Parcel in) {
        name = in.readString();
        price = in.readDouble();
        photoId = in.readInt();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getPhotoId() {
        return photoId;
    }

    public void setPhotoId(int photoId) {
        this.photoId = photoId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<Product> CREATOR = new Parcelable.Creator<Product>() {
        @Override
        public Product createFromParcel(Parcel in) {
            return new Product(in);
        }

        @Override
        public Product[] newArray(int size) {
            return new Product[size];
        }
    };
}
