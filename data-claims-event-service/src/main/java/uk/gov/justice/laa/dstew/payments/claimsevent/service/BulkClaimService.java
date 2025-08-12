package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.justice.laa.bulk.claim.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.converter.BulkClaimConverterFactory;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.BulkClaimFileReadException;
import uk.gov.justice.laa.dstew.payments.claimsevent.mapper.BulkClaimSubmissionMapper;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkClaimSubmission;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.FileExtension;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.FileSubmission;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.dto.BulkSubmissionRequest;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.dto.BulkSubmissionResponse;

/** Service responsible for handling the processing of bulk claim submission objects. */
@Service
@Slf4j
@RequiredArgsConstructor
public class BulkClaimService {

  private final BulkClaimConverterFactory bulkClaimConverterFactory;
  private final BulkClaimSubmissionMapper submissionMapper;
  private final ClaimsRestService bulkClaimsSubmissionApiClient;

  /**
   * Converts the provided file to a Java object based on the filename extension, then maps it to
   * and returns a {@link BulkClaimSubmission}.
   *
   * @param file the file to convert.
   * @return a {@link BulkClaimSubmission} representing the provided input file.
   */
  public BulkClaimSubmission getBulkClaimSubmission(MultipartFile file) {
    FileSubmission submission = convert(file);

    return submissionMapper.toBulkClaimSubmission(submission);
  }

  /**
   * Processes a bulk claim submission from the provided multipart file and returns a response with
   * a submission ID.
   *
   * @param file the multipart file containing bulk claim data; must not be null.
   * @return a {@link SubmissionResponse} object containing the ID of the submitted claim.
   */
  public SubmissionResponse submitBulkClaim(@NotNull String userId, @NotNull MultipartFile file) {

    BulkClaimSubmission bulkClaimSubmission = getBulkClaimSubmission(file);

    BulkSubmissionRequest bulkSubmissionRequest =
        new BulkSubmissionRequest(
            userId, new HashMap<>(), Collections.singletonList(bulkClaimSubmission));
    BulkSubmissionResponse bulkSubmissionResponse =
        bulkClaimsSubmissionApiClient.submitBulkClaim(bulkSubmissionRequest);

    return new SubmissionResponse(bulkSubmissionResponse.submissionId());
  }

  private FileSubmission convert(MultipartFile file) {
    FileExtension fileExtension = getFileExtension(file);

    return bulkClaimConverterFactory.converterFor(fileExtension).convert(file);
  }

  private FileExtension getFileExtension(MultipartFile file) {
    String filename =
        !StringUtils.hasText(file.getOriginalFilename())
            ? file.getName()
            : file.getOriginalFilename();
    try {
      int index = filename.lastIndexOf('.');
      return FileExtension.valueOf(filename.substring(index + 1).toUpperCase());
    } catch (NullPointerException | IllegalArgumentException e) {
      throw new BulkClaimFileReadException(
          "Failed get file extension from filename: %s".formatted(filename), e);
    }
  }
}
