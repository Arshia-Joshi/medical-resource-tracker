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
}
