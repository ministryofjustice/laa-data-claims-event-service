package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static java.util.Collections.singletonList;
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
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionClaim;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ProviderDetailsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.MandatoryFieldsRegistry;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.strategy.DuplicateClaimCivilValidationServiceStrategy;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.strategy.DuplicateClaimCrimeValidationServiceStrategy;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.strategy.DuplicateClaimValidationStrategy;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.strategy.StrategyTypes;
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

  @Mock private DuplicateClaimCrimeValidationServiceStrategy duplicateClaimCrimeValidationService;

  @Mock private FeeCalculationService feeCalculationService;

  @Mock private ProviderDetailsRestClient providerDetailsRestClient;

  @Mock private JsonSchemaValidator jsonSchemaValidator;

  @Mock private MandatoryFieldsRegistry mandatoryFieldsRegistry;

  @Mock private ClaimEffectiveDateUtil claimEffectiveDateUtil;

  @Mock
  private DuplicateClaimCivilValidationServiceStrategy
      mockDuplicateClaimCivilValidationServiceStrategy;

  @Mock private Map<String, DuplicateClaimValidationStrategy> strategies;

  @InjectMocks private ClaimValidationService claimValidationService;

  @Nested
  @DisplayName("validateClaims")
  class ValidateClaimsTests {

    @BeforeEach
    void setup() {
      lenient()
          .when(strategies.get(StrategyTypes.CRIME))
          .thenReturn(duplicateClaimCrimeValidationService);
      lenient()
          .when(strategies.get(StrategyTypes.CIVIL))
          .thenReturn(mockDuplicateClaimCivilValidationServiceStrategy);

      // Define the map for the test
      Map<String, List<String>> mandatoryFieldsByAreaOfLaw =
          Map.of(
              "CIVIL",
              List.of(
                  "uniqueFileNumber",
                  "caseReferenceNumber",
                  "scheduleReference",
                  "caseStartDate",
                  "caseConcludedDate"),
              "CRIME",
              List.of("stageReachedCode", "caseConcludedDate"),
              "MEDIATION",
              List.of(
                  "uniqueFileNumber", "caseReferenceNumber", "scheduleReference", "caseStartDate"),
              "CRIME_LOWER",
              List.of("stageReachedCode"));

      lenient()
          .when(mandatoryFieldsRegistry.getMandatoryFieldsByAreaOfLaw())
          .thenReturn(mandatoryFieldsByAreaOfLaw);
    }

    @Test
    @DisplayName("Validates category of law, duplicates and fee calculation for all claims")
    void validateCategoryOfLawAndDuplicatesAndFeeCalculation() {
      UUID claimId1 = new UUID(1, 1);
      UUID claimId2 = new UUID(2, 2);
      ClaimResponse claim1 =
          new ClaimResponse()
              .id(claimId1.toString())
              .feeCode("feeCode1")
              .caseStartDate("2025-08-14")
              .status(ClaimStatus.READY_TO_PROCESS)
              .matterTypeCode("ab:cd");
      ClaimResponse claim2 =
          new ClaimResponse()
              .id(claimId2.toString())
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
              .addClaimsItem(
                  new SubmissionClaim().status(ClaimStatus.READY_TO_PROCESS).claimId(claimId1))
              .addClaimsItem(
                  new SubmissionClaim().status(ClaimStatus.READY_TO_PROCESS).claimId(claimId2))
              .officeAccountNumber("officeAccountNumber");

      when(dataClaimsRestClient.getClaim(any(), any()))
          .thenReturn(ResponseEntity.ok(claim1))
          .thenReturn(ResponseEntity.ok(claim2));

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

      verify(mockDuplicateClaimCivilValidationServiceStrategy, times(1))
          .validateDuplicateClaims(claim1, claims, "officeAccountNumber", context);
      verify(mockDuplicateClaimCivilValidationServiceStrategy, times(1))
          .validateDuplicateClaims(claim2, claims, "officeAccountNumber", context);

      verify(feeCalculationService, times(1))
          .validateFeeCalculation(submissionResponse.getSubmissionId(), claim1, context);
      verify(feeCalculationService, times(1))
          .validateFeeCalculation(submissionResponse.getSubmissionId(), claim2, context);
    }

    @Test
    void validatePastDates() {
      UUID claimId1 = new UUID(1, 1);
      UUID claimId2 = new UUID(2, 2);
      ClaimResponse claim1 =
          new ClaimResponse()
              .id(claimId1.toString())
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
              .id(claimId2.toString())
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

      when(claimEffectiveDateUtil.getEffectiveDate(any())).thenReturn(LocalDate.of(2025, 8, 14));

      SubmissionResponse submissionResponse =
          new SubmissionResponse()
              .submissionId(new UUID(1, 1))
              .areaOfLaw("CIVIL")
              .addClaimsItem(
                  new SubmissionClaim().status(ClaimStatus.READY_TO_PROCESS).claimId(claimId1))
              .addClaimsItem(
                  new SubmissionClaim().status(ClaimStatus.READY_TO_PROCESS).claimId(claimId2))
              .officeAccountNumber("officeAccountNumber");

      when(dataClaimsRestClient.getClaim(any(), any()))
          .thenReturn(ResponseEntity.ok(claim1))
          .thenReturn(ResponseEntity.ok(claim2));

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(
          List.of(
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim2.getId())));

      claimValidationService.validateClaims(submissionResponse, context);

      // Then
      assertThat(
              getClaimMessages(context, claimId1.toString()).stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value provided for Case Start Date: 2003-13-34")))
          .isTrue();
      assertThat(
              getClaimMessages(context, claimId1.toString()).stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value for Transfer Date (Must be between 1995-01-01 "
                                      + "and today): "
                                      + "2090-12-02")))
          .isTrue();
      assertThat(
              getClaimMessages(context, claimId1.toString()).stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value for Case Concluded Date (Must be between "
                                      + "1995-01-01 and "
                                      + "today): 2090-01-01")))
          .isTrue();
      assertThat(
              getClaimMessages(context, claimId1.toString()).stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value for Representation Order Date (Must be between "
                                      + "2016-04-01 "
                                      + "and today): 2090-01-01")))
          .isTrue();
      assertThat(
              getClaimMessages(context, claimId1.toString()).stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value for Client Date of Birth (Must be between "
                                      + "1900-01-01 and "
                                      + "today): 2099-12-31")))
          .isTrue();
      assertThat(
              getClaimMessages(context, claimId1.toString()).stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value for Client2 Date of Birth (Must be between "
                                      + "1900-01-01 and "
                                      + "today): 2099-12-31")))
          .isTrue();
      assertThat(
              getClaimMessages(context, claimId2.toString()).stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value for Case Start Date (Must be between 1995-01-01"
                                      + " and today):"
                                      + " 1993-01-03")))
          .isTrue();
      assertThat(
              getClaimMessages(context, claimId2.toString()).stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value for Transfer Date (Must be between 1995-01-01 "
                                      + "and today): "
                                      + "1990-12-02")))
          .isTrue();
      assertThat(
              getClaimMessages(context, claimId2.toString()).stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value for Case Concluded Date (Must be between "
                                      + "1995-01-01 and "
                                      + "today): 1993-01-01")))
          .isTrue();
      assertThat(
              getClaimMessages(context, claimId2.toString()).stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value for Representation Order Date (Must be between "
                                      + "2016-04-01 "
                                      + "and today): 2016-03-30")))
          .isTrue();
      assertThat(
              getClaimMessages(context, claimId2.toString()).stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value for Client Date of Birth (Must be between "
                                      + "1900-01-01 and "
                                      + "today): 1899-12-31")))
          .isTrue();
      assertThat(
              getClaimMessages(context, claimId2.toString()).stream()
                  .anyMatch(
                      x ->
                          x.getDisplayMessage()
                              .equals(
                                  "Invalid date value for Client Date of Birth (Must be between "
                                      + "1900-01-01 and "
                                      + "today): 1899-12-31")))
          .isTrue();
      assertThat(
              getClaimMessages(context, claimId2.toString()).stream()
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
      UUID claimId1 = new UUID(1, 1);
      UUID claimId2 = new UUID(2, 2);
      ClaimResponse claim1 =
          new ClaimResponse()
              .id(claimId1.toString())
              .status(ClaimStatus.READY_TO_PROCESS)
              .feeCode("feeCode1")
              .caseStartDate("2025-08-14")
              .caseConcludedDate("2025-09-14")
              .caseReferenceNumber("123")
              .scheduleReference("SCH123")
              .uniqueFileNumber("010101/123")
              .matterTypeCode("AB:CD")
              .stageReachedCode("AA");
      ClaimResponse claim2 =
          new ClaimResponse()
              .id(claimId2.toString())
              .uniqueFileNumber("010101/123")
              .status(ClaimStatus.READY_TO_PROCESS)
              .feeCode("feeCode2")
              .caseStartDate("2025-05-25")
              .caseConcludedDate("2025-09-14")
              .caseReferenceNumber("124")
              .scheduleReference("SCH123")
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

      when(claimEffectiveDateUtil.getEffectiveDate(any())).thenReturn(LocalDate.of(2025, 8, 14));

      SubmissionResponse submissionResponse1 =
          new SubmissionResponse()
              .submissionId(new UUID(1, 1))
              .areaOfLaw("CIVIL")
              .addClaimsItem(
                  new SubmissionClaim().status(ClaimStatus.READY_TO_PROCESS).claimId(claimId1))
              .addClaimsItem(
                  new SubmissionClaim().status(ClaimStatus.READY_TO_PROCESS).claimId(claimId2))
              .officeAccountNumber("officeAccountNumber");

      when(dataClaimsRestClient.getClaim(any(), any()))
          .thenReturn(ResponseEntity.ok(claim1))
          .thenReturn(ResponseEntity.ok(claim2));

      SubmissionValidationContext context1 = new SubmissionValidationContext();
      context1.addClaimReports(
          List.of(
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim2.getId())));

      claimValidationService.validateClaims(submissionResponse1, context1);

      assertThat(getClaimMessages(context1, claimId1.toString()).isEmpty()).isTrue();
      assertThat(getClaimMessages(context1, claimId2.toString()).isEmpty()).isTrue();

      SubmissionResponse submissionResponse2 =
          new SubmissionResponse()
              .submissionId(new UUID(1, 1))
              .areaOfLaw("CRIME")
              .addClaimsItem(
                  new SubmissionClaim().status(ClaimStatus.READY_TO_PROCESS).claimId(claimId1))
              .addClaimsItem(
                  new SubmissionClaim().status(ClaimStatus.READY_TO_PROCESS).claimId(claimId2))
              .officeAccountNumber("officeAccountNumber");

      SubmissionValidationContext context2 = new SubmissionValidationContext();
      context1.addClaimReports(
          List.of(
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim2.getId())));

      when(dataClaimsRestClient.getClaim(any(), any()))
          .thenReturn(ResponseEntity.ok(claim1))
          .thenReturn(ResponseEntity.ok(claim2));

      claimValidationService.validateClaims(submissionResponse2, context2);

      assertThat(getClaimMessages(context2, claimId1.toString()).getFirst().getDisplayMessage())
          .isEqualTo(
              "stage_reached_code (CRIME): does not match the regex pattern ^[A-Z]{4}$ (provided "
                  + "value: AA)");
      assertThat(getClaimMessages(context2, claimId2.toString()).getFirst().getDisplayMessage())
          .isEqualTo("stageReachedCode is required for area of law: CRIME");
    }

    @ParameterizedTest(
        name = "{index} => claimId={0}, matterType={1}, areaOfLaw={2}, regex={3}, expectError={4}")
    @CsvSource({
      "1, BadMatterType, CIVIL, 123, SCH123, '^[a-zA-Z0-9]{1,4}[-:][a-zA-Z0-9]{1,4}$', true",
      "2, ab12:bc24, CIVIL, 125, SCH123, '^[a-zA-Z0-9]{1,4}[-:][a-zA-Z0-9]{1,4}$', false",
      "3, AB-CD, CIVIL, 126, SCH123, '^[a-zA-Z0-9]{1,4}[-:][a-zA-Z0-9]{1,4}$', false",
      "4, ABCD:EFGH, MEDIATION, 127, SCH123, '^[A-Z]{4}[-:][A-Z]{4}$', false",
      "5, AB12:CD34, MEDIATION, 128, SCH123, '^[A-Z]{4}[-:][A-Z]{4}$', true",
      "6, AB-CD, MEDIATION, 129, SCH123, '^[A-Z]{4}[-:][A-Z]{4}$', true",
    })
    void checkMatterType(
        int claimIdBit,
        String matterTypeCode,
        String areaOfLaw,
        String caseReferenceNumber,
        String scheduleReference,
        String regex,
        boolean expectError) {
      UUID claimId = new UUID(claimIdBit, claimIdBit);
      ClaimResponse claim =
          new ClaimResponse()
              .id(claimId.toString())
              .feeCode("feeCode1")
              .caseStartDate("2025-08-14")
              .caseConcludedDate("2025-09-14")
              .caseReferenceNumber(caseReferenceNumber)
              .scheduleReference(scheduleReference)
              .status(ClaimStatus.READY_TO_PROCESS)
              .uniqueFileNumber("010101/123")
              .matterTypeCode(matterTypeCode);

      List<ClaimResponse> claims = List.of(claim);
      Map<String, CategoryOfLawResult> categoryOfLawLookup = Collections.emptyMap();

      when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
          .thenReturn(categoryOfLawLookup);

      when(claimEffectiveDateUtil.getEffectiveDate(any())).thenReturn(LocalDate.of(2025, 8, 14));

      SubmissionResponse submissionResponse =
          new SubmissionResponse()
              .submissionId(new UUID(1, 1))
              .areaOfLaw(areaOfLaw)
              .claims(
                  singletonList(
                      new SubmissionClaim().status(ClaimStatus.READY_TO_PROCESS).claimId(claimId)))
              .officeAccountNumber("officeAccountNumber");

      ClaimResultSet claimResultSet = new ClaimResultSet();
      claimResultSet.content(claims);

      when(dataClaimsRestClient.getClaim(any(), any())).thenReturn(ResponseEntity.ok(claim));

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
        assertThat(getClaimMessages(context, claimId.toString()).getFirst().getTechnicalMessage())
            .isEqualTo(expectedMessage);
      } else {
        assertThat(getClaimMessages(context, claimId.toString()).isEmpty()).isTrue();
      }
    }

    /*
    Schedule Reference should have format validation for CIVIL, but no format validation is needed for MEDIATION.
    This field is optional for CRIME.
    Ref: https://dsdmoj.atlassian.net/browse/DSTEW-430
     */
    @ParameterizedTest(
        name =
            "{index} => claimId={0}, matterType={1}, areaOfLaw={2}, caseReferenceNumber={3}, scheduleReference={4}, stageReachedCode={5}, regex={6}, expectError={7}")
    @CsvSource({
      "1, ab12:bc24, CIVIL, 123, SCH123, AB, '^[a-zA-Z0-9/.\\-]{1,20}$', false",
      "2, ab12:bc24, CIVIL, 123, ABCDEFGHIJKLMNOPQRST123, AB, '^[a-zA-Z0-9/.\\-]{1,20}$', true",
      "3, ab12:bc24, CIVIL, 123, SCH/ABC-12.34, AB, '^[a-zA-Z0-9/.\\-]{1,20}$', false",
      "4, ab12:bc24, CIVIL, 123, Schedule Ref, AB, '^[a-zA-Z0-9/.\\-]{1,20}$', true",
      "5, ab12:bc24, CIVIL, 123, Schedule:Ref, AB, '^[a-zA-Z0-9/.\\-]{1,20}$', true",
      "6, ab12:bc24, CRIME,,, ABCD, '^[a-zA-Z0-9/.\\-]{1,20}$', false",
      "7, ab12:bc24, CRIME,, ABCD?!, ABCD, '^[a-zA-Z0-9/.\\-]{1,20}$', false",
      "8, ABCD:EFGH, MEDIATION, 123, ABCDEFGHIJKLMNOPQRST123, AB, '^[a-zA-Z0-9/.\\-]{1,20}$', false",
      "9, ABCD:EFGH, MEDIATION, 123, ABCD?!, AB, '^[a-zA-Z0-9/.\\-]{1,20}$', false"
    })
    void validateFormatForScheduleReference(
        int claimIdBit,
        String matterTypeCode,
        String areaOfLaw,
        String caseReferenceNumber,
        String scheduleReference,
        String stageReachedCode,
        String regex,
        boolean expectError) {
      UUID claimId = new UUID(claimIdBit, claimIdBit);
      ClaimResponse claim =
          new ClaimResponse()
              .id(claimId.toString())
              .feeCode("feeCode1")
              .caseStartDate("2025-08-14")
              .caseConcludedDate("2025-09-14")
              .caseReferenceNumber(caseReferenceNumber)
              .scheduleReference(scheduleReference)
              .status(ClaimStatus.READY_TO_PROCESS)
              .uniqueFileNumber("010101/123")
              .matterTypeCode(matterTypeCode)
              .stageReachedCode(stageReachedCode);

      List<ClaimResponse> claims = List.of(claim);
      Map<String, CategoryOfLawResult> categoryOfLawLookup = Collections.emptyMap();

      when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
          .thenReturn(categoryOfLawLookup);

      when(claimEffectiveDateUtil.getEffectiveDate(any())).thenReturn(LocalDate.of(2025, 8, 14));

      SubmissionResponse submissionResponse =
          new SubmissionResponse()
              .submissionId(new UUID(1, 1))
              .areaOfLaw(areaOfLaw)
              .claims(
                  singletonList(
                      new SubmissionClaim().status(ClaimStatus.READY_TO_PROCESS).claimId(claimId)))
              .officeAccountNumber("officeAccountNumber");

      ClaimResultSet claimResultSet = new ClaimResultSet();
      claimResultSet.content(claims);

      when(dataClaimsRestClient.getClaim(any(), any())).thenReturn(ResponseEntity.ok(claim));

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
                "schedule_reference (%s): does not match the regex pattern %s (provided value: %s)",
                areaOfLaw, regex, scheduleReference);
        assertThat(getClaimMessages(context, claimId.toString()).getFirst().getTechnicalMessage())
            .isEqualTo(expectedMessage);
      } else {
        assertThat(getClaimMessages(context, claimId.toString()).isEmpty()).isTrue();
      }
    }

    /*
    Schedule Reference should be mandatory for CIVIL and MEDIATION, but it's optional for CRIME.
    Ref: https://dsdmoj.atlassian.net/browse/DSTEW-430
     */
    @ParameterizedTest(
        name =
            "{index} => claimId={0}, matterType={1}, areaOfLaw={2}, caseReferenceNumber={3}, scheduleReference={4}, stageReachedCode={5}, expectError={6}")
    @CsvSource({
      "1, ab12:bc24, CIVIL, 123,, AB, true",
      "2, ab12:bc24, CIVIL, 123, SCH123, AB, false",
      "3, ABCD:EFGH, MEDIATION, 123,, AB, true",
      "4, ABCD:EFGH, MEDIATION, 123, SCH:123, AB, false",
      "5, ab12:bc24, CRIME, 123,, ABCD, false"
    })
    void checkMandatoryScheduleReference(
        int claimIdBit,
        String matterTypeCode,
        String areaOfLaw,
        String caseReferenceNumber,
        String scheduleReference,
        String stageReachedCode,
        boolean expectError) {
      UUID claimId = new UUID(claimIdBit, claimIdBit);
      ClaimResponse claim =
          new ClaimResponse()
              .id(claimId.toString())
              .feeCode("feeCode1")
              .caseStartDate("2025-08-14")
              .caseConcludedDate("2025-09-14")
              .caseReferenceNumber(caseReferenceNumber)
              .scheduleReference(scheduleReference)
              .status(ClaimStatus.READY_TO_PROCESS)
              .uniqueFileNumber("010101/123")
              .matterTypeCode(matterTypeCode)
              .stageReachedCode(stageReachedCode);

      List<ClaimResponse> claims = List.of(claim);
      Map<String, CategoryOfLawResult> categoryOfLawLookup = Collections.emptyMap();

      when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
          .thenReturn(categoryOfLawLookup);

      when(claimEffectiveDateUtil.getEffectiveDate(any())).thenReturn(LocalDate.of(2025, 8, 14));

      SubmissionResponse submissionResponse =
          new SubmissionResponse()
              .submissionId(new UUID(1, 1))
              .areaOfLaw(areaOfLaw)
              .claims(
                  singletonList(
                      new SubmissionClaim().status(ClaimStatus.READY_TO_PROCESS).claimId(claimId)))
              .officeAccountNumber("officeAccountNumber");

      ClaimResultSet claimResultSet = new ClaimResultSet();
      claimResultSet.content(claims);

      when(dataClaimsRestClient.getClaim(any(), any())).thenReturn(ResponseEntity.ok(claim));

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
            String.format("scheduleReference is required for area of law: %s", areaOfLaw);
        assertThat(getClaimMessages(context, claimId.toString()).getFirst().getTechnicalMessage())
            .isEqualTo(expectedMessage);
      } else {
        assertThat(getClaimMessages(context, claimId.toString()).isEmpty()).isTrue();
      }
    }

    /*
    Case Start Date should be mandatory for CIVIL and MEDIATION, but it's optional for CRIME.
    Ref: https://dsdmoj.atlassian.net/browse/DSTEW-430
     */
    @ParameterizedTest(
        name =
            "{index} => claimId={0}, matterType={1}, areaOfLaw={2}, caseStartDate={3}, stageReachedCode={4}, expectError={5}")
    @CsvSource({
      "1, ab12:bc24, CIVIL, 2025-08-14, AB, false, null",
      "2, ab12:bc24, CIVIL,, AB, true, caseStartDate is required for area of law: CIVIL",
      "3, ABCD:EFGH, MEDIATION, 2025-08-14, AB, false, null",
      "4, ABCD:EFGH, MEDIATION,, AB, true, caseStartDate is required for area of law: MEDIATION",
      "5, ab12:bc24, CRIME,'', ABCD, false, null"
    })
    void checkMandatoryCaseStartDate(
        int claimIdBit,
        String matterTypeCode,
        String areaOfLaw,
        String caseStartDate,
        String stageReachedCode,
        boolean expectError,
        String expectedErrorMsg) {
      UUID claimId = new UUID(claimIdBit, claimIdBit);
      ClaimResponse claim =
          new ClaimResponse()
              .id(claimId.toString())
              .feeCode("feeCode1")
              .caseStartDate(caseStartDate)
              .caseConcludedDate("2025-08-14")
              .caseReferenceNumber("123")
              .scheduleReference("SCH123")
              .status(ClaimStatus.READY_TO_PROCESS)
              .uniqueFileNumber("010101/123")
              .matterTypeCode(matterTypeCode)
              .stageReachedCode(stageReachedCode);

      List<ClaimResponse> claims = List.of(claim);
      Map<String, CategoryOfLawResult> categoryOfLawLookup = Collections.emptyMap();

      when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
          .thenReturn(categoryOfLawLookup);

      when(claimEffectiveDateUtil.getEffectiveDate(any())).thenReturn(LocalDate.of(2025, 8, 14));

      SubmissionResponse submissionResponse =
          new SubmissionResponse()
              .submissionId(new UUID(1, 1))
              .areaOfLaw(areaOfLaw)
              .claims(
                  singletonList(
                      new SubmissionClaim().status(ClaimStatus.READY_TO_PROCESS).claimId(claimId)))
              .officeAccountNumber("officeAccountNumber");

      ClaimResultSet claimResultSet = new ClaimResultSet();
      claimResultSet.content(claims);

      when(dataClaimsRestClient.getClaim(any(), any())).thenReturn(ResponseEntity.ok(claim));

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
        assertThat(getClaimMessages(context, claimId.toString()).getFirst().getTechnicalMessage())
            .isEqualTo(expectedErrorMsg);
      } else {
        assertThat(getClaimMessages(context, claimId.toString()).isEmpty()).isTrue();
      }
    }

    /*
    Case Concluded Date should be mandatory for CIVIL and CRIME, but it's optional for MEDIATION.
    Ref: https://dsdmoj.atlassian.net/browse/DSTEW-566
     */
    @ParameterizedTest(
        name =
            "{index} => claimId={0}, matterType={1}, areaOfLaw={2}, caseStartDate={3}, stageReachedCode={4}, expectError={5}")
    @CsvSource({
      "1, ab12:bc24, CIVIL, 2025-08-14, AB, false, null",
      "2, ab12:bc24, CIVIL, '', AB, true, caseConcludedDate is required for area of law: CIVIL", // empty String for caseConcludedDate
      "3, ab12:bc24, CIVIL,, AB, true, caseConcludedDate is required for area of law: CIVIL", // null for caseConcludedDate
      "4, ab12:bc24, CIVIL, ' ', AB, true, caseConcludedDate is required for area of law: CIVIL", // space for caseConcludedDate
      "5, ab12:bc24, CIVIL, 1994-08-14, AB, true, Invalid date value for Case Concluded Date (Must be between 1995-01-01 and today): 1994-08-14",
      "6, ab12:bc24, CRIME, 2017-08-14, ABCD, false, null",
      "7, ab12:bc24, CRIME, 2015-08-14, ABCD, true, Invalid date value for Case Concluded Date (Must be between 2016-04-01 and today): 2015-08-14",
      "8, ab12:bc24, CRIME, 2099-08-14, ABCD, true, Invalid date value for Case Concluded Date (Must be between 2016-04-01 and today): 2099-08-14",
      "9, ABCD:EFGH, MEDIATION, 1996-08-14, AB, false, null",
      "10, ABCD:EFGH, MEDIATION, '', AB, false, null",
      "11, ABCD:EFGH, MEDIATION, 1994-08-14, AB, true, Invalid date value for Case Concluded Date (Must be between 1995-01-01 and today): 1994-08-14"
    })
    void checkMandatoryCaseConcludedDate(
        int claimIdBit,
        String matterTypeCode,
        String areaOfLaw,
        String caseConcludedDate,
        String stageReachedCode,
        boolean expectError,
        String expectedErrorMsg) {
      UUID claimId = new UUID(claimIdBit, claimIdBit);
      ClaimResponse claim =
          new ClaimResponse()
              .id(claimId.toString())
              .feeCode("feeCode1")
              .caseStartDate("2025-08-14")
              .caseConcludedDate(caseConcludedDate)
              .caseReferenceNumber("123")
              .scheduleReference("SCH123")
              .status(ClaimStatus.READY_TO_PROCESS)
              .uniqueFileNumber("010101/123")
              .matterTypeCode(matterTypeCode)
              .stageReachedCode(stageReachedCode);

      List<ClaimResponse> claims = List.of(claim);
      Map<String, CategoryOfLawResult> categoryOfLawLookup = Collections.emptyMap();

      when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
          .thenReturn(categoryOfLawLookup);

      when(claimEffectiveDateUtil.getEffectiveDate(any())).thenReturn(LocalDate.of(2025, 8, 14));

      SubmissionResponse submissionResponse =
          new SubmissionResponse()
              .submissionId(new UUID(1, 1))
              .areaOfLaw(areaOfLaw)
              .claims(
                  singletonList(
                      new SubmissionClaim().status(ClaimStatus.READY_TO_PROCESS).claimId(claimId)))
              .officeAccountNumber("officeAccountNumber");

      ClaimResultSet claimResultSet = new ClaimResultSet();
      claimResultSet.content(claims);

      when(dataClaimsRestClient.getClaim(any(), any())).thenReturn(ResponseEntity.ok(claim));

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
        assertThat(getClaimMessages(context, claimId.toString()).getFirst().getTechnicalMessage())
            .isEqualTo(expectedErrorMsg);
      } else {
        assertThat(getClaimMessages(context, claimId.toString()).isEmpty()).isTrue();
      }
    }

    /*
    Case Reference should be mandatory for CIVIL and MEDIATION, but it's optional for CRIME.
    Ref: https://dsdmoj.atlassian.net/browse/DSTEW-430
     */
    @ParameterizedTest(
        name =
            "{index} => claimId={0}, matterType={1}, areaOfLaw={2}, caseReferenceNumber={3}, scheduleReference={4}, stageReachedCode={5}, expectError={6}")
    @CsvSource({
      "1, ab12:bc24, CIVIL,,SCH123, AB, true",
      "2, ab12:bc24, CIVIL, 123, SCH123, AB, false",
      "3, ABCD:EFGH, MEDIATION,, SCH123, AB, true",
      "4, ABCD:EFGH, MEDIATION, 123, SCH:123, AB, false",
      "5, ab12:bc24, CRIME,, SCH123, ABCD, false"
    })
    void checkMandatoryCaseReference(
        int claimIdBit,
        String matterTypeCode,
        String areaOfLaw,
        String caseReferenceNumber,
        String scheduleReference,
        String stageReachedCode,
        boolean expectError) {
      UUID claimId = new UUID(claimIdBit, claimIdBit);
      ClaimResponse claim =
          new ClaimResponse()
              .id(claimId.toString())
              .feeCode("feeCode1")
              .caseStartDate("2025-08-14")
              .caseConcludedDate("2025-09-14")
              .caseReferenceNumber(caseReferenceNumber)
              .scheduleReference(scheduleReference)
              .status(ClaimStatus.READY_TO_PROCESS)
              .uniqueFileNumber("010101/123")
              .matterTypeCode(matterTypeCode)
              .stageReachedCode(stageReachedCode);

      List<ClaimResponse> claims = List.of(claim);
      Map<String, CategoryOfLawResult> categoryOfLawLookup = Collections.emptyMap();

      when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
          .thenReturn(categoryOfLawLookup);

      when(claimEffectiveDateUtil.getEffectiveDate(any())).thenReturn(LocalDate.of(2025, 8, 14));

      SubmissionResponse submissionResponse =
          new SubmissionResponse()
              .submissionId(new UUID(1, 1))
              .areaOfLaw(areaOfLaw)
              .claims(
                  singletonList(
                      new SubmissionClaim().status(ClaimStatus.READY_TO_PROCESS).claimId(claimId)))
              .officeAccountNumber("officeAccountNumber");

      ClaimResultSet claimResultSet = new ClaimResultSet();
      claimResultSet.content(claims);

      when(dataClaimsRestClient.getClaim(any(), any())).thenReturn(ResponseEntity.ok(claim));

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
            String.format("caseReferenceNumber is required for area of law: %s", areaOfLaw);
        assertThat(getClaimMessages(context, claimId.toString()).getFirst().getTechnicalMessage())
            .isEqualTo(expectedMessage);
      } else {
        assertThat(getClaimMessages(context, claimId.toString()).isEmpty()).isTrue();
      }
    }

    @ParameterizedTest(
        name =
            "{index} => claimId={0}, stageReachedCode={1}, areaOfLaw={2}, regex={3}, "
                + "expectError={4}")
    @CsvSource({
      "1, AABB, CIVIL, 123, SCH123, '^[a-zA-Z0-9]{2}$', true",
      "2, AZ, CIVIL, 124, SCH123, '^[a-zA-Z0-9]{2}$', false",
      "3, C9, CIVIL, 125, SCH123, '^[a-zA-Z0-9]{2}$', false",
      "4, A!, CIVIL, 126, SCH123, '^[a-zA-Z0-9]{2}$', true",
      "5, ABCD, CRIME, null, null, '^[A-Z]{4}$', false",
      "6, A1, CRIME,,,'^[A-Z]{4}$', true",
      "7, A-CD, CRIME,,, '^[A-Z]{4}$', true",
    })
    void checkStageReachedCode(
        int claimIdBit,
        String stageReachedCode,
        String areaOfLaw,
        String caseReferenceNumber,
        String scheduleReference,
        String regex,
        boolean expectError) {
      UUID claimId = new UUID(claimIdBit, claimIdBit);
      ClaimResponse claim =
          new ClaimResponse()
              .id(claimId.toString())
              .feeCode("feeCode1")
              .caseStartDate("2025-08-14")
              .caseConcludedDate("2025-09-14")
              .status(ClaimStatus.READY_TO_PROCESS)
              .uniqueFileNumber("010101/123")
              .stageReachedCode(stageReachedCode)
              .caseReferenceNumber(caseReferenceNumber)
              .scheduleReference(scheduleReference);

      List<ClaimResponse> claims = List.of(claim);
      Map<String, CategoryOfLawResult> categoryOfLawLookup = Collections.emptyMap();

      when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
          .thenReturn(categoryOfLawLookup);

      when(claimEffectiveDateUtil.getEffectiveDate(any())).thenReturn(LocalDate.of(2025, 8, 14));

      SubmissionResponse submissionResponse =
          new SubmissionResponse()
              .submissionId(new UUID(1, 1))
              .areaOfLaw(areaOfLaw)
              .claims(
                  singletonList(
                      new SubmissionClaim().claimId(claimId).status(ClaimStatus.READY_TO_PROCESS)))
              .officeAccountNumber("officeAccountNumber");

      ClaimResultSet claimResultSet = new ClaimResultSet();
      claimResultSet.content(claims);

      when(dataClaimsRestClient.getClaim(any(), any())).thenReturn(ResponseEntity.ok(claim));

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
        assertThat(getClaimMessages(context, claimId.toString()).getFirst().getTechnicalMessage())
            .isEqualTo(expectedMessage);
      } else {
        assertThat(getClaimMessages(context, claimId.toString()).isEmpty()).isTrue();
      }
    }

    @ParameterizedTest(
        name =
            "{index} => claimId={0}, disbursementVatAmount={1}, areaOfLaw={2}, maxAllowed={3}, "
                + "expectError={4}")
    @CsvSource({
      "1, 99999.99, CIVIL, 123, SCH123, 99999.99, false",
      "2, 999999.99, CRIME,,, 999999.99, false",
      "3, 999999999.99, MEDIATION, 124, SCH123, 999999999.99, false",
      "4, 100000.0, CIVIL, 126, SCH123, 99999.99, true",
      "5, 1000000.0, CRIME,,, 999999.99, true",
      "6, 1000000000.0, MEDIATION, 127, SCH123, 999999999.99, true",
    })
    void checkDisbursementsVatAmount(
        int claimIdBit,
        BigDecimal disbursementsVatAmount,
        String areaOfLaw,
        String caseReferenceNumber,
        String scheduleReference,
        BigDecimal maxAllowed,
        boolean expectError) {
      UUID claimId = new UUID(claimIdBit, claimIdBit);
      ClaimResponse claim =
          new ClaimResponse()
              .id(claimId.toString())
              .feeCode("feeCode1")
              .caseStartDate("2025-08-14")
              .caseConcludedDate("2025-09-14")
              .uniqueFileNumber("010101/123")
              .caseReferenceNumber(caseReferenceNumber)
              .scheduleReference(scheduleReference)
              .status(ClaimStatus.READY_TO_PROCESS)
              .disbursementsVatAmount(disbursementsVatAmount);
      if (areaOfLaw.equals("CRIME")) {
        claim.setStageReachedCode("ABCD");
      }

      List<ClaimResponse> claims = List.of(claim);
      Map<String, CategoryOfLawResult> categoryOfLawLookup = Collections.emptyMap();

      when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
          .thenReturn(categoryOfLawLookup);

      when(claimEffectiveDateUtil.getEffectiveDate(any())).thenReturn(LocalDate.of(2025, 8, 14));

      SubmissionResponse submissionResponse =
          new SubmissionResponse()
              .submissionId(new UUID(1, 1))
              .areaOfLaw(areaOfLaw)
              .claims(
                  singletonList(
                      new SubmissionClaim().claimId(claimId).status(ClaimStatus.READY_TO_PROCESS)))
              .officeAccountNumber("officeAccountNumber");

      when(dataClaimsRestClient.getClaim(any(), any())).thenReturn(ResponseEntity.ok(claim));

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
        assertThat(getClaimMessages(context, claimId.toString()).getFirst().getDisplayMessage())
            .isEqualTo(expectedMessage);
      } else {
        for (var claimReport : context.getClaimReports()) {
          assertThat(claimReport.hasErrors()).isFalse();
        }
      }
    }

    @Nested
    class DuplicateClaimsValidation {

      @DisplayName("Area of Code CIVIL: should call civil validation strategy")
      @Test
      void callCivilValidationStrategy() {
        SubmissionResponse submissionResponse =
            new SubmissionResponse()
                .submissionId(new UUID(1, 1))
                .areaOfLaw("CIVIL")
                .claims(
                    singletonList(
                        new SubmissionClaim()
                            .status(ClaimStatus.READY_TO_PROCESS)
                            .claimId(UUID.fromString(claim.getId()))))
                .officeAccountNumber("officeAccountNumber");
        when(dataClaimsRestClient.getClaim(any(), any())).thenReturn(ResponseEntity.ok(claim));
        when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
            .thenReturn(Collections.emptyMap());

        when(claimEffectiveDateUtil.getEffectiveDate(any())).thenReturn(LocalDate.of(2025, 8, 14));
        when(providerDetailsRestClient.getProviderFirmSchedules(
                eq("officeAccountNumber"), eq("CIVIL"), any(LocalDate.class)))
            .thenReturn(Mono.just(data));

        SubmissionValidationContext context = createSubmissionValidationContext();
        claimValidationService.validateClaims(submissionResponse, context);

        verify(mockDuplicateClaimCivilValidationServiceStrategy)
            .validateDuplicateClaims(claim, claims, "officeAccountNumber", context);
        verify(duplicateClaimCrimeValidationService, times(0))
            .validateDuplicateClaims(any(), any(), any(), any());
      }

      @DisplayName("Area of Code CRIME_LOWER: should call crime validation strategy")
      @Test
      void crimeValidationStrategy() {
        SubmissionResponse submissionResponse =
            new SubmissionResponse()
                .submissionId(new UUID(1, 1))
                .areaOfLaw("CRIME_LOWER")
                .addClaimsItem(
                    new SubmissionClaim()
                        .status(ClaimStatus.READY_TO_PROCESS)
                        .claimId(UUID.fromString(claim.getId())))
                .officeAccountNumber("officeAccountNumber");

        when(dataClaimsRestClient.getClaim(any(), any())).thenReturn(ResponseEntity.ok(claim));
        when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
            .thenReturn(Collections.emptyMap());

        when(claimEffectiveDateUtil.getEffectiveDate(any())).thenReturn(LocalDate.of(2025, 8, 14));
        when(providerDetailsRestClient.getProviderFirmSchedules(
                eq("officeAccountNumber"), eq("CRIME_LOWER"), any(LocalDate.class)))
            .thenReturn(Mono.just(data));
        SubmissionValidationContext context = createSubmissionValidationContext();

        claimValidationService.validateClaims(submissionResponse, context);

        verify(duplicateClaimCrimeValidationService)
            .validateDuplicateClaims(claim, claims, "officeAccountNumber", context);
        verify(mockDuplicateClaimCivilValidationServiceStrategy, times(0))
            .validateDuplicateClaims(any(), any(), any(), any());
      }

      private final ClaimResponse claim =
          new ClaimResponse()
              .id(new UUID(1, 1).toString())
              .feeCode("feeCode1")
              .caseStartDate("2025-08-14")
              .uniqueFileNumber("010101/123")
              .status(ClaimStatus.READY_TO_PROCESS);

      private final List<ClaimResponse> claims = List.of(claim);

      ClaimResultSet claimResultSet = new ClaimResultSet().content(claims);

      ProviderFirmOfficeContractAndScheduleDto data =
          new ProviderFirmOfficeContractAndScheduleDto()
              .addSchedulesItem(
                  new FirmOfficeContractAndScheduleDetails()
                      .addScheduleLinesItem(
                          new FirmOfficeContractAndScheduleLine().categoryOfLaw("categoryOfLaw1")));

      private SubmissionValidationContext createSubmissionValidationContext() {
        SubmissionValidationContext context = new SubmissionValidationContext();
        context.addClaimReports(List.of(new ClaimValidationReport(claim.getId())));
        return context;
      }
    }
  }

  private static List<ValidationMessagePatch> getClaimMessages(
      SubmissionValidationContext context, String claim1) {
    return context.getClaimReport(claim1).get().getMessages();
  }
}
