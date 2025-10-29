package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import java.util.List;

/**
 * Strategy for validating duplicate claims for legal help law areas of law.
 *
 * @author Jamie Briggs
 */
public interface LegalHelpDuplicateClaimValidationStrategy
    extends DuplicateClaimValidationStrategy {

  default List<String> compatibleStrategies() {
    return StrategyTypes.LEGAL_HELP;
  }
}
