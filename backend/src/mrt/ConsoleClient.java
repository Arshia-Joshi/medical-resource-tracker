package mrt;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

// Small command line client used to test a node without the web UI.
public class ConsoleClient {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 1101;
        Registry registry = LocateRegistry.getRegistry("localhost", port);
        Node node = (Node) registry.lookup("Node");

        System.out.println("Hospitals with ICU Bed (nearest first):");
        for (StockEntry e : node.searchResource("ICU Bed")) {
            System.out.println("  " + e.hospital + " " + e.distanceKm + "km count=" + e.count);
        }
        Reservation r = node.reserve("City General", "ICU Bed", 1);
        System.out.println(r == null ? "Out of stock" : "Reserved " + r.id + " ts=" + r.ts);

        // Lamport check: an update with an old timestamp must be rejected
        System.out.println("Fresh update accepted: " + node.updateStock("City General", "ICU Bed", 10, 2.5, 100));
        System.out.println("Stale update accepted: " + node.updateStock("City General", "ICU Bed", 0, 2.5, 50));
    }
}
