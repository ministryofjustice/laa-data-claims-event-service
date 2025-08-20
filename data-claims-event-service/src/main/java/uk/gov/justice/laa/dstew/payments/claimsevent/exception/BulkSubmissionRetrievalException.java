package uk.gov.justice.laa.dstew.payments.claimsevent.exception;

import java.util.UUID;

/**
 * Thrown when a requested bulk submission cannot be found or retrieved from the Data Claims
 * service.
 */
public class BulkSubmissionRetrievalException extends RuntimeException {
  /**
   * Constructs the exception with the identifier of the bulk submission has errors retrieving data.
   *
   * @param bulkSubmissionId the bulk submission id that could not be retrieved
   */
  public BulkSubmissionRetrievalException(UUID bulkSubmissionId) {
    super("Bulk submission not retrievable: " + bulkSubmissionId.toString());
  }
}
