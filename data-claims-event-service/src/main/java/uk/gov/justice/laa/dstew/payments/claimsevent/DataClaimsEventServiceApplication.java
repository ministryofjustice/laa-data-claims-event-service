package uk.gov.justice.laa.dstew.payments.claimsevent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/** Entry point for the Spring Boot microservice application. */
@SpringBootApplication
@ComponentScan(
    basePackages = {
      "uk.gov.justice.laa.dstew.payments.claimsdata",
      "uk.gov.justice.laa.dstew.payments.claimsevent",
      "uk.gov.justice.laa.dstew.payments.claims.validation.core"
    })
public class DataClaimsEventServiceApplication {

  /**
   * The application main method.
   *
   * @param args the application arguments.
   */
  public static void main(String[] args) {
    SpringApplication.run(DataClaimsEventServiceApplication.class, args);
  }
}
