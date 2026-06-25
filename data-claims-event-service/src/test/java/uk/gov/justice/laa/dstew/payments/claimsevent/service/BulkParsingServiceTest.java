package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.EVENT_SERVICE;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionMatterStart;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionOutcome;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.CreateClaim201Response;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.CreateMatterStart201Response;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200Response;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200ResponseDetails;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.MatterStartPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.*;
import uk.gov.justice.laa.dstew.payments.claimsevent.mapper.BulkSubmissionMapper;
import uk.gov.justice.laa.dstew.payments.claimsevent.metrics.MetricPublisher;

@ExtendWith(MockitoExtension.class)
class BulkParsingServiceTest {

  private static final String BULK_SUBMISSION_CREATED_BY_USER_ID = "a-provider-user-id";
  private static final UUID BULK_SUBMISSION_ID = UUID.randomUUID();
  private static final String SUBMISSION_ID = UUID.randomUUID().toString();

  @Mock private DataClaimsRestClient dataClaimsRestClient;
  @Mock private BulkSubmissionMapper bulkSubmissionMapper;
  @Mock private MetricPublisher metricPublisher;
  @Mock private SubmissionDataNormaliser submissionDataNormaliser;

  @InjectMocks private BulkParsingService service;

  @Test
  void parseDataProcessesSubmissionClaimsAndMatterStarts() {
    final UUID bulkSubmissionId = UUID.randomUUID();
    final UUID submissionId = UUID.randomUUID();
    final String createdSubmissionId = "sub-created-id";

    final BulkSubmissionOutcome outcome = new BulkSubmissionOutcome();
    final List<BulkSubmissionOutcome> outcomes = List.of(outcome);

    final BulkSubmissionMatterStart matterStart = new BulkSubmissionMatterStart();
    final List<BulkSubmissionMatterStart> matterStarts = List.of(matterStart);

    final GetBulkSubmission200ResponseDetails details =
        new GetBulkSubmission200ResponseDetails().outcomes(outcomes).matterStarts(matterStarts);

    final GetBulkSubmission200Response bulkSubmission =
        new GetBulkSubmission200Response()
            .bulkSubmissionId(bulkSubmissionId)
            .details(details)
            .createdByUserId(BULK_SUBMISSION_CREATED_BY_USER_ID);

    final SubmissionPost submissionPost =
        new SubmissionPost()
            .bulkSubmissionId(bulkSubmissionId)
            .areaOfLaw(AreaOfLaw.LEGAL_HELP)
            .providerUserId(BULK_SUBMISSION_CREATED_BY_USER_ID);
    final ClaimPost claimPost = new ClaimPost();
    claimPost.setScheduleReference("S1");
    claimPost.setLineNumber(1);
    final List<ClaimPost> claimPosts = List.of(claimPost);

    final MatterStartPost matterStartRequest = new MatterStartPost();
    matterStartRequest.setScheduleReference("M1");
    matterStartRequest.setCreatedByUserId(EVENT_SERVICE);
    final List<MatterStartPost> matterStartRequests = List.of(matterStartRequest);

    when(dataClaimsRestClient.getBulkSubmission(bulkSubmissionId))
        .thenReturn(ResponseEntity.ok(bulkSubmission));
    when(submissionDataNormaliser.normalise(bulkSubmission)).thenReturn(bulkSubmission);
    when(bulkSubmissionMapper.mapToSubmissionPost(bulkSubmission, submissionId))
        .thenReturn(submissionPost);
    when(dataClaimsRestClient.createSubmission(submissionPost))
        .thenReturn(
            ResponseEntity.created(URI.create("/submissions/" + createdSubmissionId)).build());
    when(bulkSubmissionMapper.mapToClaimPosts(outcomes, AreaOfLaw.LEGAL_HELP))
        .thenReturn(claimPosts);
    when(dataClaimsRestClient.createClaim(eq(createdSubmissionId), eq(claimPost)))
        .thenReturn(ResponseEntity.created(URI.create("/claims/claim-id")).build());
    when(bulkSubmissionMapper.mapToMatterStartRequests(matterStarts))
        .thenReturn(matterStartRequests);
    when(dataClaimsRestClient.createMatterStart(eq(createdSubmissionId), eq(matterStartRequest)))
        .thenReturn(ResponseEntity.created(URI.create("/matter-starts/matter-id")).build());
    when(dataClaimsRestClient.updateSubmission(eq(createdSubmissionId), any(SubmissionPatch.class)))
        .thenReturn(ResponseEntity.noContent().build());
    when(dataClaimsRestClient.updateBulkSubmission(
            eq(bulkSubmissionId.toString()), any(BulkSubmissionPatch.class)))
        .thenReturn(ResponseEntity.noContent().build());

    service.parseData(bulkSubmissionId, submissionId);

    verify(dataClaimsRestClient).getBulkSubmission(bulkSubmissionId);
    verify(bulkSubmissionMapper).mapToSubmissionPost(bulkSubmission, submissionId);
    verify(dataClaimsRestClient).createSubmission(submissionPost);
    verify(bulkSubmissionMapper).mapToClaimPosts(outcomes, AreaOfLaw.LEGAL_HELP);
    verify(dataClaimsRestClient).createClaim(eq(createdSubmissionId), eq(claimPost));
    verify(bulkSubmissionMapper).mapToMatterStartRequests(matterStarts);
    verify(dataClaimsRestClient).createMatterStart(eq(createdSubmissionId), eq(matterStartRequest));
    verify(dataClaimsRestClient)
        .updateSubmission(
            eq(createdSubmissionId),
            argThat(
                patch ->
                    patch.getStatus() == SubmissionStatus.READY_FOR_VALIDATION
                        && patch.getNumberOfClaims() == 1));
    verify(dataClaimsRestClient)
        .updateBulkSubmission(
            eq(bulkSubmissionId.toString()),
            argThat(patch -> patch.getStatus() == BulkSubmissionStatus.PARSING_COMPLETED));
  }

  @Test
  void parseDataHandlesNullDetails() {
    final UUID bulkSubmissionId = UUID.randomUUID();
    final UUID submissionId = UUID.randomUUID();
    final String createdSubmissionId = "sub-id";

    final GetBulkSubmission200Response bulkSubmission =
        new GetBulkSubmission200Response().bulkSubmissionId(bulkSubmissionId).details(null);

    final SubmissionPost submissionPost = new SubmissionPost().areaOfLaw(AreaOfLaw.LEGAL_HELP);

    when(dataClaimsRestClient.getBulkSubmission(bulkSubmissionId))
        .thenReturn(ResponseEntity.ok(bulkSubmission));
    when(submissionDataNormaliser.normalise(bulkSubmission)).thenReturn(bulkSubmission);
    when(bulkSubmissionMapper.mapToSubmissionPost(bulkSubmission, submissionId))
        .thenReturn(submissionPost);
    when(dataClaimsRestClient.createSubmission(submissionPost))
        .thenReturn(
            ResponseEntity.created(URI.create("/submissions/" + createdSubmissionId)).build());
    when(bulkSubmissionMapper.mapToClaimPosts(List.of(), AreaOfLaw.LEGAL_HELP))
        .thenReturn(List.of());
    when(bulkSubmissionMapper.mapToMatterStartRequests(List.of())).thenReturn(List.of());
    when(dataClaimsRestClient.updateSubmission(eq(createdSubmissionId), any(SubmissionPatch.class)))
        .thenReturn(ResponseEntity.noContent().build());
    when(dataClaimsRestClient.updateBulkSubmission(
            eq(bulkSubmissionId.toString()), any(BulkSubmissionPatch.class)))
        .thenReturn(ResponseEntity.noContent().build());

    service.parseData(bulkSubmissionId, submissionId);

    verify(dataClaimsRestClient).getBulkSubmission(bulkSubmissionId);
    verify(bulkSubmissionMapper).mapToSubmissionPost(bulkSubmission, submissionId);
    verify(dataClaimsRestClient).createSubmission(submissionPost);
    verify(bulkSubmissionMapper).mapToClaimPosts(List.of(), AreaOfLaw.LEGAL_HELP);
    verify(bulkSubmissionMapper).mapToMatterStartRequests(List.of());
    verify(dataClaimsRestClient)
        .updateSubmission(
            eq(createdSubmissionId),
            argThat(
                patch ->
                    patch.getStatus() == SubmissionStatus.READY_FOR_VALIDATION
                        && patch.getNumberOfClaims() == 0));
  }

  @Test
  void createSubmissionReturnsId() {
    SubmissionPost submission = new SubmissionPost();
    when(dataClaimsRestClient.createSubmission(submission))
        .thenReturn(ResponseEntity.created(URI.create("/submissions/123")).build());

    String id = service.createSubmission(submission);

    assertThat(id).isEqualTo("123");
  }

  @Test
  void createSubmissionThrowsWhenNoLocation() {
    var bulkSubmissionId = UUID.randomUUID();
    final SubmissionPost submission = new SubmissionPost().bulkSubmissionId(bulkSubmissionId);

    when(dataClaimsRestClient.createSubmission(submission))
        .thenReturn(ResponseEntity.created(null).build());

    assertThatThrownBy(() -> service.createSubmission(submission))
        .isInstanceOf(SubmissionCreateException.class);
  }

  @Test
  void createSubmissionThrowsWhenStatusNot201() {
    var bulkSubmissionId = UUID.randomUUID();
    final SubmissionPost submission = new SubmissionPost().bulkSubmissionId(bulkSubmissionId);
    when(dataClaimsRestClient.createSubmission(submission))
        .thenReturn(ResponseEntity.badRequest().build());

    assertThatThrownBy(() -> service.createSubmission(submission))
        .isInstanceOf(SubmissionCreateException.class);
  }

  @Test
  void createClaimReturnsId() {
    ClaimPost claim = new ClaimPost();
    claim.setScheduleReference("S1");
    claim.setLineNumber(1);
    when(dataClaimsRestClient.createClaim(eq("sub1"), eq(claim)))
        .thenReturn(ResponseEntity.created(URI.create("/claims/456")).build());

    String id = service.createClaim("sub1", claim);

    assertThat(id).isEqualTo("456");
  }

  @Test
  void createClaimThrowsOnFailure() {
    ClaimPost claim = new ClaimPost();
    claim.setScheduleReference("S1");
    claim.setLineNumber(1);
    when(dataClaimsRestClient.createClaim(eq("sub1"), eq(claim)))
        .thenReturn(ResponseEntity.badRequest().build());

    assertThatThrownBy(() -> service.createClaim("sub1", claim))
        .isInstanceOf(ClaimCreateException.class);
  }

  @Test
  void createClaimThrowsWhenLocationMissing() {
    final ClaimPost claim = new ClaimPost();
    claim.setScheduleReference("S1");
    claim.setLineNumber(1);

    final HttpHeaders headers = new HttpHeaders();
    final ResponseEntity<CreateClaim201Response> response =
        new ResponseEntity<>(headers, HttpStatus.CREATED);

    when(dataClaimsRestClient.createClaim(eq("sub1"), eq(claim))).thenReturn(response);

    assertThatThrownBy(() -> service.createClaim("sub1", claim))
        .isInstanceOf(ClaimCreateException.class);
  }

  @Test
  void createMatterStartReturnsId() {
    MatterStartPost request = new MatterStartPost();
    request.setScheduleReference("SCH");
    when(dataClaimsRestClient.createMatterStart(eq("sub1"), eq(request)))
        .thenReturn(ResponseEntity.created(URI.create("/matter-starts/789")).build());

    String id = service.createMatterStart("sub1", request);

    assertThat(id).isEqualTo("789");
  }

  @Test
  void createMatterStartThrowsOnFailure() {
    MatterStartPost request = new MatterStartPost();
    request.setScheduleReference("SCH");
    when(dataClaimsRestClient.createMatterStart(eq("sub1"), eq(request)))
        .thenReturn(ResponseEntity.badRequest().build());

    assertThatThrownBy(() -> service.createMatterStart("sub1", request))
        .isInstanceOf(MatterStartCreateException.class);
  }

  @Test
  void createMatterStartThrowsWhenLocationMissing() {
    final MatterStartPost request = new MatterStartPost();
    request.setScheduleReference("SCH");

    final HttpHeaders headers = new HttpHeaders();
    final ResponseEntity<CreateMatterStart201Response> response =
        new ResponseEntity<>(headers, HttpStatus.CREATED);

    when(dataClaimsRestClient.createMatterStart(eq("sub1"), eq(request))).thenReturn(response);

    assertThatThrownBy(() -> service.createMatterStart("sub1", request))
        .isInstanceOf(MatterStartCreateException.class);
  }

  @Test
  void updateSubmissionCallsClient() {
    when(dataClaimsRestClient.updateSubmission(eq("sub1"), any(SubmissionPatch.class)))
        .thenReturn(ResponseEntity.noContent().build());

    service.updateSubmission("sub1", 2, SubmissionStatus.READY_FOR_VALIDATION);

    verify(dataClaimsRestClient)
        .updateSubmission(
            eq("sub1"),
            argThat(
                p ->
                    p.getStatus() == SubmissionStatus.READY_FOR_VALIDATION
                        && p.getNumberOfClaims() == 2));
  }

  @Test
  void updateSubmissionThrowsWhenNot2xx() {
    when(dataClaimsRestClient.updateSubmission(eq("sub1"), any(SubmissionPatch.class)))
        .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

    assertThatThrownBy(
            () -> service.updateSubmission("sub1", 2, SubmissionStatus.READY_FOR_VALIDATION))
        .isInstanceOf(SubmissionCreateException.class);
  }

  @Test
  void createClaimsReturnsEmptyWhenNoClaims() {
    assertThat(service.createClaims("sub1", null)).isEmpty();
    assertThat(service.createClaims("sub1", List.of())).isEmpty();
  }

  @Test
  void createClaimsThrowsWithIndexInfo() {
    final BulkParsingService spyService = spy(service);
    final ClaimPost claim = new ClaimPost();

    doThrow(new ClaimCreateException("boom")).when(spyService).createClaim("sub1", claim);

    List<ClaimPost> claimsList = List.of(claim);
    assertThatThrownBy(() -> spyService.createClaims("sub1", claimsList))
        .isInstanceOf(ClaimCreateException.class)
        .hasMessageContaining("index 0");
  }

  @Test
  void createMatterStartsReturnsEmptyWhenNoMatterStarts() {
    assertThat(service.createMatterStarts("sub1", null)).isEmpty();
    assertThat(service.createMatterStarts("sub1", List.of())).isEmpty();
  }

  @Test
  void createMatterStartsThrowsWithIndexInfo() {
    final BulkParsingService spyService = spy(service);
    final MatterStartPost request = new MatterStartPost();

    doThrow(new MatterStartCreateException("fail"))
        .when(spyService)
        .createMatterStart("sub1", request);

    assertThatThrownBy(() -> spyService.createMatterStarts("sub1", List.of(request)))
        .isInstanceOf(MatterStartCreateException.class)
        .hasMessageContaining("index 0");
  }

  @Test
  void getBulkSubmissionReturnsPayload() {
    final UUID id = UUID.randomUUID();
    final GetBulkSubmission200Response payload =
        new GetBulkSubmission200Response()
            .bulkSubmissionId(id)
            .createdByUserId(BULK_SUBMISSION_CREATED_BY_USER_ID);

    when(dataClaimsRestClient.getBulkSubmission(id)).thenReturn(ResponseEntity.ok(payload));

    final GetBulkSubmission200Response result = service.getBulkSubmission(id);

    assertThat(result).isSameAs(payload);
  }

  @Test
  void getBulkSubmissionThrowsWhenNotFound() {
    final UUID id = UUID.randomUUID();
    when(dataClaimsRestClient.getBulkSubmission(id)).thenReturn(ResponseEntity.notFound().build());

    assertThatThrownBy(() -> service.getBulkSubmission(id))
        .isInstanceOf(BulkSubmissionRetrievalException.class);
  }

  @Test
  void getBulkSubmissionThrowsWhenResponseIsNull() {
    final UUID id = UUID.randomUUID();
    when(dataClaimsRestClient.getBulkSubmission(id)).thenReturn(null);

    assertThatThrownBy(() -> service.getBulkSubmission(id))
        .isInstanceOf(BulkSubmissionRetrievalException.class);
  }

  @Test
  void getBulkSubmissionThrowsWhenBodyIsNull() {
    final UUID id = UUID.randomUUID();
    final ResponseEntity<GetBulkSubmission200Response> response = ResponseEntity.ok(null);

    when(dataClaimsRestClient.getBulkSubmission(id)).thenReturn(response);

    assertThatThrownBy(() -> service.getBulkSubmission(id))
        .isInstanceOf(BulkSubmissionRetrievalException.class);
  }

  @Test
  void getBulkSubmissionThrowsWhenNot2xxEvenIfBodyPresent() {
    final UUID id = UUID.randomUUID();
    final GetBulkSubmission200Response payload =
        new GetBulkSubmission200Response().bulkSubmissionId(id);
    final ResponseEntity<GetBulkSubmission200Response> response =
        new ResponseEntity<>(payload, HttpStatus.INTERNAL_SERVER_ERROR);

    when(dataClaimsRestClient.getBulkSubmission(id)).thenReturn(response);

    assertThatThrownBy(() -> service.getBulkSubmission(id))
        .isInstanceOf(BulkSubmissionRetrievalException.class);
  }

  @Test
  void updateBulkSubmissionStatusCallsClient() {
    when(dataClaimsRestClient.updateBulkSubmission(
            eq(BULK_SUBMISSION_ID.toString()), any(BulkSubmissionPatch.class)))
        .thenReturn(ResponseEntity.noContent().build());

    service.updateBulkSubmissionStatus(BULK_SUBMISSION_ID, BulkSubmissionStatus.PARSING_COMPLETED);

    verify(dataClaimsRestClient)
        .updateBulkSubmission(
            eq(BULK_SUBMISSION_ID.toString()),
            argThat(p -> p.getStatus() == BulkSubmissionStatus.PARSING_COMPLETED));
  }

  @Test
  void updateBulkSubmissionStatusThrowsWhenNot2xx() {
    when(dataClaimsRestClient.updateBulkSubmission(
            eq(BULK_SUBMISSION_ID.toString()), any(BulkSubmissionPatch.class)))
        .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

    assertThatThrownBy(
            () ->
                service.updateBulkSubmissionStatus(
                    BULK_SUBMISSION_ID, BulkSubmissionStatus.PARSING_COMPLETED))
        .isInstanceOf(BulkSubmissionUpdateException.class)
        .hasMessageContaining(
            "Failed to update bulk submission status for bulk submission "
                + BULK_SUBMISSION_ID
                + ". HTTP status: 500");
  }

  @Test
  void updateBulkSubmissionStatusThrowsWhenResponseIsNull() {
    when(dataClaimsRestClient.updateBulkSubmission(
            eq(BULK_SUBMISSION_ID.toString()), any(BulkSubmissionPatch.class)))
        .thenReturn(null);

    assertThatThrownBy(
            () ->
                service.updateBulkSubmissionStatus(
                    BULK_SUBMISSION_ID, BulkSubmissionStatus.PARSING_COMPLETED))
        .isInstanceOf(BulkSubmissionUpdateException.class)
        .hasMessageContaining(
            "Failed to update bulk submission status for bulk submission "
                + BULK_SUBMISSION_ID
                + ". HTTP status: null response");
  }

  @Test
  void createClaimsUpdatesSubmissionStatusToValidationFailedOnExceptionAfterClaimProcessing() {
    String bulkSubmissionId = "bulk-1";
    String submissionId = "sub-1";
    ClaimPost claim = new ClaimPost();
    List<ClaimPost> claims = List.of(claim);

    // Spy on the service to simulate an exception when creating a claim
    BulkParsingService spyService = spy(service);
    doThrow(new RuntimeException("outer exception"))
        .when(spyService)
        .createClaim(submissionId, claim);

    assertThatThrownBy(() -> spyService.createClaims(submissionId, claims))
        .isInstanceOf(ClaimCreateException.class)
        .hasMessageContaining("outer exception");
  }

  @Test
  void createClaimsUpdatesSubmissionStatusToValidationFailedOnFirstClaimErrorInLoop() {
    String bulkSubmissionId = "bulk-1";
    String submissionId = "sub-1";
    ClaimPost c1 = new ClaimPost();
    ClaimPost c2 = new ClaimPost();
    List<ClaimPost> claims = List.of(c1, c2);

    doThrow(new ClaimCreateException("fail"))
        .when(dataClaimsRestClient)
        .createClaim(eq(submissionId), eq(c1));

    doThrow(new ClaimCreateException("fail"))
        .when(dataClaimsRestClient)
        .createClaim(eq(submissionId), eq(c2));

    assertThatThrownBy(() -> service.createClaims(submissionId, claims))
        .isInstanceOf(ClaimCreateException.class)
        .hasMessageContaining("index 0");

    verify(dataClaimsRestClient).createClaim(eq(submissionId), eq(c1));
  }

  @Test
  void createMatterStartsUpdatesSubmissionStatusToValidationFailedOnError() {
    String submissionId = "sub-1";
    MatterStartPost ms1 = new MatterStartPost();
    MatterStartPost ms2 = new MatterStartPost();
    List<MatterStartPost> matterStarts = List.of(ms1, ms2);

    // Mock createMatterStart to throw for the first item
    doThrow(new MatterStartCreateException("fail"))
        .when(dataClaimsRestClient)
        .createMatterStart(eq(submissionId), eq(ms1));

    assertThatThrownBy(() -> service.createMatterStarts(submissionId, matterStarts))
        .isInstanceOf(MatterStartCreateException.class)
        .hasMessageContaining("index 0");

    verify(dataClaimsRestClient).createMatterStart(eq(submissionId), eq(ms1));
  }

  /**
   * Test: Failure in createSubmission triggers bulk status update to PARSING_FAILED.
   * markSubmissionAsFailed is not called since submissionId is not available yet.
   */
  @Test
  void parseData_whenCreateSubmissionFails_updatesBulkStatusOnly() {
    // Mock getBulkSubmission and normalise
    when(dataClaimsRestClient.getBulkSubmission(BULK_SUBMISSION_ID))
        .thenReturn(ResponseEntity.ok(bulkSubmission()));
    when(submissionDataNormaliser.normalise(any())).thenReturn(bulkSubmission());
    // Mock mapToSubmissionPost
    when(bulkSubmissionMapper.mapToSubmissionPost(any(), eq(UUID.fromString(SUBMISSION_ID))))
        .thenReturn(submissionPost());
    // Mock createSubmission to throw
    when(dataClaimsRestClient.createSubmission(any(SubmissionPost.class)))
        .thenReturn(ResponseEntity.status(500).build());

    assertThatThrownBy(() -> service.parseData(BULK_SUBMISSION_ID, UUID.fromString(SUBMISSION_ID)))
        .isInstanceOf(SubmissionCreateException.class);
    verify(dataClaimsRestClient)
        .updateBulkSubmission(eq(BULK_SUBMISSION_ID.toString()), any(BulkSubmissionPatch.class));
    verify(dataClaimsRestClient, never())
        .updateSubmission(
            eq(SUBMISSION_ID),
            argThat(patch -> patch.getStatus() == SubmissionStatus.VALIDATION_FAILED));
  }

  /** Test: Failure in createClaims triggers both bulk and submission status updates. */
  @Test
  void parseData_whenCreateClaimsFails_updatesBulkAndSubmissionStatus() {
    // Mock getBulkSubmission, normalise, map, createSubmission
    when(dataClaimsRestClient.getBulkSubmission(BULK_SUBMISSION_ID))
        .thenReturn(ResponseEntity.ok(bulkSubmissionWithOutcomes()));
    when(submissionDataNormaliser.normalise(any())).thenReturn(bulkSubmissionWithOutcomes());
    when(bulkSubmissionMapper.mapToSubmissionPost(any(), eq(UUID.fromString(SUBMISSION_ID))))
        .thenReturn(submissionPost());
    when(dataClaimsRestClient.createSubmission(any(SubmissionPost.class)))
        .thenReturn(
            ResponseEntity.status(201).header("Location", "/submissions/" + SUBMISSION_ID).build());
    // Mock mapToClaimPosts
    when(bulkSubmissionMapper.mapToClaimPosts(any(), any())).thenReturn(List.of(claimPost()));
    // Mock createClaim to throw
    when(dataClaimsRestClient.createClaim(eq(SUBMISSION_ID), any(ClaimPost.class)))
        .thenThrow(new RuntimeException("API error"));

    assertThatThrownBy(() -> service.parseData(BULK_SUBMISSION_ID, UUID.fromString(SUBMISSION_ID)))
        .isInstanceOf(ClaimCreateException.class);
    verify(dataClaimsRestClient)
        .updateBulkSubmission(eq(BULK_SUBMISSION_ID.toString()), any(BulkSubmissionPatch.class));
    verify(dataClaimsRestClient)
        .updateSubmission(
            eq(SUBMISSION_ID),
            argThat(patch -> patch.getStatus() == SubmissionStatus.VALIDATION_FAILED));
  }

  /** Test: Failure in createMatterStarts triggers both bulk and submission status updates. */
  @Test
  void parseData_whenCreateMatterStartsFails_updatesBulkAndSubmissionStatus() {
    // Mock getBulkSubmission, normalise, map, createSubmission, createClaims
    when(dataClaimsRestClient.getBulkSubmission(BULK_SUBMISSION_ID))
        .thenReturn(ResponseEntity.ok(bulkSubmissionWithMatterStarts()));
    when(submissionDataNormaliser.normalise(any())).thenReturn(bulkSubmissionWithMatterStarts());
    when(bulkSubmissionMapper.mapToSubmissionPost(any(), eq(UUID.fromString(SUBMISSION_ID))))
        .thenReturn(submissionPost());
    when(dataClaimsRestClient.createSubmission(any(SubmissionPost.class)))
        .thenReturn(
            ResponseEntity.status(201).header("Location", "/submissions/" + SUBMISSION_ID).build());
    when(bulkSubmissionMapper.mapToClaimPosts(any(), any())).thenReturn(List.of());
    // Mock mapToMatterStartRequests
    when(bulkSubmissionMapper.mapToMatterStartRequests(any()))
        .thenReturn(List.of(matterStartPost()));
    // Mock createMatterStart to throw
    when(dataClaimsRestClient.createMatterStart(eq(SUBMISSION_ID), any(MatterStartPost.class)))
        .thenThrow(new RuntimeException("API error"));

    assertThatThrownBy(() -> service.parseData(BULK_SUBMISSION_ID, UUID.fromString(SUBMISSION_ID)))
        .isInstanceOf(MatterStartCreateException.class);
    verify(dataClaimsRestClient)
        .updateBulkSubmission(eq(BULK_SUBMISSION_ID.toString()), any(BulkSubmissionPatch.class));
    verify(dataClaimsRestClient)
        .updateSubmission(
            eq(SUBMISSION_ID),
            argThat(patch -> patch.getStatus() == SubmissionStatus.VALIDATION_FAILED));
  }

  /**
   * Test: Failure in getBulkSubmission triggers only bulk status update (no submission status
   * update).
   */
  @Test
  void parseData_whenGetBulkSubmissionFails_updatesBulkStatusOnly() {
    UUID submissionId = UUID.randomUUID();
    // Mock getBulkSubmission to throw
    when(dataClaimsRestClient.getBulkSubmission(BULK_SUBMISSION_ID))
        .thenThrow(new RuntimeException("API error"));

    assertThatThrownBy(() -> service.parseData(BULK_SUBMISSION_ID, submissionId))
        .isInstanceOf(RuntimeException.class);
    verify(dataClaimsRestClient)
        .updateBulkSubmission(
            eq(BULK_SUBMISSION_ID.toString()),
            argThat(patch -> patch.getStatus() == BulkSubmissionStatus.PARSING_FAILED));
    verify(dataClaimsRestClient, never())
        .updateSubmission(eq(submissionId.toString()), any(SubmissionPatch.class));
  }

  /** Test: Failure in updateSubmission triggers both bulk and submission status updates. */
  @Test
  void parseData_whenUpdateSubmissionFails_updatesBulkAndSubmissionStatus() {

    // Mock getBulkSubmission, normalise, map, createSubmission, createClaims, createMatterStarts
    when(dataClaimsRestClient.getBulkSubmission(BULK_SUBMISSION_ID))
        .thenReturn(ResponseEntity.ok(bulkSubmissionWithOutcomes()));
    when(submissionDataNormaliser.normalise(any())).thenReturn(bulkSubmissionWithOutcomes());
    when(bulkSubmissionMapper.mapToSubmissionPost(any(), eq(UUID.fromString(SUBMISSION_ID))))
        .thenReturn(submissionPost());
    when(dataClaimsRestClient.createSubmission(any(SubmissionPost.class)))
        .thenReturn(
            ResponseEntity.status(201).header("Location", "/submissions/" + SUBMISSION_ID).build());
    when(bulkSubmissionMapper.mapToClaimPosts(any(), any())).thenReturn(List.of(claimPost()));
    when(dataClaimsRestClient.createClaim(eq(SUBMISSION_ID), any(ClaimPost.class)))
        .thenReturn(ResponseEntity.status(201).header("Location", "/claims/1").build());
    // Mock updateSubmission to throw
    when(dataClaimsRestClient.updateSubmission(eq(SUBMISSION_ID), any(SubmissionPatch.class)))
        .thenThrow(new RuntimeException("API error"));

    assertThatThrownBy(() -> service.parseData(BULK_SUBMISSION_ID, UUID.fromString(SUBMISSION_ID)))
        .isInstanceOf(RuntimeException.class);
    verify(dataClaimsRestClient)
        .updateBulkSubmission(eq(BULK_SUBMISSION_ID.toString()), any(BulkSubmissionPatch.class));
    verify(dataClaimsRestClient)
        .updateSubmission(
            eq(SUBMISSION_ID),
            argThat(patch -> patch.getStatus() == SubmissionStatus.VALIDATION_FAILED));
  }

  /** Test: Failure in updateBulkSubmissionStatus logs error but does not throw further. */
  @Test
  void parseData_whenUpdateBulkSubmissionStatusFails_logsError() {

    // Mock getBulkSubmission, normalise, map, createSubmission, createClaims, createMatterStarts,
    // updateSubmission
    when(dataClaimsRestClient.getBulkSubmission(BULK_SUBMISSION_ID))
        .thenReturn(ResponseEntity.ok(bulkSubmissionWithOutcomes()));
    when(submissionDataNormaliser.normalise(any())).thenReturn(bulkSubmissionWithOutcomes());
    when(bulkSubmissionMapper.mapToSubmissionPost(any(), eq(UUID.fromString(SUBMISSION_ID))))
        .thenReturn(submissionPost());
    when(dataClaimsRestClient.createSubmission(any(SubmissionPost.class)))
        .thenReturn(
            ResponseEntity.status(201).header("Location", "/submissions/" + SUBMISSION_ID).build());
    when(bulkSubmissionMapper.mapToClaimPosts(any(), any())).thenReturn(List.of(claimPost()));
    when(dataClaimsRestClient.createClaim(eq(SUBMISSION_ID), any(ClaimPost.class)))
        .thenReturn(ResponseEntity.status(201).header("Location", "/claims/1").build());
    when(dataClaimsRestClient.updateSubmission(eq(SUBMISSION_ID), any(SubmissionPatch.class)))
        .thenReturn(ResponseEntity.noContent().build());
    // Mock updateBulkSubmission to throw
    doThrow(new RuntimeException("API error"))
        .when(dataClaimsRestClient)
        .updateBulkSubmission(eq(BULK_SUBMISSION_ID.toString()), any(BulkSubmissionPatch.class));

    assertThatThrownBy(() -> service.parseData(BULK_SUBMISSION_ID, UUID.fromString(SUBMISSION_ID)))
        .isInstanceOf(RuntimeException.class);

    verify(dataClaimsRestClient)
        .updateSubmission(
            eq(SUBMISSION_ID),
            argThat(patch -> patch.getStatus() == SubmissionStatus.VALIDATION_FAILED));
  }

  /** Test: Failure in normalise triggers both bulk and submission status updates. */
  @Test
  void parseData_whenNormaliserFails_updatesBulkStatusOnly() {

    // Mock getBulkSubmission, normalise, map, createSubmission, createClaims, createMatterStarts
    when(dataClaimsRestClient.getBulkSubmission(BULK_SUBMISSION_ID))
        .thenReturn(ResponseEntity.ok(bulkSubmissionWithOutcomes()));
    when(submissionDataNormaliser.normalise(any()))
        .thenThrow(
            new SubmissionDataNormalisationException(
                "Normalise Error", new RuntimeException("API error")));

    assertThatThrownBy(() -> service.parseData(BULK_SUBMISSION_ID, UUID.fromString(SUBMISSION_ID)))
        .isInstanceOf(SubmissionDataNormalisationException.class);
    verify(dataClaimsRestClient)
        .updateBulkSubmission(eq(BULK_SUBMISSION_ID.toString()), any(BulkSubmissionPatch.class));
  }

  /** Test: Failure in mapToSubmissionPost triggers both bulk and submission status updates. */
  @Test
  void parseData_whenMapToSubmissionPostFails_updatesBulkStatusOnly() {

    // Mock getBulkSubmission, normalise, map, createSubmission, createClaims, createMatterStarts
    when(dataClaimsRestClient.getBulkSubmission(BULK_SUBMISSION_ID))
        .thenReturn(ResponseEntity.ok(bulkSubmissionWithOutcomes()));
    when(submissionDataNormaliser.normalise(any())).thenReturn(bulkSubmissionWithOutcomes());
    when(bulkSubmissionMapper.mapToSubmissionPost(any(), eq(UUID.fromString(SUBMISSION_ID))))
        .thenThrow(new RuntimeException("API error"));

    assertThatThrownBy(() -> service.parseData(BULK_SUBMISSION_ID, UUID.fromString(SUBMISSION_ID)))
        .isInstanceOf(RuntimeException.class);
    verify(dataClaimsRestClient)
        .updateBulkSubmission(
            eq(BULK_SUBMISSION_ID.toString()),
            argThat(patch -> patch.getStatus() == BulkSubmissionStatus.PARSING_FAILED));
  }

  // Helper method: returns a minimal BulkSubmission for test scenarios
  private GetBulkSubmission200Response bulkSubmission() {
    GetBulkSubmission200ResponseDetails details =
        new GetBulkSubmission200ResponseDetails().outcomes(List.of()).matterStarts(List.of());
    return new GetBulkSubmission200Response()
        .bulkSubmissionId(BULK_SUBMISSION_ID)
        .details(details)
        .createdByUserId(BULK_SUBMISSION_CREATED_BY_USER_ID);
  }

  // Helper method: returns a BulkSubmission with matterStarts for test scenarios
  private GetBulkSubmission200Response bulkSubmissionWithMatterStarts() {
    GetBulkSubmission200ResponseDetails details =
        new GetBulkSubmission200ResponseDetails()
            .outcomes(List.of())
            .matterStarts(List.of(new BulkSubmissionMatterStart()));
    return new GetBulkSubmission200Response()
        .bulkSubmissionId(BULK_SUBMISSION_ID)
        .details(details)
        .createdByUserId(BULK_SUBMISSION_CREATED_BY_USER_ID);
  }

  // Helper method: returns a BulkSubmission with outcomes for test scenarios
  private GetBulkSubmission200Response bulkSubmissionWithOutcomes() {
    BulkSubmissionOutcome outcome = new BulkSubmissionOutcome();
    outcome.setLineNumber("1");
    GetBulkSubmission200ResponseDetails details =
        new GetBulkSubmission200ResponseDetails()
            .outcomes(List.of(outcome))
            .matterStarts(List.of());
    return new GetBulkSubmission200Response()
        .bulkSubmissionId(BULK_SUBMISSION_ID)
        .details(details)
        .createdByUserId(BULK_SUBMISSION_CREATED_BY_USER_ID);
  }

  // Helper method: returns a minimal SubmissionPost for test scenarios
  private SubmissionPost submissionPost() {
    SubmissionPost sp = new SubmissionPost();
    sp.bulkSubmissionId(BULK_SUBMISSION_ID);
    sp.areaOfLaw(AreaOfLaw.LEGAL_HELP);
    sp.providerUserId(BULK_SUBMISSION_CREATED_BY_USER_ID);
    return sp;
  }

  // Helper method: returns a minimal ClaimPost for test scenarios
  private ClaimPost claimPost() {
    ClaimPost cp = new ClaimPost();
    cp.setScheduleReference("S1");
    cp.setLineNumber(1);
    return cp;
  }

  // Helper method: returns a minimal MatterStartPost for test scenarios
  private MatterStartPost matterStartPost() {
    MatterStartPost ms = new MatterStartPost();
    ms.setScheduleReference("M1");
    ms.setCreatedByUserId(EVENT_SERVICE);
    return ms;
  }
}
