package uk.gov.justice.laa.bulk.claim.data.client.http;

import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.bulk.claim.data.client.dto.BulkSubmissionRequest;
import uk.gov.justice.laa.bulk.claim.data.client.dto.BulkSubmissionResponse;
import uk.gov.justice.laa.bulk.claim.data.client.dto.UpdateClaimRequest;

/** BulkSubmissionClient interface. */
public interface BulkClaimsSubmissionApiClient {

  BulkSubmissionResponse submitBulkClaim(BulkSubmissionRequest request);

  Mono<ResponseEntity<Void>> updateClaimStatus(UpdateClaimRequest request);
}
