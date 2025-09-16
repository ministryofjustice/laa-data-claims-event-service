package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
              "CRIME", List.of("stageReachedCode"),
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
              .matterTypeCode("AB:CD")
              .stageReachedCode("AA");
      ClaimResponse claim2 =
          new ClaimResponse().id("claim2").feeCode("feeCode2").matterTypeCode("123:456");
      List<ClaimResponse> claims = List.of(claim1, claim2);

      List<String> providerCategoriesOfLaw = List.of("categoryOfLaw1");
      Map<String, CategoryOfLawResult> categoryOfLawLookup = Collections.emptyMap();

      when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
          .thenReturn(categoryOfLawLookup);

      claimValidationService.validateClaims(claims, providerCategoriesOfLaw, "CIVIL");
      claimValidationService.validateClaims(claims, providerCategoriesOfLaw, "CRIME");

      // Then
      verify(submissionValidationContext, times(0))
          .addClaimError("claim1", "uniqueFileNumber is required for area of law: CIVIL");
      verify(submissionValidationContext, times(1))
          .addClaimError("claim2", "uniqueFileNumber is required for area of law: CIVIL");
      verify(submissionValidationContext, times(0))
          .addClaimError("claim1", "stageReachedCode is required for area of law: CRIME");
      verify(submissionValidationContext, times(1))
          .addClaimError("claim2", "stageReachedCode is required for area of law: CRIME");
    }

    @ParameterizedTest(
        name = "{index} => claimId={0}, matterType={1}, areaOfLaw={2}, regex={3}, expectError={4}")
    @CsvSource({
      "claim1, BadMatterType, CIVIL, '^[a-zA-Z0-9]{1,4}[-:][a-zA-Z0-9]{1,4}$', true",
      "claim2, ab12:bc24, CIVIL, '^[a-zA-Z0-9]{1,4}[-:][a-zA-Z0-9]{1,4}$', false",
      "claim3, AB-CD, CIVIL, '^[a-zA-Z0-9]{1,4}[-:][a-zA-Z0-9]{1,4}$', false",
      "claim4, ABCD:EFGH, MEDIATION, '^[A-Z]{4}[-:][A-Z]{4}$', false",
      "claim5, AB12:CD34, MEDIATION, '^[A-Z]{4}[-:][A-Z]{4}$', true",
      "claim6, AB-CD, MEDIATION, '^[A-Z]{4}[-:][A-Z]{4}$', true",
    })
    void checkMatterType(
        String claimId,
        String matterTypeCode,
        String areaOfLaw,
        String regex,
        boolean expectError) {
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
                "matter_type_code (%s): does not match the regex pattern %s (provided value: %s)",
                areaOfLaw, regex, matterTypeCode);
        verify(submissionValidationContext).addClaimError(claimId, expectedMessage);
      } else {
        verify(submissionValidationContext, never())
            .addClaimError(
                eq(claimId),
                (String)
                    argThat(msg -> msg != null && ((String) msg).contains("matter_type_code")));
      }

      // Reset mocks for next iteration
      reset(submissionValidationContext);
    }

    @ParameterizedTest(
        name =
            "{index} => claimId={0}, stageReachedCode={1}, areaOfLaw={2}, regex={3}, expectError={4}")
    @CsvSource({
      "claim1, AABB, CIVIL, '^[a-zA-Z0-9]{2}$', true",
      "claim2, AZ, CIVIL, '^[a-zA-Z0-9]{2}$', false",
      "claim3, C9, CIVIL, '^[a-zA-Z0-9]{2}$', false",
      "claim4, A!, CIVIL, '^[a-zA-Z0-9]{2}$', true",
      "claim5, ABCD, CRIME, '^[A-Z]{4}$', false",
      "claim6, A1, CRIME, '^[A-Z]{4}$', true",
      "claim7, A-CD, CRIME, '^[A-Z]{4}$', true",
    })
    void checkStageReachedCode(
        String claimId,
        String stageReachedCode,
        String areaOfLaw,
        String regex,
        boolean expectError) {
      ClaimResponse claim =
          new ClaimResponse().id(claimId).feeCode("feeCode1").stageReachedCode(stageReachedCode);

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
                "stage_reached_code (%s): does not match the regex pattern %s (provided value: %s)",
                areaOfLaw, regex, stageReachedCode);
        verify(submissionValidationContext).addClaimError(claimId, expectedMessage);
      } else {
        verify(submissionValidationContext, never())
            .addClaimError(
                eq(claimId),
                (String)
                    argThat(msg -> msg != null && ((String) msg).contains("stage_reached_code")));
      }

      // Reset mocks for next iteration
      reset(submissionValidationContext);
    }

    @ParameterizedTest(
        name =
            "{index} => claimId={0}, disbursementVatAmount={1}, areaOfLaw={2}, maxAllowed={3}, expectError={4}")
    @CsvSource({
      "claim1, 99999.99, CIVIL, 99999.99, false",
      "claim2, 999999.99, CRIME, 999999.99, false",
      "claim3, 999999999.99, MEDIATION, 999999999.99, false",
      "claim4, 100000.0, CIVIL, 99999.99, true",
      "claim5, 1000000.0, CRIME, 999999.99, true",
      "claim6, 1000000000.0, MEDIATION, 999999999.99, true",
    })
    void checkDisbursementsVatAmount(
        String claimId,
        BigDecimal disbursementsVatAmount,
        String areaOfLaw,
        BigDecimal maxAllowed,
        boolean expectError) {
      ClaimResponse claim =
          new ClaimResponse()
              .id(claimId)
              .feeCode("feeCode1")
              .disbursementsVatAmount(disbursementsVatAmount);

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
                "disbursementsVatAmount (%s): must have a maximum value of %s (provided value: %s)",
                areaOfLaw, maxAllowed, disbursementsVatAmount);
        verify(submissionValidationContext).addClaimError(claimId, expectedMessage);
      } else {
        verify(submissionValidationContext, never())
            .addClaimError(
                eq(claimId),
                (String)
                    argThat(
                        msg -> msg != null && ((String) msg).contains("disbursements_vat_amount")));
      }

      // Reset mocks for next iteration
      reset(submissionValidationContext);
    }
  }
}
