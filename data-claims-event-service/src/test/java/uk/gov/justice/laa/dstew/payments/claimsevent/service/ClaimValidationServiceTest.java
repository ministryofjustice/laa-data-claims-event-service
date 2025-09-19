package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResultSet;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ProviderDetailsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.MandatoryFieldsRegistry;
import uk.gov.justice.laa.dstew.payments.claimsevent.util.ClaimEffectiveDateUtil;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationReport;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.JsonSchemaValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.provider.model.FirmOfficeContractAndScheduleDetails;
import uk.gov.justice.laa.provider.model.FirmOfficeContractAndScheduleLine;
import uk.gov.justice.laa.provider.model.ProviderFirmOfficeContractAndScheduleDto;

@ExtendWith(MockitoExtension.class)
class ClaimValidationServiceTest {

  @Mock private DataClaimsRestClient dataClaimsRestClient;

  @Mock private CategoryOfLawValidationService categoryOfLawValidationService;

  @Mock private DuplicateClaimValidationService duplicateClaimValidationService;

  @Mock private FeeCalculationService feeCalculationService;

  @Mock private ProviderDetailsRestClient providerDetailsRestClient;

  @Mock private JsonSchemaValidator jsonSchemaValidator;

  @Mock private MandatoryFieldsRegistry mandatoryFieldsRegistry;

  @Mock private ClaimEffectiveDateUtil claimEffectiveDateUtil;

  @InjectMocks private ClaimValidationService claimValidationService;

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

      lenient()
          .when(mandatoryFieldsRegistry.getMandatoryFieldsByAreaOfLaw())
          .thenReturn(civilMandatoryFields);
    }

    @Test
    @DisplayName("Validates category of law, duplicates and fee calculation for all claims")
    void validateCategoryOfLawAndDuplicatesAndFeeCalculation() {
      ClaimResponse claim1 =
          new ClaimResponse()
              .id("claim1")
              .feeCode("feeCode1")
              .caseStartDate("2025-08-14")
              .status(ClaimStatus.READY_TO_PROCESS)
              .matterTypeCode("ab:cd");
      ClaimResponse claim2 =
          new ClaimResponse()
              .id("claim2")
              .feeCode("feeCode2")
              .caseStartDate("2025-05-25")
              .status(ClaimStatus.READY_TO_PROCESS)
              .matterTypeCode("1:2");
      List<ClaimResponse> claims = List.of(claim1, claim2);
      List<String> providerCategoriesOfLaw = List.of("categoryOfLaw1");
      Map<String, CategoryOfLawResult> categoryOfLawLookup = Collections.emptyMap();
      ProviderFirmOfficeContractAndScheduleDto data =
          new ProviderFirmOfficeContractAndScheduleDto()
              .addSchedulesItem(
                  new FirmOfficeContractAndScheduleDetails()
                      .addScheduleLinesItem(
                          new FirmOfficeContractAndScheduleLine().categoryOfLaw("categoryOfLaw1")));

      SubmissionResponse submissionResponse =
          new SubmissionResponse()
              .submissionId(new UUID(1, 1))
              .areaOfLaw("CIVIL")
              .officeAccountNumber("officeAccountNumber");

      ClaimResultSet claimResultSet = new ClaimResultSet();
      claimResultSet.content(claims);

      when(dataClaimsRestClient.getClaims(
              submissionResponse.getOfficeAccountNumber(),
              submissionResponse.getSubmissionId().toString(),
              null,
              null,
              null,
              null,
              null,
              null))
          .thenReturn(ResponseEntity.ok(claimResultSet));

      when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
          .thenReturn(categoryOfLawLookup);

      when(providerDetailsRestClient.getProviderFirmSchedules(
              eq("officeAccountNumber"), eq("CIVIL"), any(LocalDate.class)))
          .thenReturn(Mono.just(data));

      // Two claims make two separate calls to claimEffectiveDateUtil
      when(claimEffectiveDateUtil.getEffectiveDate(any()))
          .thenReturn(LocalDate.of(2025, 8, 14))
          .thenReturn(LocalDate.of(2025, 5, 25));

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(
          List.of(
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim2.getId())));

      claimValidationService.validateClaims(submissionResponse, context);

      verify(providerDetailsRestClient, times(1))
          .getProviderFirmSchedules("officeAccountNumber", "CIVIL", LocalDate.parse("2025-08-14"));
      verify(providerDetailsRestClient, times(1))
          .getProviderFirmSchedules("officeAccountNumber", "CIVIL", LocalDate.parse("2025-05-25"));

      verify(categoryOfLawValidationService, times(1))
          .validateCategoryOfLaw(claim1, categoryOfLawLookup, providerCategoriesOfLaw, context);
      verify(categoryOfLawValidationService, times(1))
          .validateCategoryOfLaw(claim2, categoryOfLawLookup, providerCategoriesOfLaw, context);

      verify(duplicateClaimValidationService, times(1))
          .validateDuplicateClaims(claim1, claims, "CIVIL", "officeAccountNumber", context);
      verify(duplicateClaimValidationService, times(1))
          .validateDuplicateClaims(claim2, claims, "CIVIL", "officeAccountNumber", context);

      verify(feeCalculationService, times(1)).validateFeeCalculation(claim1, context);
      verify(feeCalculationService, times(1)).validateFeeCalculation(claim2, context);
    }

    @Test
    void validatePastDates() {
      ClaimResponse claim1 =
          new ClaimResponse()
              .id("claim1")
              .status(ClaimStatus.READY_TO_PROCESS)
              .feeCode("feeCode1")
              .caseStartDate("2025-08-14")
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
              .status(ClaimStatus.READY_TO_PROCESS)
              .feeCode("feeCode2")
              .caseStartDate("2025-05-25")
              .caseStartDate("1993-01-03")
              .transferDate("1990-12-02")
              .caseConcludedDate("1993-01-01")
              .representationOrderDate("2016-03-30")
              .clientDateOfBirth("1899-12-31")
              .client2DateOfBirth("1899-12-31")
              .matterTypeCode("1:2");
      List<ClaimResponse> claims = List.of(claim1, claim2);

      Map<String, CategoryOfLawResult> categoryOfLawLookup = Collections.emptyMap();

      when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
          .thenReturn(categoryOfLawLookup);

      ProviderFirmOfficeContractAndScheduleDto data =
          new ProviderFirmOfficeContractAndScheduleDto()
              .addSchedulesItem(
                  new FirmOfficeContractAndScheduleDetails()
                      .addScheduleLinesItem(
                          new FirmOfficeContractAndScheduleLine().categoryOfLaw("categoryOfLaw1")));
      when(providerDetailsRestClient.getProviderFirmSchedules(
              eq("officeAccountNumber"), eq("CIVIL"), any(LocalDate.class)))
          .thenReturn(Mono.just(data));

      when(claimEffectiveDateUtil.getEffectiveDate(any()))
          .thenReturn(LocalDate.of(2025, 8, 14));

      SubmissionResponse submissionResponse =
          new SubmissionResponse()
              .submissionId(new UUID(1, 1))
              .areaOfLaw("CIVIL")
              .officeAccountNumber("officeAccountNumber");

      ClaimResultSet claimResultSet = new ClaimResultSet();
      claimResultSet.content(claims);

      when(dataClaimsRestClient.getClaims(
              submissionResponse.getOfficeAccountNumber(),
              submissionResponse.getSubmissionId().toString(),
              null,
              null,
              null,
              null,
              null,
              null))
          .thenReturn(ResponseEntity.ok(claimResultSet));

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(
          List.of(
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim2.getId())));

      claimValidationService.validateClaims(submissionResponse, context);

      // Then
      assertThat(
              getClaimMessages(context, "claim1").stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value provided for Case Start Date: 2003-13-34")))
          .isTrue();
      assertThat(
              getClaimMessages(context, "claim1").stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value for Transfer Date (Must be between 1995-01-01 "
                                      + "and today): "
                                      + "2090-12-02")))
          .isTrue();
      assertThat(
              getClaimMessages(context, "claim1").stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value for Case Concluded Date (Must be between "
                                      + "1995-01-01 and "
                                      + "today): 2090-01-01")))
          .isTrue();
      assertThat(
              getClaimMessages(context, "claim1").stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value for Representation Order Date (Must be between "
                                      + "2016-04-01 "
                                      + "and today): 2090-01-01")))
          .isTrue();
      assertThat(
              getClaimMessages(context, "claim1").stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value for Client Date of Birth (Must be between "
                                      + "1900-01-01 and "
                                      + "today): 2099-12-31")))
          .isTrue();
      assertThat(
              getClaimMessages(context, "claim1").stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value for Client2 Date of Birth (Must be between "
                                      + "1900-01-01 and "
                                      + "today): 2099-12-31")))
          .isTrue();
      assertThat(
              getClaimMessages(context, "claim2").stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value for Case Start Date (Must be between 1995-01-01"
                                      + " and today):"
                                      + " 1993-01-03")))
          .isTrue();
      assertThat(
              getClaimMessages(context, "claim2").stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value for Transfer Date (Must be between 1995-01-01 "
                                      + "and today): "
                                      + "1990-12-02")))
          .isTrue();
      assertThat(
              getClaimMessages(context, "claim2").stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value for Case Concluded Date (Must be between "
                                      + "1995-01-01 and "
                                      + "today): 1993-01-01")))
          .isTrue();
      assertThat(
              getClaimMessages(context, "claim2").stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value for Representation Order Date (Must be between "
                                      + "2016-04-01 "
                                      + "and today): 2016-03-30")))
          .isTrue();
      assertThat(
              getClaimMessages(context, "claim2").stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value for Client Date of Birth (Must be between "
                                      + "1900-01-01 and "
                                      + "today): 1899-12-31")))
          .isTrue();
      assertThat(
              getClaimMessages(context, "claim2").stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value for Client Date of Birth (Must be between "
                                      + "1900-01-01 and "
                                      + "today): 1899-12-31")))
          .isTrue();
      assertThat(
              getClaimMessages(context, "claim2").stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value for Client2 Date of Birth (Must be between "
                                      + "1900-01-01 and "
                                      + "today): 1899-12-31")))
          .isTrue();
    }

    @Test
    void checkConditionallyMandatoryFields() {
      ClaimResponse claim1 =
          new ClaimResponse()
              .id("claim1")
              .status(ClaimStatus.READY_TO_PROCESS)
              .feeCode("feeCode1")
              .caseStartDate("2025-08-14")
              .uniqueFileNumber("010101/123")
              .matterTypeCode("AB:CD")
              .stageReachedCode("AA");
      ClaimResponse claim2 =
          new ClaimResponse()
              .id("claim2")
              .uniqueFileNumber("010101/123")
              .status(ClaimStatus.READY_TO_PROCESS)
              .feeCode("feeCode2")
              .caseStartDate("2025-05-25")
              .matterTypeCode("123:456");
      List<ClaimResponse> claims = List.of(claim1, claim2);

      Map<String, CategoryOfLawResult> categoryOfLawLookup = Collections.emptyMap();
      when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
          .thenReturn(categoryOfLawLookup);

      ProviderFirmOfficeContractAndScheduleDto data =
          new ProviderFirmOfficeContractAndScheduleDto()
              .addSchedulesItem(
                  new FirmOfficeContractAndScheduleDetails()
                      .addScheduleLinesItem(
                          new FirmOfficeContractAndScheduleLine().categoryOfLaw("categoryOfLaw1")));
      when(providerDetailsRestClient.getProviderFirmSchedules(any(), any(), any()))
          .thenReturn(Mono.just(data));

      when(claimEffectiveDateUtil.getEffectiveDate(any()))
          .thenReturn(LocalDate.of(2025, 8, 14));

      SubmissionResponse submissionResponse1 =
          new SubmissionResponse()
              .submissionId(new UUID(1, 1))
              .areaOfLaw("CIVIL")
              .officeAccountNumber("officeAccountNumber");

      ClaimResultSet claimResultSet = new ClaimResultSet();
      claimResultSet.content(claims);

      when(dataClaimsRestClient.getClaims(
              submissionResponse1.getOfficeAccountNumber(),
              submissionResponse1.getSubmissionId().toString(),
              null,
              null,
              null,
              null,
              null,
              null))
          .thenReturn(ResponseEntity.ok(claimResultSet));

      SubmissionValidationContext context1 = new SubmissionValidationContext();
      context1.addClaimReports(
          List.of(
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim2.getId())));

      claimValidationService.validateClaims(submissionResponse1, context1);

      assertThat(getClaimMessages(context1, "claim1").isEmpty()).isTrue();
      assertThat(getClaimMessages(context1, "claim2").isEmpty()).isTrue();

      SubmissionResponse submissionResponse2 =
          new SubmissionResponse()
              .submissionId(new UUID(1, 1))
              .areaOfLaw("CRIME")
              .officeAccountNumber("officeAccountNumber");

      when(dataClaimsRestClient.getClaims(
              submissionResponse2.getOfficeAccountNumber(),
              submissionResponse2.getSubmissionId().toString(),
              null,
              null,
              null,
              null,
              null,
              null))
          .thenReturn(ResponseEntity.ok(claimResultSet));

      SubmissionValidationContext context2 = new SubmissionValidationContext();
      context1.addClaimReports(
          List.of(
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim2.getId())));

      claimValidationService.validateClaims(submissionResponse2, context2);

      assertThat(getClaimMessages(context2, "claim1").getFirst().getDisplayMessage())
          .isEqualTo(
              "stage_reached_code (CRIME): does not match the regex pattern ^[A-Z]{4}$ (provided "
                  + "value: AA)");
      assertThat(getClaimMessages(context2, "claim2").getFirst().getDisplayMessage())
          .isEqualTo("stageReachedCode is required for area of law: CRIME");
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
          new ClaimResponse()
              .id(claimId)
              .feeCode("feeCode1")
              .caseStartDate("2025-08-14")
              .status(ClaimStatus.READY_TO_PROCESS)
              .uniqueFileNumber("010101/123")
              .matterTypeCode(matterTypeCode);

      List<ClaimResponse> claims = List.of(claim);
      Map<String, CategoryOfLawResult> categoryOfLawLookup = Collections.emptyMap();

      when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
          .thenReturn(categoryOfLawLookup);

      when(claimEffectiveDateUtil.getEffectiveDate(any()))
          .thenReturn(LocalDate.of(2025, 8, 14));

      SubmissionResponse submissionResponse =
          new SubmissionResponse()
              .submissionId(new UUID(1, 1))
              .areaOfLaw(areaOfLaw)
              .officeAccountNumber("officeAccountNumber");

      ClaimResultSet claimResultSet = new ClaimResultSet();
      claimResultSet.content(claims);

      when(dataClaimsRestClient.getClaims(
              submissionResponse.getOfficeAccountNumber(),
              submissionResponse.getSubmissionId().toString(),
              null,
              null,
              null,
              null,
              null,
              null))
          .thenReturn(ResponseEntity.ok(claimResultSet));

      ProviderFirmOfficeContractAndScheduleDto data =
          new ProviderFirmOfficeContractAndScheduleDto()
              .addSchedulesItem(
                  new FirmOfficeContractAndScheduleDetails()
                      .addScheduleLinesItem(
                          new FirmOfficeContractAndScheduleLine().categoryOfLaw("categoryOfLaw1")));
      when(providerDetailsRestClient.getProviderFirmSchedules(any(), any(), any()))
          .thenReturn(Mono.just(data));

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(List.of(new ClaimValidationReport(claim.getId())));

      // Run validation
      claimValidationService.validateClaims(submissionResponse, context);

      if (expectError) {
        String expectedMessage =
            String.format(
                "matter_type_code (%s): does not match the regex pattern %s (provided value: %s)",
                areaOfLaw, regex, matterTypeCode);
        assertThat(getClaimMessages(context, claimId).getFirst().getTechnicalMessage())
            .isEqualTo(expectedMessage);
      } else {
        assertThat(getClaimMessages(context, claimId).isEmpty()).isTrue();
      }
    }

    @ParameterizedTest(
        name =
            "{index} => claimId={0}, stageReachedCode={1}, areaOfLaw={2}, regex={3}, "
                + "expectError={4}")
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
          new ClaimResponse()
              .id(claimId)
              .feeCode("feeCode1")
              .caseStartDate("2025-08-14")
              .status(ClaimStatus.READY_TO_PROCESS)
              .uniqueFileNumber("010101/123")
              .stageReachedCode(stageReachedCode);

      List<ClaimResponse> claims = List.of(claim);
      Map<String, CategoryOfLawResult> categoryOfLawLookup = Collections.emptyMap();

      when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
          .thenReturn(categoryOfLawLookup);

      when(claimEffectiveDateUtil.getEffectiveDate(any()))
          .thenReturn(LocalDate.of(2025, 8, 14));

      SubmissionResponse submissionResponse =
          new SubmissionResponse()
              .submissionId(new UUID(1, 1))
              .areaOfLaw(areaOfLaw)
              .officeAccountNumber("officeAccountNumber");

      ClaimResultSet claimResultSet = new ClaimResultSet();
      claimResultSet.content(claims);

      when(dataClaimsRestClient.getClaims(
              submissionResponse.getOfficeAccountNumber(),
              submissionResponse.getSubmissionId().toString(),
              null,
              null,
              null,
              null,
              null,
              null))
          .thenReturn(ResponseEntity.ok(claimResultSet));

      ProviderFirmOfficeContractAndScheduleDto data =
          new ProviderFirmOfficeContractAndScheduleDto()
              .addSchedulesItem(
                  new FirmOfficeContractAndScheduleDetails()
                      .addScheduleLinesItem(
                          new FirmOfficeContractAndScheduleLine().categoryOfLaw("categoryOfLaw1")));
      when(providerDetailsRestClient.getProviderFirmSchedules(
              eq("officeAccountNumber"), eq(areaOfLaw), any(LocalDate.class)))
          .thenReturn(Mono.just(data));

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(List.of(new ClaimValidationReport(claim.getId())));

      // Run validation
      claimValidationService.validateClaims(submissionResponse, context);

      if (expectError) {
        String expectedMessage =
            String.format(
                "stage_reached_code (%s): does not match the regex pattern %s (provided value: %s)",
                areaOfLaw, regex, stageReachedCode);
        assertThat(getClaimMessages(context, claimId).getFirst().getTechnicalMessage())
            .isEqualTo(expectedMessage);
      } else {
        assertThat(getClaimMessages(context, claimId).isEmpty()).isTrue();
      }
    }

    @ParameterizedTest(
        name =
            "{index} => claimId={0}, disbursementVatAmount={1}, areaOfLaw={2}, maxAllowed={3}, "
                + "expectError={4}")
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
              .caseStartDate("2025-08-14")
              .uniqueFileNumber("010101/123")
              .status(ClaimStatus.READY_TO_PROCESS)
              .disbursementsVatAmount(disbursementsVatAmount);
      if (areaOfLaw.equals("CRIME")) {
        claim.setStageReachedCode("ABCD");
      }

      List<ClaimResponse> claims = List.of(claim);
      Map<String, CategoryOfLawResult> categoryOfLawLookup = Collections.emptyMap();

      when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
          .thenReturn(categoryOfLawLookup);

      when(claimEffectiveDateUtil.getEffectiveDate(any()))
          .thenReturn(LocalDate.of(2025, 8, 14));

      SubmissionResponse submissionResponse =
          new SubmissionResponse()
              .submissionId(new UUID(1, 1))
              .areaOfLaw(areaOfLaw)
              .officeAccountNumber("officeAccountNumber");

      ClaimResultSet claimResultSet = new ClaimResultSet();
      claimResultSet.content(claims);

      when(dataClaimsRestClient.getClaims(
              submissionResponse.getOfficeAccountNumber(),
              submissionResponse.getSubmissionId().toString(),
              null,
              null,
              null,
              null,
              null,
              null))
          .thenReturn(ResponseEntity.ok(claimResultSet));

      ProviderFirmOfficeContractAndScheduleDto data =
          new ProviderFirmOfficeContractAndScheduleDto()
              .addSchedulesItem(
                  new FirmOfficeContractAndScheduleDetails()
                      .addScheduleLinesItem(
                          new FirmOfficeContractAndScheduleLine().categoryOfLaw("categoryOfLaw1")));
      when(providerDetailsRestClient.getProviderFirmSchedules(
              eq("officeAccountNumber"), eq(areaOfLaw), any(LocalDate.class)))
          .thenReturn(Mono.just(data));

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(List.of(new ClaimValidationReport(claim.getId())));

      // Run validation
      claimValidationService.validateClaims(submissionResponse, context);

      if (expectError) {
        String expectedMessage =
            String.format(
                "disbursementsVatAmount (%s): must have a maximum value of %s (provided value: %s)",
                areaOfLaw, maxAllowed, disbursementsVatAmount);
        assertThat(getClaimMessages(context, claimId).getFirst().getDisplayMessage())
            .isEqualTo(expectedMessage);
      } else {
        for (var claimReport : context.getClaimReports()) {
          assertThat(claimReport.hasErrors()).isFalse();
        }
      }
    }
  }

  private static List<ValidationMessagePatch> getClaimMessages(
      SubmissionValidationContext context, String claim1) {
    return context.getClaimReport(claim1).get().getMessages();
  }
}
