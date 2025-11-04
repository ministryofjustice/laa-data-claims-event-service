package uk.gov.justice.laa.dstew.payments.claimsevent.validation.submission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.assertContextClaimError;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionBase;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionsResultSet;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationError;

@ExtendWith(MockitoExtension.class)
public class SubmissionOfficeAreaOfLawAndPeriodValidatorTest {
  private static final String OFFICE_CODE = "office1";
  private static final AreaOfLaw AREA_OF_LAW = AreaOfLaw.LEGAL_HELP;
  private static final String SUBMISSION_PERIOD = "2025-07";

  @Mock private DataClaimsRestClient mockDataClaimsRestClient;

  @InjectMocks private SubmissionOfficeAreaOfLawAndPeriodValidator validator;

  @Captor private ArgumentCaptor<List<String>> officeCodeCaptor;

  @Captor private ArgumentCaptor<AreaOfLaw> areaOfLawCaptor;

  @Captor private ArgumentCaptor<String> submissionPeriodCaptor;

  @DisplayName("Should have priority of 100")
  @Test
  public void priority() {
    Assertions.assertEquals(100, validator.priority());
  }

  @Nested
  class Validate {

    @DisplayName(
        "Should accept a submission when there is no previous submission with the same combination of Office,Area 0f law and Submission period")
    @Test
    void shouldAcceptSubmission() {
      when(mockDataClaimsRestClient.getSubmissions(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(new SubmissionsResultSet())));

      var submissionValidationContext = new SubmissionValidationContext();

      SubmissionResponse submissionResponse =
          SubmissionResponse.builder()
              .officeAccountNumber(OFFICE_CODE)
              .areaOfLaw(AREA_OF_LAW)
              .submissionPeriod(SUBMISSION_PERIOD)
              .build();

      validator.validate(submissionResponse, submissionValidationContext);

      assertThat(submissionValidationContext.hasErrors()).isFalse();
      verify(mockDataClaimsRestClient)
          .getSubmissions(
              officeCodeCaptor.capture(),
              any(),
              any(),
              any(),
              areaOfLawCaptor.capture(),
              submissionPeriodCaptor.capture(),
              any(),
              any(),
              any());
      assertThat(officeCodeCaptor.getValue()).contains(OFFICE_CODE);
      assertThat(areaOfLawCaptor.getValue()).isEqualTo(AREA_OF_LAW);
      assertThat(submissionPeriodCaptor.getValue()).isEqualTo(SUBMISSION_PERIOD);
    }

    @DisplayName(
        "Should reject a submission when there is a previous submission of status of VALIDATION_SUCCEEDED with the same combination of Office,Area 0f law and Submission period")
    @Test
    void shouldRejectSubmissionWhenExist() {
      var previousExistingSubmission =
          new SubmissionBase()
              .officeAccountNumber(OFFICE_CODE)
              .areaOfLaw(AREA_OF_LAW)
              .submissionPeriod(SUBMISSION_PERIOD)
              .status(SubmissionStatus.VALIDATION_SUCCEEDED);
      when(mockDataClaimsRestClient.getSubmissions(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(
                  Optional.of(
                      new SubmissionsResultSet().addContentItem(previousExistingSubmission))));

      var submissionValidationContext = new SubmissionValidationContext();
      SubmissionResponse submissionResponse =
          SubmissionResponse.builder()
              .officeAccountNumber(OFFICE_CODE)
              .areaOfLaw(AREA_OF_LAW)
              .submissionPeriod(SUBMISSION_PERIOD)
              .build();

      validator.validate(submissionResponse, submissionValidationContext);

      assertThat(submissionValidationContext.hasErrors()).isTrue();

      assertContextClaimError(
          submissionValidationContext,
          SubmissionValidationError.SUBMISSION_ALREADY_EXISTS,
          OFFICE_CODE,
          AREA_OF_LAW,
          SUBMISSION_PERIOD);
    }

    @DisplayName(
        "Should accept a submission even if previous submission exist with same combination of Office, Area 0f law but status not of VALIDATION_SUCCEEDED")
    @ParameterizedTest
    @EnumSource(
        value = SubmissionStatus.class,
        names = {
          "CREATED",
          "READY_FOR_VALIDATION",
          "VALIDATION_FAILED",
          "VALIDATION_IN_PROGRESS",
          "REPLACED"
        })
    void shouldAcceptSubmissionWhenPreviousSubmissionStatusIsNotValidatedSucceeded(
        final SubmissionStatus status) {
      var previousExistingSubmission =
          new SubmissionBase()
              .officeAccountNumber(OFFICE_CODE)
              .areaOfLaw(AREA_OF_LAW)
              .submissionPeriod(SUBMISSION_PERIOD)
              .status(status);
      when(mockDataClaimsRestClient.getSubmissions(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(
                  Optional.of(
                      new SubmissionsResultSet().addContentItem(previousExistingSubmission))));

      var submissionValidationContext = new SubmissionValidationContext();
      SubmissionResponse submissionResponse =
          SubmissionResponse.builder()
              .officeAccountNumber(OFFICE_CODE)
              .areaOfLaw(AREA_OF_LAW)
              .submissionPeriod(SUBMISSION_PERIOD)
              .build();

      validator.validate(submissionResponse, submissionValidationContext);

      assertThat(submissionValidationContext.hasErrors()).isFalse();
    }
  }
}
