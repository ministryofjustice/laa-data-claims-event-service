package uk.gov.justice.laa.dstew.payments.claimsevent.exception;

/** Thrown when a problem is found during submission validation. */
public class EventServiceIllegalArgumentException extends RuntimeException {
  /**
   * Constructs the exception with a message detailing the error during validation.
   *
   * @param message the message detailing the error
   */
  public EventServiceIllegalArgumentException(String message) {
    super(message);
  }
}
