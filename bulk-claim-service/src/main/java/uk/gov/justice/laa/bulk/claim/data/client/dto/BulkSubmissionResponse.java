package uk.gov.justice.laa.bulk.claim.data.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Bulk submissions Response. */
@Data
@AllArgsConstructor
public class BulkSubmissionResponse {
  private String submissionId;
}
