import { useState } from 'react'
import { updateStock } from './api.js'
import { RESOURCES, ICONS } from './constants.js'

export default function HospitalPanel({ stock, refresh }) {
  const hospitals = [...new Set(stock.map((s) => s.hospital))]

  const [hospital, setHospital] = useState('')
  const [newName, setNewName] = useState('')
  const [distance, setDistance] = useState(5)
  const [resource, setResource] = useState(RESOURCES[0])
  const [count, setCount] = useState(1)
  const [oldTs, setOldTs] = useState('')
  const [msg, setMsg] = useState(null)

  const selected = hospital || hospitals[0] || ''
  const isNew = selected === '__new__'
  const name = isNew ? newName.trim() : selected

  async function submit(e) {
    e.preventDefault()
    if (!name) {
      setMsg({ type: 'error', text: 'Enter a hospital name' })
      return
    }
    try {
      const r = await updateStock(name, resource, count, distance, oldTs)
      if (r.accepted) {
        setMsg({ type: 'ok', text: `Stock updated (Lamport timestamp ${r.ts})` })
      } else {
        setMsg({ type: 'error', text: `Rejected: timestamp ${r.ts} is older than the stored one (stale update)` })
      }
      refresh()
    } catch (err) {
      setMsg({ type: 'error', text: err.message })
    }
  }

  const mine = stock.filter((s) => s.hospital === name)

  return (
    <div>
      <div className="card">
        <h2>🏥 Update hospital stock</h2>
        <form onSubmit={submit}>
          <label>
            Hospital
            <select value={selected} onChange={(e) => setHospital(e.target.value)}>
              {hospitals.map((h) => (
                <option key={h}>{h}</option>
              ))}
              <option value="__new__">+ New hospital</option>
            </select>
          </label>

          {isNew && (
            <div className="row">
              <label>
                Name
                <input value={newName} onChange={(e) => setNewName(e.target.value)} />
              </label>
              <label>
                Distance (km)
                <input type="number" step="0.1" value={distance} onChange={(e) => setDistance(e.target.value)} />
              </label>
            </div>
          )}

          <div className="row">
            <label>
              Resource
              <select value={resource} onChange={(e) => setResource(e.target.value)}>
                {RESOURCES.map((r) => (
                  <option key={r}>{r}</option>
                ))}
              </select>
            </label>
            <label>
              Units in stock
              <input type="number" min="0" value={count} onChange={(e) => setCount(e.target.value)} />
            </label>
          </div>

          <label>
            Timestamp override (optional, to demo a stale update, e.g. 5)
            <input value={oldTs} onChange={(e) => setOldTs(e.target.value)} placeholder="leave empty for normal update" />
          </label>

          <button type="submit">Update stock</button>
        </form>
        {msg && <div className={'msg ' + msg.type}>{msg.text}</div>}
      </div>

      {mine.length > 0 && (
        <div className="card">
          <h2>{name}: current stock</h2>
          <table>
            <thead>
              <tr>
                <th>Resource</th>
                <th>Units</th>
              </tr>
            </thead>
            <tbody>
              {mine.map((s) => (
                <tr key={s.resource}>
                  <td>{ICONS[s.resource]} {s.resource}</td>
                  <td>{s.count}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
