package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResultSet;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessageType;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.EventServiceIllegalArgumentException;
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
import uk.gov.laa.springboot.metrics.aspect.annotations.CounterMetric;
import uk.gov.laa.springboot.metrics.aspect.annotations.SummaryMetric;

/**
 * A service for validating submitted claims that are ready to process. Validation errors will
 * result in claims being marked as invalid and all validation errors will be reported against the
 * claim.
 */
@Slf4j
@Service
public class ClaimValidationService {

  private final CategoryOfLawValidationService categoryOfLawValidationService;
  private final DataClaimsRestClient dataClaimsRestClient;
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
  @SummaryMetric(
      metricName = "claim_validation_time",
      hintText = "Total time taken to validate claim (Including FSP validation time)")
  private void validateClaim(
      ClaimResponse claim,
      List<ClaimResponse> submissionClaims,
      Map<String, FeeDetailsResponseWrapper> feeDetailsResponseMap,
      AreaOfLaw areaOfLaw,
      String officeCode,
      SubmissionValidationContext context) {

    Assert.notNull(claim.getId(), "Claim ID must not be null");

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
                    validator.validate(
                        claim, context, areaOfLaw, officeCode, feeDetailsResponseMap);
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
      log.error("Fee details response returned null for fee code: {}", claim.getFeeCode());
      context.addClaimError(
          claim.getId(),
          ClaimValidationError.INVALID_CATEGORY_OF_LAW_AND_FEE_CODE,
          claim.getFeeCode());
    }
  }

  @CounterMetric(
      metricName = "claims_validated_and_valid",
      hintText = "Total number of claims validated and valid",
      conditionalOnReturn = "null")
  @CounterMetric(
      metricName = "claims_validated_and_warnings_found",
      hintText = "Total number of claims validated and have warnings",
      conditionalOnReturn = "WARNING")
  @CounterMetric(
      metricName = "claims_validated_and_invalid",
      hintText = "Total number of claims validated and invalid",
      conditionalOnReturn = "ERROR")
  @SuppressWarnings("UnusedReturnValue")
  private ValidationMessageType recordClaimMetrics(
      ClaimResponse claim, SubmissionValidationContext context) {
    Optional<ClaimValidationReport> claimReportOptional = context.getClaimReport(claim.getId());
    if (claimReportOptional.isEmpty() || claimReportOptional.get().getMessages().isEmpty()) {
      return null;
    }

    List<ValidationMessageType> messages =
        claimReportOptional.get().getMessages().stream()
            .map(ValidationMessagePatch::getType)
            .toList();

    if (messages.contains(ValidationMessageType.ERROR)) {
      return ValidationMessageType.ERROR;
    } else {
      return ValidationMessageType.WARNING;
    }
  }
}
