import React, { useState } from 'react'
import Login from './pages/Login'
import Register from './pages/Register'
import Patients from './pages/Patients'
import Appointments from './pages/Appointments'

export default function App() {
  const [page, setPage] = useState<'login' | 'register' | 'patients' | 'appointments'>('login')

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand">
          <span className="brand-mark">H</span>
          <div>
            <h1>Health Management</h1>
            <p>Your care ecosystem in one place.</p>
          </div>
        </div>
        <nav className="nav-bar">
          <button className={page === 'login' ? 'active' : ''} onClick={() => setPage('login')}>Login</button>
          <button className={page === 'register' ? 'active' : ''} onClick={() => setPage('register')}>Register</button>
          <button className={page === 'patients' ? 'active' : ''} onClick={() => setPage('patients')}>Patients</button>
          <button className={page === 'appointments' ? 'active' : ''} onClick={() => setPage('appointments')}>Appointments</button>
        </nav>
      </header>
      <main className="main-panel">
        <section className="page-shell">
          {page === 'login' && <Login />}
          {page === 'register' && <Register />}
          {page === 'patients' && <Patients />}
          {page === 'appointments' && <Appointments />}
        </section>
      </main>
    </div>
  )
}
