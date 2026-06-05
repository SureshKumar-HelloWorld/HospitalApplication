import React, { useEffect, useState } from 'react'
import { appointmentApi } from '../api'

export default function Appointments() {
  const [appointments, setAppointments] = useState<any[]>([])
  const [doctorId, setDoctorId] = useState('')
  const [dateTime, setDateTime] = useState('')
  const [msg, setMsg] = useState<string | null>(null)

  useEffect(() => {
    appointmentApi.get('/api/appointments')
      .then(res => setAppointments(res.data))
      .catch(() => setMsg('Could not fetch appointments'))
  }, [])

  const book = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      await appointmentApi.post('/api/appointments', { doctorId, dateTime })
      setMsg('Appointment booked successfully')
    } catch (err: any) {
      setMsg(err?.response?.data?.message || 'Booking failed')
    }
  }

  return (
    <div className="card">
      <h2 className="page-title">Book a visit</h2>
      <p className="page-subtitle">Schedule a new appointment with your doctor or review upcoming sessions.</p>
      <form className="form-grid" onSubmit={book}>
        <div className="form-group">
          <label>Doctor ID</label>
          <input className="input-field" value={doctorId} onChange={e => setDoctorId(e.target.value)} placeholder="12345" />
        </div>
        <div className="form-group">
          <label>DateTime (ISO)</label>
          <input className="input-field" value={dateTime} onChange={e => setDateTime(e.target.value)} placeholder="2026-06-01T10:00:00" />
        </div>
        <button className="button-primary" type="submit">Book appointment</button>
      </form>
      {msg && <p className={`message ${msg.includes('failed') ? 'error' : ''}`}>{msg}</p>}
      <div className="list-grid" style={{ marginTop: '24px' }}>
        {appointments.length === 0 ? (
          <p className="no-data">No upcoming appointments yet.</p>
        ) : (
          appointments.map(a => (
            <div key={a.id} className="list-card">
              <h3>{a.doctorName || `Doctor ${a.doctorId}`}</h3>
              <p>{new Date(a.dateTime).toLocaleString()}</p>
              <p>Status: {a.status || 'Pending'}</p>
            </div>
          ))
        )}
      </div>
    </div>
  )
}
