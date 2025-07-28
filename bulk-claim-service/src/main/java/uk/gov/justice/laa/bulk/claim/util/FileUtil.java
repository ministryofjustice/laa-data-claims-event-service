package uk.gov.justice.laa.bulk.claim.util;

import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.justice.laa.bulk.claim.exception.BulkClaimFileReadException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileUtil {

  public static File createTempFile(@NotNull MultipartFile multipartFile) {
    try {
      File tempFile = File.createTempFile("upload-", multipartFile.getOriginalFilename());
      multipartFile.transferTo(tempFile);
      tempFile.deleteOnExit();
      return tempFile;
    } catch (IOException e) {
      throw new BulkClaimFileReadException("Could not open file", e);
    }

  }
}
