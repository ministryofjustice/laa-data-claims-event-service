package uk.gov.justice.laa.dstew.payments.claimsevent.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.fee.scheme.model.BoltOnType;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationRequest;

class FeeSchemeMapperTest {

  FeeSchemeMapper feeSchemeMapper = new FeeSchemeMapperImpl();

  @Nested
  @DisplayName("mapToFeeCalculationRequest")
  class MapToFeeCalculationRequestTests {

    @Test
    @DisplayName("Maps to fee calculation request")
    void mapsToFeeCalculationRequest() {

      ClaimResponse claim =
          new ClaimResponse()
              .feeCode("feeCode")
              .caseStartDate("2025-01-01")
              .netProfitCostsAmount(BigDecimal.valueOf(1.00))
              .netDisbursementAmount(BigDecimal.valueOf(1.01))
              .netCounselCostsAmount(BigDecimal.valueOf(1.02))
              .disbursementsVatAmount(BigDecimal.valueOf(1.03))
              .isVatApplicable(true)
              .priorAuthorityReference("disbursementPriorAuthority")
              .adjournedHearingFeeAmount(1)
              .cmrhOralCount(2)
              .cmrhTelephoneCount(3)
              // TODO: add additional travel property
              .hoInterview(5)
              // TODO: add net travel costs property
              .netWaitingCostsAmount(BigDecimal.valueOf(1.05))
              .travelWaitingCostsAmount(BigDecimal.valueOf(1.06))
              .detentionTravelWaitingCostsAmount(BigDecimal.valueOf(1.07))
              .caseConcludedDate("2025-01-02")
              .policeStationCourtPrisonId("policeCourtOrPrisonId")
              .isDutySolicitor(true)
              .schemeId("schemeId")
              .uniqueFileNumber("ufn")
              .mediationSessionsCount(1)
              .jrFormFillingAmount(BigDecimal.valueOf(1.08));

      BoltOnType boltOnType =
          new BoltOnType()
              .boltOnAdjournedHearing(1)
              .boltOnCmrhOral(2)
              .boltOnCrmhTelephone(3)
              // TODO: add additional travel property
              .boltOnAdditionalTravel(null)
              .boltOnHomeOfficeInterview(5);

      FeeCalculationRequest expected =
          FeeCalculationRequest.builder()
              .feeCode("feeCode")
              .startDate(LocalDate.parse("2025-01-01"))
              .netProfitCosts(1.00)
              .netDisbursementAmount(1.01)
              .netCostOfCounsel(1.02)
              .disbursementVatAmount(1.03)
              .vatIndicator(true)
              .disbursementPriorAuthority("disbursementPriorAuthority")
              .boltOns(boltOnType)
              // TODO: add net travel costs property
              .netTravelCosts(null)
              .netWaitingCosts(1.05)
              .travelAndWaitingCosts(1.06)
              .detentionAndWaitingCosts(1.07)
              .caseConcludedDate(LocalDate.parse("2025-01-02"))
              .policeCourtOrPrisonId("policeCourtOrPrisonId")
              .dutySolicitor("true") // TODO: Fix type
              .schemeId("schemeId")
              .ufn("ufn")
              .numberOfMediationSessions(1)
              .jrFormFilling(1.08)
              .build();

      FeeCalculationRequest actual = feeSchemeMapper.mapToFeeCalculationRequest(claim);

      assertThat(actual).isEqualTo(expected);
    }
  }

  @Nested
  @DisplayName("mapToBoltOnType")
  class MapToBoltOnTypeTests {

    @Test
    @DisplayName("Maps to bolt on type")
    void mapsToBoltOnType() {

      ClaimResponse claim =
          new ClaimResponse()
              .adjournedHearingFeeAmount(1)
              .cmrhOralCount(2)
              .cmrhTelephoneCount(3)
              // TODO: add additional travel property
              .hoInterview(5);

      BoltOnType expected =
          new BoltOnType()
              .boltOnAdjournedHearing(1)
              .boltOnCmrhOral(2)
              .boltOnCrmhTelephone(3)
              // TODO: add additional travel property
              .boltOnAdditionalTravel(null)
              .boltOnHomeOfficeInterview(5);

      BoltOnType actual = feeSchemeMapper.mapToBoltOnType(claim);

      assertThat(actual).isEqualTo(expected);
    }
  }
}
