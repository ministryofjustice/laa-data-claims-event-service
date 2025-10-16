package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.FeeCalculationType;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/** Validation service for civil duplicate claims. */
@Slf4j
@Service
public class DuplicateClaimCivilValidationServiceStrategy extends DuplicateClaimValidation
    implements CivilDuplicateClaimValidationStrategy {
  private static final String DISBURSEMENT_FEE_TYPE =
      FeeCalculationType.DISBURSEMENT_ONLY.toString();

  @Autowired
  public DuplicateClaimCivilValidationServiceStrategy(
      final DataClaimsRestClient dataClaimsRestClient,
      final FeeSchemePlatformRestClient feeSchemePlatformRestClient) {
    super(dataClaimsRestClient, feeSchemePlatformRestClient);
  }

  @Override
  public void validateDuplicateClaims(
      final ClaimResponse currentClaim,
      final List<ClaimResponse> submissionClaims,
      final String officeCode,
      final SubmissionValidationContext context) {

    // Don't check other claims if current claim is a disbursement claim
    List<ClaimResponse> duplicateClaimsInPreviousSubmission =
        isDisbursementClaim(currentClaim)
            ? Collections.emptyList()
            : getDuplicateClaimsInPreviousSubmission(
                officeCode,
                currentClaim.getFeeCode(),
                currentClaim.getUniqueFileNumber(),
                currentClaim.getUniqueClientNumber(),
                submissionClaims);

    duplicateClaimsInPreviousSubmission.forEach(
        duplicateClaim -> {
          logDuplicates(duplicateClaim, duplicateClaimsInPreviousSubmission);
          context.addClaimError(
              currentClaim.getId(),
              ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION);
        });
  }
}
