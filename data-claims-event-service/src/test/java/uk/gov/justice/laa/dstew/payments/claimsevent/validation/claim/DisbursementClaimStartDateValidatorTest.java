package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.getClaimMessages;
import static uk.gov.justice.laa.dstew.payments.claimsevent.util.DateUtil.DATE_FORMATTER_FOR_DISPLAY_MESSAGE;
import static uk.gov.justice.laa.dstew.payments.claimsevent.util.DateUtil.DATE_FORMATTER_YYYY_MM_DD;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.FeeCalculationType;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

class DisbursementClaimStartDateValidatorTest {

  private DisbursementClaimStartDateValidator validator;
  private SubmissionValidationContext context;
  private ClaimResponse claim;
  private final String claimId = new UUID(1, 1).toString();

  @BeforeEach
  void setUp() {
    validator = new DisbursementClaimStartDateValidator();
    context = new SubmissionValidationContext();
    claim = new ClaimResponse();

    claim.setId(claimId);
  }

  @ParameterizedTest
  @CsvSource({
    "2025-11-07, JAN-2026", // caseStartDate + 3 months = 2026-02-07 < 2026-02-20
    "2025-01-20, MAR-2025", // caseStartDate + 3 months = 2025-04-20 = 2025-04-20
    "2025-01-01, APR-2025", // caseStartDate + 3 months = 2025-04-01 < 2025-05-20
    "2025-01-31, APR-2025",
    "2025-01-01, MAY-2025",
    "2025-01-10, MAY-2025",
    "2025-01-31, MAY-2025",
    "2025-02-28, MAY-2025",
    "2024-11-30, FEB-2025",
    "2024-01-20, MAR-2024", // caseStartDate + 3 months = 2024-04-20 = 2024-04-20 (leap year)
    "2024-01-19, MAR-2024", // caseStartDate + 3 months = 2024-04-19 < 2024-04-20 (leap year)
    "2024-01-31, APR-2024",
    "2024-01-30, APR-2024"
  })
  @DisplayName(
      "Should pass validation when claimStartDate is greater than or equals to 3 months of submission period")
  void shouldPassValidationWhenClaimStartDateIsGreaterThanOrEqualTo3MonthsOfSubmissionPeriod(
      String caseStartDate, String submissionPeriod) {
    claim.setCaseStartDate(caseStartDate);
    claim.setSubmissionPeriod(submissionPeriod);

    validator.validate(claim, context, FeeCalculationType.DISB_ONLY.getValue());

    assertTrue(context.getClaimReport(claim.getId()).isEmpty());
  }

  @ParameterizedTest
  @CsvSource({
    "2025-01-10, FEB-2025", // caseStartDate + 3 months = 2025-04-10 > 2025-03-20
    "2025-01-21, MAR-2025", // caseStartDate + 3 months = 2025-04-21 > 2025-04-20
    "2025-01-31, MAR-2025", // caseStartDate + 3 months = 2025-04-31 > 2025-04-20
    "2025-02-21, APR-2025",
    "2024-12-21, FEB-2025",
    "2024-01-31, MAR-2024",
    "2024-01-21, MAR-2024", // caseStartDate + 3 months = 2024-04-21 > 2024-04-20 (leap year)
    "2024-02-29, APR-2024",
    "2024-02-21, APR-2024"
  })
  @DisplayName(
      "Should fail validation when claimStartDate is less than 3 months of submission period end date (which is 20th of following month)")
  void shouldFailValidationWhenClaimStartDateIsLessThan3MonthsOfSubmissionPeriod(
      String caseStartDate, String submissionPeriod) {
    claim.setCaseStartDate(caseStartDate);
    claim.setSubmissionPeriod(submissionPeriod);
    LocalDate startDate = LocalDate.parse(caseStartDate, DATE_FORMATTER_YYYY_MM_DD);

    validator.validate(claim, context, FeeCalculationType.DISB_ONLY.getValue());

    assertTrue(context.getClaimReport(claim.getId()).isPresent());
    assertThat(
            getClaimMessages(context, claimId).stream()
                .anyMatch(
                    x ->
                        x.getDisplayMessage()
                            .equals(
                                String.format(
                                    "Disbursement claims can only be submitted at least 3 calendar months after the Case Start Date %s",
                                    startDate.format(DATE_FORMATTER_FOR_DISPLAY_MESSAGE)))))
        .isTrue();
  }
}
