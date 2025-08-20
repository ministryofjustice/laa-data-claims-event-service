package uk.gov.justice.laa.dstew.payments.claimsevent.client;

import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PostExchange;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimFields;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.CreateMatterStart201Response;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.CreateMatterStartRequest;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200Response;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetSubmission200Response;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPost;

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
  ResponseEntity<GetSubmission200Response> getSubmission(@PathVariable("id") String id);

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
   * Get a specific claim for a submission.
   *
   * @param submissionId submission UUID
   * @param claimId claim UUID
   * @return full claim details map (fields per {@code ClaimFields})
   */
  @GetExchange("/submissions/{submission-id}/claims/{claim-id}")
  ResponseEntity<ClaimFields> getClaim(
      @PathVariable("submission-id") String submissionId, @PathVariable("claim-id") String claimId);

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
      @PathVariable("submission-id") String submissionId,
      @PathVariable("claim-id") String claimId,
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
      @PathVariable("id") String submissionId, @RequestBody CreateMatterStartRequest matterStart);
}
