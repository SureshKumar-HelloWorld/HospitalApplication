import React, { useState } from 'react'
import { authApi } from '../api'

export default function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [msg, setMsg] = useState<string | null>(null)

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      const res = await authApi.post('/api/auth/login', { email, password })
      localStorage.setItem('token', res.data.token || res.data.accessToken || '')
      setMsg('Logged in successfully')
    } catch (err: any) {
      setMsg(err?.response?.data?.message || 'Login failed')
    }
  }

  return (
    <div className="card">
      <h2 className="page-title">Sign in</h2>
      <p className="page-subtitle">Use your account credentials to access patient and appointment data.</p>
      <form className="form-grid" onSubmit={submit}>
        <div className="form-group">
          <label>Email</label>
          <input className="input-field" type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="Enter your email" />
        </div>
        <div className="form-group">
          <label>Password</label>
          <input className="input-field" type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="Enter your password" />
        </div>
        <button className="button-primary" type="submit">Login</button>
      </form>
      {msg && <p className={`message ${msg.includes('failed') ? 'error' : ''}`}>{msg}</p>}
    </div>
  )
}
