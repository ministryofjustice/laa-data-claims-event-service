package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimFields;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.fee.scheme.model.CategoryOfLawResponse;

@ExtendWith(MockitoExtension.class)
class CategoryOfLawValidationServiceTest {

  @Mock SubmissionValidationContext submissionValidationContext;

  @Mock FeeSchemePlatformRestClient feeSchemePlatformRestClient;

  @InjectMocks CategoryOfLawValidationService categoryOfLawValidationService;

  @Nested
  @DisplayName("validateCategoryOfLaw")
  class ValidateCategoryOfLawTests {

    @Test
    @DisplayName("Validates category of law without errors")
    void validatesCategoryOfLawWithoutErrors() {
      ClaimFields claim = new ClaimFields().id("claimId").feeCode("feeCode");
      Map<String, String> categoryOfLawLookup = Map.of("feeCode", "categoryOfLaw");
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
      ClaimFields claim = new ClaimFields().id("claimId").feeCode("feeCode");
      Map<String, String> categoryOfLawLookup = new HashMap<>();
      categoryOfLawLookup.put("feeCode", null);
      List<String> providerCategoriesOfLaw = List.of("categoryOfLaw");

      categoryOfLawValidationService.validateCategoryOfLaw(
          claim, categoryOfLawLookup, providerCategoriesOfLaw);

      verify(submissionValidationContext, times(1))
          .addClaimError("claimId", ClaimValidationError.INVALID_CATEGORY_OF_LAW_AND_FEE_CODE);
    }

    @Test
    @DisplayName("Marks claim as invalid when category of law not found in provider contracts")
    void marksClaimAsInvalidWhenCategoryOfLawNotFoundForProvider() {
      ClaimFields claim = new ClaimFields().id("claimId").feeCode("feeCode");
      Map<String, String> categoryOfLawLookup = new HashMap<>();
      categoryOfLawLookup.put("feeCode", "categoryOfLaw");
      List<String> providerCategoriesOfLaw = Collections.emptyList();

      categoryOfLawValidationService.validateCategoryOfLaw(
          claim, categoryOfLawLookup, providerCategoriesOfLaw);

      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claimId", ClaimValidationError.INVALID_CATEGORY_OF_LAW_NOT_AUTHORISED_FOR_PROVIDER);
    }
  }

  @Nested
  @DisplayName("getCategoryOfLawLookup")
  class GetCategoryOfLawLookupTests {

    @Test
    @DisplayName("Returns map of fee code -> category of law")
    void getCategoryOfLawLookup() {
      ClaimFields claim1 = new ClaimFields().id("claimId1").feeCode("feeCode1");

      ClaimFields claim2 = new ClaimFields().id("claimId2").feeCode("feeCode2");

      ClaimFields claim3 = new ClaimFields().id("claimId3").feeCode("feeCode3");

      CategoryOfLawResponse categoryOfLawResponse1 =
          new CategoryOfLawResponse().categoryOfLawCode("categoryOfLaw1");

      CategoryOfLawResponse categoryOfLawResponse2 =
          new CategoryOfLawResponse().categoryOfLawCode("categoryOfLaw2");

      when(feeSchemePlatformRestClient.getCategoryOfLaw("feeCode1"))
          .thenReturn(ResponseEntity.ok(categoryOfLawResponse1));

      when(feeSchemePlatformRestClient.getCategoryOfLaw("feeCode2"))
          .thenReturn(ResponseEntity.ok(categoryOfLawResponse2));

      when(feeSchemePlatformRestClient.getCategoryOfLaw("feeCode3"))
          .thenReturn(ResponseEntity.notFound().build());

      Map<String, String> actual =
          categoryOfLawValidationService.getCategoryOfLawLookup(List.of(claim1, claim2, claim3));

      Map<String, String> expected = new HashMap<>();
      expected.put("feeCode1", "categoryOfLaw1");
      expected.put("feeCode2", "categoryOfLaw2");
      expected.put("feeCode3", null);

      assertThat(actual).isEqualTo(expected);
    }
  }
}
