package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.strategy.DuplicateClaimValidationStrategy;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.strategy.StrategyTypes;

/**
 * Validates that a claim is not a duplicate of a previous claim.
 *
 * @author Jamie Briggs
 * @see ClaimResponse
 * @see SubmissionValidationContext
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DuplicateClaimValidator implements ClaimValidator {

  private final Map<String, DuplicateClaimValidationStrategy> strategies;

  /**
   * Validates a claim to ensure it is not a duplicate of another claim, based on the provided
   * validation context, area of law, office code, and existing submission claims.
   *
   * @param claim the {@code ClaimResponse} object to be validated.
   * @param context the {@code SubmissionValidationContext} containing validation-related
   *     information.
   * @param areaOfLaw the area of law to which the claim pertains (e.g., "CRIME" or "CIVIL").
   * @param officeCode the code of the office associated with the claim.
   * @param submissionClaims the list of previously submitted claims for duplicate validation.
   */
  public void validate(
      final ClaimResponse claim,
      final SubmissionValidationContext context,
      final String areaOfLaw,
      final String officeCode,
      final List<ClaimResponse> submissionClaims) {
    switch (areaOfLaw) {
      case StrategyTypes.CRIME ->
          strategies
              .get(StrategyTypes.CRIME)
              .validateDuplicateClaims(claim, submissionClaims, officeCode, context);
      case StrategyTypes.CIVIL ->
          strategies
              .get(StrategyTypes.CIVIL)
              .validateDuplicateClaims(claim, submissionClaims, officeCode, context);
      default ->
          log.debug("No duplicate claim validation strategy found for area of law: {}", areaOfLaw);
    }
  }

  @Override
  public int priority() {
    return 10000;
  }
}
