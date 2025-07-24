package uk.gov.justice.laa.bulk.claim.converter;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.bulk.claim.model.FileExtension;

/**
 * Factory for providing a bulk claim submission converter that corresponds to the given file
 * extension.
 */
@Component
public class BulkClaimConverterFactory {

  private final List<BulkClaimConverter> converters;

  @Autowired
  public BulkClaimConverterFactory(List<BulkClaimConverter> converters) {
    this.converters = converters;
  }

  /**
   * Provides a {@link BulkClaimConverter} for the given file extension.
   *
   * @param fileExtension the input file extension.
   * @return the {@link BulkClaimConverter} corresponding to the given file extension.
   */
  public BulkClaimConverter converterFor(FileExtension fileExtension) {
    return converters.stream()
        .filter(converter -> converter.handles(fileExtension))
        .findFirst()
        .orElseThrow(
            () -> new RuntimeException("No converter found for file extension: " + fileExtension));
  }
}
