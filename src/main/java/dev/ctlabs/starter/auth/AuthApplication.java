package dev.ctlabs.starter.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Auth Spring Boot Starter.
 * Bootstrap the application.
 */
@SpringBootApplication
public class AuthApplication {

    /**
     * Main entry point for the application.
     *
     * @param args Command line arguments.
     */
    static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
