package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ProviderDetailsRestClient;
import uk.gov.justice.laa.provider.model.FirmOfficeContractAndScheduleDetails;
import uk.gov.justice.laa.provider.model.FirmOfficeContractAndScheduleLine;
import uk.gov.justice.laa.provider.model.ProviderFirmOfficeContractAndScheduleDto;

@ExtendWith(MockitoExtension.class)
class ClaimValidationServiceTest {

  @Mock private CategoryOfLawValidationService categoryOfLawValidationService;

  @Mock private DuplicateClaimValidationService duplicateClaimValidationService;

  @Mock private FeeCalculationService feeCalculationService;

  @Mock private ProviderDetailsRestClient providerDetailsRestClient;

  @InjectMocks private ClaimValidationService claimValidationService;

  @Nested
  @DisplayName("validateClaims")
  class ValidateClaimsTests {

    @Test
    @DisplayName("Validates category of law, duplicates and fee calculation for all claims")
    void validateCategoryOfLawAndDuplicatesAndFeeCalculation() {
      ClaimResponse claim1 =
          new ClaimResponse().id("claim1").feeCode("feeCode1").caseStartDate("2025-08-14");
      ClaimResponse claim2 =
          new ClaimResponse().id("claim2").feeCode("feeCode2").caseStartDate("2025-05-25");
      List<ClaimResponse> claims = List.of(claim1, claim2);
      Map<String, CategoryOfLawResult> categoryOfLawLookup = Collections.emptyMap();
      ProviderFirmOfficeContractAndScheduleDto data =
          new ProviderFirmOfficeContractAndScheduleDto()
              .addSchedulesItem(
                  new FirmOfficeContractAndScheduleDetails()
                      .addScheduleLinesItem(
                          new FirmOfficeContractAndScheduleLine().categoryOfLaw("categoryOfLaw1")));

      when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
          .thenReturn(categoryOfLawLookup);
      when(providerDetailsRestClient.getProviderFirmSchedules(
              eq("officeCode"), eq("areaOfLaw"), any(LocalDate.class)))
          .thenReturn(Mono.just(data));

      claimValidationService.validateClaims(claims, "officeCode", "areaOfLaw");

      verify(providerDetailsRestClient, times(1))
          .getProviderFirmSchedules("officeCode", "areaOfLaw", LocalDate.parse("2025-08-14"));
      verify(providerDetailsRestClient, times(1))
          .getProviderFirmSchedules("officeCode", "areaOfLaw", LocalDate.parse("2025-05-25"));

      List<String> expectedProviderCategoriesOfLaw = List.of("categoryOfLaw1");
      verify(categoryOfLawValidationService, times(1))
          .validateCategoryOfLaw(claim1, categoryOfLawLookup, expectedProviderCategoriesOfLaw);
      verify(categoryOfLawValidationService, times(1))
          .validateCategoryOfLaw(claim2, categoryOfLawLookup, expectedProviderCategoriesOfLaw);

      verify(duplicateClaimValidationService, times(1)).validateDuplicateClaims(claim1);
      verify(duplicateClaimValidationService, times(1)).validateDuplicateClaims(claim2);

      verify(feeCalculationService, times(1)).validateFeeCalculation(claim1);
      verify(feeCalculationService, times(1)).validateFeeCalculation(claim2);
    }
  }
}
