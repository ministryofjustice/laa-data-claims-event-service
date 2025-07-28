package uk.gov.justice.laa.bulk.claim.service;

import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.justice.laa.bulk.claim.converter.BulkClaimConverterFactory;
import uk.gov.justice.laa.bulk.claim.exception.BulkClaimFileReadException;
import uk.gov.justice.laa.bulk.claim.mapper.BulkClaimSubmissionMapper;
import uk.gov.justice.laa.bulk.claim.model.BulkClaimSubmission;
import uk.gov.justice.laa.bulk.claim.model.FileExtension;
import uk.gov.justice.laa.bulk.claim.model.FileSubmission;
import uk.gov.justice.laa.bulk.claim.model.SubmissionResponse;
import uk.gov.justice.laa.bulk.claim.util.FileUtil;

/** Service responsible for handling the processing of bulk claim submission objects. */
@Service
@Slf4j
public class BulkClaimService {

  private final BulkClaimConverterFactory bulkClaimConverterFactory;
  private final BulkClaimSubmissionMapper submissionMapper;

  @Autowired
  public BulkClaimService(
      BulkClaimConverterFactory bulkClaimConverterFactory,
      BulkClaimSubmissionMapper submissionMapper) {
    this.bulkClaimConverterFactory = bulkClaimConverterFactory;
    this.submissionMapper = submissionMapper;
  }

  /**
   * Converts the provided file to a Java object based on the filename extension, then maps it to
   * and returns a {@link BulkClaimSubmission}.
   *
   * @param file the file to convert.
   * @return a {@link BulkClaimSubmission} representing the provided input file.
   */
  public BulkClaimSubmission getBulkClaimSubmission(File file) {
    FileSubmission submission = convert(file);

    return submissionMapper.toBulkClaimSubmission(submission);
  }

  /**
   * Processes a bulk claim submission from the provided multipart file and returns a response with
   * a submission ID. The method converts the multipart file into a temporary file, processes the
   * bulk claim details, and then deletes the temporary file upon completion.
   *
   * @param file the multipart file containing bulk claim data; must not be null.
   * @return a {@link SubmissionResponse} object containing the ID of the submitted claim.
   */
  public SubmissionResponse submitBulkClaim(@NotNull MultipartFile file) {
    File submissionFile = FileUtil.createTempFile(file);

    BulkClaimSubmission bulkClaimSubmission = getBulkClaimSubmission(submissionFile);

    // TODO: Submit bulk claim

    // Delete file now that the submission has been submitted.
    try {
      Files.delete(submissionFile.toPath());
    } catch (IOException e) {
      log.error(
          "Failed to delete temporary file: %s".formatted(submissionFile.getAbsolutePath()), e);
    }

    return new SubmissionResponse("12345");
  }

  private FileSubmission convert(File file) {
    FileExtension fileExtension = getFileExtension(file);

    return bulkClaimConverterFactory.converterFor(fileExtension).convert(file);
  }

  private FileExtension getFileExtension(File file) {
    String filename = file.getName();
    try {
      int index = filename.lastIndexOf('.');
      return FileExtension.valueOf(filename.substring(index + 1).toUpperCase());
    } catch (NullPointerException | IllegalArgumentException e) {
      throw new BulkClaimFileReadException(
          "Failed get file extension from filename: %s".formatted(filename), e);
    }
  }
}
