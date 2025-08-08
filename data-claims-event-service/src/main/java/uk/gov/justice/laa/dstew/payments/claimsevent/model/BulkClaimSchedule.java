package uk.gov.justice.laa.dstew.payments.claimsevent.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Record holding bulk claim submission schedule details.
 *
 * @param submissionPeriod the submission period
 * @param areaOfLaw the area of law for the submission
 * @param scheduleNum the submission schedule number
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public record BulkClaimSchedule(String submissionPeriod, String areaOfLaw, String scheduleNum) {}
