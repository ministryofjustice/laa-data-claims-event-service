package uk.gov.justice.laa.dstew.payments.claimsevent.validator.claim;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockserver.model.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.model.Claim;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.model.ValidationIssue;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.service.ValidationService;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.util.ClaimMapper;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MessageListenerBase;
import uk.gov.justice.laa.dstew.payments.claimsevent.helper.MockServerIntegrationTest;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.SubmissionValidationService;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

@ActiveProfiles("test")
@ImportTestcontainers(MessageListenerBase.class)
@Import(MockServerIntegrationTest.ClaimsConfiguration.class)
@TestInstance(Lifecycle.PER_CLASS)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.cloud.aws.sqs.enabled=false", "laa.bulk-claim-queue.name=not-used"})
public abstract class ClaimValidationIntegrationTestBase extends MockServerIntegrationTest {

  // ── Path prefix constants ────────────────────────────────────────────────
  protected static final String CLAIMS_BASE_PATH = "data-claims/get-claims/";
  protected static final String SUBMISSION_BASE_PATH = "data-claims/get-submission/";

  // ── Submission fixture constants ─────────────────────────────────────────
  protected static final String SUBMISSION_LEGAL_HELP =
      SUBMISSION_BASE_PATH + "get-submission-with-claim.json";
  protected static final String SUBMISSION_CRIME_LOWER =
      SUBMISSION_BASE_PATH + "get-submission-with-claim-crime-lower.json";
  protected static final String SUBMISSION_MEDIATION =
      SUBMISSION_BASE_PATH + "get-submission-with-claim-mediation.json";

  @Autowired protected ValidationService validationService;

  @Autowired protected SubmissionValidationService submissionValidationService;

  protected final ObjectMapper mapper = objectMapper;

  protected AreaOfLaw testAreaOfLaw;
  protected String testOfficeAccountNumber;

  @BeforeAll
  void beforeAllBase() {
    // parent MockServerIntegrationTest.beforeAll will have run
  }

  @BeforeEach
  void resetMockServerBeforeEach() {
    mockServerClient.reset();
  }

  /**
   * Runs the full submission validation flow using the given submission fixture and claims fixture.
   * Submission ID and bulk submission ID are read dynamically from the submission fixture file.
   */
  protected SubmissionValidationContext runSubmissionValidationWithClaims(
      String submissionFixture, String claimsFixture) throws Exception {

    String submissionJson = readJsonFromFile(submissionFixture);
    JsonNode submissionNode = mapper.readTree(submissionJson);

    UUID submissionId = UUID.fromString(submissionNode.get("submission_id").asText());
    UUID bulkSubmissionId = UUID.fromString(submissionNode.get("bulk_submission_id").asText());

    if (submissionNode.has("area_of_law") && submissionNode.get("area_of_law").isTextual()) {
      String a = submissionNode.get("area_of_law").asText().replace(' ', '_').toUpperCase();
      try {
        testAreaOfLaw = AreaOfLaw.valueOf(a);
      } catch (Exception e) {
        testAreaOfLaw = null;
      }
    }

    if (submissionNode.has("office_account_number")
        && submissionNode.get("office_account_number").isTextual()) {
      testOfficeAccountNumber = submissionNode.get("office_account_number").asText();
    }

    stubForGetSubmission(submissionId, submissionFixture);
    stubForGetClaims(Collections.emptyList(), claimsFixture);

    String claimsJson = readJsonFromFile(claimsFixture);
    JsonNode root = mapper.readTree(claimsJson);
    if (root.has("content") && root.get("content").isArray()) {
      for (JsonNode claimNode : root.get("content")) {
        if (claimNode.has("id") && claimNode.get("id").isTextual()) {
          stubForUpdateClaim(submissionId, UUID.fromString(claimNode.get("id").asText()));
        }
        if (claimNode.has("fee_code") && !claimNode.get("fee_code").isNull()) {
          String feeCode = claimNode.get("fee_code").asText();
          if (feeCode != null && !feeCode.isBlank()) {
            if (feeCode.equals("CAPA")) {
              stubForGetFeeDetails("CAPA", "fee-scheme/get-fee-details-disbursement.json");
            } else {
              stubForGetFeeDetails(feeCode, "fee-scheme/get-fee-details-200.json");
            }
          }
        }
      }
    }

    stubForPostFeeCalculation("fee-scheme/post-fee-calculation-200.json");

    stubForUpdateSubmission(submissionId);
    stubForUpdateBulkSubmission(bulkSubmissionId);

    if (testOfficeAccountNumber != null) {
      stubForGetProviderOffice(
          testOfficeAccountNumber,
          Collections.emptyList(),
          "provider-details/get-firm-schedules-openapi-200.json");
    }

    AreaOfLaw criteriaAreaOfLaw = testAreaOfLaw != null ? testAreaOfLaw : AreaOfLaw.LEGAL_HELP;
    getStubForGetSubmissionByCriteria(
        List.of(
            Parameter.param(
                "offices", testOfficeAccountNumber != null ? testOfficeAccountNumber : "AQ2B3C"),
            Parameter.param("area_of_law", criteriaAreaOfLaw.name()),
            Parameter.param("submission_period", "APR-2025")),
        "data-claims/get-submission/get-submissions-by-filter_no_content.json");

    return submissionValidationService.validateSubmission(submissionId);
  }

  /**
   * Collects all distinct ValidationIssue codes produced by the new validation engine across every
   * claim in the given claims fixture. Use this in the error-code assertion test.
   */
  protected Set<String> collectValidationIssueCodes(String submissionFixture, String claimsFixture)
      throws Exception {
    SubmissionValidationContext context =
        runSubmissionValidationWithClaims(submissionFixture, claimsFixture);
    List<ClaimResponse> claims = parseClaimsFromFixture(claimsFixture);
    Set<String> codes = new HashSet<>();

    // set up all the related claims
    List<Claim> relatedClaims = claims.stream().map(ClaimMapper::fromClaimResponse).toList();
    relatedClaims.forEach(
        c -> {
          c.setAreaOfLaw(testAreaOfLaw);
          c.setOfficeAccountNumber(testOfficeAccountNumber);
        });

    for (Claim currentClaim : relatedClaims) {

      List<ValidationIssue> issues =
          validationService.validateClaim(currentClaim, null, relatedClaims).getIssues();

      if (issues != null) {
        for (ValidationIssue issue : issues) {
          System.out.printf(
              "[collectValidationIssueCodes] claim=%s code=%s path=%s severity=%s message=%s technical=%s%n",
              currentClaim.getId(),
              issue.getCode(),
              issue.getPath(),
              issue.getSeverity(),
              issue.getMessage(),
              issue.getTechnicalMessage());
          if (issue.getCode() != null) codes.add(issue.getCode());
        }
      }
    }
    return codes;
  }

  protected List<ClaimResponse> parseClaimsFromFixture(String fixtureRelativePath)
      throws Exception {
    String fixtureJson = readJsonFromFile(fixtureRelativePath);
    JsonNode root = mapper.readTree(fixtureJson);
    List<ClaimResponse> claimResponses = new ArrayList<>();
    if (root.has("content") && root.get("content").isArray()) {
      for (JsonNode claimNode : root.get("content")) {
        ClaimResponse cr = mapper.treeToValue(claimNode, ClaimResponse.class);
        claimResponses.add(cr);
      }
    }
    return claimResponses;
  }

  protected void assertExactMatchBetweenValidationAndReport(
      ClaimResponse currentClaim, List<ClaimResponse> claims, SubmissionValidationContext context)
      throws Exception {

    // set-up claim being validated
    Claim mapped = ClaimMapper.fromClaimResponse(currentClaim);
    mapped.setAreaOfLaw(testAreaOfLaw);
    mapped.setOfficeAccountNumber(testOfficeAccountNumber);

    // set up all the related claims
    List<Claim> relatedClaims = claims.stream().map(ClaimMapper::fromClaimResponse).toList();
    relatedClaims.forEach(
        c -> {
          c.setAreaOfLaw(testAreaOfLaw);
          c.setOfficeAccountNumber(testOfficeAccountNumber);
        });

    List<ValidationIssue> issues =
        validationService.validateClaim(mapped, null, relatedClaims).getIssues();

    var reportOpt = context.getClaimReport(currentClaim.getId());
    if (reportOpt.isEmpty()) {
      if (issues == null || issues.isEmpty()) {
        return;
      }
      throw new AssertionError("No claim report available for claim " + currentClaim.getId());
    }

    List<ValidationMessagePatch> existing = reportOpt.get().getMessages();

    // ── Debug output ─────────────────────────────────────────────────────────
    System.out.printf(
        "[assertExactMatch] claim=%s issues(new)=%d patches(existing)=%d%n",
        currentClaim.getId(),
        issues == null ? 0 : issues.size(),
        existing == null ? 0 : existing.size());
    if (issues != null) {
      for (ValidationIssue ni : issues) {
        System.out.printf(
            "  [new]      code=%-50s sev=%-5s path=%-30s msg=%s | technical=%s%n",
            ni.getCode(),
            ni.getSeverity(),
            ni.getPath(),
            ni.getMessage(),
            ni.getTechnicalMessage());
      }
    }
    if (existing != null) {
      for (ValidationMessagePatch em : existing) {
        System.out.printf(
            "  [existing] src=%-30s type=%-5s display=%s | technical=%s%n",
            em.getSource(), em.getType(), em.getDisplayMessage(), em.getTechnicalMessage());
      }
    }
    // ─────────────────────────────────────────────────────────────────────────

    List<ValidationIssue> unmatchedNew = new ArrayList<>();
    if (issues != null) unmatchedNew.addAll(issues);

    List<ValidationMessagePatch> unmatchedExisting = new ArrayList<>();
    if (existing != null) unmatchedExisting.addAll(existing);

    Iterator<ValidationIssue> newIt = unmatchedNew.iterator();
    while (newIt.hasNext()) {
      ValidationIssue ni = newIt.next();
      ValidationMessagePatch match = findExactExisting(ni, unmatchedExisting);
      if (match != null) {
        newIt.remove();
        unmatchedExisting.remove(match);
      }
    }

    if (!unmatchedNew.isEmpty() || !unmatchedExisting.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      sb.append("Claim ")
          .append(currentClaim.getId())
          .append(" mismatch")
          .append(" [new=")
          .append(unmatchedNew.size())
          .append(", existing=")
          .append(unmatchedExisting.size())
          .append("]:\n");
      for (ValidationIssue ni : unmatchedNew) {
        sb.append("Only in new: code=")
            .append(ni.getCode())
            .append(" severity=")
            .append(ni.getSeverity())
            .append(" path=")
            .append(ni.getPath())
            .append(" message=")
            .append(ni.getMessage())
            .append(" technical=")
            .append(ni.getTechnicalMessage())
            .append("\n");
      }
      for (ValidationMessagePatch em : unmatchedExisting) {
        sb.append("Only in existing: source=")
            .append(em.getSource())
            .append(" type=")
            .append(em.getType())
            .append(" display=")
            .append(em.getDisplayMessage())
            .append(" technical=")
            .append(em.getTechnicalMessage())
            .append("\n");
      }
      throw new AssertionError(sb.toString());
    }
  }

  private ValidationMessagePatch findExactExisting(
      ValidationIssue ni, List<ValidationMessagePatch> existing) {
    for (ValidationMessagePatch em : existing) {
      String sev = ni.getSeverity() == null ? null : ni.getSeverity().name();
      String type = em.getType() == null ? null : em.getType().name();
      if (Objects.equals(ni.getMessage(), em.getDisplayMessage())
          && Objects.equals(ni.getTechnicalMessage(), em.getTechnicalMessage())
          && Objects.equals(sev, type)) {
        return em;
      }
    }
    return null;
  }
}
