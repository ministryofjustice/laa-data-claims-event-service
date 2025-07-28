package uk.gov.justice.laa.bulk.claim.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** The global exception handler for all exceptions. */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  /**
   * The handler for Exception.
   *
   * @param exception the exception
   * @return the response status with error message
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleGenericException(Exception exception) {
    String logMessage = "An unexpected application error has occurred.";
    log.error(logMessage, exception);
    return ResponseEntity.internalServerError().body(logMessage);
  }

  /**
   * Handles validation-related exceptions by returning a HTTP 400 Bad Request status with the
   * corresponding error message from the exception.
   *
   * @param ex the IllegalArgumentException encountered during validation
   * @return a ResponseEntity containing the HTTP Bad Request status and the exception message
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleValidationException(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
  }
}
