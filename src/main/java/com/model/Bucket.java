package com.model;

import org.apache.commons.math3.util.Precision;

import java.util.Objects;

public class Bucket {
    private final double price;
    private final double size;

    public Bucket(double price) {
        this.price = price;
        this.size = 0;
    }

    public Bucket(double price, double size) {
        this.price = price;
        this.size = size;
    }

    public double getPrice() {
        return price;
    }

    public double getSize() {
        return size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bucket bucket = (Bucket) o;
        return Precision.equals(bucket.price, price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(price);
    }

    @Override
    public String toString() {
        return "Bucket{" +
                "price=" + price +
                ", size=" + size +
                '}';
    }
}
