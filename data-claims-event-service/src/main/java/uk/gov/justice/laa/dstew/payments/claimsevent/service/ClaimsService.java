package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.claims.model.ClaimDto;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.dto.BulkSubmissionRequest;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.dto.BulkSubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.dto.UpdateClaimRequest;

/** BulkSubmissionClient interface. */
public interface ClaimsService {

  BulkSubmissionResponse submitBulkClaim(BulkSubmissionRequest request);

  Mono<ResponseEntity<Void>> updateClaimStatus(UpdateClaimRequest request);

  Mono<ClaimDto> getClaim(String claimId);
}
