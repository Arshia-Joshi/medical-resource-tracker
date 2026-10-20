import { useState } from 'react'
import { killNode } from './api.js'

export default function ClusterPanel({ stock, status, refresh }) {
  const [msg, setMsg] = useState(null)

  async function kill(id) {
    try {
      await killNode(id)
      setMsg({ type: 'ok', text: `Node ${id} stopped. Watch the other nodes elect a new leader.` })
    } catch (e) {
      setMsg({ type: 'error', text: e.message })
    }
    setTimeout(refresh, 1500)
  }

  const sorted = [...stock].sort((a, b) => a.hospital.localeCompare(b.hospital) || a.resource.localeCompare(b.resource))

  return (
    <div>
      <div className="card">
        <h2>Server nodes</h2>
        <div className="nodes">
          {status.nodes.map((n) => {
            const isLeader = n.alive && n.leaderId === n.id
            return (
              <div key={n.id} className={'node ' + (n.alive ? (isLeader ? 'leader' : 'up') : 'down')}>
                <h3>Node {n.id}</h3>
                <p>{!n.alive ? 'DOWN' : isLeader ? 'LEADER' : 'Backup'}</p>
                {n.alive && <p className="small">sees leader: node {n.leaderId}</p>}
                {n.alive && <button onClick={() => kill(n.id)}>Stop node</button>}
              </div>
            )
          })}
        </div>
        {msg && <div className={'msg ' + msg.type}>{msg.text}</div>}
        <p className="small">
          To bring a stopped node back, run <code>scripts\start-node.ps1 &lt;id&gt;</code>. It copies the data from a
          running node and takes part in the next election (the highest id wins).
        </p>
      </div>

      <div className="card">
        <h2>All stock (with Lamport timestamps)</h2>
        <table>
          <thead>
            <tr>
              <th>Hospital</th>
              <th>Resource</th>
              <th>Units</th>
              <th>Timestamp</th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((s) => (
              <tr key={s.hospital + s.resource}>
                <td>{s.hospital}</td>
                <td>{s.resource}</td>
                <td>{s.count}</td>
                <td>{s.ts}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
