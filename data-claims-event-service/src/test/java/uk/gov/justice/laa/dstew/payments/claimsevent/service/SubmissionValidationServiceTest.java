package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionClaim;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ProviderDetailsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
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

  @Mock private JsonSchemaValidator jsonSchemaValidator;

  @InjectMocks private SubmissionValidationService submissionValidationService;

  @Nested
  @DisplayName("validateSubmission")
  class ValidateSubmissionTests {

    @Test
    @DisplayName("Should have no validation errors")
    void testNoValidationErrors() {
      boolean isNilSubmission = false;
      ClaimStatus claimStatus = ClaimStatus.VALID;
      boolean expectsValidationError = false;
      // Given
      UUID submissionId = new UUID(0, 0);
      UUID claimId =
          claimStatus != null ? new UUID(1, 1) : null; // only create claimId if there is a claim
      String categoryOfLaw = "categoryOfLaw";
      String officeAccountNumber = "officeAccountNumber";
      String areaOfLaw = "areaOfLaw";

      SubmissionResponse submission = buildSubmission(submissionId, claimId, isNilSubmission);

      when(dataClaimsRestClient.getSubmission(submissionId))
          .thenReturn(ResponseEntity.of(Optional.of(submission)));

      SubmissionPatch submissionPatch = buildSubmissionPatch(submissionId);

      SubmissionValidationContext result;

      if (claimId != null) {
        mockProviderSchedules(officeAccountNumber, areaOfLaw, categoryOfLaw);

        mockSubmissionUpdate(submissionId, submissionPatch);

        ClaimPatch claimPatch = new ClaimPatch().id(claimId.toString()).status(claimStatus);
        mockClaimUpdate(submissionId, claimId, claimPatch);

        // When
        result = submissionValidationService.validateSubmission(submissionId);

        // Then
        verifyCommonInteractions(
            submissionId,
            claimId,
            officeAccountNumber,
            areaOfLaw,
            submission,
            submissionPatch,
            claimPatch);
      } else {
        // When
        result = submissionValidationService.validateSubmission(submission.getSubmissionId());
      }

      if (expectsValidationError) {
        // Determine which error to verify
        ClaimValidationError error;
        if (isNilSubmission) {
          error = ClaimValidationError.INVALID_NIL_SUBMISSION_CONTAINS_CLAIMS;
        } else {
          error = ClaimValidationError.NON_NIL_SUBMISSION_CONTAINS_NO_CLAIMS;
        }
        assertThat(result.getSubmissionValidationErrors().getFirst())
            .isEqualTo(ClaimValidationError.NON_NIL_SUBMISSION_CONTAINS_NO_CLAIMS.toPatch());
      }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName(
        "Adds submission validation error if submission not marked as nil submission contains no "
            + "claims")
    void throwsExceptionIfSubmissionNotMarkedAsNilSubmissionContainsNoClaims(
        List<SubmissionClaim> submissionClaims) {
      // Given
      UUID submissionId = new UUID(0, 0);
      String areaOfLaw = "areaOfLaw";
      String categoryOfLaw = "categoryOfLaw";
      String officeAccountNumber = "officeAccountNumber";

      SubmissionResponse submission =
          getSubmission(
              SubmissionStatus.READY_FOR_VALIDATION,
              submissionId,
              areaOfLaw,
              officeAccountNumber,
              false,
              submissionClaims);

      mockProviderSchedules(officeAccountNumber, areaOfLaw, categoryOfLaw);

      when(dataClaimsRestClient.getSubmission(submissionId))
          .thenReturn(ResponseEntity.of(Optional.of(submission)));

      // When
      SubmissionValidationContext result =
          submissionValidationService.validateSubmission(submission.getSubmissionId());

      // Then
      assertThat(result.getSubmissionValidationErrors().getFirst())
          .isEqualTo(ClaimValidationError.NON_NIL_SUBMISSION_CONTAINS_NO_CLAIMS.toPatch());
    }

    @Test
    @Disabled(
        "Disabled this test as logic will be moved to ClaimValidationServiceTest as part of"
            + " CCMSPUI-840")
    @DisplayName("Marks claims as invalid if provider contract is invalid")
    void marksClaimsAsInvalidIfProviderContractInvalid() {
      // Given
      UUID submissionId = new UUID(0, 0);
      UUID claimId = new UUID(1, 1);
      String areaOfLaw = "areaOfLaw";
      String officeAccountNumber = "officeAccountNumber";

      SubmissionResponse submission = buildSubmission(submissionId, claimId, false);

      when(dataClaimsRestClient.getSubmission(submissionId))
          .thenReturn(ResponseEntity.of(Optional.of(submission)));

      ClaimResponse claimResponse = buildClaimResponse(claimId);

      when(dataClaimsRestClient.getClaim(submissionId, claimId))
          .thenReturn(ResponseEntity.of(Optional.of(claimResponse)));

      when(providerDetailsRestClient.getProviderFirmSchedules(officeAccountNumber, areaOfLaw))
          .thenReturn(Mono.empty());

      SubmissionPatch submissionPatch = buildSubmissionPatch(submissionId);
      mockSubmissionUpdate(submissionId, submissionPatch);

      ClaimPatch claimPatch =
          new ClaimPatch()
              .id(claimId.toString())
              .status(ClaimStatus.INVALID)
              .validationMessages(
                  List.of(ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER.toPatch()));
      when(dataClaimsRestClient.updateClaim(submissionId, claimId, claimPatch))
          .thenReturn(ResponseEntity.ok().build());

      SubmissionClaim submissionClaim = new SubmissionClaim();
      submissionClaim.setClaimId(claimId);
      submissionClaim.setStatus(ClaimStatus.READY_TO_PROCESS);

      when(dataClaimsRestClient.getSubmission(submissionId))
          .thenReturn(ResponseEntity.of(Optional.of(submission)));

      // When
      SubmissionValidationContext result =
          submissionValidationService.validateSubmission(submissionId);

      // Then
      assertThat(result.getSubmissionValidationErrors().getFirst())
          .isEqualTo(ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER.toPatch());
      verify(dataClaimsRestClient, times(1)).getSubmission(submissionId);
      verify(dataClaimsRestClient, times(1))
          .updateSubmission(submissionId.toString(), submissionPatch);
      verify(dataClaimsRestClient, times(1)).getClaim(submissionId, claimId);
      verify(providerDetailsRestClient, times(1))
          .getProviderFirmSchedules(officeAccountNumber, areaOfLaw);
      verify(claimValidationService, times(1))
          .validateClaims(eq(submission), any(SubmissionValidationContext.class));
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

      SubmissionResponse submission =
          getSubmission(
              submissionStatus,
              submissionId,
              areaOfLaw,
              officeAccountNumber,
              false,
              singletonList(new SubmissionClaim().status(ClaimStatus.INVALID)));

      mockProviderSchedules(officeAccountNumber, areaOfLaw, "categoryOfLaw");

      when(dataClaimsRestClient.getSubmission(submissionId))
          .thenReturn(ResponseEntity.of(Optional.of(submission)));

      // When
      SubmissionValidationContext result =
          submissionValidationService.validateSubmission(submission.getSubmissionId());

      // Then
      assertThat(result.getSubmissionValidationErrors().get(0).getDisplayMessage())
          .isEqualTo("Submission cannot be validated in state " + submissionStatus);
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

      SubmissionResponse submission =
          getSubmission(
              null,
              submissionId,
              areaOfLaw,
              officeAccountNumber,
              false,
              singletonList(new SubmissionClaim()));

      when(dataClaimsRestClient.getSubmission(submissionId))
          .thenReturn(ResponseEntity.of(Optional.of(submission)));

      mockProviderSchedules(officeAccountNumber, areaOfLaw, "categoryOfLaw");

      // When
      SubmissionValidationContext result =
          submissionValidationService.validateSubmission(submission.getSubmissionId());

      // Then
      assertThat(result.getSubmissionValidationErrors().getFirst())
          .isEqualTo(ClaimValidationError.SUBMISSION_STATE_IS_NULL.toPatch());
    }

    private SubmissionResponse buildSubmission(
        UUID submissionId, UUID claimId, boolean isNilSubmission) {
      SubmissionClaim claim = new SubmissionClaim();
      claim.setClaimId(claimId);
      claim.setStatus(ClaimStatus.READY_TO_PROCESS);

      return getSubmission(
          SubmissionStatus.READY_FOR_VALIDATION,
          submissionId,
          "areaOfLaw",
          "officeAccountNumber",
          isNilSubmission,
          List.of(claim));
    }

    private ClaimResponse buildClaimResponse(UUID claimId) {
      ClaimResponse claimResponse = new ClaimResponse();
      claimResponse.id(claimId.toString());
      claimResponse.feeCode("feeCode");
      return claimResponse;
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
        SubmissionResponse submissionResponse,
        SubmissionPatch submissionPatch,
        ClaimPatch claimPatch) {
      verify(dataClaimsRestClient, times(1))
          .updateSubmission(submissionId.toString(), submissionPatch);
      // verify(dataClaimsRestClient, times(1)).getClaim(submissionId, claimId);
      verify(claimValidationService, times(1)).validateClaims(eq(submissionResponse), any());
      verify(providerDetailsRestClient, times(1))
          .getProviderFirmSchedules(officeAccountNumber, areaOfLaw);
      verify(claimValidationService, times(1))
          .validateClaims(eq(submissionResponse), any(SubmissionValidationContext.class));
      verify(dataClaimsRestClient, times(1)).updateClaim(submissionId, claimId, claimPatch);
    }
  }

  private static SubmissionResponse getSubmission(
      SubmissionStatus submissionStatus,
      UUID submissionId,
      String areaOfLaw,
      String officeAccountNumber,
      boolean isNilSubmission,
      List<SubmissionClaim> claims) {
    return SubmissionResponse.builder()
        .submissionId(submissionId)
        .areaOfLaw(areaOfLaw)
        .officeAccountNumber(officeAccountNumber)
        .status(submissionStatus)
        .isNilSubmission(isNilSubmission)
        .claims(claims)
        .build();
  }
}
