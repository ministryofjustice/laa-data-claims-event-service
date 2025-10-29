package uk.gov.justice.laa.dstew.payments.claimsevent.mapper;

import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.EVENT_SERVICE;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.util.StringUtils;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionAreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionMatterStart;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionOutcome;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200Response;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200ResponseDetails;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.MatterStartPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;

/** Maps bulk submission payloads into requests for the Claims Data API. */
@Mapper(
    componentModel = "spring",
    imports = {SubmissionStatus.class, ClaimStatus.class},
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BulkSubmissionMapper {

  @Mapping(target = "submissionId", source = "submissionId")
  @Mapping(target = "bulkSubmissionId", source = "bulkSubmission.bulkSubmissionId")
  @Mapping(target = "officeAccountNumber", source = "bulkSubmission.details.office.account")
  @Mapping(target = "submissionPeriod", source = "bulkSubmission.details.schedule.submissionPeriod")
  @Mapping(target = "areaOfLaw", source = "bulkSubmission.details.schedule.areaOfLaw")
  @Mapping(
      target = "crimeLowerScheduleNumber",
      source = "bulkSubmission.details.schedule.scheduleNum")
  @Mapping(target = "previousSubmissionId", ignore = true)
  @Mapping(target = "status", expression = "java(SubmissionStatus.CREATED)")
  @Mapping(
      target = "isNilSubmission",
      source = "bulkSubmission",
      qualifiedByName = "isNilSubmission")
  @Mapping(target = "numberOfClaims", source = "bulkSubmission", qualifiedByName = "countClaims")
  @Mapping(target = "providerUserId", source = "bulkSubmission.createdByUserId")
  @Mapping(target = "createdByUserId", constant = EVENT_SERVICE)
  SubmissionPost mapToSubmissionPost(
      GetBulkSubmission200Response bulkSubmission, UUID submissionId);

  /**
   * Counts the number of claim outcomes within a bulk submission payload.
   *
   * @param bulkSubmission the bulk submission to inspect
   * @return number of claim outcomes
   */
  @Named("countClaims")
  default int countClaims(GetBulkSubmission200Response bulkSubmission) {
    return Optional.ofNullable(bulkSubmission)
        .map(GetBulkSubmission200Response::getDetails)
        .map(GetBulkSubmission200ResponseDetails::getOutcomes)
        .map(List::size)
        .orElse(0);
  }

  @Named("isNilSubmission")
  default boolean isNilSubmission(GetBulkSubmission200Response bulkSubmission) {
    return countClaims(bulkSubmission) == 0;
  }

  @Mapping(target = "status", expression = "java(ClaimStatus.READY_TO_PROCESS)")
  @Mapping(target = "scheduleReference", source = "scheduleRef")
  @Mapping(target = "lineNumber", source = "lineNumber", qualifiedByName = "stringToInteger")
  @Mapping(target = "caseReferenceNumber", source = "caseRefNumber")
  @Mapping(target = "uniqueFileNumber", source = "ufn")
  @Mapping(target = "caseStartDate", source = "caseStartDate")
  @Mapping(target = "caseConcludedDate", source = "workConcludedDate")
  @Mapping(target = "representationOrderDate", source = "repOrderDate")
  @Mapping(target = "matterTypeCode", source = "matterType")
  @Mapping(target = "crimeMatterTypeCode", source = "crimeMatterType")
  @Mapping(target = "feeSchemeCode", source = "feeScheme")
  @Mapping(target = "procurementAreaCode", source = "procurementArea")
  @Mapping(target = "accessPointCode", source = "accessPoint")
  @Mapping(target = "deliveryLocation", source = "deliveryLocation")
  @Mapping(target = "outcomeCode", source = "outcomeCode")
  @Mapping(target = "designatedAccreditedRepresentativeCode", source = "desiAccRep")
  @Mapping(target = "priorAuthorityReference", source = "priorAuthorityRef")
  @Mapping(target = "mentalHealthTribunalReference", source = "mhtRefNumber")
  @Mapping(target = "exceptionalCaseFundingReference", source = "exclCaseFundingRef")
  @Mapping(target = "suspectsDefendantsCount", source = "noOfSuspects")
  @Mapping(target = "policeStationCourtAttendancesCount", source = "noOfPoliceStation")
  @Mapping(target = "policeStationCourtPrisonId", source = "policeStation")
  @Mapping(target = "dsccNumber", source = "dsccNumber")
  @Mapping(target = "maatId", source = "maatId")
  @Mapping(target = "isDutySolicitor", source = "dutySolicitor")
  @Mapping(target = "isYouthCourt", source = "youthCourt")
  @Mapping(target = "mediationSessionsCount", source = "numberOfMediationSessions")
  @Mapping(target = "mediationTimeMinutes", source = "mediationTime")
  @Mapping(target = "outreachLocation", source = "outreach")
  @Mapping(target = "referralSource", source = "referral")
  @Mapping(target = "isNrmAdvice", source = "nationalRefMechanismAdvice")
  @Mapping(target = "isLegacyCase", source = "legacyCase")
  @Mapping(target = "isToleranceApplicable", source = "toleranceIndicator")
  @Mapping(target = "isSubstantiveHearing", source = "substantiveHearing")
  @Mapping(target = "isIrcSurgery", source = "ircSurgery")
  @Mapping(target = "courtLocationCode", source = "courtLocation")
  @Mapping(target = "aitHearingCentreCode", source = "aitHearingCentre")
  @Mapping(target = "meetingsAttendedCode", source = "meetingsAttended")
  @Mapping(target = "adviceTypeCode", source = "typeOfAdvice")
  @Mapping(target = "netProfitCostsAmount", source = "profitCost")
  @Mapping(target = "netDisbursementAmount", source = "disbursementsAmount")
  @Mapping(target = "netCounselCostsAmount", source = "counselCost")
  @Mapping(target = "disbursementsVatAmount", source = "disbursementsVat")
  @Mapping(target = "travelWaitingCostsAmount", source = "travelWaitingCosts")
  @Mapping(target = "detentionTravelWaitingCostsAmount", source = "detentionTravelWaitingCosts")
  @Mapping(target = "jrFormFillingAmount", source = "jrFormFilling")
  @Mapping(target = "costsDamagesRecoveredAmount", source = "costsDamagesRecovered")
  @Mapping(target = "adjournedHearingFeeAmount", source = "adjournedHearingFee")
  @Mapping(target = "isVatApplicable", source = "vatIndicator", defaultValue = "false")
  @Mapping(target = "isLondonRate", source = "londonNonlondonRate")
  @Mapping(target = "isAdditionalTravelPayment", source = "additionalTravelPayment")
  @Mapping(target = "cmrhOralCount", source = "cmrhOral")
  @Mapping(target = "cmrhTelephoneCount", source = "cmrhTelephone")
  @Mapping(target = "prisonLawPriorApprovalNumber", source = "prisonLawPriorApproval")
  @Mapping(target = "isEligibleClient", source = "eligibleClient")
  @Mapping(target = "uniqueClientNumber", source = "ucn")
  @Mapping(target = "clientPostcode", source = "clientPostCode")
  @Mapping(target = "genderCode", source = "gender")
  @Mapping(target = "ethnicityCode", source = "ethnicity")
  @Mapping(target = "disabilityCode", source = "disability")
  @Mapping(target = "clientTypeCode", source = "clientType")
  @Mapping(target = "homeOfficeClientNumber", source = "hoUcn")
  @Mapping(target = "claReferenceNumber", source = "claRefNumber")
  @Mapping(target = "claExemptionCode", source = "claExemption")
  @Mapping(target = "isLegallyAided", source = "clientLegallyAided")
  @Mapping(target = "isPostalApplicationAccepted", source = "postalApplAccp")
  @Mapping(target = "client2Postcode", source = "client2PostCode")
  @Mapping(target = "client2GenderCode", source = "client2Gender")
  @Mapping(target = "client2EthnicityCode", source = "client2Ethnicity")
  @Mapping(target = "client2DisabilityCode", source = "client2Disability")
  @Mapping(target = "client2IsLegallyAided", source = "client2LegallyAided")
  @Mapping(target = "isClient2PostalApplicationAccepted", source = "client2PostalApplAccp")
  @Mapping(target = "stageReachedCode", source = "stageReached")
  @Mapping(target = "caseStageCode", source = "caseStageLevel")
  @Mapping(target = "standardFeeCategoryCode", source = "standardFeeCat")
  @Mapping(target = "medicalReportsCount", source = "medicalReportsClaimed")
  @Mapping(target = "surgeryClientsCount", source = "noOfClients")
  @Mapping(target = "surgeryMattersCount", source = "noOfSurgeryClients")
  @Mapping(target = "createdByUserId", constant = EVENT_SERVICE)
  ClaimPost mapToClaimPost(
      BulkSubmissionOutcome outcome, @Context BulkSubmissionAreaOfLaw areaOfLaw);

  /**
   * Adjusts the matter type and stage reached codes for crime lower claims after the initial
   * mapping. For crime lower claims, the matter type is used as the stage reached code.
   *
   * @param claimPost the target claim post object being mapped
   * @param outcome the source bulk submission outcome
   * @param areaOfLaw the area of law context for this mapping
   */
  @AfterMapping
  default void adjustMatterTypeTarget(
      @MappingTarget ClaimPost claimPost,
      BulkSubmissionOutcome outcome,
      @Context BulkSubmissionAreaOfLaw areaOfLaw) {
    if (BulkSubmissionAreaOfLaw.CRIME_LOWER.equals(areaOfLaw)) {
      claimPost.setStageReachedCode(outcome.getMatterType());
    }
  }

  List<ClaimPost> mapToClaimPosts(
      List<BulkSubmissionOutcome> outcomes, @Context BulkSubmissionAreaOfLaw areaOfLaw);

  @Mapping(target = "categoryCode", source = "categoryCode")
  @Mapping(target = "accessPointCode", source = "accessPoint")
  @Mapping(target = "deliveryLocation", source = "deliveryLocation")
  @Mapping(target = "scheduleReference", source = "scheduleRef")
  @Mapping(target = "procurementAreaCode", source = "procurementArea")
  @Mapping(target = "createdByUserId", constant = EVENT_SERVICE)
  MatterStartPost mapToMatterStart(BulkSubmissionMatterStart matterStart);

  List<MatterStartPost> mapToMatterStartRequests(List<BulkSubmissionMatterStart> matterStarts);

  /**
   * Converts a string to an {@link Integer}, returning {@code null} for invalid values.
   *
   * @param value the string representation of an integer
   * @return parsed integer or {@code null} if parsing fails
   */
  @Named("stringToInteger")
  default Integer stringToInteger(String value) {
    try {
      return !StringUtils.hasText(value) ? null : Integer.parseInt(value);
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
