package uk.gov.justice.laa.dstew.payments.claimsevent.validator.submission;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationError;

/**
 * Integration tests for {@link
 * uk.gov.justice.laa.dstew.payments.claimsevent.validation.submission.NilSubmissionValidator}.
 */
public class NilSubmissionValidatorITest extends SubmissionValidationIntegrationTestBase {

  private static final String P = SUBMISSION_BASE_PATH + "NilSubmissionValidator/";

  @Test
  @DisplayName("Nil submission with no claims - should produce no error")
  void nilSubmissionNoClaimsIsValid() throws Exception {
    var ctx = runSubmissionValidation(P + "nil-submission-no-claims.json");
    assertNoSubmissionErrors(ctx);
  }

  @Test
  @DisplayName("Non-nil submission with claims - should produce no error")
  void nonNilSubmissionWithClaimsIsValid() throws Exception {
    var ctx = runSubmissionValidation(P + "non-nil-submission-with-claims.json");
    assertNoSubmissionErrors(ctx);
  }

  @Test
  @DisplayName(
      "Nil submission that contains claims - should produce INVALID_NIL_SUBMISSION_CONTAINS_CLAIMS")
  void nilSubmissionWithClaimsIsInvalid() throws Exception {
    var ctx = runSubmissionValidation(P + "nil-submission-with-claims.json");
    assertSubmissionErrors(
        ctx, Set.of(SubmissionValidationError.INVALID_NIL_SUBMISSION_CONTAINS_CLAIMS));
  }

  @Test
  @DisplayName(
      "Non-nil submission with no claims - should produce NON_NIL_SUBMISSION_CONTAINS_NO_CLAIMS")
  void nonNilSubmissionWithNoClaimsIsInvalid() throws Exception {
    var ctx = runSubmissionValidation(P + "non-nil-submission-no-claims.json");
    assertSubmissionErrors(
        ctx, Set.of(SubmissionValidationError.NON_NIL_SUBMISSION_CONTAINS_NO_CLAIMS));
  }
}
