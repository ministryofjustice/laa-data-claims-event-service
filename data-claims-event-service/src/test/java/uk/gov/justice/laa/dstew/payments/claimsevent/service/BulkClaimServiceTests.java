package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.justice.laa.bulk.claim.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.dto.BulkSubmissionRequest;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.dto.BulkSubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.converter.BulkClaimConverterFactory;
import uk.gov.justice.laa.dstew.payments.claimsevent.converter.BulkClaimCsvConverter;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.BulkClaimFileReadException;
import uk.gov.justice.laa.dstew.payments.claimsevent.mapper.BulkClaimSubmissionMapper;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkClaimSubmission;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.FileExtension;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.FileSubmission;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.csv.CsvSubmission;

@ExtendWith(MockitoExtension.class)
public class BulkClaimServiceTests {

  @Mock BulkClaimSubmissionMapper bulkClaimSubmissionMapper;

  @Mock BulkClaimConverterFactory bulkClaimConverterFactory;

  @Mock ClaimsRestClient bulkClaimsSubmissionApiClient;

  @InjectMocks BulkClaimService bulkClaimService;

  @Nested
  @DisplayName("getBulkClaimSubmission")
  class GetBulkClaimSubmission {

    @Test
    @DisplayName("Returns a bulk claim submission")
    void returnsABulkClaimSubmission() {
      MultipartFile file = new MockMultipartFile("filePath.csv", new byte[0]);
      FileSubmission csvSubmission = mock(CsvSubmission.class);
      BulkClaimSubmission expected = mock(BulkClaimSubmission.class);
      BulkClaimCsvConverter bulkClaimCsvConverter = mock(BulkClaimCsvConverter.class);
      when(bulkClaimConverterFactory.converterFor(FileExtension.CSV))
          .thenReturn(bulkClaimCsvConverter);
      when(bulkClaimCsvConverter.convert(any(MockMultipartFile.class)))
          .thenReturn((CsvSubmission) csvSubmission);
      when(bulkClaimSubmissionMapper.toBulkClaimSubmission(csvSubmission)).thenReturn(expected);
      BulkClaimSubmission actual = bulkClaimService.getBulkClaimSubmission(file);

      assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Throws an exception for unsupported file extensions")
    void throwsExceptionForInvalidFileExtensions() {
      MultipartFile file = new MockMultipartFile("filePath.invalid", new byte[0]);
      assertThrows(
          BulkClaimFileReadException.class,
          () -> bulkClaimService.getBulkClaimSubmission(file),
          "Expected BulkClaimFileReadException to be thrown");
    }
  }

  @Nested
  @DisplayName("submitBulkClaim")
  class SubmitBulkClaimTests {

    @Test
    @DisplayName("Should submit a bulk claim submission")
    void shouldSubmitABulkClaimSubmission() {
      // Given
      MockMultipartFile mockMultipartFile =
          new MockMultipartFile("test-file", "test-file.csv", "text/csv", "one,two".getBytes());
      String userId = "12345";
      BulkClaimCsvConverter mockBulkClaimCsvConverter = mock(BulkClaimCsvConverter.class);
      when(bulkClaimConverterFactory.converterFor(FileExtension.CSV))
          .thenReturn(mockBulkClaimCsvConverter);
      when(bulkClaimsSubmissionApiClient.submitBulkClaim(any(BulkSubmissionRequest.class)))
          .thenReturn(new BulkSubmissionResponse("789"));

      // When
      SubmissionResponse result = bulkClaimService.submitBulkClaim(userId, mockMultipartFile);
      // Then
      assertThat(result.getSubmissionId()).isEqualTo("789");
    }
  }
}
