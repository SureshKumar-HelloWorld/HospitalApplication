import React, { useEffect, useState } from 'react'
import { patientApi } from '../api'

export default function Patients() {
  const [patients, setPatients] = useState<any[]>([])
  const [err, setErr] = useState<string | null>(null)

  useEffect(() => {
    patientApi.get('/api/patients')
      .then(res => setPatients(res.data))
      .catch(e => setErr(e?.response?.data?.message || 'Could not load patients'))
  }, [])

  return (
    <div className="card">
      <h2 className="page-title">Patient directory</h2>
      <p className="page-subtitle">View the list of patients registered in the system.</p>
      {err ? (
        <p className="message error">{err}</p>
      ) : patients.length === 0 ? (
        <p className="no-data">No patients available. Login and register data to see records.</p>
      ) : (
        <div className="list-grid">
          {patients.map(p => (
            <div key={p.id} className="list-card">
              <h3>{p.fullName || p.name || p.username}</h3>
              <p>ID: {p.id}</p>
              <p>Status: {p.status || 'Active'}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
