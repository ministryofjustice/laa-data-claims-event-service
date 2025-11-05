package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.lenient;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.getClaimMessages;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.FeeCalculationType;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.ExclusionsRegistry;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.MandatoryFieldsRegistry;
import uk.gov.justice.laa.dstew.payments.claimsevent.util.StringCaseUtil;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

@ExtendWith(MockitoExtension.class)
@DisplayName("Mandatory field claim validator test")
class MandatoryFieldClaimValidatorTest {

  @Mock MandatoryFieldsRegistry mandatoryFieldsRegistry;
  ExclusionsRegistry exclusionsRegistry = new ExclusionsRegistry();

  MandatoryFieldClaimValidator validator;

  @BeforeEach
  void beforeEach() {
    validator = new MandatoryFieldClaimValidator(mandatoryFieldsRegistry, exclusionsRegistry);
  }

  @Test
  void shouldHaveNoErrorsWhenNoMandatoryFields() {
    // Define the map for the test
    Map<AreaOfLaw, List<String>> legalHelpMandatoryFields = Map.of();

    lenient()
        .when(mandatoryFieldsRegistry.getMandatoryFieldsByAreaOfLaw())
        .thenReturn(legalHelpMandatoryFields);

    UUID claimId = new UUID(1, 1);
    ClaimResponse claim =
        new ClaimResponse()
            .id(claimId.toString())
            .status(ClaimStatus.READY_TO_PROCESS)
            .feeCode("feeCode1")
            .caseStartDate("2025-08-14")
            .uniqueFileNumber("010101/123")
            .matterTypeCode("AB:CD")
            .stageReachedCode("AA");

    SubmissionValidationContext context = new SubmissionValidationContext();

    validator.validate(claim, context, AreaOfLaw.LEGAL_HELP, "TYPE");

    assertThat(getClaimMessages(context, claimId.toString()).isEmpty()).isTrue();
  }

  @Test
  void shouldHaveNoErrorsWhenNoMandatoryFieldsMissing() {
    // Define the map for the test
    Map<AreaOfLaw, List<String>> legalHelpMandatoryFields =
        Map.of(
            AreaOfLaw.LEGAL_HELP,
            List.of(
                "feeCode",
                "caseStartDate",
                "uniqueFileNumber",
                "matterTypeCode",
                "stageReachedCode"));

    lenient()
        .when(mandatoryFieldsRegistry.getMandatoryFieldsByAreaOfLaw())
        .thenReturn(legalHelpMandatoryFields);

    UUID claimId = new UUID(1, 1);
    ClaimResponse claim =
        new ClaimResponse()
            .id(claimId.toString())
            .status(ClaimStatus.READY_TO_PROCESS)
            .feeCode("feeCode1")
            .caseStartDate("2025-08-14")
            .uniqueFileNumber("010101/123")
            .matterTypeCode("AB:CD")
            .stageReachedCode("AA");

    SubmissionValidationContext context = new SubmissionValidationContext();

    validator.validate(claim, context, AreaOfLaw.LEGAL_HELP, "TYPE");

    assertThat(getClaimMessages(context, claimId.toString()).isEmpty()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "feeCode",
        "caseStartDate",
        "uniqueFileNumber",
        "matterTypeCode",
        "stageReachedCode"
      })
  @DisplayName("Should have error when mandatory fields missing")
  void shouldHaveErrorWhenMandatoryFieldsMissing(String mandatoryField) {
    // Define the map for the test
    Map<AreaOfLaw, List<String>> legalHelpMandatoryFields =
        Map.of(AreaOfLaw.LEGAL_HELP, List.of(mandatoryField));

    lenient()
        .when(mandatoryFieldsRegistry.getMandatoryFieldsByAreaOfLaw())
        .thenReturn(legalHelpMandatoryFields);

    UUID claimId = new UUID(1, 1);
    ClaimResponse claim =
        new ClaimResponse().id(claimId.toString()).status(ClaimStatus.READY_TO_PROCESS);

    SubmissionValidationContext context = new SubmissionValidationContext();

    validator.validate(claim, context, AreaOfLaw.LEGAL_HELP, FeeCalculationType.FIXED.getValue());

    assertThat(getClaimMessages(context, claimId.toString()).isEmpty()).isFalse();
    assertThat(getClaimMessages(context, claimId.toString()).getFirst().getDisplayMessage())
        .isEqualTo(
            "%s is required for Legal Help claims"
                .formatted(StringCaseUtil.toTitleCase(mandatoryField)));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "travelWaitingCostsAmount",
        "adviceTime",
        "travelTime",
        "waitingTime",
        "netCounselCostsAmount",
        "netProfitCostsAmount",
        "isVatApplicable"
      })
  @DisplayName(
      "Should have no errors when excluded fields missing for disbursement only legal help claims")
  void shouldHaveNoErrorWhenExcludedFieldsMissingForDisbursementOnlyLegalHelpClaims(
      String mandatoryField) {
    // Define the map for the test
    Map<AreaOfLaw, List<String>> legalHelpMandatoryFields =
        Map.of(AreaOfLaw.LEGAL_HELP, List.of(mandatoryField));

    lenient()
        .when(mandatoryFieldsRegistry.getMandatoryFieldsByAreaOfLaw())
        .thenReturn(legalHelpMandatoryFields);

    UUID claimId = new UUID(1, 1);
    ClaimResponse claim =
        new ClaimResponse().id(claimId.toString()).status(ClaimStatus.READY_TO_PROCESS);

    SubmissionValidationContext context = new SubmissionValidationContext();

    validator.validate(
        claim, context, AreaOfLaw.LEGAL_HELP, FeeCalculationType.DISB_ONLY.getValue());

    assertThat(getClaimMessages(context, claimId.toString()).isEmpty()).isTrue();
  }
}
