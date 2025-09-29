package uk.gov.justice.laa.dstew.payments.claimsevent.service.strategy;

import java.util.List;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/** Service interface for validating duplicate claims. */
public interface DuplicateClaimValidationStrategy {
  void validateDuplicateClaims(
      ClaimResponse claim,
      List<ClaimResponse> submissionClaims,
      String officeCode,
      SubmissionValidationContext context);
}
