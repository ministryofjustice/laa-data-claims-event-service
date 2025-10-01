package uk.gov.justice.laa.dstew.payments.claimsevent.service.strategy;

import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;

public abstract class DuplicateClaim {

  protected ClaimResponse createClaim(
      String id,
      String feeCode,
      String uniqueFileNumber,
      String uniqueClientNumber,
      ClaimStatus status) {
    return new ClaimResponse()
        .id(id)
        .feeCode(feeCode)
        .uniqueFileNumber(uniqueFileNumber)
        .uniqueClientNumber(uniqueClientNumber)
        .status(status);
  }
}
