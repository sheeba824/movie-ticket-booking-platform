# Movie Ticket Booking Platform – Solution Architecture

**Confidential – Internal Assessment | Java 17 + Spring Boot 3 + Microservices + AWS**

---

# MOVIE TICKET BOOKING PLATFORM

**End-to-End Architecture & Solution Document**

**Domain Exercise – Booking Platform v1.0**

**Technology Stack:** Java 17 + Spring Boot 3 + Microservices

**Database:** Oracle DB 19c (Primary) + Redis (Cache)

**Cloud:** AWS (EKS, RDS, API Gateway, S3, SQS, SNS)

**Observability:** Spring Actuator + Grafana + ELK Stack

**API Management:** AWS API Gateway + Spring Cloud Gateway

**Architecture Style:** Event-Driven Microservices + CQRS + Saga

---

## 1. Architecture Overview

The Movie Ticket Booking Platform is designed as an event-driven microservices system deployed on AWS, following Domain-Driven Design (DDD) principles. The architecture supports both B2B (theatre partners) and B2C (end customers) use cases with **99.99% availability**.

### 1.1 Core Microservices

| Service | Responsibility | Technology | DB Schema |
|---------|---|---|---|
| **User Service** | Registration, Auth, Profile | Spring Boot + Spring Security + JWT | USER schema |
| **Theatre Service** | Theatre onboarding, screen & seat mgmt | Spring Boot + JPA | THEATRE schema |
| **Movie Service** | Movie catalog, metadata, localization | Spring Boot + JPA | MOVIE schema |
| **Show Service** | Show scheduling, availability | Spring Boot + JPA + Redis | SHOW schema |
| **Booking Service** | Seat reservation, ticket lifecycle | Spring Boot + Saga Pattern | BOOKING schema |
| **Payment Service** | Payment gateway integration, refunds | Spring Boot + Resilience4j | PAYMENT schema |
| **Notification Service** | Email/SMS/Push via SNS | Spring Boot + AWS SNS | NOTIFICATION schema |
| **Offer Service** | Discount rules engine | Spring Boot + Drools | OFFER schema |
| **Search Service** | Movie/show discovery across cities | Spring Boot + Elasticsearch | ES Index |
| **API Gateway Service** | Routing, Auth, Rate limiting | Spring Cloud Gateway | Stateless |

### 1.2 High-Level Architecture Diagram (Text)

```
Architecture Flow
[ Mobile / Web Client ]
        |
[ AWS CloudFront CDN ] ──→ [ S3 Static Assets ]
        |
[ AWS API Gateway (REST/GraphQL) ]
        |
[ Spring Cloud Gateway ] ──→ [ Keycloak / JWT Auth ]
        |
┌────┴────────────────────────────────────────┐
|          MICROSERVICES LAYER                 |
|                                             |
|  User │ Theatre │ Movie │ Show │ Booking    |
|  Payment │ Offer │ Notification │ Search    |
|                                             |
└────────────────────┬────────────────────────┘
                     |
           [ AWS SQS / SNS Event Bus ] (Async Communication)
                     |
┌────────────────────┴────────────────────────┐
|                DATA LAYER                    |
|                                             |
| Oracle RDS (primary) │ Redis ElastiCache    |
| Elasticsearch │ S3 (media) │ DynamoDB       |
|                                             |
└─────────────────────────────────────────────┘
                     |
    [ Observability: ELK + Grafana + Spring Actuator ]
    [ Infrastructure: AWS EKS + ECR + Terraform + CI/CD ]
```

---

## 2. Microservices Design & API Contracts

### 2.1 User Service

**POST /api/v1/auth/register**

**Request Body (JSON):**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "mobile": "+91-9876543210",
  "password": "Hashed@123",
  "city": "Bengaluru"
}
```

**Response 201 Created:**
```json
{
  "userId": "USR-001",
  "token": "eyJhbGciOiJIUzI1NiIsInR5..."
}
```

### 2.2 Theatre Service

**POST /api/v1/theatres (B2B Onboarding)**

**Request Body:**
```json
{
  "theatreName": "PVR Cinemas",
  "city": "Bengaluru",
  "address": "Forum Mall, Koramangala",
  "partnerId": "PRTNR-001",
  "screens": [
    {
      "screenName": "Screen 1",
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
  "theatreId": "THTR-001",
  "status": "PENDING_APPROVAL"
}
```

### 2.3 Show Service

**GET /api/v1/shows?movieId={id}&city={city}&date={date}**

**Response 200:**
```json
{
  "shows": [
    {
      "showId": "SHW-001",
      "movieTitle": "Avengers",
      "theatreName": "PVR Forum",
      "screenName": "Screen 1",
      "showTime": "14:30",
      "date": "2026-04-01",
      "language": "English",
      "availableSeats": 45,
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

**POST /api/v1/bookings (Book Tickets)**

**Request Body:**
```json
{
  "showId": "SHW-001",
  "userId": "USR-001",
  "seats": ["A1", "A2", "A3"],
  "offerId": "OFFER-50PCT-3RD"
}
```

**Response 200:**
```json
{
  "bookingId": "BKG-20260401-001",
  "status": "RESERVED",
  "amount": 450.00,
  "discountApplied": 150.00,
  "paymentUrl": "https://pay.xyz.com/checkout/BKG-001",
  "expiresAt": "2026-04-01T14:45:00Z"
}
```

**POST /api/v1/bookings/bulk**

**Request Body:**
```json
{
  "showId": "SHW-001",
  "userId": "USR-001",
  "bookings": [
    {
      "seats": ["A1", "A2"],
      "attendee": "John"
    },
    {
      "seats": ["B1", "B2"],
      "attendee": "Jane"
    }
  ]
}
```

**Response:**
```json
{
  "bulkBookingId": "BULK-001",
  "bookings": [...],
  "totalAmount": 900.00
}
```

### 2.5 Offer Service – Discount Rules

| Offer Type | Rule | Discount | Priority |
|---|---|---|---|
| **AFTERNOON_DISCOUNT** | Show time between 12:00 - 16:59 | 20% on ticket price | 2 |
| **THIRD_TICKET_DISCOUNT** | 3rd ticket in same booking | 50% on 3rd ticket only | 1 |
| **CITY_SPECIFIC** | Applicable city in offer config | Variable | 3 |
| **THEATRE_SPECIFIC** | Applicable theatre in offer config | Variable | 4 |

---

## 3. Database Design – Oracle DB

### 3.1 Core Entity Relationship Overview

**Oracle Schema Strategy**

- **Schema:** BOOKING_DB (Oracle 19c)
- **Partitioning:** SHOWS and BOOKINGS tables partitioned by DATE (Range Partitioning)
- **Indexing:** Composite indexes on (city, date, movie_id), (show_id, seat_id, status)
- **Sequences:** Used for all primary key generation (Oracle SEQUENCE objects)
- **Audit:** All tables include CREATED_AT, UPDATED_AT, CREATED_BY, VERSION (optimistic lock)

### 3.2 DDL – Key Tables

**USERS TABLE:**
```sql
CREATE TABLE users (
    user_id VARCHAR2(36) DEFAULT SYS_GUID() PRIMARY KEY,
    name VARCHAR2(100) NOT NULL,
    email VARCHAR2(150) UNIQUE NOT NULL,
    mobile VARCHAR2(20),
    city VARCHAR2(50),
    status VARCHAR2(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP,
    version NUMBER DEFAULT 0
);
```

**THEATRES TABLE:**
```sql
CREATE TABLE theatres (
    theatre_id VARCHAR2(36) DEFAULT SYS_GUID() PRIMARY KEY,
    partner_id VARCHAR2(36) NOT NULL,
    name VARCHAR2(200) NOT NULL,
    city VARCHAR2(50) NOT NULL,
    address VARCHAR2(500),
    status VARCHAR2(20) DEFAULT 'PENDING_APPROVAL',
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP
);
```

**SHOWS TABLE (Range partitioned by show_date):**
```sql
CREATE TABLE shows (
    show_id VARCHAR2(36) DEFAULT SYS_GUID(),
    theatre_id VARCHAR2(36) NOT NULL,
    screen_id VARCHAR2(36) NOT NULL,
    movie_id VARCHAR2(36) NOT NULL,
    show_date DATE NOT NULL,
    show_time VARCHAR2(10) NOT NULL,
    language VARCHAR2(30),
    status VARCHAR2(20) DEFAULT 'SCHEDULED',
    available_seats NUMBER DEFAULT 0,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP
) PARTITION BY RANGE (show_date) INTERVAL (NUMTODSINTERVAL(30,'DAY'))
(PARTITION p_initial VALUES LESS THAN (DATE '2026-01-01'));
```

**SEAT_INVENTORY TABLE:**
```sql
CREATE TABLE seat_inventory (
    seat_id VARCHAR2(36) DEFAULT SYS_GUID() PRIMARY KEY,
    show_id VARCHAR2(36) NOT NULL,
    seat_label VARCHAR2(10) NOT NULL,
    category VARCHAR2(20), -- GOLD, SILVER, PLATINUM
    status VARCHAR2(20) DEFAULT 'AVAILABLE', -- AVAILABLE, LOCKED, BOOKED
    locked_by VARCHAR2(36),
    locked_until TIMESTAMP,
    version NUMBER DEFAULT 0
);
```

**BOOKINGS TABLE:**
```sql
CREATE TABLE bookings (
    booking_id VARCHAR2(36) DEFAULT SYS_GUID() PRIMARY KEY,
    user_id VARCHAR2(36) NOT NULL,
    show_id VARCHAR2(36) NOT NULL,
    status VARCHAR2(30) DEFAULT 'RESERVED',
    total_amount NUMBER(10,2) NOT NULL,
    discount NUMBER(10,2) DEFAULT 0,
    net_amount NUMBER(10,2) NOT NULL,
    offer_id VARCHAR2(36),
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP,
    expires_at TIMESTAMP,
    version NUMBER DEFAULT 0
);
```

**BOOKING_SEATS TABLE:**
```sql
CREATE TABLE booking_seats (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_id VARCHAR2(36) NOT NULL,
    seat_id VARCHAR2(36) NOT NULL,
    seat_label VARCHAR2(10) NOT NULL,
    ticket_price NUMBER(10,2),
    discount NUMBER(10,2) DEFAULT 0
);
```

---

## 4. Design Patterns

### 4.1 Saga Pattern – Distributed Booking Transaction

The booking flow spans multiple services (Booking, Payment, Seat Inventory, Notification). Implementation uses Choreography-based Saga via AWS SQS/SNS for eventual consistency.

**Booking Saga Flow:**

1. **BookingService** → RESERVE seats (SEAT_INVENTORY: LOCKED)
2. **BookingService** → Publish 'BOOKING_INITIATED' event to SQS
3. **PaymentService** → Consume event, process payment
   - SUCCESS → Publish 'PAYMENT_SUCCESS'
   - FAILURE → Publish 'PAYMENT_FAILED' (triggers compensate)
4. **BookingService** → Consume 'PAYMENT_SUCCESS'
   - Update booking status = CONFIRMED
   - Update SEAT_INVENTORY status = BOOKED
5. **NotificationService** → Send confirmation email/SMS

**Compensating Transaction (on failure):**
- BookingService → Rollback seat status to AVAILABLE
- PaymentService → Initiate refund if charged
- BookingService → Update booking status = FAILED

### 4.2 Optimistic Locking – Concurrent Seat Booking

```java
@Entity
public class SeatInventory {
    @Id
    private String seatId;
    
    @Version // JPA Optimistic Lock
    private Long version;
    
    @Enumerated(EnumType.STRING)
    private SeatStatus status; // AVAILABLE, LOCKED, BOOKED
    
    private String lockedBy;
    private Instant lockedUntil;
}

// Service – catches OptimisticLockException for concurrent conflicts
@Transactional
public BookingResponse reserveSeats(BookingRequest request) {
    List<SeatInventory> seats = seatRepo.findByShowIdAndSeatLabelIn(
        request.getShowId(), request.getSeats(), LockModeType.OPTIMISTIC);
    
    seats.forEach(s -> {
        if (s.getStatus() != SeatStatus.AVAILABLE)
            throw new SeatNotAvailableException(s.getSeatLabel());
        
        s.setStatus(SeatStatus.LOCKED);
        s.setLockedBy(request.getUserId());
        s.setLockedUntil(Instant.now().plusSeconds(600));
    });
    
    seatRepo.saveAll(seats);
    return createBooking(request, seats);
}
```

### 4.3 CQRS – Show Search vs. Booking

| Aspect | Command Side | Query Side |
|---|---|---|
| **Pattern** | Write operations (Book, Cancel, Update) | Read operations (Search, Browse) |
| **DB** | Oracle DB (consistency) | Elasticsearch + Redis Cache |
| **Sync** | Event-driven sync via SQS | Near-real-time (< 2s lag) |
| **Service** | BookingService, TheatreService | SearchService, ShowService |
| **Scaling** | Vertical (write consistency) | Horizontal (read throughput) |

### 4.4 Circuit Breaker – Payment Service

```java
@Service
public class PaymentService {
    @CircuitBreaker(name = 'paymentGateway', fallbackMethod = 'paymentFallback')
    @Retry(name = 'paymentGateway')
    @TimeLimiter(name = 'paymentGateway')
    public CompletableFuture<PaymentResponse> processPayment(PaymentRequest req) {
        return CompletableFuture.supplyAsync(() ->
            razorpayClient.createOrder(req));
    }
    
    public CompletableFuture<PaymentResponse> paymentFallback(
            PaymentRequest req, Exception ex) {
        // Queue for retry, notify user of delay
        eventPublisher.publish(new PaymentRetryEvent(req));
        return CompletableFuture.completedFuture(
            PaymentResponse.pending('Payment queued for processing'));
    }
}
```

**Resilience4j Configuration:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentGateway:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10000
  retry:
    instances:
      paymentGateway:
        maxAttempts: 3
        waitDuration: 2000
```

---

## 5. Non-Functional Requirements

### 5.1 Scalability & High Availability (99.99%)

| Layer | Strategy | AWS Service |
|---|---|---|
| **Compute** | Kubernetes HPA – scale pods on CPU/RPS | AWS EKS + EC2 Auto Scaling |
| **Database** | Oracle RAC + Read Replicas for read-heavy | AWS RDS Oracle Multi-AZ |
| **Cache** | Redis Cluster with replication | AWS ElastiCache Redis |
| **CDN** | Static assets + API response caching | AWS CloudFront |
| **Load Balancing** | Layer 7 routing with health checks | AWS ALB + NLB |
| **Multi-Region** | Active-Active across 2 AWS regions | Route 53 Latency Routing |
| **DB Failover** | Automatic failover < 30s | RDS Multi-AZ Standby |

### 5.2 Transaction Management

**Distributed Transactions:** Saga pattern (choreography) via SQS events for cross-service consistency

**Local Transactions:** Spring @Transactional with Oracle DB isolation level READ_COMMITTED

**Idempotency:** All POST endpoints accept X-Idempotency-Key header; duplicate requests return cached response from Redis

**Seat Locking:** 10-minute TTL lock on seat reservation; scheduled job releases expired locks via Oracle DBMS_SCHEDULER

**Compensation:** Each service implements a compensate() method triggered by SAGA_ROLLBACK events

### 5.3 Payment Gateway Integration

**Payment Integration Architecture:**
- **Primary Gateway:** Razorpay (India) / Stripe (International)
- **Fallback Gateway:** PayU (auto-failover via Resilience4j)
- **Flow:** Client → API GW → PaymentService → Gateway SDK → Webhook → BookingService
- **Security:** PCI-DSS compliant; card data never touches our servers (tokenization)
- **Webhook Validation:** HMAC-SHA256 signature verification on all gateway callbacks
- **Refund SLA:** Initiated within 5 minutes of cancellation; settled in 5-7 banking days

### 5.4 Legacy Theatre IT System Integration

| Integration Type | Approach | Pattern |
|---|---|---|
| **REST API (new theatres)** | Direct REST calls via Theatre Service | Adapter Pattern |
| **SOAP/XML (legacy)** | Spring-WS + JAXB marshalling wrapper | Anti-Corruption Layer |
| **File-based (CSV/EDI)** | AWS S3 + Lambda trigger + SQS ingestion | ETL Pipeline |
| **Database link (legacy)** | Oracle DB Link + scheduled sync job | Data Replication |
| **No integration** | Manual onboarding portal for partners | Self-Service B2B |

### 5.5 OWASP Top 10 Protection

| OWASP Risk | Mitigation |
|---|---|
| **A01 – Broken Access Control** | Role-based JWT claims (ADMIN, PARTNER, CUSTOMER); @PreAuthorize on all endpoints; row-level security in Oracle (VPD) |
| **A02 – Cryptographic Failures** | TLS 1.3 everywhere; AES-256 for PII at rest in Oracle; bcrypt for passwords; AWS KMS for key management |
| **A03 – Injection** | JPA parameterized queries only; no native SQL with user input; ESAPI input sanitization; Oracle bind variables |
| **A04 – Insecure Design** | Threat modelling in design phase; principle of least privilege; security gates in CI/CD pipeline |
| **A05 – Security Misconfiguration** | Spring Security hardening; disable actuator in prod (whitelist /health, /metrics); AWS Security Hub + Config rules |
| **A06 – Vulnerable Components** | OWASP Dependency Check in pipeline; Snyk scanning; regular dependency updates via Dependabot |
| **A07 – Auth Failures** | JWT with 15-min expiry + refresh token rotation; MFA for B2B partners; account lockout after 5 failed attempts |
| **A08 – Integrity Failures** | Signed JWT (RS256); AWS ECR image scanning; Sigstore for container signing; checksum verification on artifacts |
| **A09 – Logging Failures** | Structured JSON logs via ELK; all auth events logged; PII masked in logs; CloudTrail for AWS API audit |
| **A10 – SSRF** | AWS IMDSv2 only; no user-controlled URLs in backend HTTP calls; egress firewall rules via AWS Security Groups |

---

## 6. AWS Cloud Infrastructure

### 6.1 AWS Services Map

| Category | AWS Service | Purpose |
|---|---|---|
| **Compute** | EKS (Fargate + EC2) | Kubernetes cluster for microservices |
| **Compute** | EC2 Auto Scaling Groups | Burst capacity for peak load |
| **Database** | RDS Oracle Multi-AZ | Primary transactional DB |
| **Cache** | ElastiCache Redis Cluster | Session, seat lock, response cache |
| **Search** | OpenSearch Service | Movie/show full-text search |
| **API** | API Gateway (REST) | External API management, throttling |
| **Messaging** | SQS + SNS | Async events, notifications |
| **Storage** | S3 + CloudFront | Media assets, static content CDN |
| **Container** | ECR | Docker image registry |
| **IAM** | IAM + Cognito | Service auth, user pool for B2C |
| **Security** | WAF + Shield | DDoS protection, OWASP rules |
| **Secrets** | Secrets Manager + KMS | DB credentials, encryption keys |
| **CI/CD** | CodePipeline + CodeBuild | Automated build and deploy |
| **Monitoring** | CloudWatch | Infra metrics, alarms, dashboards |
| **Networking** | VPC, ALB, NLB, Route53 | Network isolation and routing |

### 6.2 AWS Network Architecture

**VPC Design:**
- **VPC CIDR:** 10.0.0.0/16
- **Public Subnets (AZ-1a, AZ-1b):** 10.0.1.0/24, 10.0.2.0/24
  - ALB, NAT Gateway, Bastion Host
- **Private Subnets (AZ-1a, AZ-1b):** 10.0.10.0/24, 10.0.11.0/24
  - EKS Worker Nodes, Spring Boot services
- **Database Subnets (AZ-1a, AZ-1b):** 10.0.20.0/24, 10.0.21.0/24
  - Oracle RDS, ElastiCache, OpenSearch

**Security Groups:**
- **SG-ALB:** Allow 80, 443 from 0.0.0.0/0
- **SG-EKS:** Allow all from SG-ALB only
- **SG-RDS:** Allow 1521 from SG-EKS only
- **SG-REDIS:** Allow 6379 from SG-EKS only

### 6.3 EKS Deployment – Kubernetes Config

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: booking-service
  namespace: xyz-platform
spec:
  replicas: 3
  selector:
    matchLabels:
      app: booking-service
  template:
    spec:
      containers:
      - name: booking-service
        image: 123456.dkr.ecr.ap-south-1.amazonaws.com/booking-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: DB_URL
          valueFrom:
            secretKeyRef:
              name: oracle-creds
              key: url
        resources:
          requests: { memory: '512Mi', cpu: '500m' }
          limits: { memory: '1Gi', cpu: '1000m' }
        livenessProbe:
          httpGet: { path: /actuator/health/liveness, port: 8080 }
          initialDelaySeconds: 30
        readinessProbe:
          httpGet: { path: /actuator/health/readiness, port: 8080 }

---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: booking-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: booking-service
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target: { type: Utilization, averageUtilization: 70 }
```

---

## 7. Observability – Spring Actuator + Grafana + ELK

### 7.1 Spring Actuator Configuration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,loggers
  endpoint:
    health:
      show-details: when-authorized
    probes:
      enabled: true # liveness + readiness for K8s
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true
      slo:
        http.server.requests: 50ms,100ms,200ms,500ms,1s
    tags:
      application: booking-service
      environment: production
  tracing:
    sampling.probability: 0.1 # 10% sampling in prod
```

### 7.2 Grafana Dashboards

| Dashboard | Key Metrics | Alert Threshold |
|---|---|---|
| **Platform Health** | Service UP/DOWN, pod count, restart count | Any pod down > 2 min |
| **Booking Funnel** | Bookings/min, success rate, abandonment rate | Success rate < 95% |
| **API Performance** | P50, P95, P99 latency per endpoint | P99 > 2s for any API |
| **Database** | Oracle connections, query time, deadlocks | Deadlocks > 0 in 5 min |
| **Payment** | Payment success rate, gateway latency | Success rate < 98% |
| **Seat Availability** | Cache hit rate, lock expiry rate | Cache hit rate < 80% |
| **Infrastructure** | CPU, memory, network I/O per pod | CPU > 80% sustained 5 min |

### 7.3 ELK Stack Setup

**ELK Architecture:**
Filebeat (sidecar in each pod) → Logstash → Elasticsearch → Kibana

**Log Format:** Structured JSON (Logback + logstash-logback-encoder)

**Fields:** timestamp, level, service, traceId, spanId, userId, endpoint, duration, status

**Index Strategy:**
- xyz-app-logs-{YYYY.MM.DD} (application logs, 30d retention)
- xyz-access-logs-{YYYY.MM.DD} (API access logs, 90d retention)
- xyz-audit-logs-{YYYY.MM.DD} (security events, 1 year retention)

**Key Kibana Dashboards:**
- Error rate by service (last 1h/24h/7d)
- Slow query detection (> 500ms Oracle queries)
- Security alerts: failed logins, suspicious IPs
- Booking journey trace: end-to-end traceId correlation

### 7.4 Distributed Tracing

```xml
<!-- pom.xml – Micrometer + Zipkin / AWS X-Ray -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

**Trace propagation:** W3C TraceContext headers

**X-Trace-Id** injected into all MDC logs automatically

**Correlation** across: API GW → Gateway → BookingService → Oracle → Redis

---

## 8. API Gateway – AWS + Spring Cloud Gateway

### 8.1 Dual Gateway Architecture

| Layer | Component | Responsibility |
|---|---|---|
| **External** | AWS API Gateway | TLS termination, WAF, rate limiting (global), API keys for B2B |
| **Internal** | Spring Cloud Gateway | Service routing, JWT validation, request transformation, load balancing |
| **Auth** | AWS Cognito / Keycloak | Token issuance, refresh, MFA |
| **Security** | AWS WAF | OWASP managed rules, IP allow/block lists, geo-restriction |

### 8.2 Spring Cloud Gateway Config

```yaml
spring:
  cloud:
    gateway:
      routes:
      - id: booking-service
        uri: lb://booking-service
        predicates:
        - Path=/api/v1/bookings/**
        filters:
        - AuthenticationFilter
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 100
            redis-rate-limiter.burstCapacity: 200
        - name: CircuitBreaker
          args:
            name: bookingCB
            fallbackUri: forward:/fallback/booking
        - AddRequestHeader=X-Gateway-Source, xyz-gateway
      
      - id: show-service
        uri: lb://show-service
        predicates:
        - Path=/api/v1/shows/**
        filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 500
            redis-rate-limiter.burstCapacity: 1000
```

---

## 9. Platform Monetization Strategy

| Revenue Stream | Model | Approx. Rate |
|---|---|---|
| **Convenience Fee** | Charged per ticket to end customer | Rs. 20-50 per ticket |
| **Theatre Commission** | % of ticket revenue per booking via platform | 2-5% of booking value |
| **Premium Listing** | Theatres pay for featured placement in search | Fixed monthly fee |
| **Advertising** | Banner ads on movie pages (CPM model) | CPM-based billing |
| **B2B SaaS Fee** | Monthly subscription for Theatre Management Portal | Tiered pricing |
| **Data Insights** | Anonymized analytics sold to studios/distributors | Annual contracts |
| **Surge Pricing** | Dynamic pricing for blockbuster shows | 15-30% premium |

---

## 10. High-Level Project Plan & Estimates

### 10.1 Phase Breakdown

| Phase | Deliverables | Duration | Team Size |
|---|---|---|---|
| **Phase 0 – Setup** | AWS infra, CI/CD pipelines, Oracle schema, base services skeleton | 2 weeks | 4 devs |
| **Phase 1 – Core B2C** | User, Movie, Theatre, Show, Booking services + basic UI | 6 weeks | 8 devs |
| **Phase 2 – Payments** | Payment service, Saga, refunds, offer engine | 4 weeks | 6 devs |
| **Phase 3 – B2B Portal** | Theatre partner onboarding, seat mgmt, show mgmt APIs | 4 weeks | 5 devs |
| **Phase 4 – Scale & NFR** | ElasticSearch, Redis, HPA, multi-region, OWASP hardening | 4 weeks | 6 devs |
| **Phase 5 – Observability** | ELK, Grafana dashboards, alerting, runbooks | 2 weeks | 3 devs |
| **Phase 6 – UAT & Launch** | Load testing, pen testing, bug fixes, go-live | 3 weeks | Full team |

### 10.2 Key KPIs

| KPI | Target | Measurement |
|---|---|---|
| **API Availability** | 99.99% uptime | AWS CloudWatch + Route53 health checks |
| **Booking API P99 Latency** | < 500ms | Grafana / Actuator metrics |
| **Seat Reservation Success** | > 99% under 1000 concurrent users | Load test + monitoring |
| **Payment Success Rate** | > 98.5% | Payment service metrics |
| **Search Response Time** | < 200ms P95 | Elasticsearch + CloudFront cache |
| **DB Query Time** | < 100ms avg for OLTP queries | Oracle AWR + ELK slow query log |
| **Deployment Frequency** | >= 1 release/week | CodePipeline metrics |
| **MTTR** | < 15 minutes | Incident tracking |

---

## Conclusion

This solution architecture provides a comprehensive, enterprise-grade design for the Movie Ticket Booking Platform. The microservices-based approach with AWS cloud infrastructure ensures scalability, reliability, and maintainability. Adherence to design patterns, security best practices, and operational excellence principles positions the platform for sustainable growth and operational success.