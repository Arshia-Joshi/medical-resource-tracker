package mrt;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

// Browsers cannot call RMI, so the React app talks to this small HTTP server
// and the gateway calls the RMI nodes. It is the client of the cluster.
public class Gateway {

    interface Call<T> {
        T run(Node node) throws Exception;
    }

    // Gateway's own Lamport clock. Seeded with the time so it keeps growing after a restart.
    static final AtomicLong clock = new AtomicLong(System.currentTimeMillis());

    static Node lookup(int id) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", Config.PORT_BASE + id);
            return (Node) registry.lookup("Node" + id);
        } catch (Exception e) {
            return null;
        }
    }

    // Try the nodes one by one. If all fail (for example during an election) wait and retry.
    static <T> T call(Call<T> c) throws Exception {
        Exception last = new Exception("No node is running");
        for (int attempt = 0; attempt < 3; attempt++) {
            for (int i = Config.NODE_IDS.length - 1; i >= 0; i--) {
                Node n = lookup(Config.NODE_IDS[i]);
                if (n == null) continue;
                try {
                    return c.run(n);
                } catch (Exception e) {
                    last = e;
                }
            }
            Thread.sleep(700);
        }
        throw last;
    }

    // ---------- JSON helpers ----------

    static String str(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    static String stockJson(StockEntry e) {
        return "{\"hospital\":" + str(e.hospital) + ",\"resource\":" + str(e.resource)
                + ",\"count\":" + e.count + ",\"distanceKm\":" + e.distanceKm + ",\"ts\":" + e.ts + "}";
    }

    static String stockListJson(List<StockEntry> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(stockJson(list.get(i)));
        }
        return sb.append("]").toString();
    }

    static String reservationJson(Reservation r) {
        return "{\"id\":" + str(r.id) + ",\"hospital\":" + str(r.hospital) + ",\"resource\":" + str(r.resource)
                + ",\"ts\":" + r.ts + ",\"time\":" + str(r.time) + "}";
    }

    static Map<String, String> params(HttpExchange ex) throws IOException {
        Map<String, String> map = new HashMap<>();
        String q = ex.getRequestURI().getRawQuery();
        if (q == null) return map;
        for (String pair : q.split("&")) {
            String[] kv = pair.split("=", 2);
            String v = kv.length > 1 ? kv[1] : "";
            map.put(URLDecoder.decode(kv[0], "UTF-8"), URLDecoder.decode(v, "UTF-8"));
        }
        return map;
    }

    static void send(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ---------- endpoints ----------

    static void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        try {
            if (ex.getRequestMethod().equals("OPTIONS")) {
                send(ex, 204, "");
                return;
            }
            Map<String, String> p = params(ex);

            if (path.equals("/api/stock")) {
                send(ex, 200, stockListJson(call(n -> n.getAllStock())));

            } else if (path.equals("/api/search")) {
                String resource = p.get("resource");
                send(ex, 200, stockListJson(call(n -> n.searchResource(resource))));

            } else if (path.equals("/api/update")) {
                String hospital = p.get("hospital");
                String resource = p.get("resource");
                int count = Integer.parseInt(p.get("count"));
                double km = Double.parseDouble(p.getOrDefault("distanceKm", "5"));
                long ts = p.containsKey("ts") ? Long.parseLong(p.get("ts")) : clock.incrementAndGet();
                boolean ok = call(n -> n.updateStock(hospital, resource, count, km, ts));
                send(ex, 200, "{\"accepted\":" + ok + ",\"ts\":" + ts + "}");

            } else if (path.equals("/api/reserve")) {
                String hospital = p.get("hospital");
                String resource = p.get("resource");
                long ts = clock.incrementAndGet();
                Reservation r = call(n -> n.reserve(hospital, resource, ts));
                if (r == null) {
                    send(ex, 409, "{\"error\":\"Out of stock\"}");
                } else {
                    send(ex, 200, reservationJson(r));
                }

            } else if (path.equals("/api/reservations")) {
                List<Reservation> list = call(n -> n.getReservations());
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(reservationJson(list.get(i)));
                }
                send(ex, 200, sb.append("]").toString());

            } else if (path.equals("/api/status")) {
                send(ex, 200, status());

            } else if (path.equals("/api/kill")) {
                int id = Integer.parseInt(p.get("node"));
                Node n = lookup(id);
                if (n == null) {
                    send(ex, 404, "{\"error\":\"Node " + id + " is not running\"}");
                } else {
                    n.shutdown();
                    send(ex, 200, "{\"killed\":" + id + "}");
                }

            } else {
                send(ex, 404, "{\"error\":\"Not found\"}");
            }
        } catch (Exception e) {
            send(ex, 503, "{\"error\":" + str(String.valueOf(e.getMessage())) + "}");
        }
    }

    // Which nodes are alive and who is the leader (as seen by each node).
    static String status() {
        StringBuilder sb = new StringBuilder("{\"nodes\":[");
        for (int i = 0; i < Config.NODE_IDS.length; i++) {
            int id = Config.NODE_IDS[i];
            boolean alive = false;
            int leader = -1;
            Node n = lookup(id);
            if (n != null) {
                try {
                    n.ping();
                    leader = n.getLeaderId();
                    alive = true;
                } catch (Exception e) {
                    // node is down
                }
            }
            if (i > 0) sb.append(",");
            sb.append("{\"id\":" + id + ",\"alive\":" + alive + ",\"leaderId\":" + leader + "}");
        }
        return sb.append("]}").toString();
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8090;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", Gateway::handle);
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("Gateway running on http://localhost:" + port);
    }
}
