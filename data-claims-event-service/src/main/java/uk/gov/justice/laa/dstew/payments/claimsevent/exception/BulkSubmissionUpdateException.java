package uk.gov.justice.laa.dstew.payments.claimsevent.exception;

/** Exception thrown when a bulk submission cannot be updated in the Claims Data API. */
public class BulkSubmissionUpdateException extends RuntimeException {
  /**
   * Creates a new exception with the specified message.
   *
   * @param message description of the failure
   */
  public BulkSubmissionUpdateException(String message) {
    super(message);
  }
}
