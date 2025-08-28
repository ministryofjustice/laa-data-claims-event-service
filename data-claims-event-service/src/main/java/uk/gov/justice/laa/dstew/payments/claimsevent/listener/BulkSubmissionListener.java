package uk.gov.justice.laa.dstew.payments.claimsevent.listener;

import io.awspring.cloud.sqs.annotation.SqsListener;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkSubmissionMessage;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.BulkParsingService;

@Slf4j
@Component
public class BulkSubmissionListener {

  private final BulkParsingService bulkParsingService;


  public BulkSubmissionListener(BulkParsingService bulkParsingService) {
    this.bulkParsingService = bulkParsingService;
  }

  @SqsListener("${laa.bulk-claim-queue.name}")
  public void receiveBulkSubmission(BulkSubmissionMessage submissionMessage) {
    log.info(
        "Received bulk submission {}, with {} submissions", submissionMessage.bulkSubmissionId(),
        submissionMessage.submissionIds().size());

    // Loop through submission IDs and parse data for each one
    for (UUID submissionId : submissionMessage.submissionIds()) {
      bulkParsingService.parseData(submissionMessage.bulkSubmissionId(), submissionId);
    }

  }

}
