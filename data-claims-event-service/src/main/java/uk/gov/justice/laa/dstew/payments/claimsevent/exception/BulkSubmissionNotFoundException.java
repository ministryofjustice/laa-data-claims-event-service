package uk.gov.justice.laa.dstew.payments.claimsevent.exception;

import java.util.UUID;

/**
 * Thrown when a requested bulk submission cannot be found or retrieved from the Data Claims
 * service.
 */
public class BulkSubmissionNotFoundException extends RuntimeException {
  /**
   * Constructs the exception with the identifier of the bulk submission that was not found.
   *
   * @param bulkSubmissionId the bulk submission id that could not be retrieved
   */
  public BulkSubmissionNotFoundException(UUID bulkSubmissionId) {
    super("Bulk submission not found or not retrievable: " + bulkSubmissionId.toString());
  }
}
