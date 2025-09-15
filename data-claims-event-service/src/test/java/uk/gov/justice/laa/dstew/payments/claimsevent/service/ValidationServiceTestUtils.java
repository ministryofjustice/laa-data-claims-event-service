package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.InstanceOfAssertFactories;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationReport;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

public class ValidationServiceTestUtils {

  public static void assertContextClaimError(
      SubmissionValidationContext context,
      String claimId,
      ClaimValidationError claimValidationError) {
    assertThat(context.getClaimReport(claimId))
        .isPresent()
        .get()
        .extracting(ClaimValidationReport::getErrors)
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .containsExactly(claimValidationError);
  }
}
