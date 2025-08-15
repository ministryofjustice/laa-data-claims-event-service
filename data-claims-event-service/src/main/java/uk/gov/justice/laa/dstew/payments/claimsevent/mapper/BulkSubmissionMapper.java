package uk.gov.justice.laa.dstew.payments.claimsevent.mapper;

import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkSubmissionDetails;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkSubmissionOutcome;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkSubmissionResponse;

@Mapper(componentModel = "spring", imports = { SubmissionStatus.class })
public interface BulkSubmissionMapper {


  @Mapping(target = "submissionId", source = "submissionId")
  @Mapping(target = "status", source = ".", qualifiedByName = "countClaims")
  @Named("mapToSubmissionPost")
  SubmissionPost mapToSubmissionPost(BulkSubmissionResponse bulkSubmission, UUID submissionId);


  @Named("countClaims")
  default Integer countClaims(BulkSubmissionResponse bulkSubmission) {
    Integer count = 0;

    if (bulkSubmission != null && bulkSubmission.details() != null) {
      BulkSubmissionDetails details = bulkSubmission.details();
      count += details.outcomes().size();
    }
    return count;
  }

  List<ClaimPost> mapToClaimPosts(List<BulkSubmissionOutcome> outcomes);

}