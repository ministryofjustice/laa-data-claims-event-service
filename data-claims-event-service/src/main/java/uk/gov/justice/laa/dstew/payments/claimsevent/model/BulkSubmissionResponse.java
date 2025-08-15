package uk.gov.justice.laa.dstew.payments.claimsevent.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public record BulkSubmissionResponse(UUID id, BulkSubmissionDetails details) {}
