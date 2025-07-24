package uk.gov.justice.laa.bulk.claim.model;

/** Record holding submission matter starts details. */
public record BulkClaimMatterStarts(
    String scheduleRef,
    String procurementArea,
    String accessPoint,
    String mat,
    String immas,
    String categoryCode,
    String deliveryLocation) {}
