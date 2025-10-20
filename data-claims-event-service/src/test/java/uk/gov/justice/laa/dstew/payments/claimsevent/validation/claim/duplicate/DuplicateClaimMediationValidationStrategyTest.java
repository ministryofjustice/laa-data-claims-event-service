package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResultSet;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.strategy.AbstractDuplicateClaimValidatorStrategy;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

@ExtendWith(MockitoExtension.class)
class DuplicateClaimMediationValidationStrategyTest
    extends AbstractDuplicateClaimValidatorStrategy {
  @Mock DataClaimsRestClient dataClaimsRestClient;

  @InjectMocks DuplicateClaimMediationValidationStrategy duplicateClaimMediationValidationStrategy;

  @Nested
  class ValidClaim {

    @Test
    @DisplayName("When a mediation claim is submitted the claim passes duplicate checks")
    void shouldHaveNoErrorsWhenCurrentClaimIsMediation() {
      // Given
      var claimTobeProcessed =
          createClaim(
              "claimId1",
              "submissionId1",
              "MED001",
              "070722/001",
              "CLI001",
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              "220724/001");
      var claim2 =
          createClaim(
              "claimId2",
              "submissionId1",
              "MED001",
              "070722/001",
              "CLI001",
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              "220724/002");
      SubmissionValidationContext context = new SubmissionValidationContext();

      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(Optional.of(new ClaimResultSet().content(List.of(claim2)))));
      // When
      duplicateClaimMediationValidationStrategy.validateDuplicateClaims(
          claimTobeProcessed, List.of(claimTobeProcessed, claim2), "1", context);

      // Then
      assertThat(context.hasErrors()).isFalse();
      verify(dataClaimsRestClient, times(1))
          .getClaims(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Duplicate detected against a previously submitted submission claims")
    void shouldErrorWhenDuplicateDetectedAgainstAPreviouslySubmittedSubmissionClaims() {
      // Given
      var claimTobeProcessed =
          createClaim(
              "claimId1",
              "submissionId1",
              "MED001",
              "070722/001",
              "CLI001",
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              "220724/001");
      var claim2 =
          createClaim(
              "claimId2",
              "submissionId2",
              "MED001",
              "070722/001",
              "CLI001",
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              "220724/001");
      SubmissionValidationContext context = new SubmissionValidationContext();

      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(Optional.of(new ClaimResultSet().content(List.of(claim2)))));
      // When
      duplicateClaimMediationValidationStrategy.validateDuplicateClaims(
          claimTobeProcessed, List.of(claimTobeProcessed), "1", context);
      // Then
      assertThat(context.hasErrors()).isTrue();
      verify(dataClaimsRestClient, times(1))
          .getClaims(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName(
        "Duplicates detected against current claims submission returns error in validation checks")
    void shouldErrorWhenDuplicateDetectedAgainstCurrentClaimSubmission() {
      // Given
      var claimTobeProcessed =
          createClaim(
              "claimId1",
              "submissionId1",
              "MED001",
              "070722/001",
              "CLI001",
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              "220724/001");
      var previousClaim =
          createClaim(
              "claimId2",
              "submissionId2",
              "MED001",
              "070722/002",
              "CLI001",
              ClaimStatus.READY_TO_PROCESS,
              "MAY-2025",
              "220724/001");
      SubmissionValidationContext context = new SubmissionValidationContext();

      when(dataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(Optional.of(new ClaimResultSet().content(List.of(previousClaim)))));
      // When
      duplicateClaimMediationValidationStrategy.validateDuplicateClaims(
          claimTobeProcessed, List.of(claimTobeProcessed), "1", context);
      // Then
      assertThat(context.hasErrors()).isTrue();
      verify(dataClaimsRestClient, times(1))
          .getClaims(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
  }
}
