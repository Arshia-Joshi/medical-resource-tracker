import { useState } from 'react'
import { search, reserve } from './api.js'
import { RESOURCES } from './constants.js'

export default function PatientPanel({ reservations, refresh }) {
  const [resource, setResource] = useState(RESOURCES[0])
  const [results, setResults] = useState(null)
  const [msg, setMsg] = useState(null)

  async function doSearch(r = resource) {
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
    await doSearch()
    refresh()
  }

  return (
    <div>
      <div className="card">
        <h2>Find the nearest hospital</h2>
        <div className="row">
          <select value={resource} onChange={(e) => setResource(e.target.value)}>
            {RESOURCES.map((r) => (
              <option key={r}>{r}</option>
            ))}
          </select>
          <button onClick={() => doSearch()}>Search</button>
        </div>

        {msg && <div className={'msg ' + msg.type}>{msg.text}</div>}

        {results && results.length === 0 && <p>No hospital has {resource} right now.</p>}
        {results && results.length > 0 && (
          <table>
            <thead>
              <tr>
                <th>Hospital</th>
                <th>Distance</th>
                <th>Available</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {results.map((e) => (
                <tr key={e.hospital}>
                  <td>{e.hospital}</td>
                  <td>{e.distanceKm} km</td>
                  <td>{e.count}</td>
                  <td>
                    <button onClick={() => doReserve(e.hospital)}>Reserve</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="card">
        <h2>Reservations</h2>
        {reservations.length === 0 ? (
          <p>No reservations yet.</p>
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
                  <td>{r.id}</td>
                  <td>{r.hospital}</td>
                  <td>{r.resource}</td>
                  <td>{r.time}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
