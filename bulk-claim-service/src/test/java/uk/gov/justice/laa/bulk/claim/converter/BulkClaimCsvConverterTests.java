package uk.gov.justice.laa.bulk.claim.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.util.ResourceUtils.getFile;
import static uk.gov.justice.laa.bulk.claim.converter.ConverterTestUtils.getContent;
import static uk.gov.justice.laa.bulk.claim.converter.ConverterTestUtils.getMultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.justice.laa.bulk.claim.exception.BulkClaimFileReadException;
import uk.gov.justice.laa.bulk.claim.model.FileExtension;
import uk.gov.justice.laa.bulk.claim.model.csv.CsvSubmission;

public class BulkClaimCsvConverterTests {

  ObjectMapper objectMapper;

  BulkClaimCsvConverter bulkClaimCsvConverter;

  private static final String OUTCOMES_INPUT_FILE = "classpath:test_upload_files/csv/outcomes.csv";
  private static final String OUTCOMES_CONVERTED_FILE =
      "classpath:test_upload_files/csv/outcomes_converted.json";

  private static final String MATTERSTARTS_INPUT_FILE =
      "classpath:test_upload_files/csv/matterstarts.csv";
  private static final String MATTERSTARTS_CONVERTED_FILE =
      "classpath:test_upload_files/csv/matterstarts_converted.json";

  private static final String ALL_TYPES_INPUT_FILE =
      "classpath:test_upload_files/csv/outcomes_and_matterstarts.csv";
  private static final String ALL_TYPES_CONVERTED_FILE =
      "classpath:test_upload_files/csv/outcomes_and_matterstarts_converted.json";

  private static final String NIL_SUBMISSION_INPUT_FILE =
      "classpath:test_upload_files/csv/nil_submission.csv";
  private static final String NIL_SUBMISSION_CONVERTED_FILE =
      "classpath:test_upload_files/csv/nil_submission_converted.json";

  private static final String MISSING_OFFICE_INPUT_FILE =
      "classpath:test_upload_files/csv/missing_office.csv";

  private static final String MISSING_SCHEDULE_INPUT_FILE =
      "classpath:test_upload_files/csv/missing_schedule.csv";

  private static final String DUPLICATE_OFFICE_INPUT_FILE =
      "classpath:test_upload_files/csv/duplicate_office.csv";

  private static final String DUPLICATE_SCHEDULE_INPUT_FILE =
      "classpath:test_upload_files/csv/duplicate_schedule.csv";

  @BeforeEach
  public void init() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    CsvMapper csvMapper = new CsvMapper();
    bulkClaimCsvConverter = new BulkClaimCsvConverter(objectMapper, csvMapper);
  }

  @Nested
  @DisplayName("convert")
  class Convert {

    @Test
    @DisplayName("Can convert a bulk claim file with outcomes to csv submission")
    void canConvertOutcomesToCsvSubmission() throws IOException {
      runTest(OUTCOMES_INPUT_FILE, OUTCOMES_CONVERTED_FILE);
    }

    @Test
    @DisplayName("Can convert a bulk claim file with matterstarts to csv submission")
    void canConvertMatterStartsToCsvSubmission() throws IOException {
      runTest(MATTERSTARTS_INPUT_FILE, MATTERSTARTS_CONVERTED_FILE);
    }

    @Test
    @DisplayName("Can convert a bulk claim file with outcomes and matterstarts to csv submission")
    void canConvertAllTypesToCsvSubmission() throws IOException {
      runTest(ALL_TYPES_INPUT_FILE, ALL_TYPES_CONVERTED_FILE);
    }

    @Test
    @DisplayName("Can convert a bulk claim file with nil submission (office and schedule only)")
    void canConvertNilSubmission() throws IOException {
      runTest(NIL_SUBMISSION_INPUT_FILE, NIL_SUBMISSION_CONVERTED_FILE);
    }

    @Test
    @DisplayName("Throws exception when office is missing")
    void throwsExceptionWhenOfficeMissing() throws IOException {
      MultipartFile file = getMultipartFile(MISSING_OFFICE_INPUT_FILE);
      assertThrows(
          BulkClaimFileReadException.class,
          () -> bulkClaimCsvConverter.convert(file),
          "Expected exception to be thrown when office is missing");
    }

    @Test
    @DisplayName("Throws exception when schedule is missing")
    void throwsExceptionWhenScheduleMissing() throws IOException {
      MultipartFile file = getMultipartFile(MISSING_SCHEDULE_INPUT_FILE);
      assertThrows(
          BulkClaimFileReadException.class,
          () -> bulkClaimCsvConverter.convert(file),
          "Expected exception to be thrown when schedule is missing");
    }

    @Test
    @DisplayName("Throws exception when multiples offices found")
    void throwsExceptionForMultipleOffices() throws IOException {
      MultipartFile file = getMultipartFile(DUPLICATE_OFFICE_INPUT_FILE);
      assertThrows(
          BulkClaimFileReadException.class,
          () -> bulkClaimCsvConverter.convert(file),
          "Expected exception to be thrown when multiple offices found");
    }

    @Test
    @DisplayName("Throws exception when multuple schedules found")
    void throwsExceptionForMultipleSchedules() throws IOException {
      MultipartFile file = getMultipartFile(DUPLICATE_SCHEDULE_INPUT_FILE);
      assertThrows(
          BulkClaimFileReadException.class,
          () -> bulkClaimCsvConverter.convert(file),
          "Expected exception to be thrown when multiple schedules found");
    }
  }

  private void runTest(String inputFileName, String outputFileName) throws IOException {
    MultipartFile file = getMultipartFile(inputFileName);
    CsvSubmission bulkClaimSubmission = bulkClaimCsvConverter.convert(file);
    String actual = objectMapper.writeValueAsString(bulkClaimSubmission);

    File convertedFile = getFile(outputFileName);
    String expected = getContent(convertedFile);

    assertEquals(expected, actual);
  }

  @Nested
  @DisplayName("handles")
  class Handles {

    @Test
    @DisplayName("Returns true for csv extensions")
    void handlesCsv() {
      assertTrue(bulkClaimCsvConverter.handles(FileExtension.CSV));
    }

    @Test
    @DisplayName("Returns true for txt extensions")
    void handlesTxt() {
      assertTrue(bulkClaimCsvConverter.handles(FileExtension.TXT));
    }

    @Test
    @DisplayName("Returns false for xml extensions")
    void doesNotHandleXml() {
      assertFalse(bulkClaimCsvConverter.handles(FileExtension.XML));
    }
  }
}
