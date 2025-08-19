package uk.gov.justice.laa.dstew.payments.claimsevent.client;

import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.claims.model.ClaimDto;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.dto.BulkSubmissionRequest;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.dto.BulkSubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.dto.UpdateClaimRequest;

/** ClaimsClient interface. */
public interface ClaimsClient {

  BulkSubmissionResponse submitBulkClaim(BulkSubmissionRequest request);

  Mono<ResponseEntity<Void>> updateClaimStatus(UpdateClaimRequest request);

  Mono<ClaimDto> getClaim(String claimId);
}
