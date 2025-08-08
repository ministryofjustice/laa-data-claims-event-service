package uk.gov.justice.laa.dstew.payments.claimsevent.model.csv;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/** Record holding submission matter starts details. */
@JsonNaming(PropertyNamingStrategies.UpperSnakeCaseStrategy.class)
public record CsvMatterStarts(
    String scheduleRef,
    String procurementArea,
    String accessPoint,
    String mat,
    String immas,
    String categoryCode,
    String deliveryLocation) {}
