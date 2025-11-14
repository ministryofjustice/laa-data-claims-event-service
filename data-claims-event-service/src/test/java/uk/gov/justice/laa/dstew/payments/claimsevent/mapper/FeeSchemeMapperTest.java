package uk.gov.justice.laa.dstew.payments.claimsevent.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw.LEGAL_HELP;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.fee.scheme.model.BoltOnType;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationRequest;

class FeeSchemeMapperTest {

  FeeSchemeMapper feeSchemeMapper = new FeeSchemeMapperImpl();

  @Nested
  @DisplayName("mapToFeeCalculationRequest")
  class MapToFeeCalculationRequestTests {

    @ParameterizedTest
    @CsvSource({"LEGAL_HELP", "CRIME_LOWER"})
    @DisplayName("Maps to fee calculation request for {0} claim")
    void mapsToFeeCalculationRequest(String areaOfLawName) {
      AreaOfLaw areaOfLaw = AreaOfLaw.valueOf(areaOfLawName);
      boolean isLegalHelp = areaOfLaw == LEGAL_HELP;

      String claimId = UUID.randomUUID().toString();
      ClaimResponse claim =
          new ClaimResponse()
              .id(claimId)
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
              .isSubstantiveHearing(true)
              .isAdditionalTravelPayment(true)
              .hoInterview(5)
              .netWaitingCostsAmount(isLegalHelp ? null : BigDecimal.valueOf(1.05))
              .travelWaitingCostsAmount(BigDecimal.valueOf(1.06))
              .detentionTravelWaitingCostsAmount(BigDecimal.valueOf(1.07))
              .caseConcludedDate("2025-01-02")
              .policeStationCourtPrisonId("policeCourtOrPrisonId")
              .schemeId("schemeId")
              .uniqueFileNumber("ufn")
              .mediationSessionsCount(1)
              .jrFormFillingAmount(BigDecimal.valueOf(1.08))
              .isLondonRate(true)
              .representationOrderDate("2025-01-02");

      BoltOnType boltOnType =
          new BoltOnType()
              .boltOnAdjournedHearing(1)
              .boltOnCmrhOral(2)
              .boltOnCmrhTelephone(3)
              .boltOnHomeOfficeInterview(5)
              .boltOnSubstantiveHearing(true);

      FeeCalculationRequest expected =
          FeeCalculationRequest.builder()
              .claimId(claimId)
              .feeCode("feeCode")
              .startDate(LocalDate.parse("2025-01-01"))
              .netProfitCosts(1.00)
              .netDisbursementAmount(1.01)
              .netCostOfCounsel(1.02)
              .disbursementVatAmount(1.03)
              .vatIndicator(true)
              .immigrationPriorAuthorityNumber("disbursementPriorAuthority")
              .boltOns(boltOnType)
              .netTravelCosts(isLegalHelp ? null : 1.06)
              .netWaitingCosts(isLegalHelp ? null : 1.05)
              .travelAndWaitingCosts(isLegalHelp ? 1.06 : null)
              .detentionTravelAndWaitingCosts(1.07)
              .caseConcludedDate(LocalDate.parse("2025-01-02"))
              .policeStationId("policeCourtOrPrisonId")
              .policeStationSchemeId("schemeId")
              .uniqueFileNumber("ufn")
              .representationOrderDate(LocalDate.parse("2025-01-02"))
              .numberOfMediationSessions(1)
              .jrFormFilling(1.08)
              .londonRate(true)
              .build();

      FeeCalculationRequest actual = feeSchemeMapper.mapToFeeCalculationRequest(claim, areaOfLaw);

      assertThat(actual).isEqualTo(expected);
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
                .isAdditionalTravelPayment(true)
                .isSubstantiveHearing(true)
                .hoInterview(5);

        BoltOnType expected =
            new BoltOnType()
                .boltOnAdjournedHearing(1)
                .boltOnCmrhOral(2)
                .boltOnCmrhTelephone(3)
                .boltOnHomeOfficeInterview(5)
                .boltOnSubstantiveHearing(true);

        BoltOnType actual = feeSchemeMapper.mapToBoltOnType(claim);

        assertThat(actual).isEqualTo(expected);
      }
    }
  }
}
