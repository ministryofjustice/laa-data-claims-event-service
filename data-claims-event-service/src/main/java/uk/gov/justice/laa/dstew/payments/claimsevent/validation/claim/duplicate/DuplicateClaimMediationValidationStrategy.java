package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/**
 * Duplicate claims Validation Mediation Strategy.
 *
 * <p>This was originally implemented but removed as part of BC-423. Please check <a
 * href="https://github.com/ministryofjustice/laa-data-claims-event-service/tree/0.0.122">
 * 0.0.122</a> for the previous implementation. This class has been kept for auditing purposes of
 * the previous implementation, and for needing to potentially implement a different duplicate claim
 * strategy in the near future.
 */
@Deprecated(since = "0.0.122")
// @Service - Unregistered this Spring Bean
@Slf4j
public final class DuplicateClaimMediationValidationStrategy extends DuplicateClaimValidation
    implements MediationDuplicateClaimValidationStrategy {

  public DuplicateClaimMediationValidationStrategy(DataClaimsRestClient dataClaimsRestClient) {
    super(dataClaimsRestClient);
  }

  @Override
  public void validateDuplicateClaims(
      ClaimResponse claim,
      List<ClaimResponse> submissionClaims,
      String officeCode,
      SubmissionValidationContext context) {
    log.debug(
        "Duplicate check for Legal Help Mediation claim {} not performed as it has been removed.",
        claim.getId());
  }
}
