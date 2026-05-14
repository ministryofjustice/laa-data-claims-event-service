package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.model.Claim;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.model.ValidationIssue;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.model.ValidationResult;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.service.ValidationService;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.util.ClaimMapper;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResultSet;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessageType;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.EventServiceIllegalArgumentException;
import uk.gov.justice.laa.dstew.payments.claimsevent.metrics.EventServiceMetricService;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationReport;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.BasicClaimValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.ClaimValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.ClaimWithAreaOfLawValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.DisbursementClaimStartDateValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.DuplicateClaimValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.EffectiveCategoryOfLawClaimValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.MandatoryFieldClaimValidator;

/**
 * A service for validating submitted claims that are ready to process. Validation errors will
 * result in claims being marked as invalid and all validation errors will be reported against the
 * claim.
 */
@Slf4j
@Service
public class ClaimValidationService {

  private static final String SCHEMA_CONFIG_WARNING_CODE = "SCHEMA_CONFIG_WARNING";

  private final ValidationService validationService;
  private final CategoryOfLawValidationService categoryOfLawValidationService;
  private final DataClaimsRestClient dataClaimsRestClient;
  private final EventServiceMetricService eventServiceMetricService;
  private final BulkClaimUpdater bulkClaimUpdater;
  private final List<ClaimValidator> claimValidator;
  private final int claimValidationBatchSize;

  /**
   * Claim validation service constructor.
   *
   * @param categoryOfLawValidationService The category of law validation service
   * @param dataClaimsRestClient The data claims rest client
   * @param eventServiceMetricService The event service
   * @param bulkClaimUpdater The bulk claim updater
   * @param claimValidator The claim validator
   * @param claimValidationBatchSize The batch size of claims to validate at once
   */
  public ClaimValidationService(
      ValidationService validationService,
      CategoryOfLawValidationService categoryOfLawValidationService,
      DataClaimsRestClient dataClaimsRestClient,
      EventServiceMetricService eventServiceMetricService,
      BulkClaimUpdater bulkClaimUpdater,
      List<ClaimValidator> claimValidator,
      @Value("${claim.validation.claim-validation-batch-size}") int claimValidationBatchSize) {
    this.categoryOfLawValidationService = categoryOfLawValidationService;
    this.dataClaimsRestClient = dataClaimsRestClient;
    this.eventServiceMetricService = eventServiceMetricService;
    this.bulkClaimUpdater = bulkClaimUpdater;
    this.claimValidator = claimValidator;
    this.claimValidationBatchSize = claimValidationBatchSize;
    this.validationService = validationService;
  }

  /**
   * Validate a list of claims in a submission and Updates it in the Data Claims API.
   *
   * @param submission the submission
   */
  public void validateAndUpdateClaims(
      SubmissionResponse submission, SubmissionValidationContext context) {

    int pageNumber = 0;
    Integer totalPages = Integer.MAX_VALUE;

    // Loop over multiple pages in order to process claims in batches
    while (pageNumber < totalPages) {

      ClaimResultSet claims =
          dataClaimsRestClient
              .getClaims(
                  submission.getOfficeAccountNumber(),
                  String.valueOf(submission.getSubmissionId()),
                  Collections.emptyList(),
                  null,
                  null,
                  null,
                  null,
                  null,
                  pageNumber,
                  claimValidationBatchSize,
                  "id,asc")
              .getBody();

      if (claims == null) {
        throw new EventServiceIllegalArgumentException("Claims response is null from Claims API");
      }

      log.info(
          "Validating claims page {} from submission {}", pageNumber, submission.getSubmissionId());

      // Set total pages
      totalPages = claims.getTotalPages();

      List<ClaimResponse> submissionClaims = claims.getContent();

      Map<String, FeeDetailsResponseWrapper> feeDetailsResponseMap =
          categoryOfLawValidationService.getFeeDetailsResponseForAllFeeCodesInClaims(
              submissionClaims);

      List<ClaimResponse> finalSubmissionClaims = submissionClaims;

      // Submit validation tasks for each claim
      for (ClaimResponse claim : submissionClaims) {
        validateClaim(
            claim,
            finalSubmissionClaims,
            feeDetailsResponseMap,
            submission.getAreaOfLaw(),
            submission.getOfficeAccountNumber(),
            context);

        // Map and validate claim using a dedicated helper method (keeps mapping logic isolated)
        ValidationResult validationResult =
            claimsValidatorValidateClaim(claim, submission, context, finalSubmissionClaims);
      }

      // Increment page number
      pageNumber++;

      log.debug(
          "Saving claims from page {} for submission {} to Data Claims API",
          pageNumber,
          submission.getSubmissionId());

      // Update claims status after all validations
      bulkClaimUpdater.updateClaims(
          submission.getSubmissionId(),
          submissionClaims,
          submission.getAreaOfLaw(),
          context,
          feeDetailsResponseMap);
    }
  }

  /**
   * Validates the provided claim by performing various checks such as: - JSON schema validation , -
   * field level business validations (e.g. date in the past) - further validations that use
   * external APIs such as - category of law checks - duplicate claim checks - fee calculations. Any
   * errors encountered during the validation process are added to the submission validation
   * context.
   *
   * @param claim the claim object to validate
   * @param feeDetailsResponseMap a map containing FeeDetailsResponse and their corresponding
   *     feeCodes
   * @param areaOfLaw the area of law for the parent submission: some validations change depending
   *     on the area of law.
   */
  private void validateClaim(
      ClaimResponse claim,
      List<ClaimResponse> submissionClaims,
      Map<String, FeeDetailsResponseWrapper> feeDetailsResponseMap,
      AreaOfLaw areaOfLaw,
      String officeCode,
      SubmissionValidationContext context) {

    Assert.notNull(claim.getId(), "Claim ID must not be null");
    eventServiceMetricService.startClaimValidationTimer(UUID.fromString(claim.getId()));

    FeeDetailsResponseWrapper feeDetailsResponseWrapper =
        feeDetailsResponseMap.get(claim.getFeeCode());
    handleFeeDetailsError(claim, feeDetailsResponseWrapper, context);

    // Includes:
    // - JSON scheme validation
    // - Mandatory field validations
    // - UFN
    // - Stage reached validation
    // - Schedule Reference Claim
    // - Disbursements VAT amount
    // - Claim dates:
    // -- Case start date
    // -- Case concluded date
    // -- Transfer date
    // -- Representation order date
    // - Client dates
    // -- Client date of birth
    // - Matter Type Code
    // - Effective category of law
    String feeCalculationType =
        feeDetailsResponseWrapper.getFeeDetailsResponse() != null
            ? feeDetailsResponseWrapper.getFeeDetailsResponse().getFeeType()
            : null;
    claimValidator.stream()
        .sorted(
            Comparator.comparingInt(ClaimValidator::priority)) // Ensure validators are run in order
        .forEach(
            x -> {
              switch (x) {
                case BasicClaimValidator validator -> validator.validate(claim, context);
                case ClaimWithAreaOfLawValidator validator ->
                    validator.validate(claim, context, areaOfLaw);
                case EffectiveCategoryOfLawClaimValidator validator ->
                    validator.validate(claim, context, officeCode, feeDetailsResponseMap);
                case DisbursementClaimStartDateValidator validator ->
                    validator.validate(claim, context, feeCalculationType);
                case MandatoryFieldClaimValidator validator ->
                    validator.validate(claim, context, areaOfLaw, feeCalculationType);
                case DuplicateClaimValidator validator ->
                    validator.validate(
                        claim,
                        context,
                        areaOfLaw,
                        officeCode,
                        submissionClaims,
                        feeCalculationType);
                default -> throw new EventServiceIllegalArgumentException("Unknown validator used");
              }
            });

    eventServiceMetricService.stopClaimValidationTimer(UUID.fromString(claim.getId()));

    // Check claim status and record metric
    recordClaimMetrics(claim, context);
  }

  private void handleFeeDetailsError(
      ClaimResponse claim, FeeDetailsResponseWrapper wrapper, SubmissionValidationContext context) {
    if (!StringUtils.hasText(claim.getFeeCode())) {
      return;
    }

    if (wrapper.isError()) {
      log.error(
          "Fee Scheme Platform API has returned an unexpected error for feeCode: {}",
          claim.getFeeCode());
      context.addClaimError(
          claim.getId(),
          ClaimValidationError.TECHNICAL_ERROR_FEE_CALCULATION_SERVICE,
          claim.getFeeCode());
    } else if (wrapper.getFeeDetailsResponse() == null) {
      log.info("Fee details response returned null for fee code: {}", claim.getFeeCode());
      context.addClaimError(
          claim.getId(),
          ClaimValidationError.INVALID_CATEGORY_OF_LAW_AND_FEE_CODE,
          claim.getFeeCode());
    }
  }

  private void recordClaimMetrics(ClaimResponse claim, SubmissionValidationContext context) {
    Optional<ClaimValidationReport> claimReportOptional = context.getClaimReport(claim.getId());
    if (claimReportOptional.isEmpty()) {
      eventServiceMetricService.incrementTotalClaimsValidatedAndValid();
      return;
    }

    List<ValidationMessagePatch> messages = claimReportOptional.get().getMessages();

    // Record all messages (Helps track most common errors found)
    messages.forEach(x -> eventServiceMetricService.recordValidationMessage(x, true));

    // Claim could have either errors or warnings so record both
    if (messages.stream().anyMatch(x -> x.getType().equals(ValidationMessageType.ERROR))) {
      eventServiceMetricService.incrementTotalClaimsValidatedAndErrorsFound();
    }

    if (messages.stream().anyMatch(x -> x.getType().equals(ValidationMessageType.WARNING))) {
      eventServiceMetricService.incrementTotalClaimsValidatedAndWarningsFound();
    }
  }

  /**
   * Map a ClaimResponse to the internal validation Claim model and run the validation service.
   *
   * <p>Submission-level fields (areaOfLaw and officeAccountNumber) are set here because they are
   * not available on the ClaimResponse itself.
   */
  private ValidationResult claimsValidatorValidateClaim(
      ClaimResponse claimResponse,
      SubmissionResponse submission,
      SubmissionValidationContext context,
      List<ClaimResponse> finalSubmissionClaims) {

    log.info("Claim level validation start: claimId={}", claimResponse.getId());

    // Map ClaimResponse -> validation-core Claim before calling validation service
    Claim mappedClaim = ClaimMapper.fromClaimResponse(claimResponse);

    // populate submission-level context fields the mapper cannot infer
    mappedClaim.setAreaOfLaw(submission.getAreaOfLaw());
    mappedClaim.setOfficeAccountNumber(submission.getOfficeAccountNumber());

    List<Claim> relatedClaims =
        finalSubmissionClaims.stream().map(ClaimMapper::fromClaimResponse).toList();

    // V1 Claim Validation
    ValidationResult validationResult =
        validationService.validateClaim(mappedClaim, null, relatedClaims);

    if (validationResult == null) {
      log.warn("Validation service returned null for claim {}", claimResponse.getId());
      return null;
    }

    // === REGRESSION DETECTION: Compare new validator results with old validator ===
    compareValidationResults(claimResponse.getId(), validationResult.getIssues(), context);

    log.info("Claim level validation end: claimId={}", claimResponse.getId());

    return validationResult;
  }

  /**
   * Compare new validation issues against existing validation report to detect regressions.
   *
   * @param claimId the claim ID
   * @param newIssues issues from the new validator (from ValidationResult.getIssues())
   * @param context the submission validation context containing the old validator's report
   */
  private void compareValidationResults(
      String claimId, List<ValidationIssue> newIssues, SubmissionValidationContext context) {

    // This warning is expected during migration while schema coverage is being expanded.
    List<ValidationIssue> unmatchedNewIssues =
        Optional.ofNullable(newIssues).orElseGet(List::of).stream()
            .filter(
                issue ->
                    issue != null
                        && !SCHEMA_CONFIG_WARNING_CODE.equalsIgnoreCase(
                            normaliseText(issue.getCode())))
            .collect(Collectors.toCollection(ArrayList::new));

    List<ValidationMessagePatch> unmatchedExistingMessages =
        context
            .getClaimReport(claimId)
            .map(ClaimValidationReport::getMessages)
            .filter(Objects::nonNull)
            .map(ArrayList::new)
            .orElseGet(ArrayList::new);

    if (unmatchedExistingMessages.isEmpty() && unmatchedNewIssues.isEmpty()) {
      log.debug("Claim {} validators matched: both produced no issues", claimId);
      return;
    }

    removeExactMatches(unmatchedNewIssues, unmatchedExistingMessages);

    if (unmatchedExistingMessages.isEmpty() && unmatchedNewIssues.isEmpty()) {
      log.debug("Claim {} validators matched exactly", claimId);
      return;
    }

    Iterator<ValidationIssue> newIssueIterator = unmatchedNewIssues.iterator();
    while (newIssueIterator.hasNext()) {
      ValidationIssue newIssue = newIssueIterator.next();
      ValidationMessagePatch likelyExistingMessage =
          findLikelyMatchingExistingMessage(newIssue, unmatchedExistingMessages);

      if (likelyExistingMessage == null) {
        continue;
      }

      logMismatchDetails(claimId, newIssue, likelyExistingMessage);
      unmatchedExistingMessages.remove(likelyExistingMessage);
      newIssueIterator.remove();
    }

    unmatchedNewIssues.forEach(
        issue ->
            log.warn("Claim {} issue only in new validator: {}", claimId, describeNewIssue(issue)));

    unmatchedExistingMessages.forEach(
        message ->
            log.warn(
                "Claim {} issue only in existing validator: {}",
                claimId,
                describeExistingMessage(message)));
  }

  private void removeExactMatches(
      List<ValidationIssue> newIssues, List<ValidationMessagePatch> existingMessages) {
    Iterator<ValidationIssue> newIssueIterator = newIssues.iterator();
    while (newIssueIterator.hasNext()) {
      ValidationIssue newIssue = newIssueIterator.next();
      ValidationMessagePatch exactExistingMessage =
          findExactMatchingExistingMessage(newIssue, existingMessages);

      if (exactExistingMessage == null) {
        continue;
      }

      existingMessages.remove(exactExistingMessage);
      newIssueIterator.remove();
    }
  }

  private ValidationMessagePatch findExactMatchingExistingMessage(
      ValidationIssue newIssue, List<ValidationMessagePatch> existingMessages) {
    return existingMessages.stream()
        .filter(
            existingMessage ->
                sameText(newIssue.getMessage(), existingMessage.getDisplayMessage())
                    && sameText(
                        newIssue.getTechnicalMessage(), existingMessage.getTechnicalMessage())
                    && Objects.equals(
                        normaliseSeverity(newIssue), normaliseMessageType(existingMessage)))
        .findFirst()
        .orElse(null);
  }

  private ValidationMessagePatch findLikelyMatchingExistingMessage(
      ValidationIssue newIssue, List<ValidationMessagePatch> existingMessages) {
    return existingMessages.stream()
        .filter(
            existingMessage ->
                sameText(newIssue.getMessage(), existingMessage.getDisplayMessage())
                    || sameText(
                        newIssue.getTechnicalMessage(), existingMessage.getTechnicalMessage()))
        .findFirst()
        .orElse(null);
  }

  private void logMismatchDetails(
      String claimId, ValidationIssue newIssue, ValidationMessagePatch existingMessage) {
    List<String> differences = new ArrayList<>();

    if (!sameText(newIssue.getMessage(), existingMessage.getDisplayMessage())) {
      differences.add("message");
    }

    if (!sameText(newIssue.getTechnicalMessage(), existingMessage.getTechnicalMessage())) {
      differences.add("technicalMessage");
    }

    if (!Objects.equals(normaliseSeverity(newIssue), normaliseMessageType(existingMessage))) {
      differences.add("severity/type");
    }

    log.warn(
        "Claim {} validator mismatch for likely matching issue. Differences in {}. New=[{}], Existing=[{}]",
        claimId,
        String.join(", ", differences),
        describeNewIssue(newIssue),
        describeExistingMessage(existingMessage));
  }

  private boolean sameText(String left, String right) {
    return Objects.equals(normaliseText(left), normaliseText(right));
  }

  private String normaliseText(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }

    return value.trim().replaceAll("\\s+", " ");
  }

  private String normaliseSeverity(ValidationIssue issue) {
    return issue.getSeverity() == null ? null : issue.getSeverity().name();
  }

  private String normaliseMessageType(ValidationMessagePatch message) {
    return message.getType() == null ? null : message.getType().name();
  }

  private String describeNewIssue(ValidationIssue issue) {
    return String.format(
        "code=%s, severity=%s, message=%s, technicalMessage=%s",
        issue.getCode(), issue.getSeverity(), issue.getMessage(), issue.getTechnicalMessage());
  }

  private String describeExistingMessage(ValidationMessagePatch message) {
    return String.format(
        "source=%s, type=%s, displayMessage=%s, technicalMessage=%s",
        message.getSource(),
        message.getType(),
        message.getDisplayMessage(),
        message.getTechnicalMessage());
  }
}
