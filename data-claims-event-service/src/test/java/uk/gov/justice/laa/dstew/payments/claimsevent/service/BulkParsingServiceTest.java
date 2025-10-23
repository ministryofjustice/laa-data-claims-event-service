package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionMatterStart;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionOutcome;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.CreateMatterStart201Response;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200Response;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200ResponseDetails;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.MatterStartPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionStatus;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.BulkSubmissionRetrievalException;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.BulkSubmissionUpdateException;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.ClaimCreateException;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.MatterStartCreateException;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.SubmissionCreateException;
import uk.gov.justice.laa.dstew.payments.claimsevent.mapper.BulkSubmissionMapper;
import uk.gov.justice.laa.dstew.payments.claimsevent.metrics.EventServiceMetricService;

@ExtendWith(MockitoExtension.class)
class BulkParsingServiceTest {

  private static final String BULK_SUBMISSION_CREATED_BY_USER_ID = "a-provider-user-id";

  @Mock private DataClaimsRestClient dataClaimsRestClient;
  @Mock private BulkSubmissionMapper mapper;
  @Mock private EventServiceMetricService eventServiceMetricService;

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
    when(mapper.mapToSubmissionPost(bulkSubmission, submissionId)).thenReturn(submissionPost);
    when(dataClaimsRestClient.createSubmission(submissionPost))
        .thenReturn(
            ResponseEntity.created(URI.create("/submissions/" + createdSubmissionId)).build());
    when(mapper.mapToClaimPosts(outcomes)).thenReturn(claimPosts);
    when(dataClaimsRestClient.createClaim(eq(createdSubmissionId), eq(claimPost)))
        .thenReturn(ResponseEntity.created(URI.create("/claims/claim-id")).build());
    when(mapper.mapToMatterStartRequests(matterStarts)).thenReturn(matterStartRequests);
    when(dataClaimsRestClient.createMatterStart(eq(createdSubmissionId), eq(matterStartRequest)))
        .thenReturn(ResponseEntity.created(URI.create("/matter-starts/matter-id")).build());
    when(dataClaimsRestClient.updateSubmission(eq(createdSubmissionId), any(SubmissionPatch.class)))
        .thenReturn(ResponseEntity.noContent().build());
    when(dataClaimsRestClient.updateBulkSubmission(
            eq(bulkSubmissionId.toString()), any(BulkSubmissionPatch.class)))
        .thenReturn(ResponseEntity.noContent().build());

    service.parseData(bulkSubmissionId, submissionId);

    verify(dataClaimsRestClient).getBulkSubmission(bulkSubmissionId);
    verify(mapper).mapToSubmissionPost(bulkSubmission, submissionId);
    verify(dataClaimsRestClient).createSubmission(submissionPost);
    verify(mapper).mapToClaimPosts(outcomes);
    verify(dataClaimsRestClient).createClaim(eq(createdSubmissionId), eq(claimPost));
    verify(mapper).mapToMatterStartRequests(matterStarts);
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

    final SubmissionPost submissionPost = new SubmissionPost();

    when(dataClaimsRestClient.getBulkSubmission(bulkSubmissionId))
        .thenReturn(ResponseEntity.ok(bulkSubmission));
    when(mapper.mapToSubmissionPost(bulkSubmission, submissionId)).thenReturn(submissionPost);
    when(dataClaimsRestClient.createSubmission(submissionPost))
        .thenReturn(
            ResponseEntity.created(URI.create("/submissions/" + createdSubmissionId)).build());
    when(mapper.mapToClaimPosts(List.of())).thenReturn(List.of());
    when(mapper.mapToMatterStartRequests(List.of())).thenReturn(List.of());
    when(dataClaimsRestClient.updateSubmission(eq(createdSubmissionId), any(SubmissionPatch.class)))
        .thenReturn(ResponseEntity.noContent().build());
    when(dataClaimsRestClient.updateBulkSubmission(
            eq(bulkSubmissionId.toString()), any(BulkSubmissionPatch.class)))
        .thenReturn(ResponseEntity.noContent().build());

    service.parseData(bulkSubmissionId, submissionId);

    verify(dataClaimsRestClient).getBulkSubmission(bulkSubmissionId);
    verify(mapper).mapToSubmissionPost(bulkSubmission, submissionId);
    verify(dataClaimsRestClient).createSubmission(submissionPost);
    verify(mapper).mapToClaimPosts(List.of());
    verify(mapper).mapToMatterStartRequests(List.of());
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
    when(dataClaimsRestClient.updateBulkSubmission(
            eq(bulkSubmissionId.toString()), any(BulkSubmissionPatch.class)))
        .thenReturn(ResponseEntity.noContent().build());

    assertThatThrownBy(() -> service.createSubmission(submission))
        .isInstanceOf(SubmissionCreateException.class);
    verify(dataClaimsRestClient)
        .updateBulkSubmission(
            eq(bulkSubmissionId.toString()),
            argThat(patch -> patch.getStatus() == BulkSubmissionStatus.PARSING_FAILED));
  }

  @Test
  void createSubmissionThrowsWhenStatusNot201() {
    var bulkSubmissionId = UUID.randomUUID();
    final SubmissionPost submission = new SubmissionPost().bulkSubmissionId(bulkSubmissionId);
    when(dataClaimsRestClient.createSubmission(submission))
        .thenReturn(ResponseEntity.badRequest().build());
    when(dataClaimsRestClient.updateBulkSubmission(
            eq(bulkSubmissionId.toString()), any(BulkSubmissionPatch.class)))
        .thenReturn(ResponseEntity.noContent().build());

    assertThatThrownBy(() -> service.createSubmission(submission))
        .isInstanceOf(SubmissionCreateException.class);
    verify(dataClaimsRestClient)
        .updateBulkSubmission(
            eq(bulkSubmissionId.toString()),
            argThat(patch -> patch.getStatus() == BulkSubmissionStatus.PARSING_FAILED));
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
    final ResponseEntity<Void> response = new ResponseEntity<>(headers, HttpStatus.CREATED);

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
  void updateSubmissionStatusCallsClient() {
    when(dataClaimsRestClient.updateSubmission(eq("sub1"), any(SubmissionPatch.class)))
        .thenReturn(ResponseEntity.noContent().build());

    service.updateSubmissionStatus("sub1", 2);

    verify(dataClaimsRestClient)
        .updateSubmission(
            eq("sub1"),
            argThat(
                p ->
                    p.getStatus() == SubmissionStatus.READY_FOR_VALIDATION
                        && p.getNumberOfClaims() == 2));
  }

  @Test
  void updateSubmissionStatusThrowsWhenNot2xx() {
    when(dataClaimsRestClient.updateSubmission(eq("sub1"), any(SubmissionPatch.class)))
        .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

    assertThatThrownBy(() -> service.updateSubmissionStatus("sub1", 2))
        .isInstanceOf(SubmissionCreateException.class);
  }

  @Test
  void createClaimsReturnsEmptyWhenNoClaims() {
    assertThat(service.createClaims("bulk-sub1", "sub1", null)).isEmpty();
    assertThat(service.createClaims("bulk-sub1", "sub1", List.of())).isEmpty();
  }

  @Test
  void createClaimsThrowsWithIndexInfo() {
    final BulkParsingService spyService = spy(service);
    final ClaimPost claim = new ClaimPost();

    when(dataClaimsRestClient.updateBulkSubmission(
            any(String.class), any(BulkSubmissionPatch.class)))
        .thenReturn(ResponseEntity.noContent().build());

    doThrow(new ClaimCreateException("boom")).when(spyService).createClaim("sub1", claim);

    assertThatThrownBy(() -> spyService.createClaims("bulk-sub1", "sub1", List.of(claim)))
        .isInstanceOf(ClaimCreateException.class)
        .hasMessageContaining("index 0");
    verify(dataClaimsRestClient)
        .updateBulkSubmission(
            any(String.class),
            argThat(patch -> patch.getStatus() == BulkSubmissionStatus.PARSING_FAILED));
  }

  @Test
  void createMatterStartsReturnsEmptyWhenNoMatterStarts() {
    assertThat(service.createMatterStarts("bulk-sub1", "sub1", null)).isEmpty();
    assertThat(service.createMatterStarts("bulk-sub1", "sub1", List.of())).isEmpty();
  }

  @Test
  void createMatterStartsThrowsWithIndexInfo() {
    final BulkParsingService spyService = spy(service);
    final MatterStartPost request = new MatterStartPost();

    when(dataClaimsRestClient.updateBulkSubmission(
            any(String.class), any(BulkSubmissionPatch.class)))
        .thenReturn(ResponseEntity.noContent().build());

    doThrow(new MatterStartCreateException("fail"))
        .when(spyService)
        .createMatterStart("sub1", request);

    assertThatThrownBy(() -> spyService.createMatterStarts("bulk-sub1", "sub1", List.of(request)))
        .isInstanceOf(MatterStartCreateException.class)
        .hasMessageContaining("index 0");
    verify(dataClaimsRestClient)
        .updateBulkSubmission(
            any(String.class),
            argThat(patch -> patch.getStatus() == BulkSubmissionStatus.PARSING_FAILED));
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
    final ResponseEntity<GetBulkSubmission200Response> response =
        new ResponseEntity<>(null, HttpStatus.OK);

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
    when(dataClaimsRestClient.updateBulkSubmission(eq("bulk-sub1"), any(BulkSubmissionPatch.class)))
        .thenReturn(ResponseEntity.noContent().build());

    service.updateBulkSubmissionStatus("bulk-sub1", BulkSubmissionStatus.PARSING_COMPLETED);

    verify(dataClaimsRestClient)
        .updateBulkSubmission(
            eq("bulk-sub1"), argThat(p -> p.getStatus() == BulkSubmissionStatus.PARSING_COMPLETED));
  }

  @Test
  void updateBulkSubmissionStatusThrowsWhenNot2xx() {
    when(dataClaimsRestClient.updateBulkSubmission(eq("bulk-sub1"), any(BulkSubmissionPatch.class)))
        .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

    assertThatThrownBy(
            () ->
                service.updateBulkSubmissionStatus(
                    "bulk-sub1", BulkSubmissionStatus.PARSING_COMPLETED))
        .isInstanceOf(BulkSubmissionUpdateException.class)
        .hasMessageContaining(
            "Failed to update bulk submission status for bulk submission bulk-sub1. HTTP status: 500");
  }

  @Test
  void updateBulkSubmissionStatusThrowsWhenResponseIsNull() {
    when(dataClaimsRestClient.updateBulkSubmission(eq("bulk-sub1"), any(BulkSubmissionPatch.class)))
        .thenReturn(null);

    assertThatThrownBy(
            () ->
                service.updateBulkSubmissionStatus(
                    "bulk-sub1", BulkSubmissionStatus.PARSING_COMPLETED))
        .isInstanceOf(BulkSubmissionUpdateException.class)
        .hasMessageContaining(
            "Failed to update bulk submission status for bulk submission bulk-sub1. HTTP status: null response");
  }
}
