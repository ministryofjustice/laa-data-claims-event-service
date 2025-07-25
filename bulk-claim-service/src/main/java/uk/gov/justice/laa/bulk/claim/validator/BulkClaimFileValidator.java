package uk.gov.justice.laa.bulk.claim.validator;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class BulkClaimFileValidator {

  public void validate(Object target) {
    MultipartFile file = (MultipartFile) target;

    // Step 1: Check if file is null or empty
    if (file.isEmpty()) {
      throw new IllegalArgumentException("The uploaded file is empty");
    }

    // Step 2: Validate file extension
    String originalFilename = file.getOriginalFilename();
    if (originalFilename == null ||
        (!originalFilename.toLowerCase().endsWith(".csv") && !originalFilename.toLowerCase()
            .endsWith(".xml"))) {
      throw new IllegalArgumentException("Only .csv and .xml files are allowed");
    }

    // Step 3: Validate MIME Type
    String contentType = file.getContentType();
    if (originalFilename.toLowerCase().endsWith(".csv") && !"text/csv".equals(contentType)) {
      throw new IllegalArgumentException("Mime type does not match the .csv file extension");
    }
    if (originalFilename.toLowerCase().endsWith(".xml")
        && !("text/xml".equals(contentType) || "application/xml".equals(contentType))) {
      throw new IllegalArgumentException("Mime type does not match the .xml file extension");
    }
  }

}
