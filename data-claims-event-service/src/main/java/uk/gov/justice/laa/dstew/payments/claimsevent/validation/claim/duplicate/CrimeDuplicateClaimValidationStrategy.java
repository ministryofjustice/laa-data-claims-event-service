package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import java.util.List;

/**
 * Strategy for validating duplicate claims for crime law areas of law.
 *
 * @author Jamie Briggs
 */
public interface CrimeDuplicateClaimValidationStrategy extends DuplicateClaimValidationStrategy {

  default List<String> compatibleStrategies() {
    return StrategyTypes.CRIME;
  }
}
