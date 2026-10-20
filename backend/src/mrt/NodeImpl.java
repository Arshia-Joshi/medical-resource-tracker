package mrt;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeImpl extends UnicastRemoteObject implements Node {

    private final Map<String, StockEntry> stock = new HashMap<>();
    private final List<Reservation> reservations = new ArrayList<>();
    private long clock = 0; // Lamport logical clock of this node

    public NodeImpl() throws RemoteException {
        super();
        seed();
    }

    private String key(String hospital, String resource) {
        return hospital + "|" + resource;
    }

    private void put(String hospital, String resource, int count, double km, long ts) {
        stock.put(key(hospital, resource), new StockEntry(hospital, resource, count, km, ts));
    }

    private void seed() {
        String[] resources = {"ICU Bed", "Oxygen Cylinder", "Ventilator", "Blood Unit"};
        String[] hospitals = {"City General", "Sunrise Hospital", "Lifeline Medical", "Apollo Care"};
        double[] km = {2.5, 4.0, 6.5, 9.0};
        int[][] counts = {{5, 20, 3, 12}, {2, 10, 1, 8}, {0, 15, 2, 4}, {8, 30, 5, 20}};
        for (int h = 0; h < hospitals.length; h++) {
            for (int r = 0; r < resources.length; r++) {
                put(hospitals[h], resources[r], counts[h][r], km[h], 0);
            }
        }
    }

    // Lamport rule: on receiving a message, clock = max(local, received) + 1
    private long tick(long received) {
        clock = Math.max(clock, received) + 1;
        return clock;
    }

    // synchronized so two clients can never change the same stock at once
    public synchronized boolean updateStock(String hospital, String resource, int count, double distanceKm, long ts) {
        tick(ts);
        StockEntry old = stock.get(key(hospital, resource));
        if (old != null) {
            if (ts <= old.ts) {
                System.out.println("Stale update ignored: " + hospital + " " + resource + " ts=" + ts + " <= " + old.ts);
                return false;
            }
            distanceKm = old.distanceKm;
        }
        put(hospital, resource, Math.max(0, count), distanceKm, ts);
        System.out.println("Update: " + hospital + " " + resource + " = " + count + " (ts " + ts + ")");
        return true;
    }

    public synchronized List<StockEntry> searchResource(String resource) {
        List<StockEntry> result = new ArrayList<>();
        for (StockEntry e : stock.values()) {
            if (e.resource.equals(resource) && e.count > 0) {
                result.add(e);
            }
        }
        result.sort((a, b) -> Double.compare(a.distanceKm, b.distanceKm));
        return result;
    }

    // The check and the decrement happen in one synchronized block,
    // so two ambulances cannot both get the last bed.
    public synchronized Reservation reserve(String hospital, String resource, long ts) {
        StockEntry e = stock.get(key(hospital, resource));
        if (e == null || e.count <= 0) {
            return null;
        }
        e.count--;
        e.ts = tick(ts); // the reservation is a newer change than anything before it
        Reservation r = new Reservation("R" + (reservations.size() + 1), hospital, resource, e.ts, new java.util.Date().toString());
        reservations.add(r);
        System.out.println("Reserved: " + r.id + " " + hospital + " " + resource);
        return r;
    }

    public synchronized List<StockEntry> getAllStock() {
        return new ArrayList<>(stock.values());
    }

    public synchronized List<Reservation> getReservations() {
        return new ArrayList<>(reservations);
    }
}
