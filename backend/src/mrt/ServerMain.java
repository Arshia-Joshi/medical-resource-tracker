package mrt;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerMain {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 1101;
        Registry registry = LocateRegistry.createRegistry(port);
        registry.rebind("Node", new NodeImpl());
        System.out.println("Node running on RMI port " + port);
    }
}
