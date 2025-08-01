package uk.gov.justice.laa.bulk.claim.converter;

import org.springframework.web.multipart.MultipartFile;
import uk.gov.justice.laa.bulk.claim.model.FileExtension;
import uk.gov.justice.laa.bulk.claim.model.FileSubmission;

/** Interface for bulk claim submission file converters. */
public interface BulkClaimConverter {

  FileSubmission convert(MultipartFile file);

  boolean handles(FileExtension fileExtension);
}
