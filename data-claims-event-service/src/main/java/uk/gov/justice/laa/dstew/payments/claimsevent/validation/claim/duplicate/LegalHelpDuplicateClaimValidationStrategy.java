package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import java.util.List;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;

/**
 * Strategy for validating duplicate claims for legal help law areas of law.
 *
 * @author Jamie Briggs
 */
public interface LegalHelpDuplicateClaimValidationStrategy
    extends DuplicateClaimValidationStrategy {

  default List<String> compatibleStrategies() {
    return List.of(AreaOfLaw.LEGAL_HELP.getValue());
  }
}
