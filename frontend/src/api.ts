import axios from 'axios'

const AUTH_BASE = import.meta.env.VITE_AUTH_BASE_URL ?? ''
const PATIENT_BASE = import.meta.env.VITE_PATIENT_BASE_URL ?? ''
const APPOINTMENT_BASE = import.meta.env.VITE_APPOINTMENT_BASE_URL ?? ''

const authApi = axios.create({
  baseURL: AUTH_BASE,
  headers: { 'Content-Type': 'application/json' }
})

const patientApi = axios.create({
  baseURL: PATIENT_BASE,
  headers: { 'Content-Type': 'application/json' }
})

const appointmentApi = axios.create({
  baseURL: APPOINTMENT_BASE,
  headers: { 'Content-Type': 'application/json' }
})

const addToken = (config: any) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers = { ...config.headers, Authorization: `Bearer ${token}` }
  }
  return config
}

authApi.interceptors.request.use(addToken)
patientApi.interceptors.request.use(addToken)
appointmentApi.interceptors.request.use(addToken)

export { authApi, patientApi, appointmentApi }
