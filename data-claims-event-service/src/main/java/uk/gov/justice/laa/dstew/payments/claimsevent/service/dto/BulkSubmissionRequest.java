<<<<<<<< HEAD:data-claims-event-service/src/main/java/uk/gov/justice/laa/dstew/payments/claimsevent/data/client/dto/BulkSubmissionRequest.java
package uk.gov.justice.laa.dstew.payments.claimsevent.data.client.dto;
========
package uk.gov.justice.laa.bulk.claim.service.dto;
>>>>>>>> upstream/main:data-claims-event-service/src/main/java/uk/gov/justice/laa/dstew/payments/claimsevent/service/dto/BulkSubmissionRequest.java

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkClaimSubmission;

/**
 * Bulk Submissions Request payload.
 *
 * @param userId requesting User
 * @param meta miscellaneous key value paired data
 * @param submissions collection of claims submissions
 */
public record BulkSubmissionRequest(
    @NotNull String userId,
    HashMap<String, String> meta,
    @NotEmpty List<BulkClaimSubmission> submissions) {}
