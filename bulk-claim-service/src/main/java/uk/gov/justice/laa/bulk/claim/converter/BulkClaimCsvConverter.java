package uk.gov.justice.laa.bulk.claim.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import io.micrometer.common.util.StringUtils;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.bulk.claim.exception.BulkClaimFileReadException;
import uk.gov.justice.laa.bulk.claim.model.FileExtension;
import uk.gov.justice.laa.bulk.claim.model.csv.CsvBulkClaimRow;
import uk.gov.justice.laa.bulk.claim.model.csv.CsvHeader;
import uk.gov.justice.laa.bulk.claim.model.csv.CsvMatterStarts;
import uk.gov.justice.laa.bulk.claim.model.csv.CsvOffice;
import uk.gov.justice.laa.bulk.claim.model.csv.CsvOutcome;
import uk.gov.justice.laa.bulk.claim.model.csv.CsvSchedule;
import uk.gov.justice.laa.bulk.claim.model.csv.CsvSubmission;

/** Converter responsible for converting bulk claim submissions in CSV format. */
@Slf4j
@Component
public class BulkClaimCsvConverter implements BulkClaimConverter {

  private final ObjectMapper objectMapper;
  private final CsvMapper csvMapper;

  @Autowired
  public BulkClaimCsvConverter(ObjectMapper objectMapper, CsvMapper csvMapper) {
    this.objectMapper = objectMapper;
    this.csvMapper = csvMapper;
  }

  /**
   * Converts the given file to a {@link CsvSubmission} object.
   *
   * @param file the input file
   * @return the {@link CsvSubmission} object.
   */
  @Override
  public CsvSubmission convert(File file) {
    CsvOffice csvOffice = null;
    CsvSchedule csvSchedule = null;
    List<CsvOutcome> csvOutcomes = new ArrayList<>();
    List<CsvMatterStarts> csvMatterStarts = new ArrayList<>();

    try (FileReader fileReader = new FileReader(file, StandardCharsets.UTF_8)) {
      MappingIterator<List<String>> rowIterator =
          csvMapper
              .readerForListOf(String.class)
              .with(CsvParser.Feature.WRAP_AS_ARRAY)
              .readValues(fileReader);

      while (rowIterator.hasNextValue()) {
        CsvBulkClaimRow csvBulkClaimRow;
        List<String> row = rowIterator.nextValue();
        String rawHeader = row.getFirst().replaceAll("[^\\p{Print}]", "").trim();
        CsvHeader header = getHeader(rawHeader);
        Map<String, String> values = getValues(row, header);
        csvBulkClaimRow = new CsvBulkClaimRow(header, values);

        switch (csvBulkClaimRow.header()) {
          case CsvHeader.OFFICE -> {
            if (csvOffice != null) {
              throw new BulkClaimFileReadException("Multiple offices found in bulk claim file");
            }
            csvOffice = objectMapper.convertValue(csvBulkClaimRow.values(), CsvOffice.class);
          }
          case CsvHeader.SCHEDULE -> {
            if (csvSchedule != null) {
              throw new BulkClaimFileReadException("Multiple schedules found in bulk claim file");
            }
            csvSchedule = objectMapper.convertValue(csvBulkClaimRow.values(), CsvSchedule.class);
          }
          case CsvHeader.OUTCOME ->
              csvOutcomes.add(
                  objectMapper.convertValue(csvBulkClaimRow.values(), CsvOutcome.class));
          case CsvHeader.MATTERSTARTS ->
              csvMatterStarts.add(
                  objectMapper.convertValue(csvBulkClaimRow.values(), CsvMatterStarts.class));
          default -> log.debug("Unsupported header '{}'", csvBulkClaimRow.header());
        }
      }
    } catch (IOException e) {
      throw new BulkClaimFileReadException("Failed to read csv bulk claim file", e);
    }

    if (csvOffice == null) {
      throw new BulkClaimFileReadException("Office missing from csv bulk claim file");
    }

    if (csvSchedule == null) {
      throw new BulkClaimFileReadException("Schedule missing from csv bulk claim file");
    }

    CsvSubmission bulkClaimSubmission =
        new CsvSubmission(csvOffice, csvSchedule, csvOutcomes, csvMatterStarts);

    try {
      objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
      log.info(objectMapper.writeValueAsString(bulkClaimSubmission));
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to print bulk claim", e);
    }

    return bulkClaimSubmission;
  }

  /**
   * Determines whether this converter handles the given {@link FileExtension}.
   *
   * @param fileExtension the file extension to check
   * @return true if the converter can handle files with the given extension, false otherwise
   */
  @Override
  public boolean handles(FileExtension fileExtension) {
    return FileExtension.CSV.equals(fileExtension) || FileExtension.TXT.equals(fileExtension);
  }

  private Map<String, String> getValues(List<String> row, CsvHeader header) {
    Map<String, String> values = new HashMap<>();
    row.subList(1, row.size())
        .forEach(
            rowValue -> {
              rowValue = rowValue.replaceAll("[^\\p{Print}]", "").trim();
              if (StringUtils.isBlank(rowValue)) {
                log.debug("Blank row value found for {} row. Skipping...", header);
                return;
              }
              String[] entry = rowValue.split("=", 2);
              if (entry.length == 2) {
                values.put(entry[0], entry[1]);
              } else {
                throw new BulkClaimFileReadException(
                    "Unable to read entry for %s:'%s'".formatted(header.name(), rowValue));
              }
            });
    return values;
  }

  private CsvHeader getHeader(String rawHeader) {
    try {
      return CsvHeader.valueOf(rawHeader);
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new BulkClaimFileReadException(
          "Failed to parse bulk claim file header: %s".formatted(rawHeader), e);
    }
  }
}
