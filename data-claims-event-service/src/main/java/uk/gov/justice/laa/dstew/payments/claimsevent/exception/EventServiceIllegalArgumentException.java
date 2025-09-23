package uk.gov.justice.laa.dstew.payments.claimsevent.exception;

/**
 * Thrown when there is a problem with the events service.
 *
 * @author Jamie Briggs
 */
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
