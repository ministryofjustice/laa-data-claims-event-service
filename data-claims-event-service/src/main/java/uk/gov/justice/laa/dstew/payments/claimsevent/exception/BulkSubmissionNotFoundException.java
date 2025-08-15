package uk.gov.justice.laa.dstew.payments.claimsevent.exception;

import java.util.UUID;

public class BulkSubmissionNotFoundException extends RuntimeException {
  public BulkSubmissionNotFoundException(UUID bulkSubmissionId) {
    super("Bulk submission not found or not retrievable: " + bulkSubmissionId.toString());
  }
}
