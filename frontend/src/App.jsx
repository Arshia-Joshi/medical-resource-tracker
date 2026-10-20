import { useEffect, useState } from 'react'
import { getStock, getReservations, getStatus } from './api.js'
import HospitalPanel from './HospitalPanel.jsx'
import PatientPanel from './PatientPanel.jsx'
import ClusterPanel from './ClusterPanel.jsx'


export default function App() {
  const [tab, setTab] = useState('patient')
  const [stock, setStock] = useState([])
  const [reservations, setReservations] = useState([])
  const [status, setStatus] = useState({ nodes: [] })
  const [error, setError] = useState('')

  async function refresh() {
    try {
      setStock(await getStock())
      setReservations(await getReservations())
      setStatus(await getStatus())
      setError('')
    } catch (e) {
      setError(e.message)
    }
  }

  // reload the data every 2 seconds so all tabs show the live state
  useEffect(() => {
    refresh()
    const timer = setInterval(refresh, 2000)
    return () => clearInterval(timer)
  }, [])

  const aliveCount = status.nodes.filter((n) => n.alive).length

  return (
    <div>
      <header className="hero">
        <div className="hero-inner">
          <div className="logo">✚</div>
          <div>
            <h1>Medical Resource Tracker</h1>
            <p>Find ICU beds, oxygen, ventilators and blood in real time</p>
          </div>
          <div className="pills">
            <span className="pill">SDG 3 · Good Health</span>
            <span className="pill">{aliveCount} of 3 nodes online</span>
          </div>
        </div>
      </header>

      <div className="app">
      <div className="tabs">
        <button className={tab === 'patient' ? 'active' : ''} onClick={() => setTab('patient')}>
          🚑 Patient / Ambulance
        </button>
        <button className={tab === 'hospital' ? 'active' : ''} onClick={() => setTab('hospital')}>
          🏥 Hospital
        </button>
        <button className={tab === 'cluster' ? 'active' : ''} onClick={() => setTab('cluster')}>
          🖥️ Cluster
        </button>
      </div>

      {error && <div className="msg error">Cannot reach the gateway: {error}</div>}

      {tab === 'patient' && <PatientPanel reservations={reservations} refresh={refresh} />}
      {tab === 'hospital' && <HospitalPanel stock={stock} refresh={refresh} />}
      {tab === 'cluster' && <ClusterPanel stock={stock} status={status} refresh={refresh} />}
      </div>
    </div>
  )
}
