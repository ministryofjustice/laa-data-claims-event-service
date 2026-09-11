package uk.gov.justice.laa.dstew.payments.claimsevent.validator.claim;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    // Stubs ClaimValidationService.validateAndUpdateClaims' paged "fetch all claims for this
    // submission" call (office_code + submission_id + paging, no fee_code/UFN/UCN). MockServer's
    // default query-param matching is a "contains" match (a stub's specified params must be
    // present, but the request may have extra params not specified in the stub) — so an
    // unconstrained stub here would also match the differently-parameterised duplicate-check
    // calls below (which never include submission_id) and, depending on match order, shadow them
    // with the wrong (unfiltered) response. Requiring submission_id here keeps this stub scoped to
    // its actual caller.
    stubForGetClaims(
        List.of(
            Parameter.param(
                "office_code",
                testOfficeAccountNumber != null ? testOfficeAccountNumber : "AQ2B3C"),
            Parameter.param("submission_id", submissionId.toString())),
        claimsFixture);

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

        // Also stub the parameterised GET /claims requests the duplicate-checker will issue.
        //
        // The real Data Claims API filters server-side by office_code/fee_code/
        // unique_file_number/unique_client_number (see DataClaimsRestClient.getClaims). The
        // production DuplicateClaimValidation strategies now trust that server-side filtering
        // and only re-check submission scope client-side (see
        // DuplicateClaimValidation.filterDuplicateClaimsInSameSubmission /
        // filterDuplicateClaimsInPreviousSubmission). So the mocked response here must only
        // contain the claims that would actually match this specific claim's fee_code/UFN/UCN —
        // returning the whole, unfiltered claimsFixture would make every claim in a
        // multi-claim, same-submission fixture look like a duplicate of every other claim,
        // regardless of whether their UFN/UCN genuinely match.
        if (claimNode.has("unique_file_number") && claimNode.has("unique_client_number")) {
          String ufn = claimNode.get("unique_file_number").asText();
          String ucn = claimNode.get("unique_client_number").asText();
          String feeCodeForClaim =
              claimNode.has("fee_code") && !claimNode.get("fee_code").isNull()
                  ? claimNode.get("fee_code").asText()
                  : null;

          List<Parameter> baseParams = new ArrayList<>();
          baseParams.add(
              Parameter.param(
                  "office_code",
                  testOfficeAccountNumber != null ? testOfficeAccountNumber : "AQ2B3C"));

          // submission_statuses the validator uses
          baseParams.add(Parameter.param("submission_statuses", "CREATED"));
          baseParams.add(Parameter.param("submission_statuses", "VALIDATION_IN_PROGRESS"));
          baseParams.add(Parameter.param("submission_statuses", "READY_FOR_VALIDATION"));
          baseParams.add(Parameter.param("submission_statuses", "VALIDATION_SUCCEEDED"));

          if (feeCodeForClaim != null && !feeCodeForClaim.isBlank()) {
            baseParams.add(Parameter.param("fee_code", feeCodeForClaim));
          }

          baseParams.add(Parameter.param("unique_file_number", ufn));

          // claim_statuses the validator uses
          baseParams.add(Parameter.param("claim_statuses", "READY_TO_PROCESS"));
          baseParams.add(Parameter.param("claim_statuses", "VALID"));

          // Register stub so duplicate-check GET /claims returns only claims genuinely matching
          // this claim's fee_code/UFN/UCN (emulating real server-side filtering), rather than
          // the entire, unfiltered claimsFixture.
          //
          // Two variants are needed: most strategies (Legal Help/disbursement) key on fee code +
          // UFN + UCN and always send unique_client_number, but the Crime Lower strategy keys on
          // fee code + UFN only and deliberately omits unique_client_number from its request (see
          // DuplicateClaimCrimeLowerValidationServiceStrategy). MockServer's query-param matching
          // requires every parameter named in a stub to be present in the request, so a stub that
          // requires unique_client_number would never match Crime Lower's request and would 502.
          List<Parameter> withUcnParams = new ArrayList<>(baseParams);
          withUcnParams.add(Parameter.param("unique_client_number", ucn));
          stubForGetClaims(withUcnParams, filterClaimsMatching(root, feeCodeForClaim, ufn, ucn));

          // Registered after the UCN-specific stub above: MockServer checks expectations in
          // registration order and uses the first match, so real requests that do include
          // unique_client_number (Legal Help etc.) still hit the more specific stub above; only
          // requests that omit it entirely (Crime Lower) fall through to this one.
          stubForGetClaims(baseParams, filterClaimsMatching(root, feeCodeForClaim, ufn, null));
        }
      }
    }

    // Fallback stub for duplicate-check GET /claims requests that don't match any of the
    // fee_code/UFN/UCN-specific stubs registered above — e.g. a claim whose unique_file_number or
    // unique_client_number is null/blank, which never enters the per-claim stubbing block. Without
    // this, such a request would 502 (no MockServer expectation matches) and fail the whole
    // validation run rather than being handled as "no duplicates found". Registered last so it
    // only takes effect when none of the more specific stubs above match.
    stubForGetClaims(
        List.of(
            Parameter.param(
                "office_code",
                testOfficeAccountNumber != null ? testOfficeAccountNumber : "AQ2B3C"),
            Parameter.param("submission_statuses", "CREATED"),
            Parameter.param("submission_statuses", "VALIDATION_IN_PROGRESS"),
            Parameter.param("submission_statuses", "READY_FOR_VALIDATION"),
            Parameter.param("submission_statuses", "VALIDATION_SUCCEEDED"),
            Parameter.param("claim_statuses", "READY_TO_PROCESS"),
            Parameter.param("claim_statuses", "VALID")),
        mapper.createObjectNode().set("content", mapper.createArrayNode()));

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
   * Builds a filtered {@code ClaimResultSet}-shaped JSON body containing only the claims from
   * {@code root} whose {@code fee_code}, {@code unique_file_number}, and {@code
   * unique_client_number} all match the given values.
   *
   * <p>This emulates the server-side filtering the real Data Claims API performs for {@code GET
   * /claims?fee_code=...&unique_file_number=...&unique_client_number=...}, which the production
   * {@code DuplicateClaimValidation} strategies rely on (they no longer re-check fee code/UFN/UCN
   * client-side — only submission scope). Without this filtering, stubbing the whole, unfiltered
   * fixture for every claim's query would make every claim in a multi-claim, same-submission
   * fixture look like a duplicate of every other claim in that submission, regardless of whether
   * their UFN/UCN genuinely match.
   *
   * @param root the parsed root of the claims fixture (must have a {@code content} array)
   * @param feeCode the fee code to match, or {@code null} if the claim being queried for has none
   * @param uniqueFileNumber the unique file number to match
   * @param uniqueClientNumber the unique client number to match
   * @return a {@code ClaimResultSet}-shaped {@link JsonNode} containing only the matching claims
   */
  protected JsonNode filterClaimsMatching(
      JsonNode root, String feeCode, String uniqueFileNumber, String uniqueClientNumber) {
    ArrayNode filteredContent = mapper.createArrayNode();

    if (root.has("content") && root.get("content").isArray()) {
      for (JsonNode candidate : root.get("content")) {
        if (claimMatches(candidate, feeCode, uniqueFileNumber, uniqueClientNumber)) {
          filteredContent.add(candidate);
        }
      }
    }

    ObjectNode filteredResponse = mapper.createObjectNode();
    filteredResponse.set("content", filteredContent);
    filteredResponse.put("total_pages", 1);
    filteredResponse.put("total_elements", filteredContent.size());
    filteredResponse.put("number", 0);
    filteredResponse.put("size", filteredContent.size());
    return filteredResponse;
  }

  /**
   * @param uniqueClientNumber the UCN to match, or {@code null} to skip UCN filtering entirely
   *     (mirrors strategies like Crime Lower that key on fee code + UFN only and never send a
   *     unique_client_number query param)
   */
  private boolean claimMatches(
      JsonNode candidate, String feeCode, String uniqueFileNumber, String uniqueClientNumber) {
    String candidateFeeCode =
        candidate.has("fee_code") && !candidate.get("fee_code").isNull()
            ? candidate.get("fee_code").asText()
            : null;
    boolean feeCodeMatches = Objects.equals(feeCode, candidateFeeCode);

    boolean ufnMatches =
        candidate.has("unique_file_number")
            && !candidate.get("unique_file_number").isNull()
            && Objects.equals(uniqueFileNumber, candidate.get("unique_file_number").asText());

    boolean ucnMatches =
        uniqueClientNumber == null
            || (candidate.has("unique_client_number")
                && !candidate.get("unique_client_number").isNull()
                && Objects.equals(
                    uniqueClientNumber, candidate.get("unique_client_number").asText()));

    return feeCodeMatches && ufnMatches && ucnMatches;
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

    List<ValidationIssue> unmatchedNew = new ArrayList<>();
    if (issues != null) unmatchedNew.addAll(issues);

    List<ValidationMessagePatch> unmatchedExisting = new ArrayList<>();
    if (existing != null) unmatchedExisting.addAll(existing);

    Iterator<ValidationIssue> newIt = unmatchedNew.iterator();
    while (newIt.hasNext()) {
      ValidationIssue ni = newIt.next();
      ValidationMessagePatch match = findExactExisting(ni, unmatchedExisting);
      if (match != null) {
        System.out.printf(
            "[assertExactMatch] MATCHED claim=%s code=%s message=%s%n",
            currentClaim.getId(), ni.getCode(), ni.getMessage());
        newIt.remove();
        unmatchedExisting.remove(match);
      } else {
        System.out.printf(
            "[assertExactMatch] NO MATCH claim=%s code=%s severity=%s path=%s message=%s technical=%s%n",
            currentClaim.getId(),
            ni.getCode(),
            ni.getSeverity(),
            ni.getPath(),
            ni.getMessage(),
            ni.getTechnicalMessage());
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
