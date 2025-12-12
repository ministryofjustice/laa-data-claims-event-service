package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@AllArgsConstructor
public class ClaimValidationService {

  private final CategoryOfLawValidationService categoryOfLawValidationService;
  private final DataClaimsRestClient dataClaimsRestClient;
  private final EventServiceMetricService eventServiceMetricService;
  private final BulkClaimUpdater bulkClaimUpdater;
  private final List<ClaimValidator> claimValidator;

  /**
   * Validate a list of claims in a submission and Updates it in the Data Claims API.
   *
   * @param submission the submission
   */
  public void validateAndUpdateClaims(
      SubmissionResponse submission, SubmissionValidationContext context) {

    int pageNumber = 0;
    Integer totalPages = Integer.MAX_VALUE;

    List<ClaimResponse> submissionClaimsToSave = new ArrayList<>(Collections.emptyList());
    Map<String, FeeDetailsResponseWrapper> feeDetailsResponseMap = new HashMap<>();

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
                  250,
                  null)
              .getBody();

      if (claims == null) {
        throw new EventServiceIllegalArgumentException("Claims response is null from Claims API");
      }

      log.info(
          "Validating claims page {} from submission {}", pageNumber, submission.getSubmissionId());

      // Set total pages
      totalPages = claims.getTotalPages();

      List<ClaimResponse> submissionClaims = claims.getContent();

      feeDetailsResponseMap.putAll(
          categoryOfLawValidationService.getFeeDetailsResponseForAllFeeCodesInClaims(
              submissionClaims));

      submissionClaims.forEach(
          claim ->
              validateClaim(
                  claim,
                  submissionClaims,
                  feeDetailsResponseMap,
                  submission.getAreaOfLaw(),
                  submission.getOfficeAccountNumber(),
                  context));

      submissionClaimsToSave.addAll(submissionClaims);

      // Increment page number
      pageNumber++;
    }

    log.debug(
        "Saving all claims for submission {} to Data Claims API", submission.getSubmissionId());

    // Update claims status after all validations
    bulkClaimUpdater.updateClaims(
        submission.getSubmissionId(),
        submissionClaimsToSave,
        submission.getAreaOfLaw(),
        context,
        feeDetailsResponseMap);
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
      log.error("Fee details response returned null for fee code: {}", claim.getFeeCode());
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
}
