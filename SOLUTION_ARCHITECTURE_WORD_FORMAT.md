# Movie Ticket Booking Platform – Solution Architecture v1.0

**Confidential – Internal Assessment | Java 17 + Spring Boot 3 + Microservices + Kubernetes**

---

# MOVIE TICKET BOOKING PLATFORM

**End-to-End Architecture & Solution Document**  
**Domain Exercise – Booking Platform v1.0**  
**Technology Stack:** Java 17 + Spring Boot 3 + Microservices  
**Database:** PostgreSQL 15 (Primary) + Redis (Cache)  
**Cloud:** Kubernetes (Local/Cloud) + Docker  
**Observability:** Spring Actuator + Prometheus + Grafana + ELK Stack  
**API Management:** Spring Cloud Gateway + Kong  
**Architecture Style:** Event-Driven Microservices + CQRS + Saga

---

## 1. Architecture Overview

The Movie Ticket Booking Platform is designed as an event-driven microservices system deployed on Kubernetes, following Domain-Driven Design (DDD) principles. The architecture supports both B2B (theatre partners) and B2C (end customers) use cases with high availability and scalability.

### 1.1 Core Microservices

| Service | Responsibility | Technology | DB Schema |
|---------|---------------|------------|-----------|
| **Auth Service** | User registration, authentication, JWT management | Spring Boot + Spring Security + JWT | USER schema |
| **Theatre Service** | Theatre onboarding, screen & seat management | Spring Boot + JPA | THEATRE schema |
| **Show Service** | Show scheduling, availability management | Spring Boot + JPA + Redis | SHOW schema |
| **Booking Service** | Seat reservation, ticket lifecycle management | Spring Boot + Saga Pattern | BOOKING schema |
| **Payment Service** | Payment gateway integration, refunds | Spring Boot + Resilience4j | PAYMENT schema |
| **Notification Service** | Email/SMS/Push notifications | Spring Boot + Kafka | NOTIFICATION schema |
| **Offer Service** | Discount rules engine, dynamic pricing | Spring Boot + Drools | OFFER schema |
| **Search Service** | Movie/show discovery across cities | Spring Boot + Elasticsearch | ES Index |
| **Analytics Service** | Revenue reporting, KPI dashboard | Spring Boot + Kafka Streams | ANALYTICS schema |
| **API Gateway** | Routing, authentication, rate limiting | Spring Cloud Gateway | Stateless |

### 1.2 High-Level Architecture Diagram

```
Architecture Flow
[ Mobile / Web Client ]
        |
[ Kong API Gateway / Spring Cloud Gateway ] ──→ [ Static Assets (Nginx) ]
        |
[ API Gateway (REST) ]
        |
[ Spring Cloud Gateway ] ──→ [ Keycloak / JWT Auth ]
        |
┌────┴────────────────────────────────────────┐
|              MICROSERVICES LAYER             |
|                                             |
|  Auth │ Theatre │ Show │ Booking │ Payment  |
|  Offer │ Notification │ Search │ Analytics  |
|                                             |
└────────────────────┬────────────────────────┘
                     |
           [ Kafka Event Bus ] (Async Communication)
                     |
┌────────────────────┴────────────────────────┐
|                DATA LAYER                    |
|                                             |
| PostgreSQL (primary) │ Redis ElastiCache    |
| Elasticsearch │ MinIO (media)               |
|                                             |
└─────────────────────────────────────────────┘
                     |
    [ Observability: Prometheus + Grafana + ELK + Jaeger ]
    [ Infrastructure: Kubernetes + Docker + Helm + CI/CD ]
```

---

## 2. Microservices Design & API Contracts

### 2.1 Auth Service

**POST /api/v1/auth/register**

**Request Body (JSON):**
```json
{
  "email": "john@example.com",
  "password": "Hashed@123",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+91-9876543210",
  "userType": "CUSTOMER"
}
```

**Response 201 Created:**
```json
{
  "id": "user-uuid",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "userType": "CUSTOMER",
  "createdAt": "2026-04-07T10:00:00Z"
}
```

### 2.2 Theatre Service

**POST /api/v1/theatres (B2B Onboarding)**

**Request Body:**
```json
{
  "name": "PVR Cinemas",
  "city": "Mumbai",
  "address": "Forum Mall, Koramangala",
  "partnerId": "PRTNR-001",
  "screens": [
    {
      "screenNumber": 1,
      "totalSeats": 200,
      "seatLayout": {
        "rows": 10,
        "cols": 20
      }
    }
  ]
}
```

**Response 201:**
```json
{
  "id": "theatre-uuid",
  "name": "PVR Cinemas",
  "kycStatus": "PENDING",
  "status": "ACTIVE"
}
```

### 2.3 Show Service

**GET /api/v1/shows/search?movieId={id}&city={city}&date={date}**

**Response 200:**
```json
{
  "data": [
    {
      "id": "show-uuid",
      "movieTitle": "Avengers",
      "theatreName": "PVR Forum",
      "screenNumber": 1,
      "showTime": "14:30",
      "date": "2026-04-10",
      "language": "English",
      "availableSeats": 45,
      "basePrice": 250,
      "offers": [
        {
          "type": "AFTERNOON_DISCOUNT",
          "value": "20%"
        }
      ]
    }
  ]
}
```

### 2.4 Booking Service

**POST /api/v1/bookings/initiate**

**Request Body:**
```json
{
  "showId": "show-uuid",
  "numberOfSeats": 3
}
```

**Response 200:**
```json
{
  "bookingSessionId": "session-uuid",
  "showId": "show-uuid",
  "availableSeats": 80,
  "expiresAt": "2026-04-07T10:15:00Z"
}
```

**POST /api/v1/bookings/{bookingSessionId}/reserve-seats**

**Request Body:**
```json
{
  "seats": ["A1", "A2", "A3"],
  "offerId": "OFFER-50PCT-3RD"
}
```

**Response 200:**
```json
{
  "bookingSessionId": "session-uuid",
  "reservedSeats": ["A1", "A2", "A3"],
  "pricing": {
    "subtotal": 750,
    "discount": 175,
    "totalAmount": 575
  },
  "expiresAt": "2026-04-07T10:15:00Z"
}
```

**POST /api/v1/bookings/confirm**

**Request Body:**
```json
{
  "bookingSessionId": "session-uuid",
  "paymentTransactionId": "txn-uuid"
}
```

**Response 201:**
```json
{
  "bookingId": "booking-uuid",
  "bookingReference": "BK12345678",
  "status": "CONFIRMED",
  "totalAmount": 575,
  "bookingTime": "2026-04-07T10:10:00Z"
}
```

### 2.5 Offer Service – Discount Rules

| Offer Type | Rule | Discount | Priority |
|------------|------|----------|----------|
| **AFTERNOON_DISCOUNT** | Show time between 12:00 - 16:59 | 20% on ticket price | 2 |
| **THIRD_TICKET_DISCOUNT** | 3rd ticket in same booking | 50% on 3rd ticket only | 1 |
| **CITY_SPECIFIC** | Applicable city in offer config | Variable | 3 |
| **THEATRE_SPECIFIC** | Applicable theatre in offer config | Variable | 4 |

### 2.6 Payment Service

**POST /api/v1/payments/initiate**

**Request Body:**
```json
{
  "bookingSessionId": "session-uuid",
  "amount": 575.00,
  "currency": "INR",
  "paymentMethod": "CARD"
}
```

**Response 200:**
```json
{
  "paymentId": "pay-uuid",
  "paymentUrl": "https://payment-gateway.com/pay/pay-uuid",
  "expiresAt": "2026-04-07T10:20:00Z"
}
```

### 2.7 Notification Service

**POST /api/v1/notifications/send**

**Request Body:**
```json
{
  "type": "BOOKING_CONFIRMED",
  "recipient": "user@example.com",
  "channels": ["EMAIL", "SMS"],
  "templateData": {
    "bookingReference": "BK12345678",
    "movieTitle": "Avengers",
    "showTime": "2026-04-10T19:00:00Z"
  }
}
```

**Response 202:**
```json
{
  "notificationId": "notif-uuid",
  "status": "QUEUED",
  "channels": ["EMAIL", "SMS"]
}
```

### 2.8 Search Service

**GET /api/v1/search/movies?query={query}&city={city}&language={language}**

**Response 200:**
```json
{
  "movies": [
    {
      "id": "movie-uuid",
      "title": "Avengers: Endgame",
      "genre": "Action",
      "languages": ["English", "Hindi"],
      "rating": "PG-13",
      "posterUrl": "https://...",
      "shows": [
        {
          "theatreName": "PVR Forum",
          "showTimes": ["10:00", "13:30", "16:00"]
        }
      ]
    }
  ]
}
```

### 2.9 Analytics Service

**GET /api/v1/analytics/revenue?startDate={date}&endDate={date}&groupBy={theatre|movie|city}**

**Response 200:**
```json
{
  "period": {
    "startDate": "2026-04-01",
    "endDate": "2026-04-07"
  },
  "metrics": {
    "totalRevenue": 125000.00,
    "totalBookings": 500,
    "averageTicketPrice": 250.00
  },
  "breakdown": [
    {
      "group": "PVR Cinemas",
      "revenue": 75000.00,
      "bookings": 300
    }
  ]
}
```

---

## 3. Data Architecture

### 3.1 Database Schema Overview

The platform uses PostgreSQL as the primary database with the following key schemas:

- **USER Schema**: User management, authentication, roles
- **THEATRE Schema**: Theatre partners, screens, seat layouts
- **SHOW Schema**: Movie catalog, show schedules, pricing
- **BOOKING Schema**: Reservations, payments, seat inventory
- **PAYMENT Schema**: Transaction records, refunds
- **NOTIFICATION Schema**: Message templates, delivery status
- **OFFER Schema**: Discount rules, campaign management
- **ANALYTICS Schema**: Aggregated metrics, reporting data

### 3.2 Caching Strategy

- **Redis**: Session management, seat locks, rate limiting
- **Application Cache**: Movie metadata, theatre details
- **CDN**: Static assets (posters, images)

### 3.3 Event Streaming

- **Kafka Topics**: User events, booking events, payment events
- **Event Sourcing**: Audit trail, analytics aggregation
- **Saga Orchestration**: Distributed transactions across services

---

## 4. Deployment & Infrastructure

### 4.1 Containerization

- **Docker**: Service containerization
- **Multi-stage Builds**: Optimized images
- **Security Scanning**: Vulnerability assessment

### 4.2 Orchestration

- **Kubernetes**: Service deployment, scaling
- **Helm Charts**: Package management
- **ConfigMaps/Secrets**: Configuration management

### 4.3 CI/CD Pipeline

- **GitHub Actions**: Automated testing, building
- **ArgoCD**: GitOps deployment
- **Quality Gates**: Code coverage, security scans

### 4.4 Monitoring & Observability

- **Prometheus**: Metrics collection
- **Grafana**: Dashboards, alerting
- **ELK Stack**: Log aggregation, analysis
- **Jaeger**: Distributed tracing
- **Spring Actuator**: Health checks, metrics

---

## 5. Security Architecture

### 5.1 Authentication & Authorization

- **JWT Tokens**: Stateless authentication
- **OAuth 2.0/OIDC**: Third-party integration
- **Role-Based Access Control**: Fine-grained permissions

### 5.2 API Security

- **Rate Limiting**: DDoS protection
- **Input Validation**: SQL injection prevention
- **CORS**: Cross-origin resource sharing
- **HTTPS**: End-to-end encryption

### 5.3 Data Protection

- **Encryption at Rest**: Database encryption
- **PCI Compliance**: Payment data handling
- **GDPR Compliance**: Data privacy regulations

---

## 6. Performance & Scalability

### 6.1 Horizontal Scaling

- **Microservices**: Independent scaling
- **Database Sharding**: Data distribution
- **Load Balancing**: Traffic distribution

### 6.2 Caching Layers

- **Multi-level Cache**: Application, Redis, CDN
- **Cache Invalidation**: Event-driven updates
- **Read Replicas**: Database load distribution

### 6.3 Performance Optimization

- **Async Processing**: Non-blocking operations
- **Database Indexing**: Query optimization
- **CDN Integration**: Static asset delivery

---

This solution architecture document provides a comprehensive overview of the Movie Ticket Booking Platform, designed for high availability, scalability, and maintainability in a microservices environment.</content>
<parameter name="filePath">c:\Users\Sheeba Alif\workspace\movie-ticket-booking-platform\SOLUTION_ARCHITECTURE_WORD_FORMAT.md