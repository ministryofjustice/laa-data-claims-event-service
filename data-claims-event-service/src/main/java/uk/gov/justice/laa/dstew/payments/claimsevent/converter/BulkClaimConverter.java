package uk.gov.justice.laa.dstew.payments.claimsevent.converter;

import org.springframework.web.multipart.MultipartFile;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.FileExtension;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.FileSubmission;

/** Interface for bulk claim submission file converters. */
public interface BulkClaimConverter {

  FileSubmission convert(MultipartFile file);

  boolean handles(FileExtension fileExtension);
}
