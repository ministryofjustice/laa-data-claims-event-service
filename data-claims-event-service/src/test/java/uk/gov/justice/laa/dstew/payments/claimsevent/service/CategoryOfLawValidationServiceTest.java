package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.fee.scheme.model.FeeDetailsResponse;

@ExtendWith(MockitoExtension.class)
class CategoryOfLawValidationServiceTest {

  @Mock
  SubmissionValidationContext submissionValidationContext;

  @Mock
  FeeSchemePlatformRestClient feeSchemePlatformRestClient;

  @InjectMocks
  CategoryOfLawValidationService categoryOfLawValidationService;

  @Nested
  @DisplayName("validateCategoryOfLaw")
  class ValidateCategoryOfLawTests {

    @Test
    @DisplayName("Validates category of law without errors")
    void validatesCategoryOfLawWithoutErrors() {
      ClaimResponse claim = new ClaimResponse().id("claimId").feeCode("feeCode");
      Map<String, CategoryOfLawResult> categoryOfLawLookup =
          Map.of("feeCode", CategoryOfLawResult.withCategoryOfLaw("categoryOfLaw"));
      List<String> providerCategoriesOfLaw = List.of("categoryOfLaw");

      categoryOfLawValidationService.validateCategoryOfLaw(
          claim, categoryOfLawLookup, providerCategoriesOfLaw);

      verifyNoInteractions(submissionValidationContext);
    }

    @Test
    @DisplayName(
        "Marks claim as invalid when category of law could not be found for the provided "
            + "fee code")
    void marksClaimAsInvalidWhenCategoryOfLawNotFoundForFeeCode() {
      ClaimResponse claim = new ClaimResponse().id("claimId").feeCode("feeCode");
      Map<String, CategoryOfLawResult> categoryOfLawLookup = new HashMap<>();
      categoryOfLawLookup.put("feeCode", CategoryOfLawResult.withCategoryOfLaw(null));
      List<String> providerCategoriesOfLaw = List.of("categoryOfLaw");

      categoryOfLawValidationService.validateCategoryOfLaw(
          claim, categoryOfLawLookup, providerCategoriesOfLaw);

      verify(submissionValidationContext, times(1))
          .addClaimError("claimId", ClaimValidationError.INVALID_CATEGORY_OF_LAW_AND_FEE_CODE);
    }

    @Test
    @DisplayName("Marks claim as invalid when category of law not found in provider contracts")
    void marksClaimAsInvalidWhenCategoryOfLawNotFoundForProvider() {
      ClaimResponse claim = new ClaimResponse().id("claimId").feeCode("feeCode");
      Map<String, CategoryOfLawResult> categoryOfLawLookup = new HashMap<>();
      categoryOfLawLookup.put("feeCode", CategoryOfLawResult.withCategoryOfLaw("categoryOfLaw"));
      List<String> providerCategoriesOfLaw = Collections.emptyList();

      categoryOfLawValidationService.validateCategoryOfLaw(
          claim, categoryOfLawLookup, providerCategoriesOfLaw);

      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claimId", ClaimValidationError.INVALID_CATEGORY_OF_LAW_NOT_AUTHORISED_FOR_PROVIDER);
    }

    @Test
    @DisplayName("Flags claim for retry when category of law response resulted in error")
    void flagsClaimForRetryWhenCategoryOfLawResponseResultInError() {
      ClaimResponse claim = new ClaimResponse().id("claimId").feeCode("feeCode");
      Map<String, CategoryOfLawResult> categoryOfLawLookup = new HashMap<>();
      categoryOfLawLookup.put("feeCode", CategoryOfLawResult.error());
      List<String> providerCategoriesOfLaw = List.of("categoryOfLaw");

      categoryOfLawValidationService.validateCategoryOfLaw(
          claim, categoryOfLawLookup, providerCategoriesOfLaw);

      verify(submissionValidationContext, times(1)).flagForRetry("claimId");
    }
  }

  @Nested
  @DisplayName("getCategoryOfLawLookup")
  class GetCategoryOfLawLookupTests {

    @Test
    @DisplayName("Returns map of fee code -> category of law container")
    void getCategoryOfLawLookup() {
      ClaimResponse claim1 = new ClaimResponse().id("claimId1").feeCode("feeCode1");

      ClaimResponse claim2 = new ClaimResponse().id("claimId2").feeCode("feeCode2");

      ClaimResponse claim3 = new ClaimResponse().id("claimId3").feeCode("feeCode3");

      ClaimResponse claim4 = new ClaimResponse().id("claimId4").feeCode("feeCode4");

      FeeDetailsResponse categoryOfLawResponse1 =
          new FeeDetailsResponse().categoryOfLawCode("categoryOfLaw1");

      FeeDetailsResponse categoryOfLawResponse2 =
          new FeeDetailsResponse().categoryOfLawCode("categoryOfLaw2");

      when(feeSchemePlatformRestClient.getFeeDetails("feeCode1"))
          .thenReturn(ResponseEntity.ok(categoryOfLawResponse1));

      when(feeSchemePlatformRestClient.getFeeDetails("feeCode2"))
          .thenReturn(ResponseEntity.ok(categoryOfLawResponse2));

      when(feeSchemePlatformRestClient.getFeeDetails("feeCode3"))
          .thenReturn(ResponseEntity.notFound().build());

      when(feeSchemePlatformRestClient.getFeeDetails("feeCode4"))
          .thenReturn(ResponseEntity.internalServerError().build());

      Map<String, CategoryOfLawResult> actual =
          categoryOfLawValidationService.getCategoryOfLawLookup(
              List.of(claim1, claim2, claim3, claim4));

      Map<String, CategoryOfLawResult> expected = new HashMap<>();
      expected.put("feeCode1", CategoryOfLawResult.withCategoryOfLaw("categoryOfLaw1"));
      expected.put("feeCode2", CategoryOfLawResult.withCategoryOfLaw("categoryOfLaw2"));
      expected.put("feeCode3", CategoryOfLawResult.withCategoryOfLaw(null));
      expected.put("feeCode4", CategoryOfLawResult.error());

      assertThat(actual).isEqualTo(expected);
    }
  }
}
