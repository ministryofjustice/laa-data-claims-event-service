package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimFields;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetSubmission200Response;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetSubmission200ResponseClaimsInner;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetSubmission200ResponseClaimsInner.StatusEnum;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ProviderDetailsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationReport;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.JsonSchemaValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.provider.model.FirmOfficeContractAndScheduleDetails;
import uk.gov.justice.laa.provider.model.FirmOfficeContractAndScheduleLine;
import uk.gov.justice.laa.provider.model.ProviderFirmOfficeContractAndScheduleDto;

@ExtendWith(MockitoExtension.class)
public class SubmissionValidationServiceTest {

  @Mock private ClaimValidationService claimValidationService;

  @Mock private DataClaimsRestClient dataClaimsRestClient;

  @Mock private ProviderDetailsRestClient providerDetailsRestClient;

  @Mock private SubmissionValidationContext submissionValidationContext;

  @Mock private JsonSchemaValidator jsonSchemaValidator;

  @InjectMocks private SubmissionValidationService submissionValidationService;

  @Nested
  @DisplayName("validateSubmission")
  class ValidateSubmissionTests {

    @ParameterizedTest(name = "{index} => {4}")
    @MethodSource("submissionValidationArguments")
    void testSubmissionValidation(
        boolean isNilSubmission,
        ClaimStatus claimStatus,
        boolean hasErrors,
        boolean expectsValidationError,
        String displayName) {
      // Given
      UUID submissionId = new UUID(0, 0);
      UUID claimId =
          claimStatus != null ? new UUID(1, 1) : null; // only create claimId if there is a claim
      String categoryOfLaw = "categoryOfLaw";
      String officeAccountNumber = "officeAccountNumber";
      String areaOfLaw = "areaOfLaw";

      GetSubmission200Response submission = buildSubmission(submissionId, claimId, isNilSubmission);

      if (claimId != null) {
        ClaimFields claimFields = buildClaimFields(claimId);
        mockClaimRetrieval(submissionId, claimId, claimFields);
        mockProviderSchedules(officeAccountNumber, areaOfLaw, categoryOfLaw);

        SubmissionPatch submissionPatch = buildSubmissionPatch(submissionId);
        mockSubmissionUpdate(submissionId, submissionPatch);

        ClaimPatch claimPatch = new ClaimPatch().id(claimId.toString()).status(claimStatus);
        when(submissionValidationContext.hasErrors(claimId.toString())).thenReturn(hasErrors);
        mockClaimUpdate(submissionId, claimId, claimPatch);

        // When
        submissionValidationService.validateSubmission(submission);

        // Then
        verifyCommonInteractions(
            submissionId,
            claimId,
            officeAccountNumber,
            areaOfLaw,
            claimFields,
            categoryOfLaw,
            submissionPatch,
            claimPatch);
      } else {
        // When
        submissionValidationService.validateSubmission(submission);
      }

      if (expectsValidationError) {
        // Determine which error to verify
        String errorDescription;
        if (isNilSubmission) {
          errorDescription =
              ClaimValidationError.INVALID_NIL_SUBMISSION_CONTAINS_CLAIMS.getDescription();
        } else {
          errorDescription =
              ClaimValidationError.NON_NIL_SUBMISSION_CONTAINS_NO_CLAIMS.getDescription();
        }
        verify(submissionValidationContext, times(1))
            .addSubmissionValidationError(errorDescription);
      }
    }

    @Test
    @DisplayName(
        "Adds submission validation error if submission not marked as nil submission contains no claims")
    void throwsExceptionIfSubmissionNotMarkedAsNilSubmissionContainsNoClaims() {
      // Given
      UUID submissionId = new UUID(0, 0);
      String areaOfLaw = "areaOfLaw";
      String categoryOfLaw = "categoryOfLaw";
      String officeAccountNumber = "officeAccountNumber";

      GetSubmission200Response submission =
          getSubmission(
              SubmissionStatus.READY_FOR_VALIDATION,
              submissionId,
              areaOfLaw,
              officeAccountNumber,
              false,
              null);

      mockProviderSchedules(officeAccountNumber, areaOfLaw, categoryOfLaw);

      // When
      submissionValidationService.validateSubmission(submission);

      // Then
      verify(submissionValidationContext, times(1))
          .addSubmissionValidationError(
              ClaimValidationError.NON_NIL_SUBMISSION_CONTAINS_NO_CLAIMS.getDescription());
    }

    @Test
    @DisplayName("Marks claims as invalid if provider contract is invalid")
    void marksClaimsAsInvalidIfProviderContractInvalid() {
      // Given
      UUID submissionId = new UUID(0, 0);
      UUID claimId = new UUID(1, 1);
      String areaOfLaw = "areaOfLaw";
      String officeAccountNumber = "officeAccountNumber";

      GetSubmission200ResponseClaimsInner claim = new GetSubmission200ResponseClaimsInner();
      claim.setClaimId(claimId);
      claim.setStatus(StatusEnum.READY_TO_PROCESS);

      GetSubmission200Response submission = buildSubmission(submissionId, claimId, false);

      ClaimFields claimFields = buildClaimFields(claimId);

      when(dataClaimsRestClient.getClaim(submissionId, claimId))
          .thenReturn(ResponseEntity.of(Optional.of(claimFields)));

      when(providerDetailsRestClient.getProviderFirmSchedules(officeAccountNumber, areaOfLaw))
          .thenReturn(Mono.empty());

      SubmissionPatch submissionPatch = buildSubmissionPatch(submissionId);
      mockSubmissionUpdate(submissionId, submissionPatch);

      when(submissionValidationContext.hasErrors(claimId.toString())).thenReturn(false);

      ClaimValidationReport claimValidationReport =
          new ClaimValidationReport(
              claimId.toString(), List.of(ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER));
      when(submissionValidationContext.getClaimReport(claimId.toString()))
          .thenReturn(Optional.of(claimValidationReport));

      ClaimPatch claimPatch =
          new ClaimPatch()
              .id(claimId.toString())
              .status(ClaimStatus.INVALID)
              .validationErrors(
                  List.of(ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER.getDescription()));
      when(dataClaimsRestClient.updateClaim(submissionId, claimId, claimPatch))
          .thenReturn(ResponseEntity.ok().build());

      when(submissionValidationContext.hasErrors(claimId.toString())).thenReturn(true);

      // When
      submissionValidationService.validateSubmission(submission);

      // Then
      verify(dataClaimsRestClient, times(1))
          .updateSubmission(submissionId.toString(), submissionPatch);
      verify(submissionValidationContext, times(1))
          .addSubmissionValidationError(
              ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER.getDescription());
      verify(dataClaimsRestClient, times(1)).getClaim(submissionId, claimId);
      verify(providerDetailsRestClient, times(1))
          .getProviderFirmSchedules(officeAccountNumber, areaOfLaw);
      verify(claimValidationService, times(1))
          .validateClaims(List.of(claimFields), Collections.emptyList());
      verify(dataClaimsRestClient, times(1)).updateClaim(submissionId, claimId, claimPatch);
    }

    @ParameterizedTest
    @MethodSource("invalidSubmissionStatusArguments")
    @DisplayName("Throws exception is submission has invalid status")
    void throwsExceptionIfSubmissionHasInvalidStatus(SubmissionStatus submissionStatus) {
      // Given
      UUID submissionId = new UUID(0, 0);
      String areaOfLaw = "areaOfLaw";
      String officeAccountNumber = "officeAccountNumber";

      GetSubmission200Response submission =
          getSubmission(
              submissionStatus, submissionId, areaOfLaw, officeAccountNumber, false, null);

      mockProviderSchedules(officeAccountNumber, areaOfLaw, "categoryOfLaw");

      // When
      submissionValidationService.validateSubmission(submission);

      // Then
      verify(submissionValidationContext, times(1))
          .addSubmissionValidationError(
              ClaimValidationError.NON_NIL_SUBMISSION_CONTAINS_NO_CLAIMS.getDescription());
    }

    static Stream<Arguments> invalidSubmissionStatusArguments() {
      return Stream.of(
          Arguments.of(SubmissionStatus.CREATED),
          Arguments.of(SubmissionStatus.REPLACED),
          Arguments.of(SubmissionStatus.VALIDATION_SUCCEEDED),
          Arguments.of(SubmissionStatus.VALIDATION_FAILED));
    }

    @Test
    @DisplayName("Throws exception is submission status is null")
    void throwsExceptionIfSubmissionStatusIsNull() {
      // Given
      UUID submissionId = new UUID(0, 0);
      String areaOfLaw = "areaOfLaw";
      String officeAccountNumber = "officeAccountNumber";

      GetSubmission200Response submission =
          getSubmission(null, submissionId, areaOfLaw, officeAccountNumber, false, null);

      mockProviderSchedules(officeAccountNumber, areaOfLaw, "categoryOfLaw");

      // When
      submissionValidationService.validateSubmission(submission);

      // Then
      verify(submissionValidationContext, times(1))
          .addSubmissionValidationError("Submission state is null");
    }

    private static Stream<Arguments> submissionValidationArguments() {
      return Stream.of(
          // isNilSubmission, claimStatus, hasErrors, expectsValidationError
          Arguments.of(
              false,
              ClaimStatus.VALID,
              false,
              false,
              "Marks claims as valid if no validation errors found"),
          Arguments.of(
              true,
              ClaimStatus.INVALID,
              true,
              true,
              "Marks claims as invalid if nil submission contains claims"));
    }

    private GetSubmission200Response buildSubmission(
        UUID submissionId, UUID claimId, boolean isNilSubmission) {
      GetSubmission200ResponseClaimsInner claim = new GetSubmission200ResponseClaimsInner();
      claim.setClaimId(claimId);
      claim.setStatus(StatusEnum.READY_TO_PROCESS);

      return getSubmission(
          SubmissionStatus.READY_FOR_VALIDATION,
          submissionId,
          "areaOfLaw",
          "officeAccountNumber",
          isNilSubmission,
          List.of(claim));
    }

    private ClaimFields buildClaimFields(UUID claimId) {
      ClaimFields claimFields = new ClaimFields();
      claimFields.id(claimId.toString());
      claimFields.feeCode("feeCode");
      return claimFields;
    }

    private void mockClaimRetrieval(UUID submissionId, UUID claimId, ClaimFields claimFields) {
      when(dataClaimsRestClient.getClaim(submissionId, claimId))
          .thenReturn(ResponseEntity.of(Optional.of(claimFields)));
    }

    private void mockProviderSchedules(
        String officeAccountNumber, String areaOfLaw, String categoryOfLaw) {
      FirmOfficeContractAndScheduleLine scheduleLine = new FirmOfficeContractAndScheduleLine();
      scheduleLine.setCategoryOfLaw(categoryOfLaw);

      FirmOfficeContractAndScheduleDetails schedule = new FirmOfficeContractAndScheduleDetails();
      schedule.scheduleLines(List.of(scheduleLine));

      ProviderFirmOfficeContractAndScheduleDto providerFirmResponse =
          new ProviderFirmOfficeContractAndScheduleDto();
      providerFirmResponse.addSchedulesItem(schedule);

      when(providerDetailsRestClient.getProviderFirmSchedules(officeAccountNumber, areaOfLaw))
          .thenReturn(Mono.just(providerFirmResponse));
    }

    private SubmissionPatch buildSubmissionPatch(UUID submissionId) {
      return new SubmissionPatch()
          .submissionId(submissionId)
          .status(SubmissionStatus.VALIDATION_IN_PROGRESS);
    }

    private void mockSubmissionUpdate(UUID submissionId, SubmissionPatch patch) {
      when(dataClaimsRestClient.updateSubmission(submissionId.toString(), patch))
          .thenReturn(ResponseEntity.ok().build());
    }

    private void mockClaimUpdate(UUID submissionId, UUID claimId, ClaimPatch patch) {
      when(dataClaimsRestClient.updateClaim(submissionId, claimId, patch))
          .thenReturn(ResponseEntity.ok().build());
    }

    private void verifyCommonInteractions(
        UUID submissionId,
        UUID claimId,
        String officeAccountNumber,
        String areaOfLaw,
        ClaimFields claimFields,
        String categoryOfLaw,
        SubmissionPatch submissionPatch,
        ClaimPatch claimPatch) {
      verify(dataClaimsRestClient, times(1))
          .updateSubmission(submissionId.toString(), submissionPatch);
      verify(dataClaimsRestClient, times(1)).getClaim(submissionId, claimId);
      verify(providerDetailsRestClient, times(1))
          .getProviderFirmSchedules(officeAccountNumber, areaOfLaw);
      verify(claimValidationService, times(1))
          .validateClaims(List.of(claimFields), List.of(categoryOfLaw));
      verify(dataClaimsRestClient, times(1)).updateClaim(submissionId, claimId, claimPatch);
    }
  }

  private static GetSubmission200Response getSubmission(
      SubmissionStatus submissionStatus,
      UUID submissionId,
      String areaOfLaw,
      String officeAccountNumber,
      boolean isNilSubmission,
      List<GetSubmission200ResponseClaimsInner> claims) {
    return GetSubmission200Response.builder()
        .submissionId(submissionId)
        .areaOfLaw(areaOfLaw)
        .officeAccountNumber(officeAccountNumber)
        .status(submissionStatus)
        .isNilSubmission(isNilSubmission)
        .claims(claims)
        .build();
  }
}
