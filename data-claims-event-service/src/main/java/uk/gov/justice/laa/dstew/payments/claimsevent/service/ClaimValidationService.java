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
        finalSubmissionClaims.stream()
            .map(ClaimMapper::fromClaimResponse)
            .peek(
                c -> {
                  c.setAreaOfLaw(submission.getAreaOfLaw());
                  c.setOfficeAccountNumber(submission.getOfficeAccountNumber());
                })
            .toList();

    boolean presentInRelated = relatedClaims.stream().anyMatch(c -> Objects.equals(c, mappedClaim));
    if (!presentInRelated) {
      relatedClaims.stream()
          .filter(c -> Objects.equals(c.getId(), mappedClaim.getId()))
          .findFirst()
          .ifPresentOrElse(
              candidate -> logClaimDiff(claimResponse.getId(), mappedClaim, candidate),
              () ->
                  log.debug(
                      "Claim {} NOT PRESENT in relatedClaims and no id match found (size={})",
                      claimResponse.getId(),
                      relatedClaims.size()));
    } else {
      log.debug(
          "Claim {} PRESENT (exact match) in relatedClaims (size={})",
          claimResponse.getId(),
          relatedClaims.size());
    }

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
        Optional.of(newIssues).orElseGet(List::of).stream()
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

  private void logClaimDiff(String claimId, Claim expected, Claim actual) {
    List<String> diffs = new ArrayList<>();
    checkField(diffs, "areaOfLaw", expected.getAreaOfLaw(), actual.getAreaOfLaw());
    checkField(
        diffs,
        "officeAccountNumber",
        expected.getOfficeAccountNumber(),
        actual.getOfficeAccountNumber());
    checkField(diffs, "submissionId", expected.getSubmissionId(), actual.getSubmissionId());
    checkField(diffs, "status", expected.getStatus(), actual.getStatus());
    checkField(diffs, "lineNumber", expected.getLineNumber(), actual.getLineNumber());
    checkField(
        diffs, "scheduleReference", expected.getScheduleReference(), actual.getScheduleReference());
    checkField(
        diffs, "submissionPeriod", expected.getSubmissionPeriod(), actual.getSubmissionPeriod());
    checkField(
        diffs,
        "caseReferenceNumber",
        expected.getCaseReferenceNumber(),
        actual.getCaseReferenceNumber());
    checkField(
        diffs, "uniqueFileNumber", expected.getUniqueFileNumber(), actual.getUniqueFileNumber());
    checkField(diffs, "caseStartDate", expected.getCaseStartDate(), actual.getCaseStartDate());
    checkField(
        diffs, "caseConcludedDate", expected.getCaseConcludedDate(), actual.getCaseConcludedDate());
    checkField(diffs, "caseId", expected.getCaseId(), actual.getCaseId());
    checkField(diffs, "uniqueCaseId", expected.getUniqueCaseId(), actual.getUniqueCaseId());
    checkField(diffs, "caseStageCode", expected.getCaseStageCode(), actual.getCaseStageCode());
    checkField(diffs, "matterTypeCode", expected.getMatterTypeCode(), actual.getMatterTypeCode());
    checkField(
        diffs,
        "crimeMatterTypeCode",
        expected.getCrimeMatterTypeCode(),
        actual.getCrimeMatterTypeCode());
    checkField(diffs, "feeSchemeCode", expected.getFeeSchemeCode(), actual.getFeeSchemeCode());
    checkField(diffs, "feeCode", expected.getFeeCode(), actual.getFeeCode());
    checkField(
        diffs,
        "procurementAreaCode",
        expected.getProcurementAreaCode(),
        actual.getProcurementAreaCode());
    checkField(
        diffs, "accessPointCode", expected.getAccessPointCode(), actual.getAccessPointCode());
    checkField(
        diffs, "deliveryLocation", expected.getDeliveryLocation(), actual.getDeliveryLocation());
    checkField(
        diffs,
        "representationOrderDate",
        expected.getRepresentationOrderDate(),
        actual.getRepresentationOrderDate());
    checkField(
        diffs,
        "suspectsDefendantsCount",
        expected.getSuspectsDefendantsCount(),
        actual.getSuspectsDefendantsCount());
    checkField(
        diffs,
        "policeStationCourtAttendancesCount",
        expected.getPoliceStationCourtAttendancesCount(),
        actual.getPoliceStationCourtAttendancesCount());
    checkField(
        diffs,
        "policeStationCourtPrisonId",
        expected.getPoliceStationCourtPrisonId(),
        actual.getPoliceStationCourtPrisonId());
    checkField(diffs, "dsccNumber", expected.getDsccNumber(), actual.getDsccNumber());
    checkField(diffs, "maatId", expected.getMaatId(), actual.getMaatId());
    checkField(
        diffs,
        "prisonLawPriorApprovalNumber",
        expected.getPrisonLawPriorApprovalNumber(),
        actual.getPrisonLawPriorApprovalNumber());
    checkField(
        diffs, "isDutySolicitor", expected.getIsDutySolicitor(), actual.getIsDutySolicitor());
    checkField(diffs, "isYouthCourt", expected.getIsYouthCourt(), actual.getIsYouthCourt());
    checkField(diffs, "schemeId", expected.getSchemeId(), actual.getSchemeId());
    checkField(
        diffs,
        "mediationSessionsCount",
        expected.getMediationSessionsCount(),
        actual.getMediationSessionsCount());
    checkField(
        diffs,
        "mediationTimeMinutes",
        expected.getMediationTimeMinutes(),
        actual.getMediationTimeMinutes());
    checkField(
        diffs, "outreachLocation", expected.getOutreachLocation(), actual.getOutreachLocation());
    checkField(diffs, "referralSource", expected.getReferralSource(), actual.getReferralSource());
    checkField(diffs, "clientForename", expected.getClientForename(), actual.getClientForename());
    checkField(diffs, "clientSurname", expected.getClientSurname(), actual.getClientSurname());
    checkField(
        diffs, "clientDateOfBirth", expected.getClientDateOfBirth(), actual.getClientDateOfBirth());
    checkField(
        diffs,
        "uniqueClientNumber",
        expected.getUniqueClientNumber(),
        actual.getUniqueClientNumber());
    checkField(diffs, "clientPostcode", expected.getClientPostcode(), actual.getClientPostcode());
    checkField(diffs, "genderCode", expected.getGenderCode(), actual.getGenderCode());
    checkField(diffs, "ethnicityCode", expected.getEthnicityCode(), actual.getEthnicityCode());
    checkField(diffs, "disabilityCode", expected.getDisabilityCode(), actual.getDisabilityCode());
    checkField(diffs, "isLegallyAided", expected.getIsLegallyAided(), actual.getIsLegallyAided());
    checkField(diffs, "clientTypeCode", expected.getClientTypeCode(), actual.getClientTypeCode());
    checkField(
        diffs,
        "homeOfficeClientNumber",
        expected.getHomeOfficeClientNumber(),
        actual.getHomeOfficeClientNumber());
    checkField(
        diffs,
        "claReferenceNumber",
        expected.getClaReferenceNumber(),
        actual.getClaReferenceNumber());
    checkField(
        diffs, "claExemptionCode", expected.getClaExemptionCode(), actual.getClaExemptionCode());
    checkField(
        diffs, "client2Forename", expected.getClient2Forename(), actual.getClient2Forename());
    checkField(diffs, "client2Surname", expected.getClient2Surname(), actual.getClient2Surname());
    checkField(
        diffs,
        "client2DateOfBirth",
        expected.getClient2DateOfBirth(),
        actual.getClient2DateOfBirth());
    checkField(diffs, "client2Ucn", expected.getClient2Ucn(), actual.getClient2Ucn());
    checkField(
        diffs, "client2Postcode", expected.getClient2Postcode(), actual.getClient2Postcode());
    checkField(
        diffs, "client2GenderCode", expected.getClient2GenderCode(), actual.getClient2GenderCode());
    checkField(
        diffs,
        "client2EthnicityCode",
        expected.getClient2EthnicityCode(),
        actual.getClient2EthnicityCode());
    checkField(
        diffs,
        "client2DisabilityCode",
        expected.getClient2DisabilityCode(),
        actual.getClient2DisabilityCode());
    checkField(
        diffs,
        "client2IsLegallyAided",
        expected.getClient2IsLegallyAided(),
        actual.getClient2IsLegallyAided());
    checkField(
        diffs, "stageReachedCode", expected.getStageReachedCode(), actual.getStageReachedCode());
    checkField(
        diffs,
        "standardFeeCategoryCode",
        expected.getStandardFeeCategoryCode(),
        actual.getStandardFeeCategoryCode());
    checkField(diffs, "outcomeCode", expected.getOutcomeCode(), actual.getOutcomeCode());
    checkField(
        diffs,
        "designatedAccreditedRepresentativeCode",
        expected.getDesignatedAccreditedRepresentativeCode(),
        actual.getDesignatedAccreditedRepresentativeCode());
    checkField(
        diffs,
        "isPostalApplicationAccepted",
        expected.getIsPostalApplicationAccepted(),
        actual.getIsPostalApplicationAccepted());
    checkField(
        diffs,
        "isClient2PostalApplicationAccepted",
        expected.getIsClient2PostalApplicationAccepted(),
        actual.getIsClient2PostalApplicationAccepted());
    checkField(
        diffs,
        "mentalHealthTribunalReference",
        expected.getMentalHealthTribunalReference(),
        actual.getMentalHealthTribunalReference());
    checkField(diffs, "isNrmAdvice", expected.getIsNrmAdvice(), actual.getIsNrmAdvice());
    checkField(diffs, "followOnWork", expected.getFollowOnWork(), actual.getFollowOnWork());
    checkField(diffs, "transferDate", expected.getTransferDate(), actual.getTransferDate());
    checkField(
        diffs,
        "exemptionCriteriaSatisfied",
        expected.getExemptionCriteriaSatisfied(),
        actual.getExemptionCriteriaSatisfied());
    checkField(
        diffs,
        "exceptionalCaseFundingReference",
        expected.getExceptionalCaseFundingReference(),
        actual.getExceptionalCaseFundingReference());
    checkField(diffs, "isLegacyCase", expected.getIsLegacyCase(), actual.getIsLegacyCase());
    checkField(diffs, "adviceTime", expected.getAdviceTime(), actual.getAdviceTime());
    checkField(diffs, "travelTime", expected.getTravelTime(), actual.getTravelTime());
    checkField(diffs, "waitingTime", expected.getWaitingTime(), actual.getWaitingTime());
    checkField(
        diffs,
        "netProfitCostsAmount",
        expected.getNetProfitCostsAmount(),
        actual.getNetProfitCostsAmount());
    checkField(
        diffs,
        "netDisbursementAmount",
        expected.getNetDisbursementAmount(),
        actual.getNetDisbursementAmount());
    checkField(
        diffs,
        "netCounselCostsAmount",
        expected.getNetCounselCostsAmount(),
        actual.getNetCounselCostsAmount());
    checkField(
        diffs,
        "disbursementsVatAmount",
        expected.getDisbursementsVatAmount(),
        actual.getDisbursementsVatAmount());
    checkField(
        diffs,
        "travelWaitingCostsAmount",
        expected.getTravelWaitingCostsAmount(),
        actual.getTravelWaitingCostsAmount());
    checkField(
        diffs,
        "netWaitingCostsAmount",
        expected.getNetWaitingCostsAmount(),
        actual.getNetWaitingCostsAmount());
    checkField(
        diffs, "isVatApplicable", expected.getIsVatApplicable(), actual.getIsVatApplicable());
    checkField(
        diffs,
        "isToleranceApplicable",
        expected.getIsToleranceApplicable(),
        actual.getIsToleranceApplicable());
    checkField(
        diffs,
        "priorAuthorityReference",
        expected.getPriorAuthorityReference(),
        actual.getPriorAuthorityReference());
    checkField(diffs, "isLondonRate", expected.getIsLondonRate(), actual.getIsLondonRate());
    checkField(
        diffs,
        "adjournedHearingFeeAmount",
        expected.getAdjournedHearingFeeAmount(),
        actual.getAdjournedHearingFeeAmount());
    checkField(
        diffs,
        "isAdditionalTravelPayment",
        expected.getIsAdditionalTravelPayment(),
        actual.getIsAdditionalTravelPayment());
    checkField(
        diffs,
        "costsDamagesRecoveredAmount",
        expected.getCostsDamagesRecoveredAmount(),
        actual.getCostsDamagesRecoveredAmount());
    checkField(
        diffs,
        "meetingsAttendedCode",
        expected.getMeetingsAttendedCode(),
        actual.getMeetingsAttendedCode());
    checkField(
        diffs,
        "detentionTravelWaitingCostsAmount",
        expected.getDetentionTravelWaitingCostsAmount(),
        actual.getDetentionTravelWaitingCostsAmount());
    checkField(
        diffs,
        "jrFormFillingAmount",
        expected.getJrFormFillingAmount(),
        actual.getJrFormFillingAmount());
    checkField(
        diffs, "isEligibleClient", expected.getIsEligibleClient(), actual.getIsEligibleClient());
    checkField(
        diffs, "courtLocationCode", expected.getCourtLocationCode(), actual.getCourtLocationCode());
    checkField(diffs, "adviceTypeCode", expected.getAdviceTypeCode(), actual.getAdviceTypeCode());
    checkField(
        diffs,
        "medicalReportsCount",
        expected.getMedicalReportsCount(),
        actual.getMedicalReportsCount());
    checkField(diffs, "isIrcSurgery", expected.getIsIrcSurgery(), actual.getIsIrcSurgery());
    checkField(diffs, "surgeryDate", expected.getSurgeryDate(), actual.getSurgeryDate());
    checkField(
        diffs,
        "surgeryClientsCount",
        expected.getSurgeryClientsCount(),
        actual.getSurgeryClientsCount());
    checkField(
        diffs,
        "surgeryMattersCount",
        expected.getSurgeryMattersCount(),
        actual.getSurgeryMattersCount());
    checkField(diffs, "cmrhOralCount", expected.getCmrhOralCount(), actual.getCmrhOralCount());
    checkField(
        diffs,
        "cmrhTelephoneCount",
        expected.getCmrhTelephoneCount(),
        actual.getCmrhTelephoneCount());
    checkField(
        diffs,
        "aitHearingCentreCode",
        expected.getAitHearingCentreCode(),
        actual.getAitHearingCentreCode());
    checkField(
        diffs,
        "isSubstantiveHearing",
        expected.getIsSubstantiveHearing(),
        actual.getIsSubstantiveHearing());
    checkField(diffs, "hoInterview", expected.getHoInterview(), actual.getHoInterview());
    checkField(
        diffs,
        "localAuthorityNumber",
        expected.getLocalAuthorityNumber(),
        actual.getLocalAuthorityNumber());
    checkField(
        diffs, "createdByUserId", expected.getCreatedByUserId(), actual.getCreatedByUserId());
    checkField(diffs, "isAmended", expected.getIsAmended(), actual.getIsAmended());
    checkField(diffs, "hasAssessment", expected.getHasAssessment(), actual.getHasAssessment());
    checkField(diffs, "version", expected.getVersion(), actual.getVersion());

    if (diffs.isEmpty()) {
      log.debug(
          "Claim {} id-matched in relatedClaims but equals() returned false — no field diffs found (possible reference issue)",
          claimId);
    } else {
      log.debug(
          "Claim {} NOT PRESENT (exact match) in relatedClaims. Differing fields: {}",
          claimId,
          diffs);
    }
  }

  private void checkField(List<String> diffs, String name, Object expected, Object actual) {
    if (!Objects.equals(expected, actual)) {
      diffs.add(name + "[expected=" + expected + ", actual=" + actual + "]");
    }
  }
}
