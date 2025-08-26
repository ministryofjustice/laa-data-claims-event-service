package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionFields;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ProviderDetailsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.SubmissionValidationException;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
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

      SubmissionFields submissionFields = new SubmissionFields();
      submissionFields.setSubmissionId(submissionId);
      submissionFields.setAreaOfLaw(areaOfLaw);
      submissionFields.setOfficeAccountNumber(officeAccountNumber);

      GetSubmission200ResponseClaimsInner claim = new GetSubmission200ResponseClaimsInner();
      claim.setClaimId(claimId);
      claim.setStatus(StatusEnum.READY_TO_PROCESS);

      GetSubmission200Response submission =
          GetSubmission200Response.builder()
              .submission(submissionFields)
              .claims(List.of(claim))
              .build();

      ClaimFields claimFields = new ClaimFields();
      claimFields.id(claimId.toString());
      claimFields.feeCode("feeCode");

      when(dataClaimsRestClient.getClaim(submissionId, claimId))
          .thenReturn(ResponseEntity.of(Optional.of(claimFields)));

      FirmOfficeContractAndScheduleLine scheduleLine = new FirmOfficeContractAndScheduleLine();
      scheduleLine.setCategoryOfLaw(categoryOfLaw);

      FirmOfficeContractAndScheduleDetails schedule = new FirmOfficeContractAndScheduleDetails();
      schedule.scheduleLines(List.of(scheduleLine));

      ProviderFirmOfficeContractAndScheduleDto providerFirmResponse =
          new ProviderFirmOfficeContractAndScheduleDto();
      providerFirmResponse.addSchedulesItem(schedule);

      when(providerDetailsRestClient.getProviderFirmSchedules(officeAccountNumber, areaOfLaw))
          .thenReturn(Mono.just(providerFirmResponse));

      ClaimPatch claimPatch = new ClaimPatch().id(claimId.toString()).status(ClaimStatus.VALID);

      when(submissionValidationContext.hasErrors(claimId.toString())).thenReturn(false);

      when(dataClaimsRestClient.updateClaim(submissionId, claimId, claimPatch))
          .thenReturn(Mono.empty());

      // When
      submissionValidationService.validateSubmission(submission);

      // Then
      verify(dataClaimsRestClient, times(1)).getClaim(submissionId, claimId);
      verify(providerDetailsRestClient, times(1))
          .getProviderFirmSchedules(officeAccountNumber, areaOfLaw);
      verify(claimValidationService, times(1))
          .validateClaims(List.of(claimFields), List.of(categoryOfLaw));
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

      SubmissionFields submissionFields = new SubmissionFields();
      submissionFields.setSubmissionId(submissionId);
      submissionFields.setAreaOfLaw(areaOfLaw);
      submissionFields.setOfficeAccountNumber(officeAccountNumber);
      submissionFields.setIsNilSubmission(true);

      GetSubmission200ResponseClaimsInner claim = new GetSubmission200ResponseClaimsInner();
      claim.setClaimId(claimId);
      claim.setStatus(StatusEnum.READY_TO_PROCESS);

      GetSubmission200Response submission =
          GetSubmission200Response.builder()
              .submission(submissionFields)
              .claims(List.of(claim))
              .build();

      ClaimFields claimFields = new ClaimFields();
      claimFields.id(claimId.toString());
      claimFields.feeCode("feeCode");

      when(dataClaimsRestClient.getClaim(submissionId, claimId))
          .thenReturn(ResponseEntity.of(Optional.of(claimFields)));

      FirmOfficeContractAndScheduleLine scheduleLine = new FirmOfficeContractAndScheduleLine();
      scheduleLine.setCategoryOfLaw(categoryOfLaw);

      FirmOfficeContractAndScheduleDetails schedule = new FirmOfficeContractAndScheduleDetails();
      schedule.scheduleLines(List.of(scheduleLine));

      ProviderFirmOfficeContractAndScheduleDto providerFirmResponse =
          new ProviderFirmOfficeContractAndScheduleDto();
      providerFirmResponse.addSchedulesItem(schedule);

      when(providerDetailsRestClient.getProviderFirmSchedules(officeAccountNumber, areaOfLaw))
          .thenReturn(Mono.just(providerFirmResponse));

      ClaimPatch claimPatch = new ClaimPatch().id(claimId.toString()).status(ClaimStatus.INVALID);

      when(dataClaimsRestClient.updateClaim(submissionId, claimId, claimPatch))
          .thenReturn(Mono.empty());

      when(submissionValidationContext.hasErrors(claimId.toString())).thenReturn(true);

      // When
      submissionValidationService.validateSubmission(submission);

      // Then
      verify(submissionValidationContext, times(1))
          .addToAllClaimReports(ClaimValidationError.INVALID_NIL_SUBMISSION_CONTAINS_CLAIMS);
      verify(dataClaimsRestClient, times(1)).getClaim(submissionId, claimId);
      verify(providerDetailsRestClient, times(1))
          .getProviderFirmSchedules(officeAccountNumber, areaOfLaw);
      verify(claimValidationService, times(1))
          .validateClaims(List.of(claimFields), List.of(categoryOfLaw));
      verify(dataClaimsRestClient, times(1)).updateClaim(submissionId, claimId, claimPatch);
    }

    @Test
    @DisplayName("Throws exception if submission not marked as nil submission contains no claims")
    void throwsExceptionIfSubmissionNotMarkedAsNilSubmissionContainsNoClaims() {
      // Given
      UUID submissionId = new UUID(0, 0);
      String areaOfLaw = "areaOfLaw";
      String officeAccountNumber = "officeAccountNumber";

      SubmissionFields submissionFields = new SubmissionFields();
      submissionFields.setSubmissionId(submissionId);
      submissionFields.setAreaOfLaw(areaOfLaw);
      submissionFields.setOfficeAccountNumber(officeAccountNumber);
      submissionFields.setIsNilSubmission(false);

      GetSubmission200Response submission =
          GetSubmission200Response.builder().submission(submissionFields).claims(null).build();

      // When
      assertThrows(
          SubmissionValidationException.class,
          () -> submissionValidationService.validateSubmission(submission),
          "Expected SubmissionValidationException to be thrown");
    }

    @Test
    @DisplayName("Marks claims as invalid if provider contract is invalid")
    void marksClaimsAsInvalidIfProviderContractInvalid() {
      // Given
      UUID submissionId = new UUID(0, 0);
      UUID claimId = new UUID(1, 1);
      String areaOfLaw = "areaOfLaw";
      String officeAccountNumber = "officeAccountNumber";

      SubmissionFields submissionFields = new SubmissionFields();
      submissionFields.setSubmissionId(submissionId);
      submissionFields.setAreaOfLaw(areaOfLaw);
      submissionFields.setOfficeAccountNumber(officeAccountNumber);

      GetSubmission200ResponseClaimsInner claim = new GetSubmission200ResponseClaimsInner();
      claim.setClaimId(claimId);
      claim.setStatus(StatusEnum.READY_TO_PROCESS);

      GetSubmission200Response submission =
          GetSubmission200Response.builder()
              .submission(submissionFields)
              .claims(List.of(claim))
              .build();

      ClaimFields claimFields = new ClaimFields();
      claimFields.id(claimId.toString());
      claimFields.feeCode("feeCode");

      when(dataClaimsRestClient.getClaim(submissionId, claimId))
          .thenReturn(ResponseEntity.of(Optional.of(claimFields)));

      when(providerDetailsRestClient.getProviderFirmSchedules(officeAccountNumber, areaOfLaw))
          .thenReturn(Mono.empty());

      ClaimPatch claimPatch = new ClaimPatch().id(claimId.toString()).status(ClaimStatus.INVALID);

      when(submissionValidationContext.hasErrors(claimId.toString())).thenReturn(false);

      when(dataClaimsRestClient.updateClaim(submissionId, claimId, claimPatch))
          .thenReturn(Mono.empty());

      when(submissionValidationContext.hasErrors(claimId.toString())).thenReturn(true);

      // When
      submissionValidationService.validateSubmission(submission);

      // Then
      verify(submissionValidationContext, times(1))
          .addToAllClaimReports(ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER);
      verify(dataClaimsRestClient, times(1)).getClaim(submissionId, claimId);
      verify(providerDetailsRestClient, times(1))
          .getProviderFirmSchedules(officeAccountNumber, areaOfLaw);
      verify(claimValidationService, times(1))
          .validateClaims(List.of(claimFields), Collections.emptyList());
      verify(dataClaimsRestClient, times(1)).updateClaim(submissionId, claimId, claimPatch);
    }
  }
}
