package uk.gov.justice.laa.bulk.claim.controller;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.justice.laa.bulk.claim.api.SubmissionsApi;
import uk.gov.justice.laa.bulk.claim.model.SubmissionResponse;
import uk.gov.justice.laa.bulk.claim.service.BulkClaimService;
import uk.gov.justice.laa.bulk.claim.validator.BulkClaimFileValidator;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SubmissionController implements SubmissionsApi {

  private final BulkClaimService bulkClaimService;
  private final BulkClaimFileValidator bulkClaimFileValidator;

  @Override
  public ResponseEntity<SubmissionResponse> postSubmission(@NotNull MultipartFile file) {
    // Validate file
    bulkClaimFileValidator.validate(file);

    // Submit bulk claim
    SubmissionResponse submissionResponse = bulkClaimService.submitBulkClaim(file);

    // Return response entity
    return ResponseEntity.ok(submissionResponse);
  }


}
