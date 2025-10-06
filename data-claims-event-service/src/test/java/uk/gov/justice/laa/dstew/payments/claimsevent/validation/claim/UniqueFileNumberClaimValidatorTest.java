package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.assertContextClaimError;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

@DisplayName("Unique file number claim validator test")
class UniqueFileNumberClaimValidatorTest {

  UniqueFileNumberClaimValidator validator = new UniqueFileNumberClaimValidator();

  @Test
  @DisplayName("Should have no errors")
  void shouldHaveNoErrors() {
    // Given
    String uniqueFileNumber = "010101/123";
    String claimId = new UUID(1, 1).toString();
    ClaimResponse claimResponse =
        new ClaimResponse().id(claimId).uniqueFileNumber(uniqueFileNumber);

    SubmissionValidationContext context = new SubmissionValidationContext();
    // When
    validator.validate(claimResponse, context);
    // Then
    assertThat(context.hasErrors(claimId)).isFalse();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("Should have no errors if ufn is null or empty (Handled in "
      + "MandatoryFieldClaimValidator)")
  void shouldHaveErrorsIfUfnIsEmpty(String ufn) {
    // Given
    String claimId = new UUID(1, 1).toString();
    ClaimResponse claimResponse = new ClaimResponse().id(claimId).uniqueFileNumber(ufn);

    SubmissionValidationContext context = new SubmissionValidationContext();
    // When
    validator.validate(claimResponse, context);
    // Then
    assertThat(context.hasErrors(claimId)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "010130",
      "123",
      "0101030/123",
      "010101/1234",
      "01010/12345",
      "abcdef/123",
      "010130/abc",
  })
  @DisplayName("Should have errors if UFN is not in correct format")
  void shouldHaveAnErrorIfUfnIsNotInCorrectFormat(String ufn) {
    // Given
    String claimId = new UUID(1, 1).toString();
    ClaimResponse claimResponse = new ClaimResponse().id(claimId).uniqueFileNumber(ufn);

    SubmissionValidationContext context = new SubmissionValidationContext();
    // When
    validator.validate(claimResponse, context);
    // Then
    assertThat(context.hasErrors(claimId)).isTrue();
    assertContextClaimError(
        context, claimId, ClaimValidationError.INVALID_DATE_IN_UNIQUE_FILE_NUMBER);
  }

  @Test
  @DisplayName("Should have errors if date part of UFN is after today")
  void shouldHaveErrorsIfDatePartOfUfnIsAfterToday() {
    // Given
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyy");
    String format = formatter.format(LocalDate.now().plusDays(1));
    String ufn = format + "/123";
    String claimId = new UUID(1, 1).toString();
    ClaimResponse claimResponse = new ClaimResponse().id(claimId).uniqueFileNumber(ufn);

    SubmissionValidationContext context = new SubmissionValidationContext();
    // When
    validator.validate(claimResponse, context);
    // Then
    assertThat(context.hasErrors(claimId)).isTrue();
    assertContextClaimError(
        context, claimId, ClaimValidationError.INVALID_DATE_IN_UNIQUE_FILE_NUMBER);
  }

}