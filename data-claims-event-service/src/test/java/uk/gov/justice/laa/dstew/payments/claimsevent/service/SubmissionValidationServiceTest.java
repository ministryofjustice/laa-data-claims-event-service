package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
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
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionClaim;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ProviderDetailsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.SubmissionValidationException;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationReport;
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

  @InjectMocks private SubmissionValidationService submissionValidationService;

  @Nested
  @DisplayName("validateSubmission")
  class ValidateSubmissionTests {

    @Test
    @DisplayName("Marks claims as valid if no validation errors found")
    void updatesClaims() {
      // Given
      UUID submissionId = new UUID(0, 0);
      UUID claimId = new UUID(1, 1);
      String areaOfLaw = "areaOfLaw";
      String categoryOfLaw = "categoryOfLaw";
      String officeAccountNumber = "officeAccountNumber";

      SubmissionClaim claim = new SubmissionClaim();
      claim.setClaimId(claimId);
      claim.setStatus(ClaimStatus.READY_TO_PROCESS);

      SubmissionResponse submission =
          SubmissionResponse.builder()
              .submissionId(submissionId)
              .areaOfLaw(areaOfLaw)
              .officeAccountNumber(officeAccountNumber)
              .status(SubmissionStatus.READY_FOR_VALIDATION)
              .claims(List.of(claim))
              .build();

      ClaimResponse claimResponse = new ClaimResponse();
      claimResponse.id(claimId.toString());
      claimResponse.feeCode("feeCode");

      when(dataClaimsRestClient.getClaim(submissionId, claimId))
          .thenReturn(ResponseEntity.of(Optional.of(claimResponse)));

      FirmOfficeContractAndScheduleLine scheduleLine = new FirmOfficeContractAndScheduleLine();
      scheduleLine.setCategoryOfLaw(categoryOfLaw);

      FirmOfficeContractAndScheduleDetails schedule = new FirmOfficeContractAndScheduleDetails();
      schedule.scheduleLines(List.of(scheduleLine));

      ProviderFirmOfficeContractAndScheduleDto providerFirmResponse =
          new ProviderFirmOfficeContractAndScheduleDto();
      providerFirmResponse.addSchedulesItem(schedule);

      when(providerDetailsRestClient.getProviderFirmSchedules(officeAccountNumber, areaOfLaw))
          .thenReturn(Mono.just(providerFirmResponse));

      SubmissionPatch submissionPatch =
          new SubmissionPatch()
              .submissionId(submissionId)
              .status(SubmissionStatus.VALIDATION_IN_PROGRESS);

      when(dataClaimsRestClient.updateSubmission(submissionId.toString(), submissionPatch))
          .thenReturn(ResponseEntity.ok().build());

      ClaimPatch claimPatch = new ClaimPatch().id(claimId.toString()).status(ClaimStatus.VALID);

      when(submissionValidationContext.hasErrors(claimId.toString())).thenReturn(false);

      when(dataClaimsRestClient.updateClaim(submissionId, claimId, claimPatch))
          .thenReturn(ResponseEntity.ok().build());

      // When
      submissionValidationService.validateSubmission(submission);

      // Then
      verify(dataClaimsRestClient, times(1))
          .updateSubmission(submissionId.toString(), submissionPatch);
      verify(dataClaimsRestClient, times(1)).getClaim(submissionId, claimId);
      verify(providerDetailsRestClient, times(1))
          .getProviderFirmSchedules(officeAccountNumber, areaOfLaw);
      verify(claimValidationService, times(1))
          .validateClaims(List.of(claimResponse), List.of(categoryOfLaw));
      verify(dataClaimsRestClient, times(1)).updateClaim(submissionId, claimId, claimPatch);
    }

    @Test
    @DisplayName("Marks claims as invalid if nil submission contains claims")
    void marksClaimsAsInvalidIfNilSubmissionContainsClaims() {
      // Given
      UUID submissionId = new UUID(0, 0);
      UUID claimId = new UUID(1, 1);
      String areaOfLaw = "areaOfLaw";
      String categoryOfLaw = "categoryOfLaw";
      String officeAccountNumber = "officeAccountNumber";

      SubmissionClaim claim = new SubmissionClaim();
      claim.setClaimId(claimId);
      claim.setStatus(ClaimStatus.READY_TO_PROCESS);

      SubmissionResponse submission =
          SubmissionResponse.builder()
              .submissionId(submissionId)
              .areaOfLaw(areaOfLaw)
              .officeAccountNumber(officeAccountNumber)
              .status(SubmissionStatus.READY_FOR_VALIDATION)
              .isNilSubmission(true)
              .claims(List.of(claim))
              .build();

      ClaimResponse claimResponse = new ClaimResponse();
      claimResponse.id(claimId.toString());
      claimResponse.feeCode("feeCode");

      when(dataClaimsRestClient.getClaim(submissionId, claimId))
          .thenReturn(ResponseEntity.of(Optional.of(claimResponse)));

      FirmOfficeContractAndScheduleLine scheduleLine = new FirmOfficeContractAndScheduleLine();
      scheduleLine.setCategoryOfLaw(categoryOfLaw);

      FirmOfficeContractAndScheduleDetails schedule = new FirmOfficeContractAndScheduleDetails();
      schedule.scheduleLines(List.of(scheduleLine));

      ProviderFirmOfficeContractAndScheduleDto providerFirmResponse =
          new ProviderFirmOfficeContractAndScheduleDto();
      providerFirmResponse.addSchedulesItem(schedule);

      when(providerDetailsRestClient.getProviderFirmSchedules(officeAccountNumber, areaOfLaw))
          .thenReturn(Mono.just(providerFirmResponse));

      SubmissionPatch submissionPatch =
          new SubmissionPatch()
              .submissionId(submissionId)
              .status(SubmissionStatus.VALIDATION_IN_PROGRESS);

      when(dataClaimsRestClient.updateSubmission(submissionId.toString(), submissionPatch))
          .thenReturn(ResponseEntity.ok().build());

      ClaimPatch claimPatch = new ClaimPatch().id(claimId.toString()).status(ClaimStatus.INVALID);

      when(dataClaimsRestClient.updateClaim(submissionId, claimId, claimPatch))
          .thenReturn(ResponseEntity.ok().build());

      when(submissionValidationContext.hasErrors(claimId.toString())).thenReturn(true);

      // When
      submissionValidationService.validateSubmission(submission);

      // Then
      verify(dataClaimsRestClient, times(1))
          .updateSubmission(submissionId.toString(), submissionPatch);
      verify(submissionValidationContext, times(1))
          .addToAllClaimReports(ClaimValidationError.INVALID_NIL_SUBMISSION_CONTAINS_CLAIMS);
      verify(dataClaimsRestClient, times(1)).getClaim(submissionId, claimId);
      verify(providerDetailsRestClient, times(1))
          .getProviderFirmSchedules(officeAccountNumber, areaOfLaw);
      verify(claimValidationService, times(1))
          .validateClaims(List.of(claimResponse), List.of(categoryOfLaw));
      verify(dataClaimsRestClient, times(1)).updateClaim(submissionId, claimId, claimPatch);
    }

    @Test
    @DisplayName("Throws exception if submission not marked as nil submission contains no claims")
    void throwsExceptionIfSubmissionNotMarkedAsNilSubmissionContainsNoClaims() {
      // Given
      UUID submissionId = new UUID(0, 0);
      String areaOfLaw = "areaOfLaw";
      String officeAccountNumber = "officeAccountNumber";

      SubmissionResponse submission =
          SubmissionResponse.builder()
              .submissionId(submissionId)
              .areaOfLaw(areaOfLaw)
              .officeAccountNumber(officeAccountNumber)
              .status(SubmissionStatus.READY_FOR_VALIDATION)
              .isNilSubmission(false)
              .claims(null)
              .build();

      // When
      ThrowingCallable throwingCallable =
          () -> submissionValidationService.validateSubmission(submission);

      // Then
      assertThatThrownBy(throwingCallable)
          .isInstanceOf(SubmissionValidationException.class)
          .hasMessageContaining(
              "Submission is not marked as nil submission, " + "but does not contain any claims");
    }

    @Test
    @DisplayName("Marks claims as invalid if provider contract is invalid")
    void marksClaimsAsInvalidIfProviderContractInvalid() {
      // Given
      UUID submissionId = new UUID(0, 0);
      UUID claimId = new UUID(1, 1);
      String areaOfLaw = "areaOfLaw";
      String officeAccountNumber = "officeAccountNumber";

      SubmissionClaim claim = new SubmissionClaim();
      claim.setClaimId(claimId);
      claim.setStatus(ClaimStatus.READY_TO_PROCESS);

      SubmissionResponse submission =
          SubmissionResponse.builder()
              .submissionId(submissionId)
              .areaOfLaw(areaOfLaw)
              .officeAccountNumber(officeAccountNumber)
              .status(SubmissionStatus.READY_FOR_VALIDATION)
              .claims(List.of(claim))
              .build();

      ClaimResponse claimResponse = new ClaimResponse();
      claimResponse.id(claimId.toString());
      claimResponse.feeCode("feeCode");

      when(dataClaimsRestClient.getClaim(submissionId, claimId))
          .thenReturn(ResponseEntity.of(Optional.of(claimResponse)));

      when(providerDetailsRestClient.getProviderFirmSchedules(officeAccountNumber, areaOfLaw))
          .thenReturn(Mono.empty());

      SubmissionPatch submissionPatch =
          new SubmissionPatch()
              .submissionId(submissionId)
              .status(SubmissionStatus.VALIDATION_IN_PROGRESS);

      when(dataClaimsRestClient.updateSubmission(submissionId.toString(), submissionPatch))
          .thenReturn(ResponseEntity.ok().build());

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
          .addToAllClaimReports(ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER);
      verify(dataClaimsRestClient, times(1)).getClaim(submissionId, claimId);
      verify(providerDetailsRestClient, times(1))
          .getProviderFirmSchedules(officeAccountNumber, areaOfLaw);
      verify(claimValidationService, times(1))
          .validateClaims(List.of(claimResponse), Collections.emptyList());
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
          SubmissionResponse.builder()
              .submissionId(submissionId)
              .areaOfLaw(areaOfLaw)
              .officeAccountNumber(officeAccountNumber)
              .status(submissionStatus)
              .isNilSubmission(false)
              .claims(null)
              .build();

      // When
      ThrowingCallable throwingCallable =
          () -> submissionValidationService.validateSubmission(submission);

      // Then
      assertThatThrownBy(throwingCallable)
          .isInstanceOf(SubmissionValidationException.class)
          .hasMessageContaining("Submission cannot be validated in state " + submissionStatus);
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
          SubmissionResponse.builder()
              .submissionId(submissionId)
              .areaOfLaw(areaOfLaw)
              .officeAccountNumber(officeAccountNumber)
              .status(null)
              .isNilSubmission(false)
              .claims(null)
              .build();

      // When
      ThrowingCallable throwingCallable =
          () -> submissionValidationService.validateSubmission(submission);

      // Then
      assertThatThrownBy(throwingCallable)
          .isInstanceOf(SubmissionValidationException.class)
          .hasMessageContaining("Submission state is null");
    }
  }
}
