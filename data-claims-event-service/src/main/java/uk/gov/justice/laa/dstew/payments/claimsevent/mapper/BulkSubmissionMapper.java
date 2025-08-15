package uk.gov.justice.laa.dstew.payments.claimsevent.mapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionMatterStart;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionOutcome;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.CreateMatterStartRequest;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200Response;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200ResponseDetails;
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
  @Mapping(target = "scheduleNumber", source = "bulkSubmission.details.schedule.scheduleNum")
  @Mapping(target = "previousSubmissionId", ignore = true)
  @Mapping(target = "status", expression = "java(SubmissionStatus.CREATED)")
  @Mapping(
      target = "isNilSubmission",
      source = "bulkSubmission",
      qualifiedByName = "isNilSubmission")
  @Mapping(target = "numberOfClaims", source = "bulkSubmission", qualifiedByName = "countClaims")
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
  @Mapping(target = "matterTypeCode", source = "matterType")
  @Mapping(target = "crimeMatterTypeCode", source = "crimeMatterType")
  @Mapping(target = "feeSchemeCode", source = "feeScheme")
  @Mapping(target = "procurementAreaCode", source = "procurementArea")
  @Mapping(target = "accessPointCode", source = "accessPoint")
  @Mapping(target = "deliveryLocation", source = "deliveryLocation")
  @Mapping(target = "outcomeCode", source = "outcomeCode")
  ClaimPost mapToClaimPost(BulkSubmissionOutcome outcome);

  List<ClaimPost> mapToClaimPosts(List<BulkSubmissionOutcome> outcomes);

  @Mapping(target = "categoryCode", source = "categoryCode")
  @Mapping(target = "accessPointCode", source = "accessPoint")
  @Mapping(target = "deliveryLocation", source = "deliveryLocation")
  CreateMatterStartRequest mapToMatterStart(BulkSubmissionMatterStart matterStart);

  List<CreateMatterStartRequest> mapToMatterStartRequests(
      List<BulkSubmissionMatterStart> matterStarts);

  /**
   * Converts a string to an {@link Integer}, returning {@code null} for invalid values.
   *
   * @param value the string representation of an integer
   * @return parsed integer or {@code null} if parsing fails
   */
  @Named("stringToInteger")
  default Integer stringToInteger(String value) {
    try {
      return value == null ? null : Integer.parseInt(value);
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
