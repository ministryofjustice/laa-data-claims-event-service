package uk.gov.justice.laa.bulk.claim.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.bulk.claim.api.SubmissionsApi;
import uk.gov.justice.laa.bulk.claim.model.SubmissionResponse;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SubmissionController implements SubmissionsApi {

  @Override
  public ResponseEntity<SubmissionResponse> postSubmission() {
    return null;
  }
}
