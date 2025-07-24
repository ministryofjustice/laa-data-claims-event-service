package uk.gov.justice.laa.bulk.claim.converter;

import java.io.File;
import uk.gov.justice.laa.bulk.claim.model.FileExtension;
import uk.gov.justice.laa.bulk.claim.model.FileSubmission;

/** Interface for bulk claim submission file converters. */
public interface BulkClaimConverter {

  FileSubmission convert(File file);

  boolean handles(FileExtension fileExtension);
}
