package uk.gov.justice.laa.bulk.claim.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.bulk.claim.converter.BulkClaimConverterFactory;
import uk.gov.justice.laa.bulk.claim.converter.BulkClaimCsvConverter;
import uk.gov.justice.laa.bulk.claim.exception.BulkClaimFileReadException;
import uk.gov.justice.laa.bulk.claim.mapper.BulkClaimSubmissionMapper;
import uk.gov.justice.laa.bulk.claim.model.BulkClaimSubmission;
import uk.gov.justice.laa.bulk.claim.model.FileExtension;
import uk.gov.justice.laa.bulk.claim.model.FileSubmission;
import uk.gov.justice.laa.bulk.claim.model.csv.CsvSubmission;

@ExtendWith(MockitoExtension.class)
public class BulkClaimServiceTests {

  @Mock BulkClaimSubmissionMapper bulkClaimSubmissionMapper;

  @Mock BulkClaimConverterFactory bulkClaimConverterFactory;

  @InjectMocks BulkClaimService bulkClaimService;

  @Nested
  @DisplayName("getBulkClaimSubmission")
  class GetBulkClaimSubmission {

    @Test
    @DisplayName("Returns a bulk claim submission")
    void returnsABulkClaimSubmission() {
      File file = new File("filePath.csv");
      FileSubmission csvSubmission = mock(CsvSubmission.class);
      BulkClaimSubmission expected = mock(BulkClaimSubmission.class);
      BulkClaimCsvConverter bulkClaimCsvConverter = mock(BulkClaimCsvConverter.class);
      when(bulkClaimConverterFactory.converterFor(FileExtension.CSV))
          .thenReturn(bulkClaimCsvConverter);
      when(bulkClaimCsvConverter.convert(any(File.class)))
          .thenReturn((CsvSubmission) csvSubmission);
      when(bulkClaimSubmissionMapper.toBulkClaimSubmission(csvSubmission)).thenReturn(expected);
      BulkClaimSubmission actual = bulkClaimService.getBulkClaimSubmission(file);

      assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Throws an exception for unsupported file extensions")
    void throwsExceptionForInvalidFileExtensions() {
      File file = new File("filePath.invalid");
      assertThrows(
          BulkClaimFileReadException.class,
          () -> bulkClaimService.getBulkClaimSubmission(file),
          "Expected BulkClaimFileReadException to be thrown");
    }
  }
}
