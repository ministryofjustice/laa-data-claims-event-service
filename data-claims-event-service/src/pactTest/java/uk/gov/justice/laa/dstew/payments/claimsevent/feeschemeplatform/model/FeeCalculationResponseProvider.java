package uk.gov.justice.laa.dstew.payments.claimsevent.feeschemeplatform.model;

import java.util.List;
import java.util.UUID;
import uk.gov.justice.laa.fee.scheme.model.BoltOnFeeDetails;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculation;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationResponse;
import uk.gov.justice.laa.fee.scheme.model.ValidationMessagesInner;

public class FeeCalculationResponseProvider {

  public static FeeCalculationResponse getCalculationResponse(
      final String feeCode, final UUID claimId) {
    return FeeCalculationResponse.builder()
        .feeCode(feeCode)
        .schemeId("string")
        .claimId(claimId.toString())
        .validationMessages(
            List.of(
                ValidationMessagesInner.builder()
                    .type(ValidationMessagesInner.TypeEnum.WARNING)
                    .code("string")
                    .message("string")
                    .build()))
        .escapeCaseFlag(true)
        .feeCalculation(
            FeeCalculation.builder()
                .totalAmount(0d)
                .vatIndicator(true)
                .vatRateApplied(0d)
                .calculatedVatAmount(0d)
                .disbursementAmount(0d)
                .requestedNetDisbursementAmount(0d)
                .disbursementVatAmount(0d)
                .hourlyTotalAmount(0d)
                .fixedFeeAmount(0d)
                .netProfitCostsAmount(0d)
                .requestedNetProfitCostsAmount(0d)
                .netCostOfCounselAmount(0d)
                .netTravelCostsAmount(0d)
                .netWaitingCostsAmount(0d)
                .detentionTravelAndWaitingCostsAmount(0d)
                .jrFormFillingAmount(0d)
                .travelAndWaitingCostAmount(0d)
                .boltOnFeeDetails(
                    BoltOnFeeDetails.builder()
                        .boltOnTotalFeeAmount(0d)
                        .boltOnAdjournedHearingCount(0)
                        .boltOnAdjournedHearingFee(0d)
                        .boltOnCmrhTelephoneCount(0)
                        .boltOnCmrhTelephoneFee(0d)
                        .boltOnCmrhOralCount(0)
                        .boltOnCmrhOralFee(0d)
                        .boltOnHomeOfficeInterviewCount(0)
                        .boltOnHomeOfficeInterviewFee(0d)
                        .boltOnSubstantiveHearingFee(0d)
                        .build())
                .build())
        .build();
  }
}
