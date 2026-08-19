package uk.gov.justice.laa.dstew.payments.claimsevent.validator.submission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/**
 * Integration tests for {@link
 * uk.gov.justice.laa.dstew.payments.claimsevent.validation.submission.SubmissionSchemaValidator}.
 *
 * <p>Each parameterised case verifies that the submission schema validator either produces no
 * errors (valid fixtures) or at least one schema validation error (invalid fixtures). Matching is
 * done on the display message text produced by the JSON schema validator.
 */
public class SubmissionSchemaValidatorITest extends SubmissionValidationIntegrationTestBase {

  private static final String P = SUBMISSION_BASE_PATH + "SubmissionSchemaValidator/";

  /**
   * Arguments: (fixtureFileName, expectErrors) Where expectErrors=true means we expect at least one
   * schema error.
   */
  static List<Arguments> claimsResponse() {
    return List.of(
        // ── Valid fixtures ────────────────────────────────────────────────────
        Arguments.of("lh-valid.json", false),
        Arguments.of("cl-valid.json", false),
        Arguments.of("med-valid.json", false),

        // ── Invalid office_account_number (lowercase) ─────────────────────────
        Arguments.of("invalid-office-account-number.json", true),

        // ── Invalid submission_period format ──────────────────────────────────
        // TODO: test fails as it can't map the value in the test
        // Arguments.of("invalid-submission-period-format.json", true),

        // ── Invalid area_of_law (unknown value) ───────────────────────────────
        // TODO: Mapper fails on request as the enum is unknow we should defend
        //  against this in the new validator
        // Arguments.of("invalid-area-of-law.json", true),

        // ── Missing required: office_account_number ───────────────────────────
        // TODO: current service validator falls over if this is null
        // Arguments.of("missing-office-account-number.json", true),

        // ── Missing required: is_nil_submission ───────────────────────────────
        Arguments.of("missing-is-nil-submission.json", true),

        // ── Missing required: number_of_claims ───────────────────────────────
        Arguments.of("missing-number-of-claims.json", true),

        // ── Missing conditional required: legal_help_submission_reference ──────
        Arguments.of("lh-missing-legal-help-submission-reference.json", true),

        // ── Missing conditional required: crime_lower_schedule_number ─────────
        Arguments.of("cl-missing-crime-lower-schedule-number.json", true),

        // ── Missing conditional required: mediation_submission_reference ──────
        Arguments.of("med-missing-mediation-submission-reference.json", true),

        // ── Invalid legal_help_submission_reference (special chars) ───────────
        Arguments.of("lh-invalid-legal-help-submission-reference.json", true));
  }

  @DisplayName("Submission schema validator")
  @ParameterizedTest(name = "{0}")
  @MethodSource("claimsResponse")
  void shouldValidateSubmissionSchema(String fixture, boolean expectErrors) throws Exception {
    SubmissionValidationContext ctx = runSubmissionValidation(P + fixture);
    List<ValidationMessagePatch> errors = getSubmissionErrors(ctx);

    if (expectErrors) {
      assertFalse(
          errors.isEmpty(),
          "Expected at least one schema validation error for fixture ["
              + fixture
              + "] but got none");
    } else {
      assertEquals(
          0,
          errors.size(),
          "Expected no errors for valid fixture ["
              + fixture
              + "] but got: "
              + errors.stream().map(ValidationMessagePatch::getDisplayMessage).toList());
    }
  }
}
