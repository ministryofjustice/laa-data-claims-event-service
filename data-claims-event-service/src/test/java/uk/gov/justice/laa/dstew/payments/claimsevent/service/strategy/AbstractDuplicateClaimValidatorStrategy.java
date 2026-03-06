package uk.gov.justice.laa.dstew.payments.claimsevent.service.strategy;

import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;

public abstract class AbstractDuplicateClaimValidatorStrategy {

  protected ClaimResponse createClaim(
      String id,
      String submissionId,
      String feeCode,
      String uniqueFileNumber,
      String uniqueClientNumber,
      ClaimStatus status) {
    return createClaim(
        id, submissionId, feeCode, uniqueFileNumber, uniqueClientNumber, status, null, null);
  }

  protected ClaimResponse createClaim(
      String id,
      String submissionId,
      String feeCode,
      String uniqueFileNumber,
      String uniqueClientNumber,
      ClaimStatus status,
      String submissionPeriod,
      String uniqueCaseId) {
    return createClaim(
        id,
        submissionId,
        feeCode,
        uniqueFileNumber,
        uniqueClientNumber,
        status,
        submissionPeriod,
        uniqueCaseId,
        null);
  }

  protected ClaimResponse createClaim(
      String id,
      String submissionId,
      String feeCode,
      String uniqueFileNumber,
      String uniqueClientNumber,
      ClaimStatus status,
      String submissionPeriod,
      String uniqueCaseId,
      String caseConcludedDate) {
    return new ClaimResponse()
        .id(id)
        .submissionId(submissionId)
        .feeCode(feeCode)
        .uniqueFileNumber(uniqueFileNumber)
        .uniqueClientNumber(uniqueClientNumber)
        .status(status)
        .submissionPeriod(submissionPeriod)
        .uniqueCaseId(uniqueCaseId)
        .caseConcludedDate(caseConcludedDate);
  }
}
