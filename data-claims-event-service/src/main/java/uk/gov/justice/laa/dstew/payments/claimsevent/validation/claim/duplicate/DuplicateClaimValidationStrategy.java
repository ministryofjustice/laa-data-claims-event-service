package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/** Service interface for validating duplicate claims. */
public interface DuplicateClaimValidationStrategy {

  Logger log = org.slf4j.LoggerFactory.getLogger(DuplicateClaimValidationStrategy.class);

  void validateDuplicateClaims(
      ClaimResponse claim,
      List<ClaimResponse> submissionClaims,
      String officeCode,
      SubmissionValidationContext context);

  /**
   * Log the duplicate claims found for a given claim.
   *
   * @param claim The claim to log duplicates for.
   * @param duplicateClaims The list of duplicate claims.
   */
  default void logDuplicates(final ClaimResponse claim, final List<ClaimResponse> duplicateClaims) {
    String csvDuplicateClaimIds =
        duplicateClaims.stream().map(ClaimResponse::getId).collect(Collectors.joining(","));
    log.debug(
        "{} duplicate claims found matching claim {}. Duplicates: {}",
        duplicateClaims.size(),
        claim.getId(),
        csvDuplicateClaimIds);
  }

  /**
   * Get the list of compatible area of laws for this strategy.
   *
   * @return List of compatible area of laws.
   */
  default List<String> compatibleStrategies() {
    List<String> strategies = new ArrayList<>();
    strategies.addAll(StrategyTypes.CRIME);
    strategies.addAll(StrategyTypes.CIVIL);
    strategies.addAll(StrategyTypes.MEDIATION);
    return strategies;
  }
}
