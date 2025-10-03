package uk.gov.justice.laa.dstew.payments.claimsevent.mapper;

import java.math.BigDecimal;
import java.util.UUID;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.FeeCalculationPatch;
import uk.gov.justice.laa.fee.scheme.model.BoltOnFeeDetails;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculation;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationResponse;

@DisplayName("Fee calculation patch mapper test")
class FeeCalculationPatchMapperTest {

  FeeCalculationPatchMapper mapper = new FeeCalculationPatchMapperImpl();

  @Test
  @DisplayName("Should map to fee calculation patch from fee calculation response")
  void shouldMapToFeeCalculationPatchFromFeeCalculationResponse() {
    // Given
    BoltOnFeeDetails boltOnFeeDetails =
        new BoltOnFeeDetails()
            .boltOnTotalFeeAmount(100.0)
            .boltOnAdjournedHearingCount(1)
            .boltOnAdjournedHearingFee(101.01)
            .boltOnCmrhTelephoneCount(2)
            .boltOnCmrhTelephoneFee(102.02)
            .boltOnCmrhOralCount(3)
            .boltOnCmrhOralFee(103.03)
            .boltOnHomeOfficeInterviewCount(4)
            .boltOnHomeOfficeInterviewFee(104.04);

    FeeCalculation feeCalculationRequest =
        new FeeCalculation()
            .totalAmount(100.0)
            .vatIndicator(true)
            .vatRateApplied(101.01)
            .calculatedVatAmount(102.02)
            .disbursementAmount(103.03)
            .requestedNetDisbursementAmount(104.04)
            .disbursementVatAmount(105.05)
            .hourlyTotalAmount(106.06)
            .fixedFeeAmount(107.07)
            .netProfitCostsAmount(108.08)
            .requestedNetProfitCostsAmount(109.09)
            .netCostOfCounselAmount(110.10)
            .netTravelCostsAmount(111.11)
            .netWaitingCosts(112.12)
            .detentionAndWaitingCostsAmount(113.13)
            .jrFormFillingAmount(114.14)
            .boltOnFeeDetails(boltOnFeeDetails);

    FeeCalculationResponse feeCalculationResponse =
        new FeeCalculationResponse()
            .feeCode("feeCode")
            .schemeId("schemeId")
            .claimId(new UUID(1, 1).toString())
            .escapeCaseFlag(true)
            .feeCalculation(feeCalculationRequest);
    // When
    FeeCalculationPatch result = mapper.mapToFeeCalculationPatch(feeCalculationResponse);
    // Then
    SoftAssertions.assertSoftly(
        softAssertions -> {
          // Check details from FeeCalculationResponse has mapped
          softAssertions.assertThat(result.getFeeCode()).isEqualTo("feeCode");
          softAssertions.assertThat(result.getClaimId()).isEqualTo(new UUID(1, 1));
          softAssertions.assertThat(result.getBoltOnDetails().getEscapeCaseFlag()).isEqualTo(true);
          softAssertions.assertThat(result.getBoltOnDetails().getSchemeId()).isEqualTo("schemeId");

          // Check details from FeeCalculation has mapped
          softAssertions.assertThat(result.getTotalAmount()).isEqualTo(BigDecimal.valueOf(100.0));
          softAssertions.assertThat(result.getVatIndicator()).isEqualTo(true);
          softAssertions
              .assertThat(result.getVatRateApplied())
              .isEqualTo(BigDecimal.valueOf(101.01));
          softAssertions
              .assertThat(result.getCalculatedVatAmount())
              .isEqualTo(BigDecimal.valueOf(102.02));
          softAssertions
              .assertThat(result.getDisbursementAmount())
              .isEqualTo(BigDecimal.valueOf(103.03));
          softAssertions
              .assertThat(result.getRequestedNetDisbursementAmount())
              .isEqualTo(BigDecimal.valueOf(104.04));
          softAssertions
              .assertThat(result.getDisbursementVatAmount())
              .isEqualTo(BigDecimal.valueOf(105.05));
          softAssertions
              .assertThat(result.getHourlyTotalAmount())
              .isEqualTo(BigDecimal.valueOf(106.06));
          softAssertions
              .assertThat(result.getFixedFeeAmount())
              .isEqualTo(BigDecimal.valueOf(107.07));
          softAssertions
              .assertThat(result.getNetProfitCostsAmount())
              .isEqualTo(BigDecimal.valueOf(108.08));
          softAssertions
              .assertThat(result.getRequestedNetProfitCostsAmount())
              .isEqualTo(BigDecimal.valueOf(109.09));
          softAssertions
              .assertThat(result.getNetCostOfCounselAmount())
              .isEqualTo(BigDecimal.valueOf(110.10));
          softAssertions
              .assertThat(result.getNetTravelCostsAmount())
              .isEqualTo(BigDecimal.valueOf(111.11));
          softAssertions
              .assertThat(result.getNetWaitingCostsAmount())
              .isEqualTo(BigDecimal.valueOf(112.12));
          softAssertions
              .assertThat(result.getDetentionAndWaitingCostsAmount())
              .isEqualTo(BigDecimal.valueOf(113.13));
          softAssertions
              .assertThat(result.getJrFormFillingAmount())
              .isEqualTo(BigDecimal.valueOf(114.14));

          // Check details from BoltOnFeeDetails has mapped
          softAssertions
              .assertThat(result.getBoltOnDetails().getBoltOnTotalFeeAmount())
              .isEqualTo(BigDecimal.valueOf(100.0));
          softAssertions
              .assertThat(result.getBoltOnDetails().getBoltOnAdjournedHearingCount())
              .isEqualTo(1);
          softAssertions
              .assertThat(result.getBoltOnDetails().getBoltOnAdjournedHearingFee())
              .isEqualTo(BigDecimal.valueOf(101.01));
          softAssertions
              .assertThat(result.getBoltOnDetails().getBoltOnCmrhTelephoneCount())
              .isEqualTo(2);
          softAssertions
              .assertThat(result.getBoltOnDetails().getBoltOnCmrhTelephoneFee())
              .isEqualTo(BigDecimal.valueOf(102.02));
          softAssertions
              .assertThat(result.getBoltOnDetails().getBoltOnCmrhOralCount())
              .isEqualTo(3);
          softAssertions
              .assertThat(result.getBoltOnDetails().getBoltOnCmrhOralFee())
              .isEqualTo(BigDecimal.valueOf(103.03));
          softAssertions
              .assertThat(result.getBoltOnDetails().getBoltOnHomeOfficeInterviewCount())
              .isEqualTo(4);
          softAssertions
              .assertThat(result.getBoltOnDetails().getBoltOnHomeOfficeInterviewFee())
              .isEqualTo(BigDecimal.valueOf(104.04));
        });
  }
}
