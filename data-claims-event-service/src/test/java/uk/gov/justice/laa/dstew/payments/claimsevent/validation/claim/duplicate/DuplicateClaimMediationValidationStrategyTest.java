package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.FeeCalculationType;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.strategy.AbstractDuplicateClaimValidatorStrategy;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

@ExtendWith(MockitoExtension.class)
class DuplicateClaimMediationValidationStrategyTest
    extends AbstractDuplicateClaimValidatorStrategy {

  @Mock DataClaimsRestClient dataClaimsRestClient;

  @InjectMocks DuplicateClaimMediationValidationStrategy duplicateClaimMediationValidationStrategy;

  @Test
  @DisplayName("When a mediation claim is submitted the claim passes duplicate checks")
  void whenMediationClaimSubmittedDuplicateChecksPass() {
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

    // When
    duplicateClaimMediationValidationStrategy.validateDuplicateClaims(
        claimTobeProcessed,
        List.of(claimTobeProcessed, claim2),
        "1",
        context,
        FeeCalculationType.FIXED.toString());

    // Then
    verify(dataClaimsRestClient, times(0))
        .getClaims(any(), any(), any(), any(), any(), any(), any(), any(), any());
    assertThat(context.hasErrors()).isFalse();
  }
}
