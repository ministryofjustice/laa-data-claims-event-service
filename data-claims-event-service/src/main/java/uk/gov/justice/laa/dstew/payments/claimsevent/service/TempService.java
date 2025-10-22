package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionMatterStart;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionOutcome;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200Response;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.MatterStartPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPost;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.mapper.BulkSubmissionMapper;
import uk.gov.justice.laa.dstew.payments.claimsevent.metrics.EventServiceMetricService;

/** Temporary service used to manually trigger parsing of a bulk submission. */
@Service
@Slf4j
public class TempService extends BulkParsingService {

  private final BulkSubmissionMapper bulkSubmissionMapper;

  /**
   * Construct temporary service.
   *
   * @param dataClaimsRestClient the data claims rest client
   * @param bulkSubmissionMapper the bulk submission mapper
   */
  public TempService(
      final DataClaimsRestClient dataClaimsRestClient,
      final BulkSubmissionMapper bulkSubmissionMapper,
      final EventServiceMetricService eventServiceMetricService) {
    super(dataClaimsRestClient, bulkSubmissionMapper, eventServiceMetricService);
    this.bulkSubmissionMapper = bulkSubmissionMapper;
  }

  /**
   * Processes a provided bulk submission payload without retrieving it from the remote service.
   *
   * @param bulkSubmission the bulk submission to process
   * @param submissionId identifier to use when creating the submission
   */
  public void parseData(GetBulkSubmission200Response bulkSubmission, UUID submissionId) {
    SubmissionPost submissionPost =
        bulkSubmissionMapper.mapToSubmissionPost(bulkSubmission, submissionId);

    String createdSubmissionId = createSubmission(submissionPost);

    List<BulkSubmissionOutcome> outcomes =
        bulkSubmission.getDetails() != null ? bulkSubmission.getDetails().getOutcomes() : List.of();
    List<ClaimPost> claims = bulkSubmissionMapper.mapToClaimPosts(outcomes);
    assert bulkSubmission.getBulkSubmissionId() != null;
    List<String> claimIds =
        createClaims(bulkSubmission.getBulkSubmissionId().toString(), createdSubmissionId, claims);

    List<BulkSubmissionMatterStart> matterStarts =
        bulkSubmission.getDetails() != null
            ? bulkSubmission.getDetails().getMatterStarts()
            : List.of();
    List<MatterStartPost> matterStartRequests =
        bulkSubmissionMapper.mapToMatterStartRequests(matterStarts);
    createMatterStarts(
        bulkSubmission.getBulkSubmissionId().toString(), createdSubmissionId, matterStartRequests);

    updateSubmissionStatus(createdSubmissionId, claimIds.size());
    updateBulkSubmissionStatus(
        bulkSubmission.getBulkSubmissionId().toString(), BulkSubmissionStatus.PARSING_COMPLETED);
  }
}
