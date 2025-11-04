package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.FeeCalculationType;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/** Validation service for legal help duplicate claims. */
@Slf4j
@Service
public final class DuplicateClaimLegalHelpValidationServiceStrategy extends DuplicateClaimValidation
    implements LegalHelpDuplicateClaimValidationStrategy {
  private static final String DISBURSEMENT_FEE_TYPE = FeeCalculationType.DISB_ONLY.getValue();

  @Autowired
  public DuplicateClaimLegalHelpValidationServiceStrategy(
      final DataClaimsRestClient dataClaimsRestClient,
      final FeeSchemePlatformRestClient feeSchemePlatformRestClient) {
    super(dataClaimsRestClient);
  }

  @Override
  public void validateDuplicateClaims(
      final ClaimResponse currentClaim,
      final List<ClaimResponse> submissionClaims,
      final String officeCode,
      final SubmissionValidationContext context,
      final String feeType) {

    // Don't check other claims if the current claim is a disbursement claim
    List<ClaimResponse> duplicateClaimsInPreviousSubmission =
        isDisbursementClaim(feeType)
            ? Collections.emptyList()
            : getDuplicateClaimsInPreviousSubmission(
                officeCode,
                currentClaim.getFeeCode(),
                currentClaim.getUniqueFileNumber(),
                currentClaim.getUniqueClientNumber(),
                null,
                submissionClaims);

    // add an error against the currentClaim if there are any duplicates are found.
    if (!duplicateClaimsInPreviousSubmission.isEmpty()) {
      logDuplicates(currentClaim, duplicateClaimsInPreviousSubmission);
      context.addClaimError(
          currentClaim.getId(),
          ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION);
    }
  }

  private Boolean isDisbursementClaim(final String feeType) {
    return Objects.equals(feeType, DISBURSEMENT_FEE_TYPE);
  }
}
