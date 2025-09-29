package uk.gov.justice.laa.dstew.payments.claimsevent.service.strategy;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/** Validation service for civil duplicate claims. */
@Slf4j
@Service(StrategyTypes.CIVIL)
public class DuplicateClaimCivilValidationServiceStrategy extends DuplicateClaimValidation
    implements DuplicateClaimValidationStrategy {
  private static final String DISBURSEMENT_FEE_TYPE = "DISBURSEMENT ONLY";

  private final FeeSchemePlatformRestClient feeSchemePlatformRestClient;

  @Autowired
  public DuplicateClaimCivilValidationServiceStrategy(
      final DataClaimsRestClient dataClaimsRestClient,
      final FeeSchemePlatformRestClient feeSchemePlatformRestClient) {
    super(dataClaimsRestClient);
    this.feeSchemePlatformRestClient = feeSchemePlatformRestClient;
  }

  @Override
  public void validateDuplicateClaims(
      final ClaimResponse currentClaim,
      final List<ClaimResponse> submissionClaims,
      final String officeCode,
      final SubmissionValidationContext context) {

    List<ClaimResponse> otherClaimsWithNonInvalidStatus =
        filterCurrentClaimWithNonInvalidStatus(currentClaim, submissionClaims);

    List<ClaimResponse> duplicateClaimsInThisSubmission =
        getDuplicateClaimsInCurrentSubmission(
            otherClaimsWithNonInvalidStatus,
            duplicatePredicate ->
                duplicatePredicate.getFeeCode().equals(currentClaim.getFeeCode())
                    && duplicatePredicate
                        .getUniqueFileNumber()
                        .equals(currentClaim.getUniqueFileNumber())
                    && duplicatePredicate
                        .getUniqueClientNumber()
                        .equals(currentClaim.getUniqueClientNumber()));

    List<ClaimResponse> duplicateClaimsInPreviousSubmission =
        isDisbursementClaim(currentClaim)
            ? Collections.emptyList()
            : getDuplicateClaimsInPreviousSubmission(
                officeCode,
                currentClaim.getFeeCode(),
                currentClaim.getUniqueFileNumber(),
                currentClaim.getUniqueClientNumber(),
                submissionClaims);

    duplicateClaimsInThisSubmission.forEach(
        duplicateClaim -> {
          logDuplicates(duplicateClaim, duplicateClaimsInThisSubmission);
          context.addClaimError(
              duplicateClaim.getId(),
              ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_EXISTING_SUBMISSION);
        });

    duplicateClaimsInPreviousSubmission.forEach(
        duplicateClaim -> {
          logDuplicates(duplicateClaim, duplicateClaimsInPreviousSubmission);
          context.addClaimError(
              currentClaim.getId(),
              ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION);
        });
  }

  private Boolean isDisbursementClaim(final ClaimResponse currentClaim) {
    return Objects.equals(
        Objects.requireNonNull(
                feeSchemePlatformRestClient.getFeeDetails(currentClaim.getFeeCode()).getBody())
            .getFeeType(),
        DISBURSEMENT_FEE_TYPE);
  }

  private void logDuplicates(final ClaimResponse claim, final List<ClaimResponse> duplicateClaims) {
    String csvDuplicateClaimIds =
        duplicateClaims.stream().map(ClaimResponse::getId).collect(Collectors.joining(","));
    log.debug(
        "{} duplicate claims found matching claim {}. Duplicates: {}",
        duplicateClaims.size(),
        claim.getId(),
        csvDuplicateClaimIds);
  }
}
