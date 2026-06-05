# Health Frontend

Minimal React + Vite frontend for the Health microservices.

Run locally:

```bash
cd frontend
npm install
npm run dev
```

Environment:
- `VITE_AUTH_BASE_URL` — base URL for auth endpoints (default: `http://localhost:8082`).
- `VITE_PATIENT_BASE_URL` — base URL for patient endpoints (default: `http://localhost:8082`).
- `VITE_APPOINTMENT_BASE_URL` — base URL for appointment endpoints (default: `http://localhost:8083`).

Endpoints used by the UI:
- `POST /api/auth/login` — login (auth service)
- `POST /api/auth/register` — register (auth service)
- `GET /api/patients` — list patients (patient service)
- `GET /api/appointments` — list appointments (appointment service)
- `POST /api/appointments` — book appointment (appointment service)

Note: if you want the frontend to talk to backends running on different ports, set all three environment variables before starting the dev server:

```powershell
$env:VITE_AUTH_BASE_URL="http://localhost:8081"
$env:VITE_PATIENT_BASE_URL="http://localhost:8082"
$env:VITE_APPOINTMENT_BASE_URL="http://localhost:8083"
npm run dev
```
