package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.assertContextClaimError;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationReport;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.fee.scheme.model.FeeDetailsResponse;

@ExtendWith(MockitoExtension.class)
class CategoryOfLawValidationServiceTest {

  @Mock FeeSchemePlatformRestClient feeSchemePlatformRestClient;

  @InjectMocks CategoryOfLawValidationService categoryOfLawValidationService;

  @Nested
  @DisplayName("validateCategoryOfLaw")
  class ValidateCategoryOfLawTests {

    @Test
    @DisplayName("Validates category of law without errors")
    void validatesCategoryOfLawWithoutErrors() {
      ClaimResponse claim = new ClaimResponse().id("claimId").feeCode("feeCode");
      FeeDetailsResponse feeDetailsResponse =
          new FeeDetailsResponse().categoryOfLawCode("categoryOfLaw").feeType("feeType");
      Map<String, FeeDetailsResponseWrapper> feeDetailsResponseWrapperMap =
          Map.of("feeCode", FeeDetailsResponseWrapper.withFeeDetailsResponse(feeDetailsResponse));
      List<String> providerCategoriesOfLaw = List.of("categoryOfLaw");

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(List.of(new ClaimValidationReport(claim.getId())));

      categoryOfLawValidationService.validateCategoryOfLaw(
          claim, feeDetailsResponseWrapperMap, providerCategoriesOfLaw, context);

      assertThat(context.hasErrors()).isFalse();
    }

    @Test
    @DisplayName(
        "Marks claim as invalid when category of law could not be found for the provided "
            + "fee code")
    void marksClaimAsInvalidWhenCategoryOfLawNotFoundForFeeCode() {
      ClaimResponse claim = new ClaimResponse().id("claimId").feeCode("feeCode");
      FeeDetailsResponse feeDetailsResponse =
          new FeeDetailsResponse().categoryOfLawCode(null).feeType("feeType");
      Map<String, FeeDetailsResponseWrapper> feeDetailsResponseWrapperMap = new HashMap<>();
      feeDetailsResponseWrapperMap.put(
          "feeCode", FeeDetailsResponseWrapper.withFeeDetailsResponse(feeDetailsResponse));
      List<String> providerCategoriesOfLaw = List.of("categoryOfLaw");

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(List.of(new ClaimValidationReport(claim.getId())));

      categoryOfLawValidationService.validateCategoryOfLaw(
          claim, feeDetailsResponseWrapperMap, providerCategoriesOfLaw, context);

      assertContextClaimError(
          context,
          claim.getId(),
          ClaimValidationError.INVALID_CATEGORY_OF_LAW_AND_FEE_CODE,
          "feeCode");
    }

    @Test
    @DisplayName("Marks claim as invalid when category of law not found in provider contracts")
    void marksClaimAsInvalidWhenCategoryOfLawNotFoundForProvider() {
      ClaimResponse claim = new ClaimResponse().id("claimId").feeCode("feeCode");
      FeeDetailsResponse feeDetailsResponse =
          new FeeDetailsResponse().categoryOfLawCode("categoryOfLaw").feeType("feeType");
      Map<String, FeeDetailsResponseWrapper> feeDetailsResponseWrapperMap = new HashMap<>();
      feeDetailsResponseWrapperMap.put(
          "feeCode", FeeDetailsResponseWrapper.withFeeDetailsResponse(feeDetailsResponse));
      List<String> providerCategoriesOfLaw = Collections.emptyList();

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(List.of(new ClaimValidationReport(claim.getId())));

      categoryOfLawValidationService.validateCategoryOfLaw(
          claim, feeDetailsResponseWrapperMap, providerCategoriesOfLaw, context);

      assertContextClaimError(
          context,
          claim.getId(),
          ClaimValidationError.INVALID_CATEGORY_OF_LAW_NOT_AUTHORISED_FOR_PROVIDER);
    }

    @Test
    @DisplayName("Flags claim for retry when category of law response resulted in error")
    void flagsClaimForRetryWhenCategoryOfLawResponseResultInError() {
      ClaimResponse claim = new ClaimResponse().id("claimId").feeCode("feeCode");
      Map<String, FeeDetailsResponseWrapper> feeDetailsResponseWrapperMap = new HashMap<>();
      feeDetailsResponseWrapperMap.put("feeCode", FeeDetailsResponseWrapper.error());
      List<String> providerCategoriesOfLaw = List.of("categoryOfLaw");

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(List.of(new ClaimValidationReport(claim.getId())));

      categoryOfLawValidationService.validateCategoryOfLaw(
          claim, feeDetailsResponseWrapperMap, providerCategoriesOfLaw, context);

      assertThat(context.isFlaggedForRetry(claim.getId())).isTrue();
    }
  }

  @Nested
  @DisplayName("getFeeDetailsResponse")
  class GetFeeDetailsResponseTests {

    @Test
    @DisplayName("Returns map of fee code -> fee details response container")
    void getFeeDetailsResponse() {
      ClaimResponse claim1 = new ClaimResponse().id("claimId1").feeCode("feeCode1");

      ClaimResponse claim2 = new ClaimResponse().id("claimId2").feeCode("feeCode2");

      ClaimResponse claim3 = new ClaimResponse().id("claimId3").feeCode("feeCode3");

      ClaimResponse claim4 = new ClaimResponse().id("claimId4").feeCode("feeCode4");

      FeeDetailsResponse feeDetailsResponseA =
          new FeeDetailsResponse().categoryOfLawCode("categoryOfLaw1");

      FeeDetailsResponse feeDetailsResponseB =
          new FeeDetailsResponse().categoryOfLawCode("categoryOfLaw2");

      when(feeSchemePlatformRestClient.getFeeDetails("feeCode1"))
          .thenReturn(ResponseEntity.ok(feeDetailsResponseA));

      when(feeSchemePlatformRestClient.getFeeDetails("feeCode2"))
          .thenReturn(ResponseEntity.ok(feeDetailsResponseB));

      when(feeSchemePlatformRestClient.getFeeDetails("feeCode3"))
          .thenReturn(ResponseEntity.notFound().build());

      when(feeSchemePlatformRestClient.getFeeDetails("feeCode4"))
          .thenThrow(HttpServerErrorException.InternalServerError.class);

      Map<String, FeeDetailsResponseWrapper> actual =
          categoryOfLawValidationService.getFeeDetailsResponseForAllFeeCodesInClaims(
              List.of(claim1, claim2, claim3, claim4));

      assertThat(actual.get("feeCode1").getFeeDetailsResponse()).isEqualTo(feeDetailsResponseA);
      assertThat(actual.get("feeCode2").getFeeDetailsResponse()).isEqualTo(feeDetailsResponseB);
      assertThat(actual.get("feeCode3").getFeeDetailsResponse()).isNull();
      assertThat(actual.get("feeCode4").getFeeDetailsResponse()).isNull();
      assertThat(actual.get("feeCode4").isError()).isTrue();
    }
  }
}
