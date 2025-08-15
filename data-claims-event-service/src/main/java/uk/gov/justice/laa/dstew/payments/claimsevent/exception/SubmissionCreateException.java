package uk.gov.justice.laa.dstew.payments.claimsevent.exception;

/** Exception thrown when a submission cannot be created in the Claims Data API. */
public class SubmissionCreateException extends RuntimeException {
  /**
   * Creates a new exception with the specified message.
   *
   * @param message description of the failure
   */
  public SubmissionCreateException(String message) {
    super(message);
  }
}
