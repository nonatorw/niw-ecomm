package com.niw.ecomm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the niw-ecomm Spring Boot application.
 *
 * <p>
 * Starts the embedded web server, initialises the JPA context, and loads seed
 * data from {@code classpath:db/data.sql} into the file-based H2 database.
 */
@SpringBootApplication
public class NiwEcommApplication {

  /**
   * Application entry point.
   *
   * @param args command-line arguments passed to the Spring context
   */
  public static void main(String[] args) {
    SpringApplication.run(NiwEcommApplication.class, args);
  }
}
