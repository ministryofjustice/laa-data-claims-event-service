package uk.gov.justice.laa.bulk.claim.controller;

import java.io.File;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.justice.laa.bulk.claim.api.SubmissionsApi;
import uk.gov.justice.laa.bulk.claim.exception.BulkClaimFileReadException;
import uk.gov.justice.laa.bulk.claim.model.BulkClaimSubmission;
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
  public ResponseEntity<SubmissionResponse> postSubmission(MultipartFile file) {
    // Validate file
    bulkClaimFileValidator.validate(file);

    if (file != null) {
      log.info(
          "Processing file submission - filename: {}, size: {} bytes",
          file.getOriginalFilename(), file.getSize());
    }
    try {
      File tempFile = File.createTempFile("upload-", file.getOriginalFilename());
      file.transferTo(tempFile);
      BulkClaimSubmission bulkClaimSubmission = bulkClaimService.getBulkClaimSubmission(tempFile);
      tempFile.deleteOnExit();
    } catch (IOException e) {
      throw new BulkClaimFileReadException("Could not open file", e);
    }
    return null;
  }
}
