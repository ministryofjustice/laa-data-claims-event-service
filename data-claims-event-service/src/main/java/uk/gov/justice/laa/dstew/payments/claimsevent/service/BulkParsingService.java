package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;

@Service
@AllArgsConstructor
public class BulkParsingService {

  private final DataClaimsRestClient dataClaimsRestClient;


  //1. get the bulk submission data from the data claims service

  //2. use mapstruct mapper to map data form bulk submission to submission
  //calculate total amount of claims

  //3. post the submission to the claims data client - status = CREATED


  //4. for each outcome in the bulk submission, map it to a claim object

  // 5. post the claim to the submission

  // 6. for each matter-start in the bulk submission, map it to a matter-start object

  // 7. post the matter-start to the submission

  // 8. when all claims and matter-starts are posted, update the submission status to 'READY_FOR_VALIDATION'
  //and update the total amount of claims in the submission

  // 9. acknowledge the message


}
