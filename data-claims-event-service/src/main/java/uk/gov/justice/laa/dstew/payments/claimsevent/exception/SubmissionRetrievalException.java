package uk.gov.justice.laa.dstew.payments.claimsevent.exception;

import java.util.UUID;

/** Thrown when a requested submission cannot be retrieved from the Data Claims service. */
public class SubmissionRetrievalException extends RuntimeException {
  /**
   * Constructs the exception with the identifier of the submission has errors retrieving data.
   *
   * @param submissionId the submission id that could not be retrieved
   */
  public SubmissionRetrievalException(UUID submissionId) {
    super("Submission not retrievable: " + submissionId.toString());
  }
}
