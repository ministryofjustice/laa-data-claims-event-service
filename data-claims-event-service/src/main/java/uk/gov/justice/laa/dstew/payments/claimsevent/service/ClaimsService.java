package uk.gov.justice.laa.bulk.claim.service;

import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.bulk.claim.service.dto.BulkSubmissionRequest;
import uk.gov.justice.laa.bulk.claim.service.dto.BulkSubmissionResponse;
import uk.gov.justice.laa.bulk.claim.service.dto.UpdateClaimRequest;
import uk.gov.justice.laa.claims.model.ClaimDto;

/** BulkSubmissionClient interface. */
public interface ClaimsService {

  BulkSubmissionResponse submitBulkClaim(BulkSubmissionRequest request);

  Mono<ResponseEntity<Void>> updateClaimStatus(UpdateClaimRequest request);

  Mono<ClaimDto> getClaim(String claimId);
}
