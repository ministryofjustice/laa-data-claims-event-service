package uk.gov.justice.laa.dstew.payments.claimsevent.model;

/** Record holding submission matter starts details. */
public record BulkSubmissionMatterStarts(String scheduleRef, String procurementArea,
    String accessPoint, String mat, String immas, String categoryCode, String deliveryLocation) {}
