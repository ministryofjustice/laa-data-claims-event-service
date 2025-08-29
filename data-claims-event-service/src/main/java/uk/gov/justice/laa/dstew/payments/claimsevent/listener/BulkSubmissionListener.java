package uk.gov.justice.laa.dstew.payments.claimsevent.listener;

import io.awspring.cloud.sqs.annotation.SqsListener;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsevent.model.BulkSubmissionMessage;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.BulkParsingService;

/**
 * Listener for bulk submissions from the Data Claims service.
 *
 * <p>Listens to an SQS queue for any new messages. Processes new bulk submissions when they are
 * added to the queue.
 *
 * @author Jamie Briggs
 * @see BulkSubmissionMessage
 * @see BulkParsingService
 */
@Slf4j
@Component
public class BulkSubmissionListener {

  private final BulkParsingService bulkParsingService;

  public BulkSubmissionListener(BulkParsingService bulkParsingService) {
    this.bulkParsingService = bulkParsingService;
  }

  /**
   * Listens for new bulk submissions from the Data Claims service, and parses each submission using
   * the {@link BulkParsingService}.
   *
   * @param submissionMessage the message containing the bulk submission ID and list of submission
   *     IDs
   */
  @SqsListener("${laa.bulk-claim-queue.name}")
  public void receiveBulkSubmission(BulkSubmissionMessage submissionMessage) {
    log.info(
        "Received bulk submission {}, with {} submissions",
        submissionMessage.bulkSubmissionId(),
        submissionMessage.submissionIds().size());

    // Loop through submission IDs and parse data for each one
    for (UUID submissionId : submissionMessage.submissionIds()) {
      bulkParsingService.parseData(submissionMessage.bulkSubmissionId(), submissionId);
    }
  }
}
