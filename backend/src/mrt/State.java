package mrt;

import java.io.Serializable;
import java.util.List;

// Full copy of a node's data, used to bring a restarted node up to date.
public class State implements Serializable {
    public List<StockEntry> stock;
    public List<Reservation> reservations;
    public long clock;

    public State(List<StockEntry> stock, List<Reservation> reservations, long clock) {
        this.stock = stock;
        this.reservations = reservations;
        this.clock = clock;
    }
}
