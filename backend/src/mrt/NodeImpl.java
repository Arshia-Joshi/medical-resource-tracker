package mrt;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeImpl extends UnicastRemoteObject implements Node {

    private final int id;
    private volatile int leaderId = 1; // fixed for now, election comes in a later branch

    private final Map<String, StockEntry> stock = new HashMap<>();
    private final List<Reservation> reservations = new ArrayList<>();
    private long clock = 0; // Lamport logical clock of this node

    public NodeImpl(int id) throws RemoteException {
        super();
        this.id = id;
        seed();
    }

    // Called after the node is registered: copy the data from a running peer.
    public void start() {
        syncFromPeers();
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

    // ---------- talking to other nodes ----------

    // Returns null if the node cannot be reached.
    private Node peer(int peerId) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", Config.PORT_BASE + peerId);
            return (Node) registry.lookup("Node" + peerId);
        } catch (Exception e) {
            return null;
        }
    }

    private void syncFromPeers() {
        for (int peerId : Config.NODE_IDS) {
            if (peerId == id) continue;
            Node p = peer(peerId);
            if (p == null) continue;
            try {
                loadState(p.getState());
                System.out.println("Synced data from node " + peerId);
                return;
            } catch (RemoteException e) {
                // try the next peer
            }
        }
        System.out.println("No peer found, starting with seed data");
    }

    private synchronized void loadState(State s) {
        stock.clear();
        for (StockEntry e : s.stock) {
            stock.put(key(e.hospital, e.resource), e);
        }
        reservations.clear();
        reservations.addAll(s.reservations);
        clock = Math.max(clock, s.clock);
    }

    // Leader copies every change to all other nodes. A dead node is skipped,
    // it will sync from a peer when it is started again.
    private void replicateStockToPeers(StockEntry e) {
        for (int peerId : Config.NODE_IDS) {
            if (peerId == id) continue;
            Node p = peer(peerId);
            if (p == null) continue;
            try {
                p.replicateStock(e);
            } catch (RemoteException ex) {
                System.out.println("Could not replicate to node " + peerId);
            }
        }
    }

    private void replicateReservationToPeers(Reservation r, StockEntry e) {
        for (int peerId : Config.NODE_IDS) {
            if (peerId == id) continue;
            Node p = peer(peerId);
            if (p == null) continue;
            try {
                p.replicateReservation(r, e);
            } catch (RemoteException ex) {
                System.out.println("Could not replicate to node " + peerId);
            }
        }
    }

    // ---------- client calls (writes go to the leader) ----------

    public boolean updateStock(String hospital, String resource, int count, double distanceKm, long ts) throws RemoteException {
        if (leaderId != id) {
            Node leader = peer(leaderId);
            if (leader == null) throw new RemoteException("Leader node " + leaderId + " is not reachable");
            return leader.updateStock(hospital, resource, count, distanceKm, ts);
        }
        return applyUpdate(hospital, resource, count, distanceKm, ts);
    }

    // synchronized so two clients can never change the same stock at once
    private synchronized boolean applyUpdate(String hospital, String resource, int count, double distanceKm, long ts) {
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
        replicateStockToPeers(stock.get(key(hospital, resource)));
        return true;
    }

    public Reservation reserve(String hospital, String resource, long ts) throws RemoteException {
        if (leaderId != id) {
            Node leader = peer(leaderId);
            if (leader == null) throw new RemoteException("Leader node " + leaderId + " is not reachable");
            return leader.reserve(hospital, resource, ts);
        }
        return applyReserve(hospital, resource, ts);
    }

    // The check and the decrement happen in one synchronized block,
    // so two ambulances cannot both get the last bed.
    private synchronized Reservation applyReserve(String hospital, String resource, long ts) {
        StockEntry e = stock.get(key(hospital, resource));
        if (e == null || e.count <= 0) {
            return null;
        }
        e.count--;
        e.ts = tick(ts); // the reservation is a newer change than anything before it
        Reservation r = new Reservation("R" + (reservations.size() + 1), hospital, resource, e.ts, new java.util.Date().toString());
        reservations.add(r);
        System.out.println("Reserved: " + r.id + " " + hospital + " " + resource);
        replicateReservationToPeers(r, e);
        return r;
    }

    // ---------- reads (served from this node's own copy) ----------

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

    public synchronized List<StockEntry> getAllStock() {
        return new ArrayList<>(stock.values());
    }

    public synchronized List<Reservation> getReservations() {
        return new ArrayList<>(reservations);
    }

    // ---------- calls from other nodes ----------

    public int ping() {
        return id;
    }

    public int getLeaderId() {
        return leaderId;
    }

    public synchronized void replicateStock(StockEntry e) {
        tick(e.ts);
        StockEntry old = stock.get(key(e.hospital, e.resource));
        if (old == null || e.ts > old.ts) {
            stock.put(key(e.hospital, e.resource), e);
        }
    }

    public synchronized void replicateReservation(Reservation r, StockEntry e) {
        replicateStock(e);
        reservations.add(r);
    }

    public synchronized State getState() {
        return new State(new ArrayList<>(stock.values()), new ArrayList<>(reservations), clock);
    }
}
