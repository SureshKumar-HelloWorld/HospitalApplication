## System Design (High level)

```mermaid
flowchart LR
  subgraph Gateway[API Gateway]
    GW[Spring Cloud Gateway]
  end

  subgraph Discovery[Eureka]
    E[Eureka Server]
  end

  GW -->|routes| UserService[User Service]
  GW -->|routes| AppointmentService[Appointment Service]
  GW -->|routes| ReportService[Report Service]
  GW -->|routes| NotificationService[Notification Service]

  UserService -->|publishes events| Kafka[(Kafka)]
  AppointmentService -->|publishes events| Kafka
  NotificationService -->|consumes events| Kafka

  ReportService -->|stores files| S3[(AWS S3)]

  UserService --> UserDB[(MySQL - userdb)]
  AppointmentService --> AppointmentDB[(MySQL - appointmentdb)]
  ReportService --> ReportDB[(MySQL - reportdb)]
  NotificationService --> NotificationDB[(MySQL - notificationdb)]

  subgraph Observability
    Prom[Prometheus]
    Graf[Grafana]
  end

  UserService --> Prom
  AppointmentService --> Prom
  ReportService --> Prom
  NotificationService --> Prom

  Prom --> Graf

  style GW fill:#f9f,stroke:#333,stroke-width:1px
```

Notes:
- Microservices communicate via REST and Kafka events.
- Gateway handles routing and JWT validation.
- Eureka provides service discovery.
- Config Server (not shown) holds central configuration.
