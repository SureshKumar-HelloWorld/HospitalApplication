# Hospital Application

A cloud-native Hospital Management System built using a microservices architecture.

## Overview

This project provides a scalable hospital management platform for managing users, patients, and appointments through independently deployable services.

## Architecture

The application consists of the following components:

### Services

| Service | Description |
|-----------|-------------|
| user-service | User authentication and management |
| patient-service | Patient registration and records |
| appointment-service | Appointment scheduling and management |
| frontend | User interface for hospital operations |

### Infrastructure

- Kubernetes deployment manifests
- GitHub Actions CI/CD workflows
- Centralized observability support
- Containerized microservices architecture

## Project Structure

```text
HospitalApplication
├── appointment-service/
├── patient-service/
├── user-service/
├── frontend/
├── k8s/
├── .github/workflows/
├── ARCHITECTURE.md
├── DEVELOPER_GUIDE.md
└── README.md
```

## Technology Stack

### Backend
- Java
- Spring Boot
- Spring Data JPA
- REST APIs

### Frontend
- Modern Web Frontend

### DevOps
- Docker
- Kubernetes
- GitHub Actions

### Observability
- OpenTelemetry

## Getting Started

### Clone Repository

```bash
git clone https://github.com/SureshKumar-HelloWorld/HospitalApplication.git
cd HospitalApplication
```

### Start Backend Services

Start each microservice independently:

```bash
cd user-service
```

```bash
mvn spring-boot:run
```

Repeat for:

- patient-service
- appointment-service

### Start Frontend

Navigate to the frontend directory and run the corresponding startup command.

## Kubernetes Deployment

Deployment manifests are available in the `k8s` directory.

```bash
kubectl apply -f k8s/
```

## CI/CD

GitHub Actions workflows are configured under:

```text
.github/workflows/
```

These workflows automate build, test, and deployment processes.

## Documentation

Additional documentation is available:

- ARCHITECTURE.md
- DEVELOPER_GUIDE.md
- RCA_DEMO.md

## Features

- User Management
- Patient Management
- Appointment Scheduling
- RESTful APIs
- Microservices Architecture
- Kubernetes Deployment
- CI/CD Automation
- Observability Integration

## Future Enhancements

- Billing Service
- Notification Service
- Medical Records Service
- API Gateway
- Service Discovery
- Distributed Tracing Dashboard

## Author

Suresh Kumar

GitHub:
https://github.com/SureshKumar-HelloWorld

## License

This project is intended for learning, demonstration, and development purposes.
