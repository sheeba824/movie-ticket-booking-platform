# Low Level Design (LLD) - Movie Ticket Booking Platform

## 1. Service-Level Architecture

### 1.1 Authentication & Authorization Service

#### Responsibilities
- User authentication (login/logout/signup)
- JWT token generation and validation
- Role-Based Access Control (RBAC)
- OAuth 2.0 integration
- MFA management

#### Key Classes
```java
public class AuthService {
    - authenticate(credentials): JWT
    - validateToken(token): boolean
    - refreshToken(token): JWT
    - getUserPermissions(userId): List<Permission>
}

public class TokenManager {
    - generateJWT(claims): String
    - validateJWT(token): Claims
    - revokeToken(token): void
}

public class RBACManager {
    - assignRole(userId, role): void
    - checkPermission(userId, permission): boolean
    - getRoles(userId): List<Role>
}
```

#### Database Tables
```sql
users
├── id (PK)
├── email (UNIQUE)
├── password_hash
├── first_name
├── last_name
├── phone
├── user_type (enum: CUSTOMER, PARTNER)
├── status (enum: ACTIVE, INACTIVE, SUSPENDED)
├── created_at
└── updated_at

roles
├── id (PK)
├── name
├── description
└── permissions (JSON)

user_roles
├── user_id (FK)
├── role_id (FK)
└── assigned_at

tokens_blacklist
├── token (PK)
├── user_id (FK)
├── revoked_at
└── expires_at
```

#### API Endpoints
```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/logout
POST   /api/v1/auth/refresh
GET    /api/v1/auth/validate
GET    /api/v1/users/{id}/permissions
```

---

### 1.2 Theatre Service

#### Responsibilities
- Theatre registration and management
- Partner KYC verification
- Screen and seat management
- Theatre location and metadata
- Settlement account management

#### Key Classes
```java
public class TheatreService {
    - registerTheatre(theatreInfo): Theatre
    - updateTheatreInfo(theatreId, info): Theatre
    - getTheatreDetails(theatreId): Theatre
    - verifyTheatreKYC(theatreId): boolean
    - listTheatresByCity(city): List<Theatre>
}

public class ScreenService {
    - addScreen(theatreId, screenInfo): Screen
    - configureSeats(screenId, seatLayout): SeatLayout
    - updateScreenStatus(screenId, status): void
}

public class SeatInventoryService {
    - initializeSeatInventory(screenId, layout): void
    - reserveSeat(screenId, seatNumber, showId): void
    - releaseSeat(screenId, seatNumber, showId): void
}
```

#### Database Tables
```sql
theatres
├── id (PK)
├── partner_id (FK)
├── name
├── city
├── state
├── country
├── address
├── latitude/longitude
├── kyc_status (enum)
├── verification_date
├── total_screens
└── created_at

screens
├── id (PK)
├── theatre_id (FK)
├── screen_number
├── total_seats
├── seat_layout (JSON: row-wise configuration)
├── status (enum: ACTIVE, MAINTENANCE)
└── created_at

seat_inventory
├── id (PK)
├── screen_id (FK)
├── show_id (FK)
├── seat_number (e.g., A1, A2, B1)
├── status (enum: AVAILABLE, BOOKED, LOCKED, MAINTENANCE)
├── locked_until
└── updated_at
```

---

### 1.3 Show Service

#### Responsibilities
- Movie and show management
- Show scheduling and timing
- Content metadata (language, genre, ratings)
- Show lifecycle management

#### Key Classes
```java
public class ShowService {
    - createShow(showInfo): Show
    - updateShow(showId, info): Show
    - getShowsByTheatreAndDate(theatreId, date): List<Show>
    - getShowsByMovieAndCity(movieId, city, date): List<Show>
    - cancelShow(showId): void
}

public class MovieService {
    - addMovie(movieInfo): Movie
    - updateMovieInfo(movieId, info): Movie
    - searchMovies(filters): List<Movie>
    - getMovieDetails(movieId): Movie
}

public class ContentFilter {
    - filterByCity(shows, city): List<Show>
    - filterByLanguage(shows, language): List<Show>
    - filterByGenre(shows, genre): List<Show>
    - filterByRating(shows, minRating): List<Show>
}
```

#### Database Tables
```sql
movies
├── id (PK)
├── title
├── description
├── genre (array)
├── language (array)
├── duration_minutes
├── release_date
├── rating (e.g., U, UA, A, S)
├── imdb_rating
├── poster_url
└── created_at

shows
├── id (PK)
├── movie_id (FK)
├── screen_id (FK)
├── theatre_id (FK)
├── show_time
├── base_price
├── total_seats
├── available_seats
├── status (enum: SCHEDULED, ONGOING, COMPLETED, CANCELLED)
├── created_by (theatre_id)
└── created_at

show_pricing
├── show_id (FK)
├── price_type (STANDARD, PREMIUM, ECONOMY)
├── base_price
├── dynamic_discount_percentage
└── applicable_from
```

---

### 1.4 Booking Service

#### Responsibilities
- Real-time seat availability
- Booking creation and confirmation
- Seat selection and locking mechanism
- Booking cancellation and refund initiation
- Inventory management

#### Key Classes
```java
public class BookingService {
    - initiateBooking(userId, showId): BookingSession
    - reserveSeats(sessionId, seatNumbers): ReservationDetails
    - confirmBooking(bookingId, paymentId): Booking
    - cancelBooking(bookingId): void
    - getBookingHistory(userId): List<Booking>
}

public class SeatLockingManager {
    - lockSeats(showId, seatNumbers, duration): void
    - unlockSeats(showId, seatNumbers): void
    - isLocked(showId, seatNumber): boolean
    - cleanupExpiredLocks(): void
}

public class BookingTransactionManager {
    - beginTransaction(bookingId): Transaction
    - commitTransaction(transactionId): void
    - rollbackTransaction(transactionId): void
}
```

#### Database Tables
```sql
bookings
├── id (PK)
├── user_id (FK)
├── show_id (FK)
├── theatre_id (FK)
├── booking_status (enum: INITIATED, CONFIRMED, CANCELLED, EXPIRED)
├── total_amount
├── final_amount (after discounts)
├── discount_applied
├── booking_time
├── confirmation_time
└── cancellation_time

booking_seats
├── id (PK)
├── booking_id (FK)
├── seat_number
├── seat_type (STANDARD, PREMIUM)
├── original_price
├── discount_percentage
├── final_price
└── status (ALLOCATED, CANCELLED)

seat_locks
├── id (PK)
├── show_id (FK)
├── seat_number (FK)
├── user_id (FK)
├── locked_at
├── expires_at
└── released_at
```

---

### 1.5 Payment Service

#### Responsibilities
- Payment processing via gateway integration
- Transaction logging and reconciliation
- Refund management
- Invoice generation
- Settlement to theatre partners

#### Key Classes
```java
public class PaymentService {
    - initiatePayment(booking): PaymentRequest
    - processPayment(paymentRequest, gateway): PaymentResponse
    - handlePaymentCallback(callbackData): void
    - refundPayment(transactionId): RefundResponse
}

public class PaymentGatewayIntegration {
    - charge(amount, paymentMethod): ChargeResponse
    - refund(chargeId, amount): RefundResponse
    - validatePaymentMethod(paymentMethod): boolean
}

public class SettlementService {
    - calculateSettlement(theatreId, period): Settlement
    - generateSettlementReport(theatreId, period): Report
    - initiateTransfer(theatreId, amount): Transfer
}
```

#### Database Tables
```sql
transactions
├── id (PK)
├── booking_id (FK)
├── user_id (FK)
├── amount
├── payment_method (CARD, UPI, WALLET)
├── gateway_transaction_id
├── status (enum: PENDING, SUCCESS, FAILED, REFUNDED)
├── created_at
└── updated_at

refunds
├── id (PK)
├── transaction_id (FK)
├── original_amount
├── refund_amount
├── reason
├── status (enum: INITIATED, PROCESSING, COMPLETED, FAILED)
├── requested_at
└── completed_at

settlements
├── id (PK)
├── theatre_id (FK)
├── period_start
├── period_end
├── total_revenue
├── commission_percentage
├── platform_commission
├── theatre_amount
├── status (CALCULATED, PROCESSED)
└── processed_at
```

---

### 1.6 Offer & Pricing Service

#### Responsibilities
- Dynamic pricing calculation
- Apply promotional offers
- Implement business rules (50% off 3rd ticket, 20% off afternoon)
- Bulk booking discounts

#### Key Classes
```java
public class OfferService {
    - getAvailableOffers(show, user): List<Offer>
    - applyOffer(bookingId, offerId): Discount
    - validateOffer(offer, booking): boolean
}

public class PricingEngine {
    - calculateBasePrice(show): BigDecimal
    - applyDynamicPricing(show, demand): BigDecimal
    - applyTimeBasedDiscount(show): BigDecimal
    // 20% off for afternoon shows (2 PM - 5 PM)
}

public class BulkBookingDiscountCalculator {
    - calculate3rdTicketDiscount(seatCount): BigDecimal
    // 50% off on 3rd ticket onwards
    - calculateBulkDiscount(seatCount): BigDecimal
}

public class PromotionEngine {
    - getActivePromotions(theatre, city): List<Promotion>
    - validatePromotionCode(code): Promotion
    - applyPromotion(booking, code): void
}
```

#### Database Tables
```sql
offers
├── id (PK)
├── name
├── description
├── offer_type (enum: DISCOUNT_PERCENTAGE, DISCOUNT_AMOUNT, BOGO)
├── value
├── applicable_shows (JSON: show_ids or criteria)
├── applicable_users (JSON: user_segments)
├── start_date
├── end_date
├── max_usage_count
├── usage_count
└── active (boolean)

promotions
├── id (PK)
├── theatre_id (FK)
├── code (UNIQUE)
├── discount_percentage
├── max_discount_amount
├── applicable_for_movie_ids (array)
├── applicable_shows (time-based JSON)
├── min_booking_amount
├── max_usage
├── current_usage
├── valid_from
├── valid_till
└── created_at
```

---

### 1.7 Notification Service

#### Responsibilities
- Send email, SMS, and push notifications
- Notification templating
- Delivery retry logic
- Notification preferences management

#### Key Classes
```java
public class NotificationService {
    - sendNotification(userId, notificationType, data): void
    - sendBulkNotifications(userIds, notification): void
    - retryFailedNotifications(): void
}

public class EmailNotificationHandler {
    - sendEmail(recipient, template, data): void
    - buildEmailContent(template, data): EmailContent
}

public class SMSNotificationHandler {
    - sendSMS(phoneNumber, message): void
}

public class PushNotificationHandler {
    - sendPushNotification(deviceTokens, notification): void
}

public class NotificationPreferenceManager {
    - setPreferences(userId, preferences): void
    - shouldNotify(userId, notificationType): boolean
}
```

#### Database Tables
```sql
notifications
├── id (PK)
├── user_id (FK)
├── notification_type (enum: BOOKING_CONFIRMATION, PAYMENT_SUCCESS, OFFER_AVAILABLE)
├── title
├── body
├── data (JSON)
├── channel (EMAIL, SMS, PUSH)
├── status (enum: PENDING, SENT, FAILED)
├── created_at
├── sent_at
└── failed_reason

notification_preferences
├── user_id (PK, FK)
├── email_notifications (boolean)
├── sms_notifications (boolean)
├── push_notifications (boolean)
├── offer_notifications (boolean)
├── marketing_emails (boolean)
└── updated_at
```

---

### 1.8 Search & Discovery Service

#### Responsibilities
- Full-text search on movies
- Advanced filtering (city, language, genre, ratings)
- Search result ranking
- Search analytics

#### Key Classes
```java
public class SearchService {
    - searchMovies(keyword, filters): List<SearchResult>
    - getPopularMovies(city, days): List<Movie>
    - getUpcomingMovies(city): List<Movie>
    - getRecommendedMovies(userId): List<Movie>
}

public class FilterEngine {
    - applyFilters(results, filters): List<Movie>
    - filterByCity(results, city): List<Movie>
    - filterByLanguage(results, language): List<Movie>
    - filterByGenre(results, genre): List<Movie>
}

public class SearchIndexer {
    - buildIndex(movies): void
    - updateIndex(movie): void
    - removeFromIndex(movieId): void
}
```

#### Technology
- Elasticsearch for full-text search
- Index updates via Kafka event stream
- Caching popular searches in Redis

---

### 1.9 Analytics Service

#### Responsibilities
- Revenue analytics
- Booking metrics
- Theatre performance tracking
- Customer behavior analysis
- KPI dashboards

#### Key Classes
```java
public class AnalyticsService {
    - getRevenueMetrics(theatreId, period): RevenueMetrics
    - getBookingMetrics(theatreId, period): BookingMetrics
    - getTheatrePerformance(theatreId): PerformanceMetrics
    - getCustomerMetrics(userId): CustomerMetrics
}

public class MetricsCalculator {
    - calculateOccupancyRate(theatreId, period): Double
    - calculateAverageTicketPrice(theatreId, period): BigDecimal
    - calculateCustomerAcquisitionCost(): BigDecimal
}
```

---

## 2. Cross-Cutting Concerns

### 2.1 Exception Handling Strategy

```java
public abstract class CustomException extends RuntimeException {
    private ErrorCode errorCode;
    private String message;
}

public class BookingException extends CustomException { }
public class PaymentException extends CustomException { }
public class ValidationException extends CustomException { }
```

### 2.2 Logging Strategy

```java
// Use SLF4J with structured logging
private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

// Examples:
logger.info("Booking initiated", Map.of("bookingId", id, "userId", userId));
logger.error("Payment failed", Map.of("bookingId", id, "error", error));
```

### 2.3 Transactional Patterns

**Local Transactions:**
- Service-level transactions within single database
- @Transactional annotations

**Distributed Transactions:**
- Saga pattern for cross-service operations
- Compensating transactions for rollback
- Event-driven orchestration

Example Booking Saga:
```
1. Begin Booking
2. Reserve Seats (if fails → compensate)
3. Initiate Payment (if fails → release seats)
4. Update Inventory (if fails → refund)
5. Send Confirmation (async, non-critical)
```

---

## 3. Design Patterns Used

| Pattern | Use Case |
|---------|----------|
| **Singleton** | Service initialization, Database connections |
| **Factory** | Creating payment gateway instances |
| **Observer** | Event listeners for booking events |
| **Strategy** | Different discount calculation strategies |
| **Adapter** | Payment gateway integration |
| **Decorator** | Adding behaviors to notifications |
| **Repository** | Data access abstraction |
| **DTO** | API request/response objects |

---

## 4. Performance Optimization

### 4.1 Caching Strategy

```
Data Access Pattern:
1. Check Redis cache
2. If miss, query database
3. Update Redis with TTL
4. Return cached data

Key Caches:
- User permissions (TTL: 1 hour)
- Show availability (TTL: 5 minutes)
- Movie metadata (TTL: 24 hours)
- Theatre info (TTL: 12 hours)
```

### 4.2 Database Optimization

```sql
-- Indexes
CREATE INDEX idx_shows_theatre_date 
    ON shows(theatre_id, show_time);

CREATE INDEX idx_bookings_user_status 
    ON bookings(user_id, booking_status);

CREATE INDEX idx_seats_show_status 
    ON seat_inventory(show_id, status);
```

### 4.3 API Response Optimization

- Pagination (default page size: 20)
- Selective field projection
- Lazy loading relationships
- Response compression (GZIP)

---

## 5. Testing Strategy

### 5.1 Unit Tests
- Service business logic
- Utility functions
- Coverage target: 80%+

### 5.2 Integration Tests
- Service-to-database interactions
- External service mocks (payment gateway)
- Transactional behavior

### 5.3 End-to-End Tests
- Complete booking flow
- Payment integration
- Notification dispatch

```java
@Test
public void testCompleteBookingFlow() {
    // 1. Search shows
    // 2. Select seats
    // 3. Make payment
    // 4. Confirm booking
    // 5. Verify notifications sent
}
```

---

## 6. Error Handling & Recovery

### 6.1 Retry Logic

```java
@Retry(maxAttempts = 3, delay = 1000, multiplier = 2.0)
public PaymentResponse processPayment(Payment payment) {
    // Exponential backoff: 1s, 2s, 4s
}
```

### 6.2 Circuit Breaker

```java
@CircuitBreaker(failureThreshold = 5, successThreshold = 2, delay = 10000)
public void callExternalPaymentGateway() {
    // If 5 failures, open circuit for 10s
    // Return cached response or fail fast
}
```

---

**Document Version**: 1.0  
**Last Updated**: April 2026
