package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPost;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.BulkSubmissionNotFoundException;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.ClaimCreateException;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.SubmissionCreateException;
import uk.gov.justice.laa.dstew.payments.claimsevent.mapper.BulkSubmissionMapper;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkSubmissionMatterStarts;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkSubmissionOutcome;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkSubmissionResponse;

@Service
@AllArgsConstructor
@Slf4j
public class BulkParsingService {

  private final DataClaimsRestClient dataClaimsRestClient;
  private final BulkSubmissionMapper bulkSubmissionMapper;

  //todo: remove this method when the service is fully implemented
  public void parseData(BulkSubmissionResponse bulkSubmission, UUID submissionId) {
    SubmissionPost submissionPost =
        bulkSubmissionMapper.mapToSubmissionPost(bulkSubmission, submissionId);

    createSubmission(submissionPost);
  }

  public void ParseData(UUID bulkSubmissionId, UUID submissionId) {
    // Step 1: Get the bulk submission data
    BulkSubmissionResponse bulkSubmission = getBulkSubmission(bulkSubmissionId);

    // Step 2: Map the bulk submission to a post-requests
    SubmissionPost submissionPost =
        bulkSubmissionMapper.mapToSubmissionPost(bulkSubmission, submissionId);

    // Step 3: Create the submission in the Claims Data API
    createSubmission(submissionPost);

    //todo null check
    List<BulkSubmissionOutcome> outcomes = bulkSubmission.data().outcomes();

    // Step 4: Map each outcome to a claim object
    List<ClaimPost> claims = bulkSubmissionMapper.mapToClaimPosts(outcomes);

    // Step 5: Post all claims and return their IDs
//    return createClaims(submissionId.toString(), claims);

    //todo null check
    List<BulkSubmissionMatterStarts> matterStarts = bulkSubmission.data().matterStarts();

    // Step 6: Map each matter-start to a matter-start object

    // Step 7: Post each matter-start to the submission

    // Step 8: Update the submission status to 'READY_FOR_VALIDATION' and set total claims

    // Step 9: Acknowledge the message (if applicable, e.g., in a messaging system)
  }

  /**
   * Step 1: Get the bulk submission data from the Data Claims service.
   *
   * @param bulkSubmissionId UUID of the bulk submission
   * @return the bulk submission payload from the Data Claims service
   * @throws BulkSubmissionNotFoundException when the bulk submission cannot be retrieved
   */
  //todo need to be change from Map<String, Object> to BulkSubmission
  protected BulkSubmissionResponse getBulkSubmission(UUID bulkSubmissionId) {
    log.info("Fetching bulk submission [{}] from Data Claims service", bulkSubmissionId);

    ResponseEntity<BulkSubmissionResponse> response =
        dataClaimsRestClient.getBulkSubmission(bulkSubmissionId);

    if (response == null || !response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      log.warn("Bulk submission [{}] could not be retrieved. Status: {}",
          bulkSubmissionId, response != null ? response.getStatusCode() : "null response");
      throw new BulkSubmissionNotFoundException(bulkSubmissionId);
    }

    log.debug("Bulk submission [{}] retrieved successfully", bulkSubmissionId);
    return response.getBody();
  }


  //2. todo use mapstruct mapper to map data form bulk submission to submission
  // calculate total amount of claims
  // need bulkSubmission get object


  /**
   * Step 3: POST the submission to the Claims Data API.
   * This variant keeps your original signature and just logs the created ID.
   */
  protected void createSubmission(SubmissionPost submission) {
    log.info("Creating submission for bulk [{}], period [{}], OAN [{}]",
        submission.getBulkSubmissionId(),
        submission.getSubmissionPeriod(),
        submission.getOfficeAccountNumber());

    ResponseEntity<Void> response = dataClaimsRestClient.createSubmission(submission);

    if (response == null || response.getStatusCode().value() != 201) {
      throw new SubmissionCreateException(
          "Failed to create submission. HTTP status: " +
              (response == null ? "null response" : response.getStatusCode()));
    }

    String createdId = extractIdFromLocation(response);
    if (createdId == null || createdId.isBlank()) {
      throw new SubmissionCreateException("Submission created but Location header was missing or invalid");
    }

    log.debug("Created submission Location resolved to id={}", createdId);
  }


  //4. for each outcome in the bulk submission, map it to a claim object

  /**
   * Step 5: Post multiple claims and return their created IDs in order.
   *
   * @param submissionId parent submission UUID
   * @param claims       list of ClaimPost payloads
   * @return list of created claim UUIDs
   */
  protected List<String> createClaims(String submissionId, List<ClaimPost> claims) {
    if (claims == null || claims.isEmpty()) {
      return List.of();
    }
    List<String> createdIds = new ArrayList<>(claims.size());
    int index = 0;
    for (ClaimPost claim : claims) {
      try {
        createdIds.add(createClaim(submissionId, claim));
      } catch (RuntimeException ex) {
        throw new ClaimCreateException("Failed to create claim at index " + index +
            " (lineNumber=" + (claim != null ? claim.getLineNumber() : "null") + "): " + ex.getMessage(), ex);
      } finally {
        index++;
      }
    }
    log.info("Created {} claims for submission [{}]", createdIds.size(), submissionId);
    return createdIds;
  }

  /**
   * Step 5: Post a claim to a submission and return the created claim ID.
   *
   * @param submissionId parent submission UUID (as a string)
   * @param claim        populated ClaimPost (status MUST be set by the mapper in step 4)
   * @return created claim UUID (from Location header)
   */
  protected String createClaim(String submissionId, ClaimPost claim) {
    if (submissionId == null || submissionId.isBlank()) {
      throw new ClaimCreateException("submissionId is required to create a claim");
    }
    if (claim == null) {
      throw new ClaimCreateException("claim payload is required");
    }

    log.info("Creating claim for submission [{}], schedule [{}], line [{}]",
        submissionId, claim.getScheduleReference(), claim.getLineNumber());

    ResponseEntity<Void> response = dataClaimsRestClient.createClaim(submissionId, claim);

    if (response == null || response.getStatusCode() != HttpStatusCode.valueOf(201)) {
      throw new ClaimCreateException(
          "Failed to create claim for submission " + submissionId +
              ". HTTP status: " + (response == null ? "null response" : response.getStatusCode()));
    }

    String createdId = extractIdFromLocation(response);
    if (createdId == null || createdId.isBlank()) {
      throw new ClaimCreateException("Claim created but Location header was missing or invalid");
    }

    log.debug("Created claim Location resolved to id={}", createdId);
    return createdId;
  }


  // 6. for each matter-start in the bulk submission, map it to a matter-start object

  // 7. post the matter-start to the submission

  // 8. when all claims and matter-starts are posted, update the submission status to 'READY_FOR_VALIDATION'
  //and update the total amount of claims in the submission

  // 9. acknowledge the message


  /**
   * Extracts the ID from the Location header of a ResponseEntity.
   * Assumes the Location is in the format "../{id}".
   *
   * @param response ResponseEntity containing the Location header
   * @return extracted ID as a String, or null if not found
   */
  private String extractIdFromLocation(ResponseEntity<?> response) {
    URI location = response.getHeaders().getLocation();
    if (location == null) {
      return null;
    }

    String path = location.getPath();
    if (path == null || !path.contains("/")) {
      return null;
    }
    String id = path.substring(path.lastIndexOf('/') + 1);
    return id.isBlank() ? null : id;
  }


}
