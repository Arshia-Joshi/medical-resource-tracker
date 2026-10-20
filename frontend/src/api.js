// Calls the Java gateway, which forwards them to the RMI nodes.
const BASE = 'http://localhost:8090/api'

async function request(path, method = 'GET') {
  const res = await fetch(BASE + path, { method })
  const data = await res.json()
  if (!res.ok) throw new Error(data.error || 'Request failed')
  return data
}

const q = (s) => encodeURIComponent(s)

export const getStock = () => request('/stock')
export const getReservations = () => request('/reservations')
export const getStatus = () => request('/status')
export const search = (resource) => request('/search?resource=' + q(resource))
export const reserve = (hospital, resource) =>
  request(`/reserve?hospital=${q(hospital)}&resource=${q(resource)}`, 'POST')
export const killNode = (id) => request('/kill?node=' + id, 'POST')

// ts is optional. Sending an old ts shows the Lamport stale-update check.
export function updateStock(hospital, resource, count, distanceKm, ts) {
  let path = `/update?hospital=${q(hospital)}&resource=${q(resource)}&count=${count}&distanceKm=${distanceKm}`
  if (ts) path += '&ts=' + ts
  return request(path, 'POST')
}
