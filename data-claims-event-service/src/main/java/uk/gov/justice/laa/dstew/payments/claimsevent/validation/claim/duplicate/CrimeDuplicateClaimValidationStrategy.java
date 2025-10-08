package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import java.util.List;

public interface CrimeDuplicateClaimValidationStrategy extends DuplicateClaimValidationStrategy {

  default List<String> compatibleStrategies() {
    return List.of(StrategyTypes.CRIME);
  }

}
