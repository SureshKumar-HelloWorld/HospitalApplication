# Appointment Service

Handles doctor master data and appointment bookings. Publishes events to Kafka on appointment actions.

Run locally:

```bash
mvn -f appointment-service clean package
java -jar appointment-service/target/appointment-service-0.0.1-SNAPSHOT.jar
```
