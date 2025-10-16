package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import java.util.List;

public interface MediationDuplicateClaimValidationStrategy extends DuplicateClaimValidationStrategy {

  default List<String> compatibleStrategies() {
    return StrategyTypes.MEDIATION;
  }
}
