package mrt;

import java.io.Serializable;

// One row of stock: how many units of a resource a hospital has.
public class StockEntry implements Serializable {
    public String hospital;
    public String resource;
    public int count;
    public double distanceKm;
    public long ts; // Lamport timestamp of the last change

    public StockEntry(String hospital, String resource, int count, double distanceKm, long ts) {
        this.hospital = hospital;
        this.resource = resource;
        this.count = count;
        this.distanceKm = distanceKm;
        this.ts = ts;
    }
}
