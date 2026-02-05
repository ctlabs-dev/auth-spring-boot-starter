# Auth Spring Boot Starter

A production-ready Spring Boot Starter providing complete authentication and authorization out-of-the-box. Seamlessly integrate JWT authentication, user registration, multi-channel verification, and role management into your Spring Boot applications.

## 📚 Documentation

Full documentation is available at **[docs.ctlabs.dev](https://docs.ctlabs.dev)**.

## ✨ Key Features

*   **JWT Authentication**: Automatic token generation, validation, and refresh.
*   **User Registration**: Email and phone-based registration.
*   **Multi-channel Verification**: Email (SMTP), SMS (Twilio), and WhatsApp.
*   **Isolated Database**: Dedicated Flyway migrations with separate history table.
*   **Security Filter Chain**: Pre-configured security for auth endpoints.

## 🚀 Quick Start

### Installation

Add the dependency to your project:

**Maven**
```xml
<dependency>
    <groupId>dev.ctlabs</groupId>
    <artifactId>auth-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

**Gradle**
```groovy
implementation 'dev.ctlabs:auth-spring-boot-starter:0.0.1-SNAPSHOT'
```

### Basic Configuration

Add to your `application.yml`:

```yaml
ctlabs:
  auth:
    jwt:
      secret-key: ${JWT_SECRET} # Must be a long, secure Base64 string
      expiration: 86400000 # 24 hours
    frontend-url: http://localhost:3000
```

For detailed configuration and guides, please visit the [official documentation](https://docs.ctlabs.dev).

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
