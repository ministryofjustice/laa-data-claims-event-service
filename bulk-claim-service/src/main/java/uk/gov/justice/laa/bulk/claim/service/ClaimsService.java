package uk.gov.justice.laa.bulk.claim.service;

import uk.gov.justice.laa.bulk.claim.service.dto.BulkSubmissionRequest;
import uk.gov.justice.laa.bulk.claim.service.dto.BulkSubmissionResponse;

/** BulkSubmissionClient interface. */
public interface ClaimsService {

  BulkSubmissionResponse submitBulkClaim(BulkSubmissionRequest request);
}
