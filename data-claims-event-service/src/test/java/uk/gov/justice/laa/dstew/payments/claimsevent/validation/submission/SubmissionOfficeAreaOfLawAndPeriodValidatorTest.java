package uk.gov.justice.laa.dstew.payments.claimsevent.validation.submission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.assertContextClaimError;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
@DisplayName("Submission office, area of law and period duplicate validator")
class SubmissionOfficeAreaOfLawAndPeriodValidatorTest {
  private static final String OFFICE_CODE = "office1";
  private static final AreaOfLaw AREA_OF_LAW = AreaOfLaw.LEGAL_HELP;
  private static final String SUBMISSION_PERIOD = "2025-07";
  private static final UUID SUBMISSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID OTHER_SUBMISSION_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final OffsetDateTime CREATED_ON = OffsetDateTime.parse("2025-07-01T10:00:00Z");
  private static final OffsetDateTime EARLIER = CREATED_ON.minusSeconds(1);
  private static final OffsetDateTime LATER = CREATED_ON.plusSeconds(1);

  @Mock private DataClaimsRestClient mockDataClaimsRestClient;

  @InjectMocks private SubmissionOfficeAreaOfLawAndPeriodValidator validator;

  @Captor private ArgumentCaptor<List<String>> officeCodeCaptor;

  @Captor private ArgumentCaptor<AreaOfLaw> areaOfLawCaptor;

  @Captor private ArgumentCaptor<String> submissionPeriodCaptor;

  private static SubmissionResponse submissionUnderValidation() {
    return SubmissionResponse.builder()
        .submissionId(SUBMISSION_ID)
        .officeAccountNumber(OFFICE_CODE)
        .areaOfLaw(AREA_OF_LAW)
        .submissionPeriod(SUBMISSION_PERIOD)
        .submitted(CREATED_ON)
        .build();
  }

  private static SubmissionResponse submissionWithoutTimestamp() {
    return SubmissionResponse.builder()
        .submissionId(SUBMISSION_ID)
        .officeAccountNumber(OFFICE_CODE)
        .areaOfLaw(AREA_OF_LAW)
        .submissionPeriod(SUBMISSION_PERIOD)
        .submitted(null)
        .build();
  }

  private static SubmissionBase existingSubmission(
      UUID submissionId, SubmissionStatus status, OffsetDateTime createdOn) {
    return new SubmissionBase()
        .submissionId(submissionId)
        .officeAccountNumber(OFFICE_CODE)
        .areaOfLaw(AREA_OF_LAW)
        .submissionPeriod(SUBMISSION_PERIOD)
        .status(status)
        .submitted(createdOn);
  }

  private void stubExistingSubmissions(SubmissionBase... submissions) {
    var resultSet = new SubmissionsResultSet();
    for (SubmissionBase submission : submissions) {
      resultSet.addContentItem(submission);
    }
    when(mockDataClaimsRestClient.getSubmissions(any(), any(), any()))
        .thenReturn(ResponseEntity.of(Optional.of(resultSet)));
  }

  @DisplayName("Should have priority of 100")
  @Test
  void priority() {
    Assertions.assertEquals(100, validator.priority());
  }

  @Nested
  class Validate {

    @DisplayName(
        "Should accept a submission when there is no previous submission with the same combination of Office, Area of law and Submission period")
    @Test
    void shouldAcceptSubmission() {
      when(mockDataClaimsRestClient.getSubmissions(any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(new SubmissionsResultSet())));

      var submissionValidationContext = new SubmissionValidationContext();

      validator.validate(submissionUnderValidation(), submissionValidationContext);

      assertThat(submissionValidationContext.hasErrors()).isFalse();
      verify(mockDataClaimsRestClient)
          .getSubmissions(
              officeCodeCaptor.capture(),
              areaOfLawCaptor.capture(),
              submissionPeriodCaptor.capture());
      assertThat(officeCodeCaptor.getValue()).contains(OFFICE_CODE);
      assertThat(areaOfLawCaptor.getValue()).isEqualTo(AREA_OF_LAW);
      assertThat(submissionPeriodCaptor.getValue()).isEqualTo(SUBMISSION_PERIOD);
    }

    @DisplayName(
        "Should reject a submission when an earlier submission in a non-terminal status shares the same Office, Area of law and Submission period")
    @ParameterizedTest(name = "earlier submission in status {0} is a duplicate")
    @EnumSource(
        value = SubmissionStatus.class,
        names = {
          "CREATED",
          "READY_FOR_VALIDATION",
          "VALIDATION_IN_PROGRESS",
          "VALIDATION_SUCCEEDED"
        })
    void shouldRejectSubmissionWhenEarlierNonTerminalSubmissionExists(
        final SubmissionStatus status) {
      stubExistingSubmissions(existingSubmission(OTHER_SUBMISSION_ID, status, EARLIER));

      var submissionValidationContext = new SubmissionValidationContext();

      validator.validate(submissionUnderValidation(), submissionValidationContext);

      assertThat(submissionValidationContext.hasErrors()).as("status %s", status).isTrue();
      assertContextClaimError(
          submissionValidationContext,
          SubmissionValidationError.SUBMISSION_ALREADY_EXISTS,
          OFFICE_CODE,
          AREA_OF_LAW,
          SUBMISSION_PERIOD);
    }

    @DisplayName(
        "Should accept a submission when the only earlier submission with the same combination is in a terminal status")
    @ParameterizedTest(name = "earlier submission in status {0} is ignored")
    @EnumSource(
        value = SubmissionStatus.class,
        names = {"VALIDATION_FAILED", "REPLACED"})
    void shouldAcceptSubmissionWhenEarlierSubmissionIsTerminal(final SubmissionStatus status) {
      stubExistingSubmissions(existingSubmission(OTHER_SUBMISSION_ID, status, EARLIER));

      var submissionValidationContext = new SubmissionValidationContext();

      validator.validate(submissionUnderValidation(), submissionValidationContext);

      assertThat(submissionValidationContext.hasErrors()).as("status %s", status).isFalse();
    }

    @DisplayName(
        "Should accept a submission when a non-terminal duplicate exists but was created after it, so the earliest of concurrent submissions proceeds")
    @Test
    void shouldAcceptSubmissionWhenDuplicateWasCreatedLater() {
      stubExistingSubmissions(
          existingSubmission(OTHER_SUBMISSION_ID, SubmissionStatus.READY_FOR_VALIDATION, LATER));

      var submissionValidationContext = new SubmissionValidationContext();

      validator.validate(submissionUnderValidation(), submissionValidationContext);

      assertThat(submissionValidationContext.hasErrors()).isFalse();
    }

    @DisplayName(
        "Should accept a submission when the only match returned is the submission under validation itself")
    @Test
    void shouldAcceptSubmissionWhenOnlyMatchIsItself() {
      stubExistingSubmissions(
          existingSubmission(SUBMISSION_ID, SubmissionStatus.READY_FOR_VALIDATION, CREATED_ON));

      var submissionValidationContext = new SubmissionValidationContext();

      validator.validate(submissionUnderValidation(), submissionValidationContext);

      assertThat(submissionValidationContext.hasErrors()).isFalse();
    }

    @DisplayName(
        "Should reject a submission when an otherwise-matching live candidate has no created-on timestamp")
    @Test
    void shouldRejectSubmissionWhenCandidateHasNoTimestamp() {
      stubExistingSubmissions(
          existingSubmission(OTHER_SUBMISSION_ID, SubmissionStatus.READY_FOR_VALIDATION, null));

      var submissionValidationContext = new SubmissionValidationContext();

      validator.validate(submissionUnderValidation(), submissionValidationContext);

      assertThat(submissionValidationContext.hasErrors()).isTrue();
      assertContextClaimError(
          submissionValidationContext,
          SubmissionValidationError.SUBMISSION_ALREADY_EXISTS,
          OFFICE_CODE,
          AREA_OF_LAW,
          SUBMISSION_PERIOD);
    }

    @DisplayName(
        "Should reject a submission that has no created-on timestamp when an earlier matching candidate exists")
    @Test
    void shouldRejectSubmissionWhenItHasNoTimestamp() {
      stubExistingSubmissions(
          existingSubmission(OTHER_SUBMISSION_ID, SubmissionStatus.READY_FOR_VALIDATION, EARLIER));

      var submissionValidationContext = new SubmissionValidationContext();

      validator.validate(submissionWithoutTimestamp(), submissionValidationContext);

      assertThat(submissionValidationContext.hasErrors()).isTrue();
      assertContextClaimError(
          submissionValidationContext,
          SubmissionValidationError.SUBMISSION_ALREADY_EXISTS,
          OFFICE_CODE,
          AREA_OF_LAW,
          SUBMISSION_PERIOD);
    }
  }
}
