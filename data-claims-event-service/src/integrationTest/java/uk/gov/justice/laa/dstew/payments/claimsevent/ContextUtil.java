package uk.gov.justice.laa.dstew.payments.claimsevent;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.NoArgsConstructor;
import org.assertj.core.api.SoftAssertions;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationError;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ContextUtil {
  public static void assertContextHasNoErrors(SubmissionValidationContext context) {
    SoftAssertions.assertSoftly(
        softly -> {
          context
              .getClaimReports()
              .forEach(
                  claimReport -> {
                    if (claimReport != null) {
                      for (var error : claimReport.getMessages()) {
                        softly.fail(error.getDisplayMessage());
                      }
                    }
                  });
          if (context.hasErrors()) {
            for (var error : context.getSubmissionValidationErrors()) {
              softly.fail(error.getDisplayMessage());
            }
          }
          softly.assertThat(context.hasErrors()).isFalse();
        });
  }

  public static void assertContextClaimError(
      SubmissionValidationContext context,
      SubmissionValidationError submissionValidationError,
      Object... args) {
    assertThat(context.getSubmissionValidationErrors())
        .isNotEmpty()
        .contains(submissionValidationError.toPatch(args));
  }
}
