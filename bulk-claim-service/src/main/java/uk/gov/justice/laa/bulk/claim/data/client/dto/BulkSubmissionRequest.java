package uk.gov.justice.laa.bulk.claim.data.client.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import lombok.Data;
import uk.gov.justice.laa.bulk.claim.model.BulkClaimSubmission;

/** Bulk Submissions Request. */
@Data
public class BulkSubmissionRequest {

  @NotNull private String userId;

  private HashMap<String, String> meta;

  @NotEmpty private List<BulkClaimSubmission> submissions;
}
