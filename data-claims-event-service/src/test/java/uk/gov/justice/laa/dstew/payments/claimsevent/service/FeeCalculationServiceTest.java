package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw.LEGAL_HELP;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.mapper.FeeSchemeMapper;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationReport;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationRequest;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationResponse;
import uk.gov.justice.laa.fee.scheme.model.ValidationMessagesInner;

@ExtendWith(MockitoExtension.class)
class FeeCalculationServiceTest {

  @Mock private FeeSchemePlatformRestClient feeSchemePlatformRestClient;

  @Mock private FeeSchemeMapper feeSchemeMapper;

  @InjectMocks private FeeCalculationService feeCalculationService;

  @Nested
  @DisplayName("validateFeeCalculation")
  class ValidateFeeCalculationTests {

    @Test
    @DisplayName("Successful validation does not update context")
    void successfulValidationDoesNotUpdateContext() {

      ClaimResponse claim = new ClaimResponse().id("claimId").feeCode("feeCode");

      FeeCalculationRequest feeCalculationRequest = new FeeCalculationRequest().feeCode("feeCode");
      FeeCalculationResponse feeCalculationResponse = new FeeCalculationResponse();

      when(feeSchemeMapper.mapToFeeCalculationRequest(claim, LEGAL_HELP))
          .thenReturn(feeCalculationRequest);
      when(feeSchemePlatformRestClient.calculateFee(feeCalculationRequest))
          .thenReturn(ResponseEntity.ok(feeCalculationResponse));

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(List.of(new ClaimValidationReport(claim.getId())));

      var actualResponse = feeCalculationService.calculateFee(claim, context, LEGAL_HELP);

      verify(feeSchemePlatformRestClient, times(1)).calculateFee(feeCalculationRequest);
      assertThat(context.hasErrors()).isFalse();
      assertThat(actualResponse.get()).isEqualTo(feeCalculationResponse);
    }

    @Test
    @DisplayName("Warning in fee calculation response results in claim error added to context")
    void warningResponseResultsInClaimErrorAddedToContext() {

      ClaimResponse claim =
          new ClaimResponse().id("0199a9c0-63ba-7bc2-bf71-0a8acfe1700e").feeCode("feeCode");

      FeeCalculationRequest feeCalculationRequest = new FeeCalculationRequest().feeCode("feeCode");

      ValidationMessagesInner validationMessagesInner =
          new ValidationMessagesInner()
              .message("test")
              .type(ValidationMessagesInner.TypeEnum.ERROR);
      FeeCalculationResponse feeCalculationResponse =
          new FeeCalculationResponse()
              .validationMessages(Collections.singletonList(validationMessagesInner));

      when(feeSchemeMapper.mapToFeeCalculationRequest(claim, LEGAL_HELP))
          .thenReturn(feeCalculationRequest);
      when(feeSchemePlatformRestClient.calculateFee(feeCalculationRequest))
          .thenReturn(ResponseEntity.ok(feeCalculationResponse));

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(List.of(new ClaimValidationReport(claim.getId())));

      UUID submissionId = new UUID(1, 1);
      feeCalculationService.calculateFee(claim, context, LEGAL_HELP);

      verify(feeSchemePlatformRestClient, times(1)).calculateFee(feeCalculationRequest);
      /*   verify(feeCalculationUpdaterService, times(1))
      .updateClaimWithFeeCalculationDetails(submissionId, claim, feeCalculationResponse, null);*/

      assertThat(context.hasErrors(claim.getId())).isTrue();
    }

    @Test
    @DisplayName("404 Not found response results in claims error")
    void notFoundResponseResultsInValidClaim() {

      ClaimResponse claim = new ClaimResponse().id("claimId").feeCode("feeCode");

      FeeCalculationRequest feeCalculationRequest = new FeeCalculationRequest().feeCode("feeCode");

      when(feeSchemeMapper.mapToFeeCalculationRequest(claim, LEGAL_HELP))
          .thenReturn(feeCalculationRequest);
      when(feeSchemePlatformRestClient.calculateFee(feeCalculationRequest))
          .thenThrow(
              new WebClientResponseException(
                  HttpStatus.NOT_FOUND,
                  "Not Found\",\"message\":\"Start Date is required for feeCode: CAPA",
                  null,
                  null,
                  null,
                  null));

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(List.of(new ClaimValidationReport(claim.getId())));

      var actualResponse = feeCalculationService.calculateFee(claim, context, LEGAL_HELP);

      verify(feeSchemePlatformRestClient, times(1)).calculateFee(feeCalculationRequest);

      Assertions.assertTrue(actualResponse.isEmpty());
      var actualClaimValidationReport = context.getClaimReport(claim.getId()).get();

      assertThat(actualClaimValidationReport.getMessages())
          .extracting(ValidationMessagePatch::getDisplayMessage)
          .contains(
              ClaimValidationError.INVALID_FEE_CALCULATION_VALIDATION_FAILED.getDisplayMessage());
      assertThat(actualClaimValidationReport.getMessages())
          .extracting(ValidationMessagePatch::getTechnicalMessage)
          .contains("404 Not Found\",\"message\":\"Start Date is required for feeCode: CAPA");
    }

    @Test
    @DisplayName("400 Not found response results in claims error")
    void badRequestResponseResultsInValidClaim() {
      ClaimResponse claim = new ClaimResponse().id("claimId").feeCode("feeCode");

      FeeCalculationRequest feeCalculationRequest = new FeeCalculationRequest().feeCode("feeCode");

      when(feeSchemeMapper.mapToFeeCalculationRequest(claim, LEGAL_HELP))
          .thenReturn(feeCalculationRequest);
      when(feeSchemePlatformRestClient.calculateFee(feeCalculationRequest))
          .thenThrow(
              new WebClientResponseException(
                  HttpStatus.BAD_REQUEST,
                  "Bad Request\",\"message\":\"invalid",
                  null,
                  null,
                  null,
                  null));

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(List.of(new ClaimValidationReport(claim.getId())));

      var actualResponse = feeCalculationService.calculateFee(claim, context, LEGAL_HELP);

      verify(feeSchemePlatformRestClient, times(1)).calculateFee(feeCalculationRequest);

      Assertions.assertTrue(actualResponse.isEmpty());
      var actualClaimValidationReport = context.getClaimReport(claim.getId()).get();

      assertThat(actualClaimValidationReport.getMessages())
          .extracting(ValidationMessagePatch::getDisplayMessage)
          .contains(
              ClaimValidationError.INVALID_FEE_CALCULATION_VALIDATION_FAILED.getDisplayMessage());
      assertThat(actualClaimValidationReport.getMessages())
          .extracting(ValidationMessagePatch::getTechnicalMessage)
          .contains("400 Bad Request\",\"message\":\"invalid");
    }

    @Test
    @DisplayName("500 Server error response results in claim being flagged for retry")
    void serverErrorResponseResultsInClaimBeingFlaggedForRetry() {

      ClaimResponse claim = new ClaimResponse().id("claimId").feeCode("feeCode");

      FeeCalculationRequest feeCalculationRequest = new FeeCalculationRequest().feeCode("feeCode");

      when(feeSchemeMapper.mapToFeeCalculationRequest(claim, LEGAL_HELP))
          .thenReturn(feeCalculationRequest);
      when(feeSchemePlatformRestClient.calculateFee(feeCalculationRequest))
          .thenReturn(ResponseEntity.internalServerError().build());

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(List.of(new ClaimValidationReport(claim.getId())));

      feeCalculationService.calculateFee(claim, context, LEGAL_HELP);

      verify(feeSchemePlatformRestClient, times(1)).calculateFee(feeCalculationRequest);

      assertThat(context.isFlaggedForRetry(claim.getId())).isTrue();
    }

    @Test
    @DisplayName("Skips validation if claim is flagged for retry")
    void skipsValidationIfClaimIsFlaggedForRetry() {

      ClaimResponse claim = new ClaimResponse().id("claimId").feeCode("feeCode");

      ClaimValidationReport validationReport = new ClaimValidationReport(claim.getId());
      validationReport.flagForRetry();

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(List.of(new ClaimValidationReport(claim.getId())));
      context.flagForRetry(claim.getId());

      feeCalculationService.calculateFee(claim, context, LEGAL_HELP);

      verifyNoInteractions(feeSchemeMapper);
      verifyNoInteractions(feeSchemePlatformRestClient);
      assertThat(context.isFlaggedForRetry(claim.getId())).isTrue();
    }
  }
}
