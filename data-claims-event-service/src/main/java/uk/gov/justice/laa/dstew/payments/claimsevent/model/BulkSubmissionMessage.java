package uk.gov.justice.laa.dstew.payments.claimsevent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public record BulkSubmissionMessage(
    @JsonProperty("bulk_submission_id") UUID bulkSubmissionId,
    @JsonProperty("submission_ids") List<UUID> submissionIds) {}
