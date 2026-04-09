# Movie Ticket Booking Platform

## Overview
A B2B/B2C online movie ticket booking platform enabling theatre partners to digitize operations and end customers to seamlessly browse and book movie tickets across cities, languages, and genres.

## Architecture
- **Architecture Type**: Microservices with Event-Driven Architecture
- **Language**: Java
- **Frameworks**: Spring Boot, Spring Cloud
- **Database**: PostgreSQL (Primary), Redis (Cache)
- **Cloud**: AWS/Multi-cloud capable
- **API Style**: REST with async capabilities

## Project Structure
```
├── design/
│   ├── hld/               # High Level Design documents
│   ├── lld/               # Low Level Design documents
│   └── diagrams/          # Architecture diagrams (Mermaid)
├── api-contracts/         # OpenAPI/Swagger specifications
├── backend/               # Java Spring Boot microservices
├── database/              # Schema and migrations
├── deployment/            # K8s, Docker configs
└── .github/               # GitHub Actions, etc.
```

## Key Features
### B2B (Theatre Partner)
- Theatre onboarding and management
- Show creation and management
- Seat inventory allocation
- Revenue analytics

### B2C (End Customer)
- Browse theatres and shows
- Seat selection and booking
- Dynamic pricing and offers
- Booking history

## Tech Stack
- **Backend**: Java 17+, Spring Boot 3.x, Spring Cloud
- **Database**: PostgreSQL, Redis
- **Message Queue**: RabbitMQ/Kafka
- **Container**: Docker
- **Orchestration**: Kubernetes
- **CI/CD**: GitHub Actions
- **Monitoring**: Prometheus + Grafana
- **Logging**: ELK Stack

## Quick Start
```bash
# Build
mvn clean install

# Run services
docker-compose up -d

# Access services
- Auth Service: http://localhost:8801
- Theatre Service: http://localhost:8802
- Show Service: http://localhost:8803
- Booking Service: http://localhost:8804
```

## Documentation
- [HLD Design](./design/hld/HLD.md)
- [LLD Design](./design/lld/LLD.md)
- [API Documentation](./api-contracts/README.md)
- [Database Schema](./database/schema/schema.md)

## Security
- OWASP Top 10 compliance
- OAuth 2.0 / OpenID Connect
- JWT authentication
- Data encryption (TLS 1.3)
- SQL Injection prevention
- CSRF protection

## Scalability & Reliability
- 99.99% uptime SLA (Multi-AZ deployment)
- Horizontal scaling via Kubernetes
- Database replication
- Load balancing
- Circuit breakers & retries
- Cache-first strategy

## Contributors
[Your Name]

## License
Apache License 2.0
