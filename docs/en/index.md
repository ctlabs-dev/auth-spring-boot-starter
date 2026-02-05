---
title: Auth Spring Boot Starter
description: A production-ready Spring Boot Starter providing complete authentication and authorization out-of-the-box.
navigation.icon: uil:bolt
---

::u-page-hero
#title
Auth Spring Boot Starter

#description
A production-ready Spring Boot Starter providing complete authentication and authorization out-of-the-box.

Seamlessly integrate JWT authentication, user registration, multi-channel verification, and role management into your Spring Boot applications.

#links
  :::u-button
  ---
  color: neutral
  size: xl
  to: /en/getting-started/introduction
  trailing-icon: i-lucide-arrow-right
  ---
  Get started
  :::

  :::u-button
  ---
  color: neutral
  icon: simple-icons-github
  size: xl
  to: https://github.com/your-org/auth-spring-boot-starter
  variant: outline
  ---
  View on GitHub
  :::
::

::u-page-section
#title
Key Features

#features
  :::u-page-feature
  ---
  icon: i-heroicons-lock-closed
  ---
  #title
  JWT Authentication
  
  #description
  Automatic token generation, validation, and refresh with configurable expiration.
  :::

  :::u-page-feature
  ---
  icon: i-heroicons-user-plus
  ---
  #title
  User Registration
  
  #description
  Support for email and phone-based registration with validation.
  :::

  :::u-page-feature
  ---
  icon: i-heroicons-device-phone-mobile
  ---
  #title
  Multi-channel Verification
  
  #description
  Email (SMTP), SMS (Twilio), and WhatsApp verification support.
  :::

  :::u-page-feature
  ---
  icon: i-heroicons-circle-stack
  ---
  #title
  Isolated Database
  
  #description
  Dedicated Flyway migrations with separate history table.
  :::
::

::u-page-section
#title
Architecture

#description
The starter follows Spring Boot's auto-configuration principles and integrates cleanly with your existing application.

#features
  :::u-page-feature
  ---
  icon: i-heroicons-shield-check
  ---
  #title
  Security Filter Chain
  
  #description
  Operates with `@Order(1)` for auth endpoints only.
  :::

  :::u-page-feature
  ---
  icon: i-heroicons-server
  ---
  #title
  Database Isolation
  
  #description
  Uses separate Flyway history table to avoid conflicts.
  :::
::
