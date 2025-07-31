package uk.gov.justice.laa.bulk.claim.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTests {

  GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

  @Test
  @DisplayName("Handle Generic Exception")
  void handleGenericException_returnsInternalServerErrorStatusAndErrorMessage() {
    ResponseEntity<String> result =
        globalExceptionHandler.handleGenericException(new RuntimeException("Something went wrong"));

    assertThat(result).isNotNull();
    assertThat(result.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody()).isEqualTo("An unexpected application error has occurred.");
  }

  @Test
  @DisplayName("Handle BulkClaimValidationException")
  void handleBulkClaimValidationException_returnsBadRequestStatusAndErrorMessage() {
    ResponseEntity<String> result =
        globalExceptionHandler.handleValidationException(
            new BulkClaimValidationException("Field is required"));

    assertThat(result).isNotNull();
    assertThat(result.getStatusCode()).isEqualTo(BAD_REQUEST);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody()).isEqualTo("Field is required");
  }
}
