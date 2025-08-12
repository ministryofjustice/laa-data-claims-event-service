<<<<<<<< HEAD:data-claims-event-service/src/main/java/uk/gov/justice/laa/dstew/payments/claimsevent/data/client/dto/UpdateClaimRequest.java
package uk.gov.justice.laa.dstew.payments.claimsevent.data.client.dto;
========
package uk.gov.justice.laa.bulk.claim.service.dto;
>>>>>>>> upstream/main:data-claims-event-service/src/main/java/uk/gov/justice/laa/dstew/payments/claimsevent/service/dto/UpdateClaimRequest.java

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Record representing a request to the claims data API to update a claim.
 *
 * @param claimStatus the new claim status.
 * @param errors an optional list of validation errors.
 */
public record UpdateClaimRequest(@NotNull ClaimStatus claimStatus, List<String> errors) {}
