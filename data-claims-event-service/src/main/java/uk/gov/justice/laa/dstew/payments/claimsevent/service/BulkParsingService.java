package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionMatterStart;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionOutcome;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200Response;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.MatterStartPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.BulkSubmissionRetrievalException;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.BulkSubmissionUpdateException;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.ClaimCreateException;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.MatterStartCreateException;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.SubmissionCreateException;
import uk.gov.justice.laa.dstew.payments.claimsevent.mapper.BulkSubmissionMapper;
import uk.gov.justice.laa.dstew.payments.claimsevent.metrics.EventServiceMetricService;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.AreaOfLaw;

/** Service responsible for retrieving bulk submissions and sending them to the Claims Data API. */
@Service
@AllArgsConstructor
@Slf4j
public class BulkParsingService {

  private final DataClaimsRestClient dataClaimsRestClient;
  private final BulkSubmissionMapper bulkSubmissionMapper;
  private final EventServiceMetricService eventServiceMetricService;

  private static final int MAX_CONCURRENCY =
      Math.max(2, Runtime.getRuntime().availableProcessors());

  /**
   * Retrieves a bulk submission by its identifier and processes it.
   *
   * @param bulkSubmissionId identifier of the bulk submission to fetch
   * @param submissionId identifier to use when creating the submission
   */
  public void parseData(UUID bulkSubmissionId, UUID submissionId) {
    GetBulkSubmission200Response bulkSubmission = getBulkSubmission(bulkSubmissionId);

    SubmissionPost submissionPost =
        bulkSubmissionMapper.mapToSubmissionPost(bulkSubmission, submissionId);

    String createdSubmissionId = createSubmission(submissionPost);

    List<BulkSubmissionOutcome> outcomes =
        bulkSubmission.getDetails() != null
            ? bulkSubmission.getDetails().getOutcomes()
            : Collections.emptyList();
    List<ClaimPost> claims =
        bulkSubmissionMapper.mapToClaimPosts(
            outcomes, AreaOfLaw.fromValue(submissionPost.getAreaOfLaw()));
    List<String> claimIds = createClaims(bulkSubmissionId.toString(), createdSubmissionId, claims);

    List<BulkSubmissionMatterStart> matterStarts =
        bulkSubmission.getDetails() != null
            ? bulkSubmission.getDetails().getMatterStarts()
            : List.of();
    List<MatterStartPost> matterStartRequests =
        bulkSubmissionMapper.mapToMatterStartRequests(matterStarts);
    createMatterStarts(bulkSubmissionId.toString(), createdSubmissionId, matterStartRequests);

    updateSubmissionStatus(createdSubmissionId, claimIds.size());
    updateBulkSubmissionStatus(bulkSubmissionId.toString(), BulkSubmissionStatus.PARSING_COMPLETED);
  }

  /**
   * Get the bulk submission data from the Data Claims service.
   *
   * @param bulkSubmissionId UUID of the bulk submission
   * @return the bulk submission payload from the Data Claims service
   * @throws
   *     uk.gov.justice.laa.dstew.payments.claimsevent.exception.BulkSubmissionRetrievalException
   *     when the bulk submission cannot be retrieved
   */
  protected GetBulkSubmission200Response getBulkSubmission(UUID bulkSubmissionId) {
    log.info("Fetching bulk submission [{}] from Data Claims service", bulkSubmissionId);

    ResponseEntity<GetBulkSubmission200Response> response =
        dataClaimsRestClient.getBulkSubmission(bulkSubmissionId);

    if (response == null
        || !response.getStatusCode().is2xxSuccessful()
        || response.getBody() == null) {
      log.warn(
          "Bulk submission [{}] could not be retrieved. Status: {}",
          bulkSubmissionId,
          response != null ? response.getStatusCode() : "null response");
      throw new BulkSubmissionRetrievalException(bulkSubmissionId);
    }

    log.debug("Bulk submission [{}] retrieved successfully", bulkSubmissionId);
    return response.getBody();
  }

  /**
   * POST the submission to the Claims Data API. This variant keeps your original signature and just
   * logs the created ID.
   */
  protected String createSubmission(SubmissionPost submission) {
    log.info(
        "Creating submission for bulk [{}], period [{}], OAN [{}]",
        submission.getBulkSubmissionId(),
        submission.getSubmissionPeriod(),
        submission.getOfficeAccountNumber());

    ResponseEntity<Void> response = dataClaimsRestClient.createSubmission(submission);

    if (response == null || response.getStatusCode().value() != 201) {
      var bulkSubmissionId = submission.getBulkSubmissionId().toString();
      log.error(
          "Failed to create submission for bulkSubmissionId [{}]. HTTP status: {}",
          bulkSubmissionId,
          response == null ? "null response" : response.getStatusCode());
      updateBulkSubmissionStatus(bulkSubmissionId, BulkSubmissionStatus.PARSING_FAILED);
      throw new SubmissionCreateException(
          "Failed to create submission. HTTP status: "
              + (response == null ? "null response" : response.getStatusCode()));
    }

    eventServiceMetricService.incrementTotalSubmissionsCreated();

    String createdId = extractIdFromLocation(response);
    if (!StringUtils.hasText(createdId)) {
      var bulkSubmissionId = submission.getBulkSubmissionId().toString();
      log.error(
          "Submission created for bulkSubmissionId [{}] but Location header missing or invalid",
          bulkSubmissionId);
      updateBulkSubmissionStatus(bulkSubmissionId, BulkSubmissionStatus.PARSING_FAILED);
      throw new SubmissionCreateException(
          "Submission created but Location header was missing or invalid");
    }

    log.debug("Created submission Location resolved to id={}", createdId);
    return createdId;
  }

  /**
   * Post multiple claims and return their created IDs in order.
   *
   * @param bulkSubmissionId string containing the id of the bulk submission
   * @param submissionId parent submission UUID
   * @param claims list of ClaimPost payloads
   * @return list of created claim UUIDs
   */
  protected List<String> createClaims(
      String bulkSubmissionId, String submissionId, List<ClaimPost> claims) {
    if (claims == null || claims.isEmpty()) {
      return Collections.emptyList();
    }

    // 1) Assign line numbers up-front
    for (int i = 0; i < claims.size(); i++) {
      ClaimPost claim = claims.get(i);
      if (claim != null && claim.getLineNumber() == null) {
        claim.setLineNumber(i + 1);
      }
    }

    // 2) Post concurrently (preserve order in results)
    ExecutorService pool = Executors.newFixedThreadPool(Math.min(claims.size(), MAX_CONCURRENCY));
    try {
      List<CompletableFuture<Result>> futures = new ArrayList<>(claims.size());

      for (int i = 0; i < claims.size(); i++) {
        final int index = i;
        final ClaimPost claim = claims.get(i);

        CompletableFuture<Result> fut =
            CompletableFuture.supplyAsync(
                () -> {
                  try {
                    String id = createClaim(submissionId, claim);
                    return new Result(index, id);
                  } catch (RuntimeException ex) {
                    String ln = (claim != null ? String.valueOf(claim.getLineNumber()) : "null");
                    updateBulkSubmissionStatus(
                        bulkSubmissionId, BulkSubmissionStatus.PARSING_FAILED);
                    throw new ClaimCreateException(
                        "Failed to create claim at index "
                            + index
                            + " (lineNumber="
                            + ln
                            + "): "
                            + ex.getMessage(),
                        ex);
                  }
                },
                pool);

        futures.add(fut);
      }

      // Wait for all to complete, fail fast if any failed
      CompletableFuture<Void> all =
          CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

      all.join();

      // Collect IDs in index order
      String[] ids = new String[claims.size()];
      for (CompletableFuture<Result> f : futures) {
        Result r = f.join();
        ids[r.index] = r.id;
      }

      log.info("Created {} claims for submission [{}]", ids.length, submissionId);
      return List.of(ids);

    } catch (CompletionException ce) {
      Throwable cause = ce.getCause();
      if (cause instanceof ClaimCreateException cce) {
        throw cce;
      }
      updateBulkSubmissionStatus(bulkSubmissionId, BulkSubmissionStatus.PARSING_FAILED);
      throw new ClaimCreateException("Failed to create claims: " + cause.getMessage(), cause);
    } finally {
      pool.shutdown();
    }
  }

  private static final class Result {
    final int index;
    final String id;

    Result(int index, String id) {
      this.index = index;
      this.id = id;
    }
  }

  /**
   * Post a claim to a submission and return the created claim ID.
   *
   * @param submissionId parent submission UUID (as a string)
   * @param claim populated ClaimPost (status MUST be set by the mapper in step 4)
   * @return created claim UUID (from Location header)
   */
  protected String createClaim(String submissionId, ClaimPost claim) {
    if (!StringUtils.hasText(submissionId)) {
      throw new ClaimCreateException("submissionId is required to create a claim");
    }
    if (claim == null) {
      throw new ClaimCreateException("claim payload is required");
    }

    log.info(
        "Creating claim for submission [{}], schedule [{}], line [{}]",
        submissionId,
        claim.getScheduleReference(),
        claim.getLineNumber());

    ResponseEntity<Void> response = dataClaimsRestClient.createClaim(submissionId, claim);

    if (response == null || response.getStatusCode() != HttpStatusCode.valueOf(201)) {
      throw new ClaimCreateException(
          "Failed to create claim for submission "
              + submissionId
              + ". HTTP status: "
              + (response == null ? "null response" : response.getStatusCode()));
    }

    eventServiceMetricService.incrementTotalClaimsCreated();

    String createdId = extractIdFromLocation(response);
    if (!StringUtils.hasText(createdId)) {
      throw new ClaimCreateException("Claim created but Location header was missing or invalid");
    }

    log.debug("Created claim Location resolved to id={}", createdId);
    return createdId;
  }

  protected List<String> createMatterStarts(
      String bulkSubmissionId, String submissionId, List<MatterStartPost> matterStarts) {
    if (matterStarts == null || matterStarts.isEmpty()) {
      return Collections.emptyList();
    }
    List<String> createdIds = new ArrayList<>(matterStarts.size());
    int index = 0;
    for (MatterStartPost ms : matterStarts) {
      try {
        createdIds.add(createMatterStart(submissionId, ms));
      } catch (RuntimeException ex) {
        updateBulkSubmissionStatus(bulkSubmissionId, BulkSubmissionStatus.PARSING_FAILED);
        throw new MatterStartCreateException(
            "Failed to create matter start at index " + index + ": " + ex.getMessage(), ex);
      } finally {
        index++;
      }
    }
    log.info("Created {} matter starts for submission [{}]", createdIds.size(), submissionId);
    return createdIds;
  }

  protected String createMatterStart(String submissionId, MatterStartPost matterStart) {
    if (!StringUtils.hasText(submissionId)) {
      throw new MatterStartCreateException("submissionId is required to create a matter start");
    }
    if (matterStart == null) {
      throw new MatterStartCreateException("matter start payload is required");
    }

    log.info(
        "Creating matter start for submission [{}], schedule [{}]",
        submissionId,
        matterStart.getScheduleReference());

    ResponseEntity<?> response = dataClaimsRestClient.createMatterStart(submissionId, matterStart);

    if (response == null || response.getStatusCode().value() != 201) {
      throw new MatterStartCreateException(
          "Failed to create matter start for submission "
              + submissionId
              + ". HTTP status: "
              + (response == null ? "null response" : response.getStatusCode()));
    }

    String createdId = extractIdFromLocation(response);
    if (!StringUtils.hasText(createdId)) {
      throw new MatterStartCreateException(
          "Matter start created but Location header was missing or invalid");
    }

    log.debug("Created matter start Location resolved to id={}", createdId);
    return createdId;
  }

  protected void updateSubmissionStatus(String submissionId, int numberOfClaims) {
    SubmissionPatch patch = new SubmissionPatch();
    patch.setStatus(SubmissionStatus.READY_FOR_VALIDATION);
    patch.setNumberOfClaims(numberOfClaims);

    ResponseEntity<Void> response = dataClaimsRestClient.updateSubmission(submissionId, patch);
    if (response == null || !response.getStatusCode().is2xxSuccessful()) {
      throw new SubmissionCreateException(
          "Failed to update submission status for submission "
              + submissionId
              + ". HTTP status: "
              + (response == null ? "null response" : response.getStatusCode()));
    }
    log.info(
        "Submission [{}] marked as READY_FOR_VALIDATION with {} claims",
        submissionId,
        numberOfClaims);
  }

  protected void updateBulkSubmissionStatus(
      String bulkSubmissionId, BulkSubmissionStatus bulkSubmissionStatus) {
    BulkSubmissionPatch patch = new BulkSubmissionPatch();
    patch.setStatus(bulkSubmissionStatus);

    ResponseEntity<Void> response =
        dataClaimsRestClient.updateBulkSubmission(bulkSubmissionId, patch);

    if (response == null || !response.getStatusCode().is2xxSuccessful()) {
      throw new BulkSubmissionUpdateException(
          "Failed to update bulk submission status for bulk submission "
              + bulkSubmissionId
              + ". HTTP status: "
              + (response == null ? "null response" : response.getStatusCode()));
    }
    log.info("Bulk submission [{}] marked as [{}]", bulkSubmissionId, bulkSubmissionStatus.name());
  }

  /**
   * Extracts the ID from the Location header of a ResponseEntity. Assumes the Location is in the
   * format "../{id}".
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
