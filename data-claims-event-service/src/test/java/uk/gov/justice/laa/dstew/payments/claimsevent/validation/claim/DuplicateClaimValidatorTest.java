package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.FeeCalculationType;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate.CivilDuplicateClaimValidationStrategy;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate.CrimeDuplicateClaimValidationStrategy;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate.StrategyTypes;

@ExtendWith(MockitoExtension.class)
@DisplayName("Duplicate claim validator test")
class DuplicateClaimValidatorTest {

  DuplicateClaimValidator validator;

  @Mock private CivilDuplicateClaimValidationStrategy mockCivilValidationStrategy;
  @Mock private CrimeDuplicateClaimValidationStrategy mockCrimeValidationStrategy;

  private final ClaimResponse claim =
      new ClaimResponse()
          .id(new UUID(1, 1).toString())
          .feeCode("feeCode1")
          .caseStartDate("2025-08-14")
          .uniqueFileNumber("010101/123")
          .status(ClaimStatus.READY_TO_PROCESS);

  private final List<ClaimResponse> claims = List.of(claim);

  @BeforeEach
  void setup() {
    lenient()
        .when(mockCivilValidationStrategy.compatibleStrategies())
        .thenReturn(StrategyTypes.CIVIL);
    lenient()
        .when(mockCrimeValidationStrategy.compatibleStrategies())
        .thenReturn(StrategyTypes.CRIME);
    validator =
        new DuplicateClaimValidator(
            List.of(mockCivilValidationStrategy, mockCrimeValidationStrategy));
  }

  @Test
  @DisplayName("Area of Code CIVIL: should call civil validation strategy")
  void callCivilValidationStrategy() {
    // Given
    SubmissionValidationContext context = new SubmissionValidationContext();

    // When
    validator.validate(
        claim,
        context,
        "CIVIL",
        "officeAccountNumber",
        singletonList(claim),
        FeeCalculationType.FIXED.toString());

    // Then
    verify(mockCivilValidationStrategy).validateDuplicateClaims(any(), any(), any(), any(), any());
    verify(mockCrimeValidationStrategy, times(0))
        .validateDuplicateClaims(any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("Area of Code LEGAL HELP: should call civil validation strategy")
  void callLegalHelpValidationStrategy() {
    // Given
    SubmissionValidationContext context = new SubmissionValidationContext();

    // When
    validator.validate(
        claim,
        context,
        "LEGAL HELP",
        "officeAccountNumber",
        singletonList(claim),
        FeeCalculationType.FIXED.toString());

    // Then
    verify(mockCivilValidationStrategy).validateDuplicateClaims(any(), any(), any(), any(), any());
    verify(mockCrimeValidationStrategy, times(0))
        .validateDuplicateClaims(any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("Area of Code CRIME LOWER: should call crime validation strategy")
  void crimeLowerValidationStrategy() {
    // Given
    SubmissionValidationContext context = new SubmissionValidationContext();

    // When
    validator.validate(
        claim,
        context,
        "CRIME LOWER",
        "officeAccountNumber",
        singletonList(claim),
        FeeCalculationType.FIXED.toString());

    // Then
    verify(mockCrimeValidationStrategy)
        .validateDuplicateClaims(
            claim, claims, "officeAccountNumber", context, FeeCalculationType.FIXED.toString());
    verify(mockCivilValidationStrategy, times(0))
        .validateDuplicateClaims(any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("Area of Code CRIME: should call crime validation strategy")
  void crimeValidationStrategy() {
    // Given
    SubmissionValidationContext context = new SubmissionValidationContext();

    // When
    validator.validate(
        claim,
        context,
        "CRIME",
        "officeAccountNumber",
        singletonList(claim),
        FeeCalculationType.FIXED.toString());

    // Then
    verify(mockCrimeValidationStrategy)
        .validateDuplicateClaims(
            claim, claims, "officeAccountNumber", context, FeeCalculationType.FIXED.toString());
    verify(mockCivilValidationStrategy, times(0))
        .validateDuplicateClaims(any(), any(), any(), any(), any());
  }
}
