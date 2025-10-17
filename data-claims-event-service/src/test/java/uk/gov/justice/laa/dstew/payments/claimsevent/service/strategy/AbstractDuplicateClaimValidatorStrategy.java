package uk.gov.justice.laa.dstew.payments.claimsevent.service.strategy;

import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;

public abstract class AbstractDuplicateClaimValidatorStrategy {

  protected ClaimResponse createClaim(
      String id,
      String feeCode,
      String uniqueFileNumber,
      String uniqueClientNumber,
      ClaimStatus status) {
    return createClaim(id, feeCode, uniqueFileNumber, uniqueClientNumber, status, null, null);
  }

  protected ClaimResponse createClaim(
      String id,
      String feeCode,
      String uniqueFileNumber,
      String uniqueClientNumber,
      ClaimStatus status,
      String submissionPeriod,
      String uniqueCaseId) {
    return new ClaimResponse()
        .id(id)
        .feeCode(feeCode)
        .uniqueFileNumber(uniqueFileNumber)
        .uniqueClientNumber(uniqueClientNumber)
        .status(status)
        .submissionPeriod(submissionPeriod)
        .uniqueCaseId(uniqueCaseId);
  }
}
