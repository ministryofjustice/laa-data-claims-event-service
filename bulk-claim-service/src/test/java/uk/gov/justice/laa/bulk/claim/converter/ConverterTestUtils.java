package uk.gov.justice.laa.bulk.claim.converter;

import io.micrometer.core.instrument.util.IOUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ConverterTestUtils {

  public static String getContent(File file) {
    String content;
    try (FileInputStream inputStream = new FileInputStream(file)) {
      content = IOUtils.toString(inputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return content;
  }
}
