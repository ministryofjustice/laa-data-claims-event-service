package uk.gov.justice.laa.bulk.claim.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.util.ResourceUtils.getFile;
import static uk.gov.justice.laa.bulk.claim.converter.ConverterTestUtils.getContent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.bulk.claim.exception.BulkClaimFileReadException;
import uk.gov.justice.laa.bulk.claim.model.FileExtension;
import uk.gov.justice.laa.bulk.claim.model.xml.XmlSubmission;

public class BulkClaimXmlConverterTests {

  ObjectMapper objectMapper;

  BulkClaimXmlConverter bulkClaimXmlConverter;

  private static final String OUTCOMES_INPUT_FILE = "classpath:test_upload_files/xml/outcomes.xml";
  private static final String OUTCOMES_CONVERTED_FILE =
      "classpath:test_upload_files/xml/outcomes_converted.json";

  private static final String MISSING_OFFICE_INPUT_FILE =
      "classpath:test_upload_files/xml/missing_office.xml";

  private static final String MISSING_SCHEDULE_INPUT_FILE =
      "classpath:test_upload_files/xml/missing_schedule.xml";

  @BeforeEach
  public void init() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    XmlMapper xmlMapper = new XmlMapper();
    bulkClaimXmlConverter = new BulkClaimXmlConverter(xmlMapper);
  }

  @Nested
  @DisplayName("convert")
  class Convert {

    @Test
    @DisplayName("Can convert a bulk claim file with outcomes to xml submission")
    void canConvertOutcomesToXmlSubmission() throws IOException {
      File inputFile = getFile(OUTCOMES_INPUT_FILE);
      XmlSubmission bulkClaimSubmission = bulkClaimXmlConverter.convert(inputFile);
      String actual = objectMapper.writeValueAsString(bulkClaimSubmission);

      File convertedFile = getFile(OUTCOMES_CONVERTED_FILE);
      String expected = getContent(convertedFile);

      assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Throws exception when office is missing")
    void throwsExceptionWhenOfficeMissing() throws IOException {
      File inputFile = getFile(MISSING_OFFICE_INPUT_FILE);
      assertThrows(
          BulkClaimFileReadException.class,
          () -> bulkClaimXmlConverter.convert(inputFile),
          "Expected exception to be thrown when office is missing");
    }

    @Test
    @DisplayName("Throws exception when schedule is missing")
    void throwsExceptionWhenScheduleMissing() throws IOException {
      File inputFile = getFile(MISSING_SCHEDULE_INPUT_FILE);
      assertThrows(
          BulkClaimFileReadException.class,
          () -> bulkClaimXmlConverter.convert(inputFile),
          "Expected exception to be thrown when schedule is missing");
    }
  }

  @Nested
  @DisplayName("handles")
  class Handles {

    @Test
    @DisplayName("Returns true for xml extensions")
    void handlesXml() {
      assertTrue(bulkClaimXmlConverter.handles(FileExtension.XML));
    }

    @Test
    @DisplayName("Returns false for csv extensions")
    void doesNotHandleCsv() {
      assertFalse(bulkClaimXmlConverter.handles(FileExtension.CSV));
    }

    @Test
    @DisplayName("Returns false for txt extensions")
    void doesNotHandleTxt() {
      assertFalse(bulkClaimXmlConverter.handles(FileExtension.TXT));
    }
  }
}
