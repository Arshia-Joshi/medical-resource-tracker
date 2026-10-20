package mrt;

// Fixed cluster layout shared by the nodes and the gateway.
public class Config {
    public static final int[] NODE_IDS = {1, 2, 3};
    public static final int PORT_BASE = 1100; // node 1 -> 1101, node 2 -> 1102 ...
}
