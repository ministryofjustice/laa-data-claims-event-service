package uk.gov.justice.laa.dstew.payments.claimsevent.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

/**
 * Record holding bulk claim submission details.
 *
 * @param office the office submitting the claim
 * @param schedule the schedule details.
 * @param outcomes the submission outcomes
 * @param matterStarts the submission matter starts
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public record BulkClaimSubmission(
    BulkClaimOffice office,
    BulkClaimSchedule schedule,
    List<BulkClaimOutcome> outcomes,
    List<BulkClaimMatterStarts> matterStarts) {}
