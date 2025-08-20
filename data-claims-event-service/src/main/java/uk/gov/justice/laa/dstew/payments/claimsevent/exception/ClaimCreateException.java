package uk.gov.justice.laa.dstew.payments.claimsevent.exception;

/** Exception thrown when a claim cannot be created in the Claims Data API. */
public class ClaimCreateException extends RuntimeException {
  /**
   * Creates a new exception with the specified message.
   *
   * @param message description of the failure
   */
  public ClaimCreateException(String message) {
    super(message);
  }

  /**
   * Creates a new exception with the specified message and cause.
   *
   * @param message description of the failure
   * @param cause underlying cause of the exception
   */
  public ClaimCreateException(String message, Throwable cause) {
    super(message, cause);
  }
}
