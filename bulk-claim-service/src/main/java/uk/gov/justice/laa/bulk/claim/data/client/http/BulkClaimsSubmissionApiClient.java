package uk.gov.justice.laa.bulk.claim.data.client.http;

import uk.gov.justice.laa.bulk.claim.data.client.dto.BulkSubmissionRequest;
import uk.gov.justice.laa.bulk.claim.data.client.dto.BulkSubmissionResponse;

/** BulkSubmissionClient interface. */
public interface BulkClaimsSubmissionApiClient {

  BulkSubmissionResponse submitBulkClaim(BulkSubmissionRequest request);
}
