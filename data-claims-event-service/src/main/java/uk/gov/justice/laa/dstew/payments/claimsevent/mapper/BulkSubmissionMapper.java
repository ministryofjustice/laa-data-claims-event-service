package uk.gov.justice.laa.dstew.payments.claimsevent.mapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import org.mapstruct.ReportingPolicy;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkSubmissionDetails;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkSubmissionOutcome;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkSubmissionResponse;

@Mapper(componentModel = "spring",
    imports = { SubmissionStatus.class },
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BulkSubmissionMapper {

  @Mapping(target = "submissionId", source = "submissionId")
  @Mapping(target = "bulkSubmissionId", source = "bulkSubmission.id")
  @Mapping(target = "officeAccountNumber", source = "bulkSubmission.data.office.account")
  @Mapping(target = "submissionPeriod", source = "bulkSubmission.data.schedule.submissionPeriod")
  @Mapping(target = "areaOfLaw", source = "bulkSubmission.data.schedule.areaOfLaw")
  @Mapping(target = "scheduleNumber", source = "bulkSubmission.data.schedule.scheduleNum")
  @Mapping(target = "previousSubmissionId", ignore = true)
  @Mapping(target = "status", expression = "java(SubmissionStatus.CREATED)")
  @Mapping(target = "isNilSubmission", source = "bulkSubmission", qualifiedByName = "isNilSubmission")
  @Mapping(target = "numberOfClaims", source = "bulkSubmission", qualifiedByName = "countClaims")
  SubmissionPost mapToSubmissionPost(BulkSubmissionResponse bulkSubmission, UUID submissionId);

  @Named("countClaims")
  default int countClaims(BulkSubmissionResponse bulkSubmission) {
    return Optional.ofNullable(bulkSubmission)
        .map(BulkSubmissionResponse::data)
        .map(BulkSubmissionDetails::outcomes)
        .map(List::size)
        .orElse(0);
  }

  @Named("isNilSubmission")
  default boolean isNilSubmission(BulkSubmissionResponse bulkSubmission) {
    return countClaims(bulkSubmission) == 0;
  }

  List<ClaimPost> mapToClaimPosts(List<BulkSubmissionOutcome> outcomes);
}
