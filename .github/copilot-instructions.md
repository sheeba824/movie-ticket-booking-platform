name: "GitHub Copilot Instructions"
description: "Movie Ticket Booking Platform - Development Guidelines"

# Project context for GitHub Copilot
project:
  name: Movie Ticket Booking Platform
  type: Microservices
  language: Java
  framework: Spring Boot 3.x
  version: 1.0.0

# Architecture guidelines
architecture:
  pattern: "Microservices with Event-Driven Design"
  layers:
    - name: "API Layer"
      technologies: ["Spring Boot", "REST", "JWT"]
    - name: "Service Layer"
      technologies: ["Service Pattern", "Business Logic"]
    - name: "Data Layer"
      technologies: ["PostgreSQL", "Redis", "Elasticsearch"]
    - name: "Message Layer"
      technologies: ["Kafka", "Event Streaming"]

# Code standards
code_standards:
  language_version: "Java 17+"
  package_naming: "com.movietickets.[service-name]"
  components:
    - entity: "JPA Entity classes"
    - repository: "Spring Data repositories"
    - service: "Business logic services"
    - controller: "REST endpoints"
    - dto: "Data transfer objects"
    - exception: "Custom exceptions"
  
# Service structure
services:
  auth:
    port: 8801
    functions:
      - User authentication
      - JWT management
      - RBAC
  
  theatre:
    port: 8802
    functions:
      - Partner onboarding
      - Screen management
      - KYC verification
  
  show:
    port: 8803
    functions:
      - Show CRUD
      - Content management
      - Movie scheduling
  
  booking:
    port: 8804
    functions:
      - Seat reservation
      - Booking management
      - Inventory control
  
  payment:
    port: 8805
    functions:
      - Payment processing
      - Gateway integration
      - Settlement
  
  offer:
    port: 8806
    functions:
      - Dynamic pricing
      - Promotions
      - Discounts
  
  notification:
    port: 8807
    functions:
      - Email notifications
      - SMS alerts
      - Push notifications

# Database guidelines
database:
  primary: PostgreSQL
  cache: Redis
  search: Elasticsearch
  message_queue: Kafka

# Best practices for code generation
suggestions:
  - Use Lombok for reducing boilerplate
  - Implement proper error handling with custom exceptions
  - Use @Transactional for database operations
  - Implement proper logging with SLF4J
  - Use DTOs for API contracts
  - Implement repository pattern for data access
  - Use dependency injection throughout
  - Follow SOLID principles
  - Implement comprehensive exception handling
  - Add proper validation annotations
  - Use Spring Security for authentication
  - Implement caching where appropriate

# Common patterns implemented
patterns:
  - Repository Pattern
  - Service Layer Pattern
  - DTO Pattern
  - Factory Pattern
  - Singleton Pattern
  - Observer Pattern
  - Strategy Pattern

# Testing guidelines
testing:
  unit_test_framework: JUnit5
  mocking_framework: Mockito
  integration_test_framework: TestContainers
  coverage_target: "80%+"
  test_locations:
    - "src/test/java/com/movietickets/[module]/[ComponentName]Test.java"

# Deployment guidelines
deployment:
  container: Docker
  orchestration: Kubernetes
  ci_cd: GitHub Actions
  environments:
    - development
    - staging
    - production

# Documentation requirements
documentation:
  - API contracts in api-contracts/
  - Architecture decisions in design/hld/
  - Implementation details in design/lld/
  - Database schema in database/
  - Deployment configs in deployment/

# Security requirements
security:
  - OAuth 2.0 / OpenID Connect
  - JWT for token management
  - RBAC for authorization
  - OWASP Top 10 compliance
  - TLS 1.3 for data in transit
  - Encryption for sensitive data
  - SQL injection prevention
  - CSRF protection

# Performance considerations
performance:
  - Cache frequently accessed data
  - Use connection pooling
  - Implement pagination
  - Optimize database queries
  - Use async processing where possible
  - Implement rate limiting
  - Use CDN for static content
