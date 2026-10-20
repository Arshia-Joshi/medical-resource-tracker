package mrt;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

// Remote interface of a server node.
public interface Node extends Remote {

    // Hospital sets its stock for a resource. Creates the hospital if it is new.
    // ts is the Lamport timestamp of the caller. Returns false if the update is older
    // than what the node already has (stale update is ignored).
    boolean updateStock(String hospital, String resource, int count, double distanceKm, long ts) throws RemoteException;

    // Hospitals that have the resource in stock, nearest first.
    List<StockEntry> searchResource(String resource) throws RemoteException;

    // Reserve one unit. Returns null if the hospital is out of stock.
    Reservation reserve(String hospital, String resource, long ts) throws RemoteException;

    List<StockEntry> getAllStock() throws RemoteException;

    List<Reservation> getReservations() throws RemoteException;

    // ---- used between nodes ----

    // Returns the id of this node. Used to check that a node is alive.
    int ping() throws RemoteException;

    int getLeaderId() throws RemoteException;

    // Leader pushes every change to the other nodes with these two calls.
    void replicateStock(StockEntry entry) throws RemoteException;

    void replicateReservation(Reservation reservation, StockEntry entry) throws RemoteException;

    // Bully algorithm: a lower node asks "are you alive?". A live node answers true
    // and starts its own election.
    boolean election(int fromId) throws RemoteException;

    // The winner of an election tells everyone that it is the leader.
    void coordinator(int leaderId) throws RemoteException;

    // Stops this node. Used to demo a node failure.
    void shutdown() throws RemoteException;

    // Full copy of the data, used by a restarted node to catch up.
    State getState() throws RemoteException;
}
