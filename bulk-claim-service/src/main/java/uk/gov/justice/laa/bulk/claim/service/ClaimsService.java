package uk.gov.justice.laa.bulk.claim.service;

import java.util.Optional;
import uk.gov.justice.laa.bulk.claim.service.dto.BulkSubmissionRequest;
import uk.gov.justice.laa.bulk.claim.service.dto.BulkSubmissionResponse;
import uk.gov.justice.laa.claims.model.ClaimDto;

/** BulkSubmissionClient interface. */
public interface ClaimsService {

  BulkSubmissionResponse submitBulkClaim(BulkSubmissionRequest request);
  Optional<ClaimDto> getClaim(String claimId);
}
