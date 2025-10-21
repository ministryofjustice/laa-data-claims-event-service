package uk.gov.justice.laa.dstew.payments.claimsevent.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.fee.scheme.model.BoltOnType;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationRequest;

/** Maps submission and claim data into requests for the Fee Scheme Platform API. */
@Mapper(componentModel = "spring")
public interface FeeSchemeMapper {

  /**
   * Maps a claim to a fee calculation request to send to the Fee Scheme Platform API.
   *
   * @param claim the claim to map
   * @return a {@code FeeCalculationRequest} representing the claim
   */
  @Mapping(target = "claimId", source = "id")
  @Mapping(target = "feeCode", source = "feeCode")
  @Mapping(target = "startDate", source = "caseStartDate")
  @Mapping(target = "netProfitCosts", source = "netProfitCostsAmount")
  @Mapping(target = "netDisbursementAmount", source = "netDisbursementAmount")
  @Mapping(target = "netCostOfCounsel", source = "netCounselCostsAmount")
  @Mapping(target = "disbursementVatAmount", source = "disbursementsVatAmount")
  @Mapping(target = "vatIndicator", source = "isVatApplicable")
  @Mapping(target = "immigrationPriorAuthorityNumber", source = "priorAuthorityReference")
  @Mapping(target = "policeStationId", source = "policeStationCourtPrisonId")
  @Mapping(target = "policeStationSchemeId", source = "schemeId")
  @Mapping(target = "policeCourtOrPrisonId", source = "policeStationCourtPrisonId")
  @Mapping(target = "boltOns", source = "claim")
  @Mapping(target = "netTravelCosts", source = "travelWaitingCostsAmount")
  @Mapping(target = "netWaitingCosts", source = "netWaitingCostsAmount")
  @Mapping(target = "travelAndWaitingCosts", source = "travelWaitingCostsAmount")
  @Mapping(target = "detentionTravelAndWaitingCosts", source = "detentionTravelWaitingCostsAmount")
  @Mapping(target = "caseConcludedDate", source = "caseConcludedDate")
  @Mapping(
      target = "dutySolicitor",
      source = "isDutySolicitor") // TODO: Mismatch in types String <-> Boolean
  @Mapping(target = "schemeId", source = "schemeId")
  @Mapping(target = "uniqueFileNumber", source = "uniqueFileNumber")
  @Mapping(target = "numberOfMediationSessions", source = "mediationSessionsCount")
  @Mapping(target = "jrFormFilling", source = "jrFormFillingAmount")
  FeeCalculationRequest mapToFeeCalculationRequest(ClaimResponse claim);

  /**
   * Map claim fields to an object holding the bolt ons.
   *
   * @param claim the claim to map
   * @return a {@code BoltOnType} representing the claim
   */
  @Mapping(
      target = "boltOnAdjournedHearing",
      source = "adjournedHearingFeeAmount") // TODO: Mismatch in types Integer <-> BigDecimal
  @Mapping(target = "boltOnCmrhOral", source = "cmrhOralCount")
  @Mapping(target = "boltOnCmrhTelephone", source = "cmrhTelephoneCount")
  @Mapping(
      target = "boltOnAdditionalTravel",
      constant = "1") // this field will be dropped from the FSP side, temporarily setting it to 1.
  @Mapping(target = "boltOnHomeOfficeInterview", source = "hoInterview")
  @Mapping(target = "boltOnSubstantiveHearing", source = "isSubstantiveHearing")
  BoltOnType mapToBoltOnType(ClaimResponse claim);
}
