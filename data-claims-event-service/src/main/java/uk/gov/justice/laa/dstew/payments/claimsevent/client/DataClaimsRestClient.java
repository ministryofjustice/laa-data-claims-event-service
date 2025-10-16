package uk.gov.justice.laa.dstew.payments.claimsevent.client;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResultSet;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.CreateMatterStart201Response;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200Response;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.MatterStartPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionsResultSet;

/**
 * REST client interface for fetching claims data. This interface communicates with the Data Claims
 * API.
 */
@HttpExchange(
    value = "/api/v0",
    accept = MediaType.APPLICATION_JSON_VALUE,
    contentType = MediaType.APPLICATION_JSON_VALUE)
public interface DataClaimsRestClient {

  /**
   * Get the raw JSON document of a bulk submission.
   *
   * @param id UUID of the bulk submission
   * @return the stored JSON document as a map
   */
  @GetExchange("/bulk-submissions/{id}")
  ResponseEntity<GetBulkSubmission200Response> getBulkSubmission(@PathVariable("id") UUID id);

  /**
   * Create a new submission.
   *
   * @param submission payload shaped like {@code SubmissionPost}
   * @return 201 Created with JSON body containing the created submission {@code id}; {@code
   *     Location} header points to the created resource
   */
  @PostExchange("/submissions")
  ResponseEntity<Void> createSubmission(@RequestBody SubmissionPost submission);

  /**
   * Update (patch) an existing submission's fields (typically status).
   *
   * @param id submission UUID
   * @param submissionPatch payload shaped like {@code SubmissionPatch}
   * @return 204 No Content on success
   */
  @PatchExchange("/submissions/{id}")
  ResponseEntity<Void> updateSubmission(
      @PathVariable("id") String id, @RequestBody SubmissionPatch submissionPatch);

  /**
   * Get a submission summary by ID, including claim IDs/statuses and matter start IDs.
   *
   * @param id submission UUID
   * @return submission details map: {@code submission}, {@code claims[]}, {@code matter_starts[]}
   */
  @GetExchange("/submissions/{id}")
  ResponseEntity<SubmissionResponse> getSubmission(@PathVariable("id") UUID id);

  /**
   * Return claim submissions by office account numbers (mandatory), submissionId,
   * submittedDateFrom, submittedDateTo, areaOfLaw, submissionPeriod.
   *
   * @param offices list of office account numbers
   * @param submissionId submission UUID
   * @param submittedDateFrom submitted date from
   * @param submittedDateTo submitted date to
   * @param areaOfLaw area of law
   * @param submissionPeriod submission period
   * @param page page number
   * @param size page size
   * @param sort sort order
   * @return returns a list of paginated claim submissions (status code 200)
   */
  @GetExchange("/submissions")
  ResponseEntity<SubmissionsResultSet> getSubmissions(
      @RequestParam(value = "offices") List<String> offices,
      @RequestParam(value = "submission_id", required = false) String submissionId,
      @RequestParam(value = "submitted_date_from", required = false) LocalDate submittedDateFrom,
      @RequestParam(value = "submitted_date_to", required = false) LocalDate submittedDateTo,
      @RequestParam(value = "area_of_law", required = false) String areaOfLaw,
      @RequestParam(value = "submission_period", required = false) String submissionPeriod,
      @RequestParam(value = "page", required = false) Integer page,
      @RequestParam(value = "size", required = false) Integer size,
      @RequestParam(value = "sort", required = false) String sort);

  /**
   * Add a claim to a submission.
   *
   * @param submissionId parent submission UUID
   * @param claim payload shaped like {@code ClaimPost}
   * @return 201 Created with JSON body containing the created claim {@code id}; {@code Location}
   *     header points to the created resource
   */
  @PostExchange("/submissions/{id}/claims")
  ResponseEntity<Void> createClaim(
      @PathVariable("id") String submissionId, @RequestBody ClaimPost claim);

  /**
   * Get claims in an office, filtering on certain criteria.
   *
   * <p>Currently using Pageable within HttpExchange is not supported. This method aims to work
   * around this limitation until it has been implemented. <a
   * href="https://github.com/spring-projects/spring-data-commons/issues/3046">Issue #25899</a>
   *
   * @param officeCode the office code of the claims to be retrieved
   * @param submissionId the submission id of the claims to be retrieved
   * @param submissionStatuses the statuses of the parent submissions
   * @param feeCode the fee code of the claims to be retrieved
   * @param uniqueFileNumber the unique file number of the claims to be retrieved
   * @param uniqueClientNumber the unique client number of the claims to be retrieved
   * @param uniqueCaseId the unique case id of the retrieved claims
   * @param claimStatuses the claim statuses
   * @param pageable the pageable object containing the page number and page size
   * @return 200 OK with JSON body containing the list of matched claims
   */
  default ResponseEntity<ClaimResultSet> getClaims(
      String officeCode,
      String submissionId,
      List<SubmissionStatus> submissionStatuses,
      String feeCode,
      String uniqueFileNumber,
      String uniqueClientNumber,
      String uniqueCaseId,
      List<ClaimStatus> claimStatuses,
      Pageable pageable) {
    Integer pageNumber = Objects.nonNull(pageable) ? pageable.getPageNumber() : null;
    Integer pageSize = Objects.nonNull(pageable) ? pageable.getPageSize() : null;
    Sort sort = Objects.nonNull(pageable) ? pageable.getSort() : null;
    return this.getClaims(
        officeCode,
        submissionId,
        submissionStatuses,
        feeCode,
        uniqueFileNumber,
        uniqueClientNumber,
        uniqueCaseId,
        claimStatuses,
        pageNumber,
        pageSize,
        sort);
  }

  /**
   * Get claims in an office, filtering on certain criteria.
   *
   * @param officeCode the office code of the claims to be retrieved
   * @param submissionId the submission id of the claims to be retrieved
   * @param submissionStatuses the statuses of the parent submissions
   * @param feeCode the fee code of the claims to be retrieved
   * @param uniqueFileNumber the unique file number of the claims to be retrieved
   * @param uniqueClientNumber the unique client number of the claims to be retrieved
   * @param uniqueCaseId the unique case id of the retrieved claims
   * @param claimStatuses the claim statuses
   * @param page the page number
   * @param size the page size
   * @param sort the sort order
   * @return 200 OK with JSON body containing the list of matched claims
   */
  @GetExchange("/claims")
  ResponseEntity<ClaimResultSet> getClaims(
      @RequestParam(value = "office_code") String officeCode,
      @RequestParam(value = "submission_id", required = false) String submissionId,
      @RequestParam(value = "submission_statuses", required = false)
          List<SubmissionStatus> submissionStatuses,
      @RequestParam(value = "fee_code", required = false) String feeCode,
      @RequestParam(value = "unique_file_number", required = false) String uniqueFileNumber,
      @RequestParam(value = "unique_client_number", required = false) String uniqueClientNumber,
      @RequestParam(value = "unique_case_id", required = false) String uniqueCaseId,
      @RequestParam(value = "claim_statuses", required = false) List<ClaimStatus> claimStatuses,
      @RequestParam(value = "page", required = false) Integer page,
      @RequestParam(value = "size", required = false) Integer size,
      @RequestParam(value = "sort", required = false) Sort sort);

  /**
   * Get a specific claim for a submission.
   *
   * @param submissionId submission UUID
   * @param claimId claim UUID
   * @return full claim details map (fields per {@code ClaimResponse})
   */
  @GetExchange("/submissions/{submission-id}/claims/{claim-id}")
  ResponseEntity<ClaimResponse> getClaim(
      @PathVariable("submission-id") UUID submissionId, @PathVariable("claim-id") UUID claimId);

  /**
   * Partially update fields of a specific claim.
   *
   * @param submissionId submission UUID
   * @param claimId claim UUID
   * @param claimPatch payload shaped like {@code ClaimPatch}
   * @return 204 No Content on success
   */
  @PatchExchange("/submissions/{submission-id}/claims/{claim-id}")
  ResponseEntity<Void> updateClaim(
      @PathVariable("submission-id") UUID submissionId,
      @PathVariable("claim-id") UUID claimId,
      @RequestBody ClaimPatch claimPatch);

  /**
   * Create a Matter Start for a Submission.
   *
   * <p>OpenAPI: {@code POST /submissions/{id}/matter-starts}
   *
   * @param submissionId submission UUID
   * @param matterStart payload containing matter start details
   * @return 201 Created with JSON body containing the created matter start {@code id}; {@code
   *     Location} header points to the created resource
   */
  @PostExchange("/submissions/{id}/matter-starts")
  ResponseEntity<CreateMatterStart201Response> createMatterStart(
      @PathVariable("id") String submissionId, @RequestBody MatterStartPost matterStart);
}
