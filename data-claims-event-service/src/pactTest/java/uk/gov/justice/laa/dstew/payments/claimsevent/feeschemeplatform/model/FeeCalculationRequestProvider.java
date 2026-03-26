package uk.gov.justice.laa.dstew.payments.claimsevent.feeschemeplatform.model;

import java.time.LocalDate;
import java.util.UUID;
import uk.gov.justice.laa.fee.scheme.model.BoltOnType;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationRequest;

public class FeeCalculationRequestProvider {

  public static FeeCalculationRequest getCalculationRequest(
      final String feeCode, final UUID claimId) {
    return FeeCalculationRequest.builder()
        .feeCode(feeCode)
        .claimId(claimId.toString())
        .startDate(LocalDate.of(2026, 3, 25))
        .policeStationId("string")
        .policeStationSchemeId("string")
        .uniqueFileNumber("string")
        .netProfitCosts(0d)
        .netCostOfCounsel(0d)
        .netDisbursementAmount(0d)
        .disbursementVatAmount(0d)
        .vatIndicator(true)
        .netTravelCosts(0d)
        .netWaitingCosts(0d)
        .travelAndWaitingCosts(0d)
        .detentionTravelAndWaitingCosts(0d)
        .caseConcludedDate(LocalDate.of(2026, 3, 25))
        .numberOfMediationSessions(0)
        .jrFormFilling(0d)
        .immigrationPriorAuthorityNumber("string")
        .representationOrderDate(LocalDate.of(2026, 3, 25))
        .londonRate(true)
        .boltOns(
            BoltOnType.builder()
                .boltOnAdjournedHearing(0)
                .boltOnCmrhOral(0)
                .boltOnCmrhTelephone(0)
                .boltOnHomeOfficeInterview(0)
                .boltOnSubstantiveHearing(true)
                .build())
        .build();
  }
}
