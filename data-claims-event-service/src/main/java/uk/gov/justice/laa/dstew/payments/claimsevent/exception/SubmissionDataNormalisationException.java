package uk.gov.justice.laa.dstew.payments.claimsevent.exception;

/**
 * Exception thrown when a field on a bulk submission DTO cannot be read or written during
 * normalisation.
 *
 * <p>This is typically caused by a reflective access failure ({@link IllegalAccessException}) while
 * the {@code SubmissionDataNormaliser} is trimming or uppercasing string fields on a DTO object.
 */
public class SubmissionDataNormalisationException extends RuntimeException {

  /**
   * Constructs the exception with a description of the field that could not be normalised.
   *
   * @param message description of the failure, typically including the field name
   */
  public SubmissionDataNormalisationException(String message) {
    super(message);
  }

  /**
   * Constructs the exception with a description of the field that could not be normalised and the
   * underlying cause.
   *
   * @param message description of the failure, typically including the field name
   * @param cause the {@link IllegalAccessException} that triggered this exception
   */
  public SubmissionDataNormalisationException(String message, Throwable cause) {
    super(message, cause);
  }
}
