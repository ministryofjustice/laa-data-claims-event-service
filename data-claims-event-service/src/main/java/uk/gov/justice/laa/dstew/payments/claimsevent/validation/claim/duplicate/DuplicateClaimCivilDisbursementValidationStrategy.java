package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.FeeCalculationType;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/** Validation strategy for civil duplicate claims. */
@Slf4j
@Component
public class DuplicateClaimCivilDisbursementValidationStrategy extends DuplicateClaimValidation
    implements CivilDuplicateClaimValidationStrategy {

  private static final String DISBURSEMENT_FEE_TYPE =
      FeeCalculationType.DISBURSEMENT_ONLY.toString();
  private static final int MAXIMUM_MONTHS_DIFFERENCE = 3;

  private final FeeSchemePlatformRestClient feeSchemePlatformRestClient;

  private final DateTimeFormatter formatter;

  /**
   * Creates a new {@code DuplicateClaimCivilDisbursementValidationStrategy}.
   *
   * @param dataClaimsRestClient the data claims rest client
   * @param feeSchemePlatformRestClient the fee scheme platform rest client
   */
  @Autowired
  public DuplicateClaimCivilDisbursementValidationStrategy(
      final DataClaimsRestClient dataClaimsRestClient,
      final FeeSchemePlatformRestClient feeSchemePlatformRestClient) {
    super(dataClaimsRestClient);
    this.feeSchemePlatformRestClient = feeSchemePlatformRestClient;
    formatter =
        new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMM-yyyy")
            .toFormatter(Locale.ENGLISH);
  }

  @Override
  public void validateDuplicateClaims(
      final ClaimResponse currentClaim,
      final List<ClaimResponse> submissionClaims,
      final String officeCode,
      final SubmissionValidationContext context) {

    // Don't check if current claim is not a disbursement, this validation strategy only applies
    //  to disbursement claims.
    if (!isDisbursementClaim(currentClaim)) {
      return;
    }

    List<ClaimResponse> duplicateClaimsInPreviousSubmission =
        getDuplicateClaimsInPreviousSubmission(
            officeCode,
            currentClaim.getFeeCode(),
            currentClaim.getUniqueFileNumber(),
            currentClaim.getUniqueClientNumber(),
            submissionClaims);

    YearMonth currentClaimYearMonth =
        YearMonth.parse(currentClaim.getSubmissionPeriod(), formatter);

    duplicateClaimsInPreviousSubmission.stream()
        .filter(
            x ->
                // Only include claims that are within 3 months of the current claim (date not
                // included)
                filterByInclusiveMonths(x, currentClaimYearMonth))
        .forEach(
            duplicateClaim -> {
              logDuplicates(duplicateClaim, duplicateClaimsInPreviousSubmission);
              context.addClaimError(
                  currentClaim.getId(),
                  ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION);
            });
  }

  private boolean filterByInclusiveMonths(
      ClaimResponse claimResponse, YearMonth currentClaimYearMonth) {
    YearMonth duplicateClaimYearMonth =
        YearMonth.parse(claimResponse.getSubmissionPeriod(), formatter);

    long monthsDifference =
        YearMonth.from(duplicateClaimYearMonth).until(currentClaimYearMonth, ChronoUnit.MONTHS);
    return monthsDifference < MAXIMUM_MONTHS_DIFFERENCE;
  }

  private Boolean isDisbursementClaim(final ClaimResponse currentClaim) {
    return Objects.equals(
        Objects.requireNonNull(
                feeSchemePlatformRestClient.getFeeDetails(currentClaim.getFeeCode()).getBody())
            .getFeeType(),
        DISBURSEMENT_FEE_TYPE);
  }
}
