package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate;

import java.util.List;

public interface CivilDuplicateClaimValidationStrategy
    extends DuplicateClaimValidationStrategy {

  default List<String> compatibleStrategies(){
    return List.of(StrategyTypes.CIVIL);
  }

}
