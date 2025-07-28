package uk.gov.justice.laa.bulk.claim.controller;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.gov.justice.laa.bulk.claim.api.SubmissionsApi;
import uk.gov.justice.laa.bulk.claim.model.SubmissionResponse;
import uk.gov.justice.laa.bulk.claim.service.BulkClaimService;
import uk.gov.justice.laa.bulk.claim.validator.BulkClaimFileValidator;

/**
 * Controller that handles submissions for bulk claims. This REST API controller provides an
 * endpoint to process bulk claim files in CSV or XML format, validate their structure, and submit
 * them for further processing by the Claims API.
 *
 * @author Jamie Briggs
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class SubmissionController implements SubmissionsApi {

  private final BulkClaimService bulkClaimService;
  private final BulkClaimFileValidator bulkClaimFileValidator;

  /**
   * Submits a bulk claim file for further processing by the Claims API.
   *
   * @param file The submission file in CSV or XML format.
   * @return A response entity containing the ID of the submitted claim.
   */
  @Override
  public ResponseEntity<SubmissionResponse> postSubmission(@NotNull MultipartFile file) {
    // Validate file
    bulkClaimFileValidator.validate(file);

    // Submit bulk claim
    SubmissionResponse submissionResponse = bulkClaimService.submitBulkClaim(file);
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(submissionResponse.getSubmissionId())
            .toUri();

    // Return response entity
    return ResponseEntity.created(location).body(submissionResponse);
  }
}
