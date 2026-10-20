import { useState } from 'react'
import { search, reserve } from './api.js'
import { RESOURCES, ICONS } from './constants.js'

export default function PatientPanel({ reservations, refresh }) {
  const [resource, setResource] = useState(null)
  const [results, setResults] = useState(null)
  const [msg, setMsg] = useState(null)

  async function doSearch(r) {
    setResource(r)
    try {
      setResults(await search(r))
    } catch (e) {
      setMsg({ type: 'error', text: e.message })
    }
  }

  async function doReserve(hospital) {
    try {
      const r = await reserve(hospital, resource)
      setMsg({ type: 'ok', text: `Reserved ${resource} at ${hospital} (reservation ${r.id})` })
    } catch (e) {
      setMsg({ type: 'error', text: e.message })
    }
    await doSearch(resource)
    refresh()
  }

  return (
    <div>
      <div className="card">
        <h2>What do you need?</h2>
        <div className="chips">
          {RESOURCES.map((r) => (
            <button key={r} className={'chip' + (resource === r ? ' selected' : '')} onClick={() => doSearch(r)}>
              <span className="chip-icon">{ICONS[r]}</span>
              {r}
            </button>
          ))}
        </div>

        {msg && <div className={'msg ' + msg.type}>{msg.text}</div>}

        {results && results.length === 0 && <p className="empty">No hospital has {resource} right now.</p>}

        {results && results.length > 0 && (
          <div className="results">
            {results.map((e, i) => (
              <div key={e.hospital} className="result">
                <div className="rank">{i + 1}</div>
                <div className="result-info">
                  <strong>{e.hospital}</strong>
                  <span className="small">📍 {e.distanceKm} km away</span>
                </div>
                <span className={'badge ' + (e.count <= 3 ? 'low' : 'good')}>{e.count} available</span>
                <button onClick={() => doReserve(e.hospital)}>Reserve</button>
              </div>
            ))}
          </div>
        )}
        {!results && <p className="empty">Choose a resource above to see the nearest hospitals.</p>}
      </div>

      <div className="card">
        <h2>Reservations</h2>
        {reservations.length === 0 ? (
          <p className="empty">No reservations yet.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Hospital</th>
                <th>Resource</th>
                <th>Time</th>
              </tr>
            </thead>
            <tbody>
              {[...reservations].reverse().map((r) => (
                <tr key={r.id}>
                  <td>
                    <span className="badge good">{r.id}</span>
                  </td>
                  <td>{r.hospital}</td>
                  <td>
                    {ICONS[r.resource]} {r.resource}
                  </td>
                  <td className="small">{r.time}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
