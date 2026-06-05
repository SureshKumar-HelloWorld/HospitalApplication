import React, { useState } from 'react'
import { authApi } from '../api'

export default function Register() {
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [phoneNumber, setPhoneNumber] = useState('')
  const [msg, setMsg] = useState<string | null>(null)

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      await authApi.post('/api/auth/register', {
        firstName,
        lastName,
        email,
        password,
        phoneNumber
      })
      setMsg('Registered successfully. You can now login.')
    } catch (err: any) {
      setMsg(err?.response?.data?.message || 'Registration failed')
    }
  }

  return (
    <div className="card">
      <h2 className="page-title">Create account</h2>
      <p className="page-subtitle">Register with a username and password to access the health dashboard.</p>
      <form className="form-grid" onSubmit={submit}>
        <div className="form-group">
          <label>First Name</label>
          <input className="input-field" value={firstName} onChange={e => setFirstName(e.target.value)} placeholder="First name" />
        </div>
        <div className="form-group">
          <label>Last Name</label>
          <input className="input-field" value={lastName} onChange={e => setLastName(e.target.value)} placeholder="Last name" />
        </div>
        <div className="form-group">
          <label>Email</label>
          <input className="input-field" type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="Email address" />
        </div>
        <div className="form-group">
          <label>Password</label>
          <input className="input-field" type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="Choose a strong password" />
        </div>
        <div className="form-group">
          <label>Phone</label>
          <input className="input-field" value={phoneNumber} onChange={e => setPhoneNumber(e.target.value)} placeholder="Phone number (optional)" />
        </div>
        <button className="button-primary" type="submit">Register</button>
      </form>
      {msg && <p className={`message ${msg.includes('failed') ? 'error' : ''}`}>{msg}</p>}
    </div>
  )
}
