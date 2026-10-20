package mrt;

import java.io.Serializable;

// A confirmed reservation of one unit.
public class Reservation implements Serializable {
    public String id;
    public String hospital;
    public String resource;
    public String time;

    public Reservation(String id, String hospital, String resource, String time) {
        this.id = id;
        this.hospital = hospital;
        this.resource = resource;
        this.time = time;
    }
}
