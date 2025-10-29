package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionAreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ProviderDetailsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.CategoryOfLawValidationService;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.FeeDetailsResponseWrapper;
import uk.gov.justice.laa.dstew.payments.claimsevent.util.ClaimEffectiveDateUtil;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.provider.model.FirmOfficeContractAndScheduleDetails;
import uk.gov.justice.laa.provider.model.FirmOfficeContractAndScheduleLine;
import uk.gov.justice.laa.provider.model.ProviderFirmOfficeContractAndScheduleDto;

@ExtendWith(MockitoExtension.class)
@DisplayName("Effective category of law claim validator test")
class EffectiveCategoryOfLawClaimValidatorTest {

  EffectiveCategoryOfLawClaimValidator validator;

  @Mock CategoryOfLawValidationService categoryOfLawValidationService;
  @Mock ClaimEffectiveDateUtil claimEffectiveDateUtil;
  @Mock ProviderDetailsRestClient providerDetailsRestClient;

  @BeforeEach
  void beforeEach() {
    validator =
        new EffectiveCategoryOfLawClaimValidator(
            categoryOfLawValidationService, claimEffectiveDateUtil, providerDetailsRestClient);
  }

  @Test
  @DisplayName("Verify category of law validation service is correctly called")
  void verifyCategoryOfLawValidationServiceIsCorrectlyCalled() {
    UUID claimId = new UUID(1, 1);
    ClaimResponse claim =
        new ClaimResponse()
            .id(claimId.toString())
            .feeCode("feeCode1")
            .caseStartDate("2025-08-14")
            .status(ClaimStatus.READY_TO_PROCESS)
            .matterTypeCode("ab:cd");
    List<String> providerCategoriesOfLaw = List.of("categoryOfLaw1");

    Map<String, FeeDetailsResponseWrapper> feeDetailsResponseMap = Collections.emptyMap();

    ProviderFirmOfficeContractAndScheduleDto data =
        new ProviderFirmOfficeContractAndScheduleDto()
            .addSchedulesItem(
                new FirmOfficeContractAndScheduleDetails()
                    .addScheduleLinesItem(
                        new FirmOfficeContractAndScheduleLine().categoryOfLaw("categoryOfLaw1")));

    when(providerDetailsRestClient.getProviderFirmSchedules(
            eq("officeAccountNumber"),
            eq(BulkSubmissionAreaOfLaw.LEGAL_HELP.getValue()),
            any(LocalDate.class)))
        .thenReturn(Mono.just(data));

    // Two claims make two separate calls to claimEffectiveDateUtil
    when(claimEffectiveDateUtil.getEffectiveDate(any())).thenReturn(LocalDate.of(2025, 8, 14));

    SubmissionValidationContext context = new SubmissionValidationContext();

    validator.validate(
        claim,
        context,
        BulkSubmissionAreaOfLaw.LEGAL_HELP,
        "officeAccountNumber",
        feeDetailsResponseMap);

    verify(claimEffectiveDateUtil, times(1)).getEffectiveDate(claim);

    verify(categoryOfLawValidationService, times(1))
        .validateCategoryOfLaw(claim, feeDetailsResponseMap, providerCategoriesOfLaw, context);
  }
}
