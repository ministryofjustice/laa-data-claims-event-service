package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.JsonSchemaValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

@ExtendWith(MockitoExtension.class)
class ClaimValidationServiceTest {

  @Mock private CategoryOfLawValidationService categoryOfLawValidationService;

  @Mock private DuplicateClaimValidationService duplicateClaimValidationService;

  @Mock private FeeCalculationService feeCalculationService;

  @Mock private SubmissionValidationContext submissionValidationContext;

  @Mock private JsonSchemaValidator jsonSchemaValidator;

  @InjectMocks private ClaimValidationService claimValidationService;

  @Nested
  @DisplayName("validateClaims")
  class ValidateClaimsTests {

    @Test
    @DisplayName("Validates category of law, duplicates and fee calculation for all claims")
    void validateCategoryOfLawAndDuplicatesAndFeeCalculation() {
      ClaimResponse claim1 = new ClaimResponse().id("claim1").feeCode("feeCode1");
      ClaimResponse claim2 = new ClaimResponse().id("claim2").feeCode("feeCode2");
      List<ClaimResponse> claims = List.of(claim1, claim2);
      List<String> providerCategoriesOfLaw = List.of("categoryOfLaw1");
      Map<String, CategoryOfLawResult> categoryOfLawLookup = Collections.emptyMap();

      when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
          .thenReturn(categoryOfLawLookup);

      claimValidationService.validateClaims(claims, providerCategoriesOfLaw);

      verify(categoryOfLawValidationService, times(1))
          .validateCategoryOfLaw(claim1, categoryOfLawLookup, providerCategoriesOfLaw);
      verify(categoryOfLawValidationService, times(1))
          .validateCategoryOfLaw(claim2, categoryOfLawLookup, providerCategoriesOfLaw);

      verify(duplicateClaimValidationService, times(1)).validateDuplicateClaims(claim1);
      verify(duplicateClaimValidationService, times(1)).validateDuplicateClaims(claim2);

      verify(feeCalculationService, times(1)).validateFeeCalculation(claim1);
      verify(feeCalculationService, times(1)).validateFeeCalculation(claim2);
    }

    @Test
    void validatePastDates() {
      ClaimFields claim1 =
          new ClaimFields().id("claim1").feeCode("feeCode1").caseStartDate("34/13/2003").transferDate("02/12/2090").caseConcludedDate("01/01/2090").representationOrderDate("01/01/2090").clientDateOfBirth("31/12/2099").client2DateOfBirth("31/12/2099");
      ClaimFields claim2 =
          new ClaimFields().id("claim2").feeCode("feeCode2").caseStartDate("03/01/1993").transferDate("02/12/1990").caseConcludedDate("01/01/1993").representationOrderDate("30/03/2016").clientDateOfBirth("31/12/1899").client2DateOfBirth("31/12/1899");
      List<ClaimFields> claims = List.of(claim1, claim2);

      List<String> providerCategoriesOfLaw = List.of("categoryOfLaw1");
      Map<String, CategoryOfLawResult> categoryOfLawLookup = Collections.emptyMap();

      when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
          .thenReturn(categoryOfLawLookup);

      claimValidationService.validateClaims(claims, providerCategoriesOfLaw);

      // Then
      verify(submissionValidationContext, times(1))
          .addClaimError("claim1", "Invalid date value provided for Case Start Date: 34/13/2003");
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claim1",
              "Invalid date value for Transfer Date (Must be between 01/01/1995 and today): 02/12/2090");
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claim1",
              "Invalid date value for Case Concluded Date (Must be between 01/01/1995 and today): 01/01/2090");
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claim1",
              "Invalid date value for Representation Order Date (Must be between 01/04/2016 and today): 01/01/2090");
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claim1",
              "Invalid date value for Client Date of Birth (Must be between 01/01/1900 and today): 31/12/2099");
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claim1",
              "Invalid date value for Client2 Date of Birth (Must be between 01/01/1900 and today): 31/12/2099");

      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claim2",
              "Invalid date value for Case Start Date (Must be between 01/01/1995 and today): 03/01/1993");
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claim2",
              "Invalid date value for Transfer Date (Must be between 01/01/1995 and today): 02/12/1990");
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claim2",
              "Invalid date value for Case Concluded Date (Must be between 01/01/1995 and today): 01/01/1993");
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claim2",
              "Invalid date value for Representation Order Date (Must be between 01/04/2016 and today): 30/03/2016");
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claim2",
              "Invalid date value for Client Date of Birth (Must be between 01/01/1900 and today): 31/12/1899");
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claim2",
              "Invalid date value for Client Date of Birth (Must be between 01/01/1900 and today): 31/12/1899");
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claim2",
              "Invalid date value for Client2 Date of Birth (Must be between 01/01/1900 and today): 31/12/1899");
    }
  }
}
