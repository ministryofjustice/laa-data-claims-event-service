package uk.gov.justice.laa.dstew.payments.claimsevent.exception;

/** Exception thrown when a submission event could not be processed. */
public class SubmissionEventProcessingException extends RuntimeException {
  /**
   * Constructs the exception with a description of the reason the submission event could not be
   * processed.
   *
   * @param message the description of the problem encountered
   */
  public SubmissionEventProcessingException(String message) {
    super(message);
  }

  /**
   * Constructs the exception with a description of the reason the submission event could not be
   * processed.
   *
   * @param message the description of the problem encountered
   * @param cause the cause of this exception
   */
  public SubmissionEventProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
