package uk.gov.justice.laa.dstew.payments.claimsevent.service.dto;

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
