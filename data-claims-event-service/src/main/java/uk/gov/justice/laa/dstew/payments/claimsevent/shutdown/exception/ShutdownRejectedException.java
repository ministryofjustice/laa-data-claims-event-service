package uk.gov.justice.laa.dstew.payments.claimsevent.shutdown.exception;

/**
 * Unchecked exception thrown when a request to acquire processing permission is rejected due to an
 * initiated shutdown/drain. Callers should handle this by re-queuing/rejecting the message as
 * appropriate for their processing semantics.
 */
public class ShutdownRejectedException extends RuntimeException {

  public ShutdownRejectedException() {
    super();
  }

  public ShutdownRejectedException(String message) {
    super(message);
  }

  public ShutdownRejectedException(String message, Throwable cause) {
    super(message, cause);
  }
}
