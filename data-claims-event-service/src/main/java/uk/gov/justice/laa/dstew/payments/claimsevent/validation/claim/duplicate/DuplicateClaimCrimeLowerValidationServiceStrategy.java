package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/** Service responsible for validating whether a claim is a duplicate. */
@Slf4j
@Service
public final class DuplicateClaimCrimeLowerValidationServiceStrategy
    extends DuplicateClaimValidation {

  @Autowired
  public DuplicateClaimCrimeLowerValidationServiceStrategy(
      DataClaimsRestClient dataClaimsRestClient) {
    super(dataClaimsRestClient);
  }

  @Override
  public void validateDuplicateClaims(
      ClaimResponse currentClaim,
      List<ClaimResponse> submissionClaims,
      String officeCode,
      SubmissionValidationContext context) {

    log.debug(
        "[{}] Validating duplicates for claim {}",
        getClass().getSimpleName(),
        currentClaim.getId());

    // Skip the duplicate check entirely for claims already flagged for retry (e.g. a transient
    // Fee Scheme Platform error earlier in this same validation pass - see
    // EffectiveCategoryOfLawClaimValidator/CategoryOfLawValidationService). This is not a no-op:
    // BulkClaimUpdater only defers persisting a retry-flagged claim when it has no errors. If the
    // duplicate check ran anyway and added an error here, the claim would be persisted as INVALID
    // this round using partial/unresolved fee data, instead of being cleanly deferred for retry.
    if (context.isFlaggedForRetry(currentClaim.getId())) {
      log.debug(
          "[{}] Claim {} is flagged for retry, skipping duplicate check",
          getClass().getSimpleName(),
          currentClaim.getId());
      return;
    }

    if ("PROD".equals(currentClaim.getFeeCode())) {
      // Skipping PRD duplicate check. This is because PROD fee code do not have a unique
      // identifier and client details are not mandatory for this fee code. This was originally
      // implemented but then removed as part of BC-418. Please check
      // https://github.com/ministryofjustice/laa-data-claims-event-service/releases/tag/0.0.121
      // for the previous implementation of this check.
      log.debug("Fee code is PROD, skipping duplicate check for claim {}", currentClaim.getId());
      return;
    }

    // Get all claims from the API and report any duplicates found in the current submission
    // or a previous submission.
    checkSameAndPreviousSubmissionDuplicates(
        currentClaim,
        officeCode,
        currentClaim.getFeeCode(),
        currentClaim.getUniqueFileNumber(),
        null,
        context);

    log.debug(
        "[{}] Duplicate validation completed for claim {}",
        getClass().getSimpleName(),
        currentClaim.getId());
  }

  @Override
  public List<String> compatibleStrategies() {
    return List.of(AreaOfLaw.CRIME_LOWER.getValue());
  }
}
