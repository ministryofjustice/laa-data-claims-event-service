package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import java.util.List;

/** Strategy for validating duplicate claims for mediation categories of law. * */
public interface MediationDuplicateClaimValidationStrategy
    extends DuplicateClaimValidationStrategy {

  default List<String> compatibleStrategies() {
    return StrategyTypes.MEDIATION;
  }
}
