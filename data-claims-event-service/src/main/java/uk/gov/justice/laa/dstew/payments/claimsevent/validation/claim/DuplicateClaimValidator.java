package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import java.util.List;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate.DuplicateClaimValidationStrategy;

/**
 * Validates that a claim is not a duplicate of a previous claim.
 *
 * @author Jamie Briggs
 * @see ClaimResponse
 * @see SubmissionValidationContext
 */
@Component
@Slf4j
public final class DuplicateClaimValidator implements ClaimValidator {

  private final List<DuplicateClaimValidationStrategy> strategyList;

  public DuplicateClaimValidator(List<DuplicateClaimValidationStrategy> strategyList) {
    this.strategyList = strategyList;
  }

  /**
   * Validates a claim to ensure it is not a duplicate of another claim, based on the provided
   * validation context, area of law, office code, and existing submission claims.
   *
   * @param claim the {@code ClaimResponse} object to be validated.
   * @param context the {@code SubmissionValidationContext} containing validation-related
   *     information.
   * @param areaOfLaw the area of law to which the claim pertains (e.g., "CRIME LOWER" or "LEGAL
   *     HELP").
   * @param officeCode the code of the office associated with the claim.
   * @param submissionClaims the list of previously submitted claims for duplicate validation.
   */
  public void validate(
      final ClaimResponse claim,
      final SubmissionValidationContext context,
      final AreaOfLaw areaOfLaw,
      final String officeCode,
      final List<ClaimResponse> submissionClaims,
      final String feeType) {

    final Predicate<DuplicateClaimValidationStrategy> areaOfLawPredicate =
        x -> x.compatibleStrategies().contains(areaOfLaw.getValue());

    final List<DuplicateClaimValidationStrategy> compatibleStrategies =
        strategyList.stream().filter(areaOfLawPredicate).toList();

    if (compatibleStrategies.isEmpty()) {
      log.debug("No duplicate claim validation strategy found for area of law: {}", areaOfLaw);
    }

    compatibleStrategies.forEach(
        strategy ->
            strategy.validateDuplicateClaims(
                claim, submissionClaims, officeCode, context, feeType));
  }

  @Override
  public int priority() {
    return 10000;
  }
}
