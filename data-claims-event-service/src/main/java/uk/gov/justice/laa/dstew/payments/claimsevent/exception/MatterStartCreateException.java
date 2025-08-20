package uk.gov.justice.laa.dstew.payments.claimsevent.exception;

/** Exception thrown when a matter start cannot be created for a submission. */
public class MatterStartCreateException extends RuntimeException {
  /**
   * Creates a new exception with the specified message.
   *
   * @param message description of the failure
   */
  public MatterStartCreateException(String message) {
    super(message);
  }

  /**
   * Creates a new exception with the specified message and cause.
   *
   * @param message description of the failure
   * @param cause underlying cause of the exception
   */
  public MatterStartCreateException(String message, Throwable cause) {
    super(message, cause);
  }
}
