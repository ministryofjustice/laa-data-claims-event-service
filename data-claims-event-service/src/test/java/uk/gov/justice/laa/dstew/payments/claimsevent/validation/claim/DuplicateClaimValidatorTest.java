package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.strategy.DuplicateClaimCivilValidationServiceStrategy;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.strategy.DuplicateClaimCrimeValidationServiceStrategy;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.strategy.DuplicateClaimValidationStrategy;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.strategy.StrategyTypes;

@ExtendWith(MockitoExtension.class)
@DisplayName("Duplicate claim validator test")
class DuplicateClaimValidatorTest {

  DuplicateClaimValidator validator;

  @Mock private Map<String, DuplicateClaimValidationStrategy> strategies;

  @Mock private DuplicateClaimCrimeValidationServiceStrategy duplicateClaimCrimeValidationService;

  @Mock
  private DuplicateClaimCivilValidationServiceStrategy
      mockDuplicateClaimCivilValidationServiceStrategy;

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
        .when(strategies.get(StrategyTypes.CRIME))
        .thenReturn(duplicateClaimCrimeValidationService);
    lenient()
        .when(strategies.get(StrategyTypes.CIVIL))
        .thenReturn(mockDuplicateClaimCivilValidationServiceStrategy);
    validator = new DuplicateClaimValidator(strategies);
  }

  @DisplayName("Area of Code CIVIL: should call civil validation strategy")
  @Test
  void callCivilValidationStrategy() {
    // Given
    SubmissionValidationContext context = new SubmissionValidationContext();

    // When
    validator.validate(claim, context, "CIVIL", "officeAccountNumber", singletonList(claim));

    // Then
    verify(mockDuplicateClaimCivilValidationServiceStrategy)
        .validateDuplicateClaims(this.claim, claims, "officeAccountNumber", context);
    verify(duplicateClaimCrimeValidationService, times(0))
        .validateDuplicateClaims(any(), any(), any(), any());
  }

  @DisplayName("Area of Code CRIME_LOWER: should call crime validation strategy")
  @Test
  void crimeValidationStrategy() {
    // Given
    SubmissionValidationContext context = new SubmissionValidationContext();

    // When
    validator.validate(claim, context, "CRIME_LOWER", "officeAccountNumber", singletonList(claim));

    // Then
    verify(duplicateClaimCrimeValidationService)
        .validateDuplicateClaims(claim, claims, "officeAccountNumber", context);
    verify(mockDuplicateClaimCivilValidationServiceStrategy, times(0))
        .validateDuplicateClaims(any(), any(), any(), any());
  }
}
