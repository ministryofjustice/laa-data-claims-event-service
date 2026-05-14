package uk.gov.justice.laa.dstew.payments.claimsevent.validator.claim;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
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

  @Autowired protected ValidationService validationService;

  @Autowired protected SubmissionValidationService submissionValidationService;

  protected static final UUID SUBMISSION_ID =
      UUID.fromString("0561d67b-30ed-412e-8231-f6296a53538d");
  protected static final UUID BULK_SUBMISSION_ID =
      UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

  protected final ObjectMapper mapper = objectMapper;

  protected uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw testAreaOfLaw;
  protected String testOfficeAccountNumber;

  @BeforeAll
  void beforeAllBase() {
    // parent MockServerIntegrationTest.beforeAll will have run
  }

  protected SubmissionValidationContext runSubmissionValidationWithClaims(
      String fixtureRelativePath) throws Exception {
    String submissionFixture = "data-claims/get-submission/get-submission-with-claim.json";
    stubForGetSubmission(SUBMISSION_ID, submissionFixture);
    stubForGetClaims(Collections.emptyList(), fixtureRelativePath);

    // capture submission-level values used by mapping/validation
    String submissionJson = readJsonFromFile(submissionFixture);
    JsonNode submissionNode = mapper.readTree(submissionJson);
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

    String claimsJson = readJsonFromFile(fixtureRelativePath);
    JsonNode root = mapper.readTree(claimsJson);
    if (root.has("content") && root.get("content").isArray()) {
      for (JsonNode claimNode : root.get("content")) {
        if (claimNode.has("id") && claimNode.get("id").isTextual()) {
          stubForUpdateClaim(SUBMISSION_ID, UUID.fromString(claimNode.get("id").asText()));
        }
        if (claimNode.has("fee_code") && !claimNode.get("fee_code").isNull()) {
          String feeCode = claimNode.get("fee_code").asText();
          if (feeCode != null && !feeCode.isBlank()) {
            try {
              stubForGetFeeDetails(feeCode, "fee-scheme/get-fee-details-200.json");
            } catch (Exception ignored) {
            }
          }
        }
      }
    }

    stubForPostFeeCalculation("fee-scheme/post-fee-calculation-200.json");
    stubForUpdateSubmission(SUBMISSION_ID);
    stubForUpdateBulkSubmission(BULK_SUBMISSION_ID);
    stubForGetFeeDetails("CAPA", "fee-scheme/get-fee-details-disbursement.json");
    stubForGetProviderOffice(
        "AQ2B3C", Collections.emptyList(), "provider-details/get-firm-schedules-openapi-200.json");

    // Stub submission search by criteria used by submission validation flow
    getStubForGetSubmissionByCriteria(
        List.of(
            Parameter.param("offices", "AQ2B3C"),
            Parameter.param("area_of_law", AreaOfLaw.LEGAL_HELP.name()),
            Parameter.param("submission_period", "APR-2025")),
        "data-claims/get-submission/get-submissions-by-filter_no_content.json");

    return submissionValidationService.validateSubmission(SUBMISSION_ID);
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
      ClaimResponse claimResponse, SubmissionValidationContext context) throws Exception {
    List<ClaimResponse> onlyThis = List.of(claimResponse);
    List<Claim> relatedClaims = onlyThis.stream().map(ClaimMapper::fromClaimResponse).toList();

    // Map and populate submission-level fields the validation service expects (same as
    // claimsValidatorValidateClaim)
    Claim mapped = ClaimMapper.fromClaimResponse(claimResponse);
    if (testAreaOfLaw != null) {
      mapped.setAreaOfLaw(testAreaOfLaw);
    }
    if (testOfficeAccountNumber != null) {
      mapped.setOfficeAccountNumber(testOfficeAccountNumber);
    }

    // ensure related claims also have submission-level fields set
    List<Claim> related =
        relatedClaims.stream()
            .peek(
                c -> {
                  if (testAreaOfLaw != null) c.setAreaOfLaw(testAreaOfLaw);
                  if (testOfficeAccountNumber != null)
                    c.setOfficeAccountNumber(testOfficeAccountNumber);
                })
            .collect(Collectors.toList());

    // TODO: remove when mapper fixed
    mapped.setStageReachedCode(claimResponse.getStageReachedCode());

    List<ValidationIssue> issues =
        validationService.validateClaim(mapped, null, related).getIssues();

    var reportOpt = context.getClaimReport(claimResponse.getId());
    if (reportOpt.isEmpty()) {
      if (issues == null || issues.isEmpty()) {
        return;
      }
      throw new AssertionError("No claim report available for claim " + claimResponse.getId());
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
        newIt.remove();
        unmatchedExisting.remove(match);
      }
    }

    if (!unmatchedNew.isEmpty() || !unmatchedExisting.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      sb.append("Claim ").append(claimResponse.getId()).append(" mismatch:\n");
      for (ValidationIssue ni : unmatchedNew) {
        sb.append("Only in new: code=")
            .append(ni.getCode())
            .append(" severity=")
            .append(ni.getSeverity())
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
