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

    public NodeImpl() throws RemoteException {
        super();
        seed();
    }

    private String key(String hospital, String resource) {
        return hospital + "|" + resource;
    }

    private void put(String hospital, String resource, int count, double km) {
        stock.put(key(hospital, resource), new StockEntry(hospital, resource, count, km));
    }

    private void seed() {
        String[] resources = {"ICU Bed", "Oxygen Cylinder", "Ventilator", "Blood Unit"};
        String[] hospitals = {"City General", "Sunrise Hospital", "Lifeline Medical", "Apollo Care"};
        double[] km = {2.5, 4.0, 6.5, 9.0};
        int[][] counts = {{5, 20, 3, 12}, {2, 10, 1, 8}, {0, 15, 2, 4}, {8, 30, 5, 20}};
        for (int h = 0; h < hospitals.length; h++) {
            for (int r = 0; r < resources.length; r++) {
                put(hospitals[h], resources[r], counts[h][r], km[h]);
            }
        }
    }

    // synchronized so two clients can never change the same stock at once
    public synchronized boolean updateStock(String hospital, String resource, int count, double distanceKm) {
        StockEntry old = stock.get(key(hospital, resource));
        if (old != null) {
            distanceKm = old.distanceKm;
        }
        put(hospital, resource, Math.max(0, count), distanceKm);
        System.out.println("Update: " + hospital + " " + resource + " = " + count);
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
    public synchronized Reservation reserve(String hospital, String resource) {
        StockEntry e = stock.get(key(hospital, resource));
        if (e == null || e.count <= 0) {
            return null;
        }
        e.count--;
        Reservation r = new Reservation("R" + (reservations.size() + 1), hospital, resource, new java.util.Date().toString());
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
