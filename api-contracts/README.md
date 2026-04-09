# API Contracts - Movie Ticket Booking Platform

## Authentication Service API

### User Registration
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securePassword123",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+91-9876543210",
  "userType": "CUSTOMER"  // or PARTNER
}

Response (201 Created):
{
  "id": "user-uuid",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "userType": "CUSTOMER",
  "createdAt": "2026-04-07T10:00:00Z"
}
```

### User Login
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securePassword123"
}

Response (200 OK):
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "expiresIn": 3600,
  "user": {
    "id": "user-uuid",
    "email": "user@example.com",
    "roles": ["CUSTOMER"]
  }
}
```

---

## Theatre Service API

### Register Theatre (B2B Partner)
```http
POST /api/v1/theatres
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "PVR Cinemas",
  "city": "Mumbai",
  "state": "Maharashtra",
  "country": "India",
  "address": "123 Main Street, Mumbai",
  "latitude": 19.0760,
  "longitude": 72.8777,
  "phone": "+91-2123456789",
  "email": "contact@pvr.com",
  "website": "https://www.pvrcinemas.com",
  "screens": [
    {
      "screenNumber": 1,
      "totalSeats": 150,
      "seatLayout": {
        "rows": ["A", "B", "C"],
        "seatsPerRow": 50
      }
    }
  ]
}

Response (201 Created):
{
  "id": "theatre-uuid",
  "name": "PVR Cinemas",
  "kycStatus": "PENDING",
  "verificationDate": null,
  "screens": [...],
  "createdAt": "2026-04-07T10:00:00Z"
}
```

### Get Theatres by City
```http
GET /api/v1/theatres?city=Mumbai&language=Hindi&date=2026-04-10

Response (200 OK):
{
  "data": [
    {
      "id": "theatre-uuid-1",
      "name": "PVR Cinemas",
      "city": "Mumbai",
      "screens": 5,
      "shows": [
        {
          "movieTitle": "Inception",
          "language": "Hindi",
          "showTimes": ["10:00", "13:30", "16:00", "19:00", "21:30"]
        }
      ]
    }
  ],
  "totalCount": 15,
  "page": 1,
  "pageSize": 10
}
```

---

## Show Service API

### Create Show
```http
POST /api/v1/shows
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "movieId": "movie-uuid",
  "screenId": "screen-uuid",
  "showTime": "2026-04-10T19:00:00Z",
  "basePrice": 250,
  "specialPrice": null
}

Response (201 Created):
{
  "id": "show-uuid",
  "movieId": "movie-uuid",
  "screenId": "screen-uuid",
  "showTime": "2026-04-10T19:00:00Z",
  "basePrice": 250,
  "availableSeats": 150,
  "status": "SCHEDULED"
}
```

### Get Shows by Movie & City
```http
GET /api/v1/shows/search?movieId=movie-uuid&city=Mumbai&date=2026-04-10&language=Hindi

Response (200 OK):
{
  "data": [
    {
      "id": "show-uuid",
      "theatreName": "PVR Cinemas",
      "screenNumber": 1,
      "showTime": "2026-04-10T19:00:00Z",
      "basePrice": 250,
      "availableSeats": 45,
      "movieLanguage": "Hindi"
    }
  ]
}
```

---

## Booking Service API

### Initiate Booking Session
```http
POST /api/v1/bookings/initiate
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "showId": "show-uuid",
  "numberOfSeats": 3
}

Response (201 Created):
{
  "bookingSessionId": "session-uuid",
  "showId": "show-uuid",
  "availableSeats": 80,
  "expiresAt": "2026-04-07T10:15:00Z"
}
```

### Reserve Seats
```http
POST /api/v1/bookings/{bookingSessionId}/reserve-seats
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "seats": ["A1", "A2", "A3"]
}

Response (200 OK):
{
  "bookingSessionId": "session-uuid",
  "reservedSeats": ["A1", "A2", "A3"],
  "pricing": {
    "basePrice": 250,
    "offers": [
      {
        "type": "AFTERNOON_DISCOUNT",
        "percentage": 20,
        "amount": 50
      },
      {
        "type": "THIRD_TICKET_DISCOUNT",
        "percentage": 50,
        "amount": 125
      }
    ],
    "subtotal": 750,
    "discount": 175,
    "totalAmount": 575
  },
  "lockedUntil": "2026-04-07T10:15:00Z"
}
```

### Confirm Booking (After Payment)
```http
POST /api/v1/bookings/confirm
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "bookingSessionId": "session-uuid",
  "paymentTransactionId": "txn-uuid"
}

Response (201 Created):
{
  "bookingId": "booking-uuid",
  "bookingReference": "BK12345678",
  "userId": "user-uuid",
  "showId": "show-uuid",
  "seats": ["A1", "A2", "A3"],
  "totalAmount": 575,
  "bookingStatus": "CONFIRMED",
  "bookingTime": "2026-04-07T10:10:00Z"
}
```

### Cancel Booking
```http
POST /api/v1/bookings/{bookingId}/cancel
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "reason": "Unable to attend"
}

Response (200 OK):
{
  "bookingId": "booking-uuid",
  "previousStatus": "CONFIRMED",
  "newStatus": "CANCELLED",
  "refundStatus": "INITIATED",
  "refundAmount": 575,
  "cancellationTime": "2026-04-07T10:15:00Z"
}
```

### Get Booking History
```http
GET /api/v1/bookings/history?status=CONFIRMED&limit=10&offset=0
Authorization: Bearer {accessToken}

Response (200 OK):
{
  "data": [
    {
      "bookingId": "booking-uuid",
      "bookingReference": "BK12345678",
      "movieTitle": "Inception",
      "theatreName": "PVR Cinemas",
      "showTime": "2026-04-10T19:00:00Z",
      "seats": ["A1", "A2", "A3"],
      "totalAmount": 575,
      "bookingStatus": "CONFIRMED",
      "bookingTime": "2026-04-07T10:10:00Z"
    }
  ],
  "totalCount": 25,
  "page": 1,
  "pageSize": 10
}
```

---

## Payment Service API

### Initiate Payment
```http
POST /api/v1/payments/initiate
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "bookingId": "booking-uuid",
  "amount": 575,
  "currency": "INR",
  "paymentMethod": "CARD"
}

Response (201 Created):
{
  "paymentId": "payment-uuid",
  "bookingId": "booking-uuid",
  "amount": 575,
  "currency": "INR",
  "paymentGateway": "STRIPE",
  "redirectUrl": "https://payment-gateway.com/authorize?token=...",
  "orderId": "ORDER-12345"
}
```

### Payment Webhook Callback
```http
POST /api/v1/payments/webhook/callback
Content-Type: application/json

{
  "orderId": "ORDER-12345",
  "status": "SUCCESS",
  "transactionId": "TXN-12345",
  "amount": 575,
  "timestamp": "2026-04-07T10:12:00Z",
  "signature": "hash-signature"
}

Response (200 OK):
{
  "message": "Webhook processed"
}
```

---

## Offer & Pricing Service API

### Get Available Offers
```http
GET /api/v1/offers?showId=show-uuid&userId=user-uuid

Response (200 OK):
{
  "data": [
    {
      "offerId": "offer-1",
      "name": "Afternoon Show Discount",
      "description": "20% off on shows between 2 PM - 5 PM",
      "offerType": "DISCOUNT_PERCENTAGE",
      "value": 20,
      "maxDiscount": 50
    },
    {
      "offerId": "offer-2",
      "name": "Third Ticket Free",
      "description": "50% off on 3rd ticket onwards",
      "offerType": "DISCOUNT_PERCENTAGE",
      "value": 50,
      "applicable": true
    }
  ]
}
```

### Validate Promo Code
```http
POST /api/v1/offers/validate-promo
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "promoCode": "SUMMER50",
  "showId": "show-uuid",
  "bookingAmount": 750
}

Response (200 OK):
{
  "successful": true,
  "promoCode": "SUMMER50",
  "discountPercentage": 50,
  "maxDiscountAmount": 100,
  "applicableAmount": 100,
  "finalAmount": 650
}
```

---

## Search Service API

### Search Movies
```http
GET /api/v1/search?keyword=Inception&city=Mumbai&language=Hindi&genre=Sci-Fi&sort=POPULARITY

Response (200 OK):
{
  "data": [
    {
      "movieId": "movie-uuid",
      "title": "Inception",
      "genre": ["Sci-Fi", "Thriller"],
      "languages": ["Hindi", "English"],
      "rating": "A",
      "duration": 148,
      "posterUrl": "https://...",
      "theatres": 12,
      "shows": 48
    }
  ],
  "totalCount": 1,
  "searchTime": "2ms"
}
```

---

## Notification Service API

### Get User Notifications
```http
GET /api/v1/notifications?limit=20&offset=0
Authorization: Bearer {accessToken}

Response (200 OK):
{
  "data": [
    {
      "id": "notification-uuid",
      "type": "BOOKING_CONFIRMATION",
      "title": "Booking Confirmed",
      "body": "Your booking for Inception is confirmed",
      "data": {
        "bookingId": "booking-uuid"
      },
      "createdAt": "2026-04-07T10:10:00Z",
      "read": false
    }
  ]
}
```

### Update Notification Preferences
```http
PUT /api/v1/notifications/preferences
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "emailNotifications": true,
  "smsNotifications": true,
  "pushNotifications": true,
  "offerNotifications": true,
  "marketingEmails": false
}

Response (200 OK):
{
  "message": "Preferences updated successfully"
}
```

---

## Error Response Format

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid email format",
    "details": [
      {
        "field": "email",
        "message": "Email must be a valid email address"
      }
    ],
    "timestamp": "2026-04-07T10:00:00Z",
    "requestId": "req-uuid"
  }
}
```

---

## Common Error Codes

| Code | HTTP Status | Description |
|------|------------|-------------|
| VALIDATION_ERROR | 400 | Input validation failed |
| UNAUTHORIZED | 401 | Authentication required |
| FORBIDDEN | 403 | Insufficient permissions |
| NOT_FOUND | 404 | Resource not found |
| CONFLICT | 409 | Resource conflict (e.g., seat already booked) |
| RATE_LIMIT_EXCEEDED | 429 | Rate limit exceeded |
| INTERNAL_ERROR | 500 | Server error |
| SERVICE_UNAVAILABLE | 503 | Service temporarily unavailable |

---

**Document Version**: 1.0  
**Last Updated**: April 2026
