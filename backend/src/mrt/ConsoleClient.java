package mrt;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

// Small command line client used to test a node without the web UI.
// Usage: ConsoleClient <nodeId>
public class ConsoleClient {
    public static void main(String[] args) throws Exception {
        int id = args.length > 0 ? Integer.parseInt(args[0]) : 1;
        Registry registry = LocateRegistry.getRegistry("localhost", Config.PORT_BASE + id);
        Node node = (Node) registry.lookup("Node" + id);

        System.out.println("Node " + id + " thinks the leader is node " + node.getLeaderId());
        System.out.println("Hospitals with ICU Bed (nearest first):");
        for (StockEntry e : node.searchResource("ICU Bed")) {
            System.out.println("  " + e.hospital + " " + e.distanceKm + "km count=" + e.count);
        }
        long ts = System.currentTimeMillis();
        Reservation r = node.reserve("City General", "ICU Bed", ts);
        System.out.println(r == null ? "Out of stock" : "Reserved " + r.id + " ts=" + r.ts);
        System.out.println("Fresh update accepted: " + node.updateStock("City General", "ICU Bed", 10, 2.5, ts + 10));
        System.out.println("Stale update accepted: " + node.updateStock("City General", "ICU Bed", 0, 2.5, 50));
    }
}
