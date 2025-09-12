package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.MandatoryFieldsRegistry;
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

  @Mock private MandatoryFieldsRegistry mandatoryFieldsRegistry;

  @Nested
  @DisplayName("validateClaims")
  class ValidateClaimsTests {

    @BeforeEach
    void setup() {
      // Define the map for the test
      Map<String, List<String>> civilMandatoryFields =
          Map.of(
              "CIVIL", List.of("uniqueFileNumber"),
              "CRIME", List.of("uniqueFileNumber"),
              "MEDIATION", List.of("uniqueFileNumber"));

      when(mandatoryFieldsRegistry.getMandatoryFieldsByAreaOfLaw())
          .thenReturn(civilMandatoryFields);
    }

    @Test
    @DisplayName("Validates category of law, duplicates and fee calculation for all claims")
    void validateCategoryOfLawAndDuplicatesAndFeeCalculation() {
      ClaimResponse claim1 =
          new ClaimResponse().id("claim1").feeCode("feeCode1").matterTypeCode("ab:cd");
      ClaimResponse claim2 =
          new ClaimResponse().id("claim2").feeCode("feeCode2").matterTypeCode("1:2");
      List<ClaimResponse> claims = List.of(claim1, claim2);
      List<String> providerCategoriesOfLaw = List.of("categoryOfLaw1");
      Map<String, CategoryOfLawResult> categoryOfLawLookup = Collections.emptyMap();

      when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
          .thenReturn(categoryOfLawLookup);

      claimValidationService.validateClaims(claims, providerCategoriesOfLaw, "CIVIL");

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
      ClaimResponse claim1 =
          new ClaimResponse()
              .id("claim1")
              .feeCode("feeCode1")
              .caseStartDate("2003-13-34")
              .transferDate("2090-12-02")
              .caseConcludedDate("2090-01-01")
              .representationOrderDate("2090-01-01")
              .clientDateOfBirth("2099-12-31")
              .client2DateOfBirth("2099-12-31")
              .matterTypeCode("a:b");
      ClaimResponse claim2 =
          new ClaimResponse()
              .id("claim2")
              .feeCode("feeCode2")
              .caseStartDate("1993-01-03")
              .transferDate("1990-12-02")
              .caseConcludedDate("1993-01-01")
              .representationOrderDate("2016-03-30")
              .clientDateOfBirth("1899-12-31")
              .client2DateOfBirth("1899-12-31")
              .matterTypeCode("1:2");
      List<ClaimResponse> claims = List.of(claim1, claim2);

      List<String> providerCategoriesOfLaw = List.of("categoryOfLaw1");
      Map<String, CategoryOfLawResult> categoryOfLawLookup = Collections.emptyMap();

      when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
          .thenReturn(categoryOfLawLookup);

      claimValidationService.validateClaims(claims, providerCategoriesOfLaw, "CIVIL");

      // Then
      verify(submissionValidationContext, times(1))
          .addClaimError("claim1", "Invalid date value provided for Case Start Date: 2003-13-34");
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claim1",
              "Invalid date value for Transfer Date (Must be between 1995-01-01 and today): 2090-12-02");
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claim1",
              "Invalid date value for Case Concluded Date (Must be between 1995-01-01 and today): 2090-01-01");
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claim1",
              "Invalid date value for Representation Order Date (Must be between 2016-04-01 and today): 2090-01-01");
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claim1",
              "Invalid date value for Client Date of Birth (Must be between 1900-01-01 and today): 2099-12-31");
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claim1",
              "Invalid date value for Client2 Date of Birth (Must be between 1900-01-01 and today): 2099-12-31");

      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claim2",
              "Invalid date value for Case Start Date (Must be between 1995-01-01 and today): 1993-01-03");
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claim2",
              "Invalid date value for Transfer Date (Must be between 1995-01-01 and today): 1990-12-02");
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claim2",
              "Invalid date value for Case Concluded Date (Must be between 1995-01-01 and today): 1993-01-01");
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claim2",
              "Invalid date value for Representation Order Date (Must be between 2016-04-01 and today): 2016-03-30");
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claim2",
              "Invalid date value for Client Date of Birth (Must be between 1900-01-01 and today): 1899-12-31");
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claim2",
              "Invalid date value for Client Date of Birth (Must be between 1900-01-01 and today): 1899-12-31");
      verify(submissionValidationContext, times(1))
          .addClaimError(
              "claim2",
              "Invalid date value for Client2 Date of Birth (Must be between 1900-01-01 and today): 1899-12-31");
    }

    @Test
    void checkConditionallyMandatoryFields() {
      ClaimResponse claim1 =
          new ClaimResponse()
              .id("claim1")
              .feeCode("feeCode1")
              .uniqueFileNumber("uniqueFileNumber1")
              .matterTypeCode("AB:CD");
      ClaimResponse claim2 =
          new ClaimResponse().id("claim2").feeCode("feeCode2").matterTypeCode("123:456");
      List<ClaimResponse> claims = List.of(claim1, claim2);

      List<String> providerCategoriesOfLaw = List.of("categoryOfLaw1");
      Map<String, CategoryOfLawResult> categoryOfLawLookup = Collections.emptyMap();

      when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
          .thenReturn(categoryOfLawLookup);

      claimValidationService.validateClaims(claims, providerCategoriesOfLaw, "CIVIL");

      // Then
      verify(submissionValidationContext, times(0))
          .addClaimError("claim1", "uniqueFileNumber is required for area of law: CIVIL");
      verify(submissionValidationContext, times(1))
          .addClaimError("claim2", "uniqueFileNumber is required for area of law: CIVIL");
    }

    @ParameterizedTest(
        name = "{index} => claimId={0}, matterType={1}, areaOfLaw={2}, expectedError={3}")
    @CsvSource({
      "claim1, BadMatterType, CIVIL, true",
      "claim2, ab12:bc24, CIVIL, false",
      "claim3, AB-CD, CIVIL, false",
      "claim4, ABCD:EFGH, MEDIATION, false",
      "claim5, AB12:CD34, MEDIATION, true",
      "claim3, AB-CD, MEDIATION, true",
    })
    void checkMatterType(
        String claimId, String matterTypeCode, String areaOfLaw, boolean expectError) {
      ClaimResponse claim =
          new ClaimResponse().id(claimId).feeCode("feeCode1").matterTypeCode(matterTypeCode);

      List<ClaimResponse> claims = List.of(claim);
      List<String> providerCategoriesOfLaw = List.of("categoryOfLaw1");
      Map<String, CategoryOfLawResult> categoryOfLawLookup = Collections.emptyMap();

      when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
          .thenReturn(categoryOfLawLookup);

      // Run validation
      claimValidationService.validateClaims(claims, providerCategoriesOfLaw, areaOfLaw);

      if (expectError) {
        String expectedMessage =
            String.format(
                "Invalid Matter Type [%s] for Area of Law: %s", matterTypeCode, areaOfLaw);
        verify(submissionValidationContext).addClaimError(claimId, expectedMessage);
      } else {
        verify(submissionValidationContext, never())
            .addClaimError(
                eq(claimId),
                (String)
                    argThat(msg -> msg != null && ((String) msg).contains("Invalid Matter Type")));
      }

      // Reset mocks for next iteration
      reset(submissionValidationContext);
    }
  }
}
