---
title: Auth Spring Boot Starter
description: Un Spring Boot Starter listo para producción que proporciona autenticación y autorización completas desde el primer momento.
navigation.icon: uil:bolt
---

::u-page-hero
#title
Auth Spring Boot Starter

#description
Un Spring Boot Starter listo para producción que proporciona autenticación y autorización completas desde el primer momento.

Integra sin problemas autenticación JWT, registro de usuarios, verificación multicanal y gestión de roles en tus aplicaciones Spring Boot.

#links
  :::u-button
  ---
  color: neutral
  size: xl
  to: /es/getting-started/introduction
  trailing-icon: i-lucide-arrow-right
  ---
  Empezar
  :::

  :::u-button
  ---
  color: neutral
  icon: simple-icons-github
  size: xl
  to: https://github.com/your-org/auth-spring-boot-starter
  variant: outline
  ---
  Ver en GitHub
  :::
::

::u-page-section
#title
Características Clave

#features
  :::u-page-feature
  ---
  icon: i-heroicons-lock-closed
  ---
  #title
  Autenticación JWT
  
  #description
  Generación automática de tokens, validación y actualización con expiración configurable.
  :::

  :::u-page-feature
  ---
  icon: i-heroicons-user-plus
  ---
  #title
  Registro de Usuarios
  
  #description
  Soporte para registro basado en correo electrónico y teléfono con validación.
  :::

  :::u-page-feature
  ---
  icon: i-heroicons-device-phone-mobile
  ---
  #title
  Verificación Multicanal
  
  #description
  Soporte para verificación por correo electrónico (SMTP), SMS (Twilio) y WhatsApp.
  :::

  :::u-page-feature
  ---
  icon: i-heroicons-circle-stack
  ---
  #title
  Base de Datos Aislada
  
  #description
  Migraciones Flyway dedicadas con tabla de historial separada.
  :::
::

::u-page-section
#title
Arquitectura

#description
El starter sigue los principios de autoconfiguración de Spring Boot y se integra limpiamente con tu aplicación existente.

#features
  :::u-page-feature
  ---
  icon: i-heroicons-shield-check
  ---
  #title
  Cadena de Filtros de Seguridad
  
  #description
  Opera con `@Order(1)` solo para endpoints de autenticación.
  :::

  :::u-page-feature
  ---
  icon: i-heroicons-server
  ---
  #title
  Aislamiento de Base de Datos
  
  #description
  Usa una tabla de historial Flyway separada para evitar conflictos.
  :::
::
