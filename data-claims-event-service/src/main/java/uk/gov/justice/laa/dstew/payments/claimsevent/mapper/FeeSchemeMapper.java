package uk.gov.justice.laa.dstew.payments.claimsevent.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimFields;
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
  @Mapping(target = "feeCode", source = "feeCode")
  @Mapping(target = "startDate", source = "caseStartDate")
  @Mapping(target = "netProfitCosts", source = "netProfitCostsAmount")
  @Mapping(target = "netDisbursementAmount", source = "netDisbursementAmount")
  @Mapping(target = "netCostOfCounsel", source = "netCounselCostsAmount")
  @Mapping(target = "disbursementVatAmount", source = "disbursementsVatAmount")
  @Mapping(target = "vatIndicator", source = "isVatApplicable")
  @Mapping(target = "disbursementPriorAuthority", source = "priorAuthorityReference")
  @Mapping(target = "boltOns", source = "claim")
  @Mapping(
      target = "netTravelCosts",
      ignore = true) // TODO: netTravelCosts missing from ClaimFields?
  @Mapping(target = "netWaitingCosts", source = "netWaitingCostsAmount")
  @Mapping(target = "travelAndWaitingCosts", source = "travelWaitingCostsAmount")
  @Mapping(target = "detentionAndWaitingCosts", source = "detentionTravelWaitingCostsAmount")
  @Mapping(target = "caseConcludedDate", source = "caseConcludedDate")
  @Mapping(target = "policeCourtOrPrisonId", source = "policeStationCourtPrisonId")
  @Mapping(
      target = "dutySolicitor",
      source = "isDutySolicitor") // TODO: Mismatch in types String <-> Boolean
  @Mapping(target = "schemeId", source = "schemeId")
  @Mapping(target = "ufn", source = "uniqueFileNumber")
  @Mapping(target = "numberOfMediationSessions", source = "mediationSessionsCount")
  @Mapping(target = "jrFormFilling", source = "jrFormFillingAmount")
  FeeCalculationRequest mapToFeeCalculationRequest(ClaimFields claim);

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
  @Mapping(target = "boltOnCrmhTelephone", source = "cmrhTelephoneCount")
  @Mapping(target = "boltOnAdditionalTravel", ignore = true) // TODO: Missing from ClaimFields?
  @Mapping(target = "boltOnHomeOfficeInterview", source = "hoInterview")
  BoltOnType mapToBoltOnType(ClaimFields claim);
}
