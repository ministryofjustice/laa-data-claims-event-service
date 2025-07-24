package uk.gov.justice.laa.bulk.claim.service;

import java.io.File;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.bulk.claim.converter.BulkClaimConverterFactory;
import uk.gov.justice.laa.bulk.claim.exception.BulkClaimFileReadException;
import uk.gov.justice.laa.bulk.claim.mapper.BulkClaimSubmissionMapper;
import uk.gov.justice.laa.bulk.claim.model.BulkClaimSubmission;
import uk.gov.justice.laa.bulk.claim.model.FileExtension;
import uk.gov.justice.laa.bulk.claim.model.FileSubmission;

/** Service responsible for handling the processing of bulk claim submission objects. */
@Service
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
