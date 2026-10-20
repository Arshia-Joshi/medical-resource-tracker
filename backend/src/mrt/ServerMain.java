package mrt;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServerMain {
    public static void main(String[] args) throws Exception {
        int id = Integer.parseInt(args[0]); // 1, 2 or 3
        Registry registry = LocateRegistry.createRegistry(Config.PORT_BASE + id);
        NodeImpl node = new NodeImpl(id);
        registry.rebind("Node" + id, node);
        System.out.println("Node " + id + " running on RMI port " + (Config.PORT_BASE + id));
        node.start();
    }
}
