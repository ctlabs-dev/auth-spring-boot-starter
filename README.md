# Auth Spring Boot Starter

A production-ready Spring Boot Starter that provides complete JWT authentication and authorization out-of-the-box.

## 📚 Documentation

**Complete documentation available at [docs.ctlabs.dev](https://docs.ctlabs.dev)**

## ✨ Features

- JWT-based authentication with automatic token management
- User registration with email/phone verification
- Multi-channel notifications (SMTP, Twilio, Brevo)
- Session management
- Role-based authorization
- Isolated database migrations

## 🚀 Quick Start

### 1. Add Dependency

**Maven**
```xml
<dependency>
    <groupId>dev.ctlabs</groupId>
    <artifactId>auth-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>
```

**Gradle**
```groovy
implementation 'dev.ctlabs:auth-spring-boot-starter:0.1.0'
```

### 2. Configure

Minimal `application.properties`:

```properties
# Auth Starter Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/your_db
spring.datasource.username=your_user
spring.datasource.password=your_password

ctlabs.auth.jwt.secret-key=${JWT_SECRET_KEY:long_and_secure_secret_key}

# Exclude default Spring Security config for compatibility
spring.autoconfigure.exclude=org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration

```

### 3. Use

The starter automatically exposes these authentication endpoints:

- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login
- `POST /api/auth/refresh-token` - Refresh access token
- `POST /api/auth/logout` - Logout (current device)
- `POST /api/auth/email-verification` - Verify email with code
- `POST /api/auth/phone-verification` - Verify phone with code
- `POST /api/auth/resend-verification` - Resend verification code
- `POST /api/auth/forgot-password` - Request password reset
- `POST /api/auth/reset-password` - Reset password with code

**For complete guides, configuration options, and examples, visit [docs.ctlabs.dev](https://docs.ctlabs.dev)**

## 📦 Requirements

- Java 17+
- Spring Boot 3.x / 4.x
- PostgreSQL (recommended) or any JPA-compatible database

## 📄 License

Apache License 2.0 - See [LICENSE](LICENSE) file for details.
