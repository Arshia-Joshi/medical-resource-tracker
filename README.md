# Distributed Medical Resource Availability Tracker

Mini project for Distributed Computing System Laboratory (PCCoE, BIT27PC04).

Hospitals publish their stock of ICU beds, oxygen cylinders, ventilators and blood units.
Patients and ambulances search for the nearest hospital with stock and reserve a unit.
If a server node crashes, the others elect a new leader and the system keeps working.

**SDG 3** (Good Health and Well-being) and **SDG 9** (Industry, Innovation and Infrastructure).

## Architecture

```
React UI (5173) --HTTP--> Gateway (8090) --Java RMI--> Node 1 (1101)
                                                       Node 2 (1102)   <-- replicate / Bully election
                                                       Node 3 (1103)
```

- **Nodes** (`backend/src/mrt/NodeImpl.java`): three RMI servers holding the same data. Writes go to the leader and are copied to the others.
- **Gateway** (`Gateway.java`): browsers cannot call RMI, so this small HTTP server turns REST calls into RMI calls. It tries the nodes one by one, so it keeps working when a node is down.
- **React UI** (`frontend/`): Patient / Ambulance, Hospital and Cluster tabs.

## Concepts used (syllabus mapping)

| Concept | Where |
|---|---|
| Java RMI (Assignment 3) | `Node.java`, `NodeImpl.java` |
| Concurrency, locking | `synchronized` check-and-decrement in `applyReserve`, so the last bed cannot be reserved twice |
| Lamport logical clock (Assignment 5) | every update and reservation carries a timestamp, an older update is rejected as stale |
| Replication, consistency (Unit 4) | leader copies every change to the backups, a restarted node copies the full state (`State.java`) |
| Bully election (Assignment 6) | `startElection` / `becomeLeader` in `NodeImpl.java`, the highest live node id wins |
| Fault tolerance | each node pings the leader every second and starts an election if it is down |

## Run

Needs Java 17+ and Node 18+.

```powershell
scripts\start-all.ps1
```

This compiles the code and opens a window for each node, the gateway and the UI. Then open http://localhost:5173.

To start things by hand: `javac -d backend/out backend/src/mrt/*.java`, then `java -cp backend/out mrt.ServerMain 1` (and 2, 3), `java -cp backend/out mrt.Gateway`, and `npm run dev` inside `frontend`.

## Demo script

1. **Patient tab**: search "ICU Bed", the list is sorted by distance. Click Reserve, the count drops.
2. **Hospital tab**: update the stock of a hospital, or add a new hospital.
3. **Stale update (Lamport)**: in the Hospital tab type `5` in the timestamp field and submit. It is rejected because 5 is older than the stored timestamp.
4. **Cluster tab**: node 3 is the leader. Click "Stop node" on it. Within a few seconds node 2 becomes the leader and reservations still work.
5. Run `scripts\start-node.ps1 3`. Node 3 copies the data, wins the election (highest id) and becomes leader again, with no reservations lost.

## Test a node without the UI

```
java -cp backend/out mrt.ConsoleClient 1
```

## Gateway API

| Call | Purpose |
|---|---|
| `GET /api/stock` | all stock |
| `GET /api/search?resource=ICU Bed` | hospitals with stock, nearest first |
| `POST /api/update?hospital=&resource=&count=&distanceKm=&ts=` | set stock (`ts` optional) |
| `POST /api/reserve?hospital=&resource=` | reserve one unit |
| `GET /api/reservations` | all reservations |
| `GET /api/status` | node status and leader |
| `POST /api/kill?node=2` | stop a node (demo) |

Port 8090 is used because 8080 is often taken by other tools.
