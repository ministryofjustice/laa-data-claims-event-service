package uk.gov.justice.laa.dstew.payments.claimsevent.service.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResultSet;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.FeeCalculationType;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.duplicate.DuplicateClaimCivilValidationServiceStrategy;
import uk.gov.justice.laa.fee.scheme.model.FeeDetailsResponse;

@ExtendWith(MockitoExtension.class)
class DuplicateClaimCivilValidationServiceStrategyTest
    extends AbstractDuplicateClaimValidatorStrategy {

  private static final String DISBURSEMENT_FEE_TYPE =
      FeeCalculationType.DISBURSEMENT_ONLY.toString();

  @Mock private DataClaimsRestClient mockDataClaimsRestClient;

  @Mock private FeeSchemePlatformRestClient mockFeeSchemePlatformRestClient;

  @InjectMocks private DuplicateClaimCivilValidationServiceStrategy duplicateClaimCivilValidation;

  @Captor private ArgumentCaptor<String> officeCodeArgumentCaptor;

  @Captor private ArgumentCaptor<String> feeCodeArgumentCaptor;

  @Captor private ArgumentCaptor<String> uniqueFileNumberArgumentCaptor;

  @Captor private ArgumentCaptor<String> uniqueClientNumberArgumentCaptor;

  @Captor private ArgumentCaptor<List<ClaimStatus>> claimStatusArgumentCaptor;

  @Captor private ArgumentCaptor<List<SubmissionStatus>> submissionStatusArgumentCaptor;

  @Nested
  class ValidClaim {

    @DisplayName(
        "No validation error: When there is no existing civil claim with the same Office, UFN, "
            + "Fee Code, and UCN in the same submission or previous submission")
    @Test
    void whenNoExistingClaim() {
      var claimTobeProcessed =
          createClaim("claimId1", "CIV123", "070722/001", "CLI001", ClaimStatus.READY_TO_PROCESS);
      var otherClaim =
          createClaim("claimId2", "CIV123", "070722/002", "CLI001", ClaimStatus.READY_TO_PROCESS);
      var submissionClaims = List.of(claimTobeProcessed, otherClaim);
      var context = new SubmissionValidationContext();

      when(mockFeeSchemePlatformRestClient.getFeeDetails(any()))
          .thenReturn(ResponseEntity.of(Optional.of(new FeeDetailsResponse())));

      when(mockDataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(new ClaimResultSet())));

      duplicateClaimCivilValidation.validateDuplicateClaims(
          claimTobeProcessed, submissionClaims, "2Q286D", context);

      verify(mockDataClaimsRestClient)
          .getClaims(
              officeCodeArgumentCaptor.capture(),
              any(),
              submissionStatusArgumentCaptor.capture(),
              feeCodeArgumentCaptor.capture(),
              uniqueFileNumberArgumentCaptor.capture(),
              uniqueClientNumberArgumentCaptor.capture(),
              any(),
              claimStatusArgumentCaptor.capture(),
              any());

      Assertions.assertEquals("2Q286D", officeCodeArgumentCaptor.getValue());
      Assertions.assertEquals("CIV123", feeCodeArgumentCaptor.getValue());
      Assertions.assertEquals("070722/001", uniqueFileNumberArgumentCaptor.getValue());
      Assertions.assertEquals("CLI001", uniqueClientNumberArgumentCaptor.getValue());
      Assertions.assertEquals(
          List.of(ClaimStatus.READY_TO_PROCESS, ClaimStatus.VALID),
          claimStatusArgumentCaptor.getValue());
      Assertions.assertEquals(
          List.of(
              SubmissionStatus.CREATED,
              SubmissionStatus.VALIDATION_IN_PROGRESS,
              SubmissionStatus.READY_FOR_VALIDATION,
              SubmissionStatus.VALIDATION_SUCCEEDED),
          submissionStatusArgumentCaptor.getValue());

      assertThat(context.hasErrors()).isFalse();
    }

    @DisplayName(
        "No validation error: When current claims is of disbursement type should check again "
            + "previous submissions")
    @Test
    void whenCurrentClaimIsDisbursement() {
      var claimTobeProcessed =
          createClaim("claimId1", "DISB01", "070722/001", "CLI001", ClaimStatus.READY_TO_PROCESS);
      var otherClaim =
          createClaim("claimId2", "CIV123", "070722/002", "CLI001", ClaimStatus.READY_TO_PROCESS);
      var submissionClaims = List.of(claimTobeProcessed, otherClaim);
      var context = new SubmissionValidationContext();

      when(mockFeeSchemePlatformRestClient.getFeeDetails(eq("DISB01")))
          .thenReturn(
              ResponseEntity.of(
                  Optional.of(new FeeDetailsResponse().feeType(DISBURSEMENT_FEE_TYPE))));

      duplicateClaimCivilValidation.validateDuplicateClaims(
          claimTobeProcessed, submissionClaims, "2Q286D", context);

      verify(mockFeeSchemePlatformRestClient).getFeeDetails(eq("DISB01"));

      verify(mockDataClaimsRestClient, times(0))
          .getClaims(any(), any(), any(), any(), any(), any(), any(), any(), any());

      assertThat(context.hasErrors()).isFalse();
    }

    @DisplayName(
        "No validation error: when  same Office, UFN, Fee Code exists but for different client "
            + "(UCN differs)")
    @Test
    void whenDifferentClient() {
      var claimTobeProcessed =
          createClaim("claimId1", "CIV123", "070722/001", "CLI001", ClaimStatus.READY_TO_PROCESS);
      var otherClaim =
          createClaim("claimId2", "CIV123", "070722/001", "CLI002", ClaimStatus.READY_TO_PROCESS);
      var submissionClaims = List.of(claimTobeProcessed, otherClaim);
      var context = new SubmissionValidationContext();

      when(mockFeeSchemePlatformRestClient.getFeeDetails(any()))
          .thenReturn(ResponseEntity.of(Optional.of(new FeeDetailsResponse())));

      when(mockDataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(new ClaimResultSet())));

      duplicateClaimCivilValidation.validateDuplicateClaims(
          claimTobeProcessed, submissionClaims, "2Q286D", context);

      assertThat(context.hasErrors()).isFalse();
    }

    @DisplayName(
        "No validation error: when there exists a claim with same UFN and UCN but different fee "
            + "code in same submission")
    @Test
    void whenExistingClaimInPreviousSubmission() {
      var claimTobeProcessed =
          createClaim("claimId1", "CIV123", "070722/001", "CLI001", ClaimStatus.READY_TO_PROCESS);
      var otherClaim = createClaim("claimId2", "CIV456", "070722/001", "CLI001", ClaimStatus.VALID);
      var submissionClaims = List.of(claimTobeProcessed, otherClaim);
      var context = new SubmissionValidationContext();

      when(mockFeeSchemePlatformRestClient.getFeeDetails(any()))
          .thenReturn(ResponseEntity.of(Optional.of(new FeeDetailsResponse())));

      when(mockDataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(new ClaimResultSet())));

      duplicateClaimCivilValidation.validateDuplicateClaims(
          claimTobeProcessed, submissionClaims, "2Q286D", context);

      assertThat(context.hasErrors()).isFalse();
    }

    @DisplayName("No Validation error: when same UFN with different fee code and UCN")
    @Test
    void whenDifferentFeeCodeOfficeUcn() {
      var claimTobeProcessed =
          createClaim("claimId1", "CIV123", "070722/001", "CLI001", ClaimStatus.READY_TO_PROCESS);
      var otherClaim = createClaim("claimId2", "CIV456", "070722/001", "CLI002", ClaimStatus.VALID);
      var submissionClaims = List.of(claimTobeProcessed, otherClaim);
      var context = new SubmissionValidationContext();

      when(mockFeeSchemePlatformRestClient.getFeeDetails(any()))
          .thenReturn(ResponseEntity.of(Optional.of(new FeeDetailsResponse())));

      when(mockDataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(ResponseEntity.of(Optional.of(new ClaimResultSet())));

      duplicateClaimCivilValidation.validateDuplicateClaims(
          claimTobeProcessed, submissionClaims, "2Q286D", context);

      assertThat(context.hasErrors()).isFalse();
    }
  }

  @Nested
  class InvalidClaim {

    @DisplayName(
        "Validation error: When there is an existing civil claim with the same Office, UFN, Fee Code, "
            + "and UCN in the previous submission")
    @Test
    void whenExistingClaimInPreviousSubmission() {
      var claimTobeProcessed =
          createClaim("claimId1", "CIV123", "070722/001", "CLI001", ClaimStatus.READY_TO_PROCESS);
      var otherClaim =
          createClaim("claimId2", "CIV123", "070722/002", "CLI001", ClaimStatus.READY_TO_PROCESS);
      var otherClaim1 =
          createClaim("claimId3", "CIV123", "070722/003", "CLI001", ClaimStatus.READY_TO_PROCESS);
      var claimInPreviousSubmission =
          createClaim("claimId4", "CIV123", "070722/003", "CLI001", ClaimStatus.READY_TO_PROCESS);
      var submissionClaims = List.of(claimTobeProcessed, otherClaim, otherClaim1);
      var context = new SubmissionValidationContext();

      when(mockFeeSchemePlatformRestClient.getFeeDetails(any()))
          .thenReturn(ResponseEntity.of(Optional.of(new FeeDetailsResponse())));

      when(mockDataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(
                  Optional.of(new ClaimResultSet().addContentItem(claimInPreviousSubmission))));

      duplicateClaimCivilValidation.validateDuplicateClaims(
          claimTobeProcessed, submissionClaims, "2Q286D", context);

      assertThat(context.hasErrors()).isTrue();

      assertThat(context.getClaimReports()).extracting("claimId").contains("claimId1");

      context
          .getClaimReport("claimId2")
          .ifPresent(
              claimValidationReport ->
                  assertThat(claimValidationReport.getMessages())
                      .extracting("displayMessage")
                      .isEqualTo(
                          List.of(
                              ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION
                                  .getDisplayMessage())));
    }

    @DisplayName(
        "Validation error: When there is an existing civil claim with the same Office, UFN, Fee Code, "
            + "and UCN in the previous and current submission")
    @Test
    void whenExistingClaimInPreviousAndCurrentSubmission() {
      var claimTobeProcessed =
          createClaim("claimId1", "CIV123", "070722/001", "CLI001", ClaimStatus.READY_TO_PROCESS);
      var otherClaim =
          createClaim("claimId2", "CIV123", "070722/001", "CLI001", ClaimStatus.READY_TO_PROCESS);
      var claimInPreviousSubmission =
          createClaim("claimId4", "CIV123", "070722/003", "CLI001", ClaimStatus.READY_TO_PROCESS);
      var submissionClaims = List.of(claimTobeProcessed, otherClaim);
      var context = new SubmissionValidationContext();

      when(mockFeeSchemePlatformRestClient.getFeeDetails(any()))
          .thenReturn(ResponseEntity.of(Optional.of(new FeeDetailsResponse())));

      when(mockDataClaimsRestClient.getClaims(
              any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(
              ResponseEntity.of(
                  Optional.of(new ClaimResultSet().addContentItem(claimInPreviousSubmission))));

      duplicateClaimCivilValidation.validateDuplicateClaims(
          claimTobeProcessed, submissionClaims, "2Q286D", context);

      assertThat(context.hasErrors()).isTrue();

      assertThat(context.getClaimReports()).extracting("claimId").contains("claimId1");

      context
          .getClaimReport("claimId1")
          .ifPresent(
              claimValidationReport ->
                  assertThat(claimValidationReport.getMessages())
                      .extracting("displayMessage")
                      .isEqualTo(
                          List.of(
                              ClaimValidationError.INVALID_CLAIM_HAS_DUPLICATE_IN_ANOTHER_SUBMISSION
                                  .getDisplayMessage())));

      context
          .getClaimReport("claimId2")
          .ifPresent(
              claimValidationReport ->
                  assertThat(claimValidationReport.getMessages())
                      .extracting("displayMessage")
                      .isEqualTo(
                          List.of(
                              ClaimValidationError
                                  .INVALID_CLAIM_HAS_DUPLICATE_IN_EXISTING_SUBMISSION
                                  .getDisplayMessage())));
    }
  }
}
