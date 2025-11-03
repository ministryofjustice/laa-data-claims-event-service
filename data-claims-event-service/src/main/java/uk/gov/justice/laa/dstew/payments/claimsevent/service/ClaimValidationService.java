package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionClaim;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
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
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.DuplicateClaimValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.EffectiveCategoryOfLawClaimValidator;

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
  private final FeeCalculationService feeCalculationService;
  private final EventServiceMetricService eventServiceMetricService;

  private final List<ClaimValidator> claimValidator;

  /**
   * Validate a list of claims in a submission.
   *
   * @param submission the submission
   */
  public void validateClaims(SubmissionResponse submission, SubmissionValidationContext context) {

    List<ClaimResponse> submissionClaims =
        submission.getClaims().stream()
            .filter(claim -> ClaimStatus.READY_TO_PROCESS.equals(claim.getStatus()))
            .map(SubmissionClaim::getClaimId)
            .map(
                claimId ->
                    dataClaimsRestClient.getClaim(submission.getSubmissionId(), claimId).getBody())
            .toList();

    Map<String, FeeDetailsResponseWrapper> feeDetailsResponseMap =
        categoryOfLawValidationService.getFeeDetailsResponseForAllFeeCodesInClaims(
            submissionClaims);

    submissionClaims.stream()
        .filter(claim -> ClaimStatus.READY_TO_PROCESS.equals(claim.getStatus()))
        .forEach(
            claim ->
                validateClaim(
                    submission.getSubmissionId(),
                    claim,
                    submissionClaims,
                    feeDetailsResponseMap,
                    submission.getAreaOfLaw(),
                    submission.getOfficeAccountNumber(),
                    context));
  }

  /**
   * Validates the provided claim by performing various checks such as: - JSON schema validation , -
   * field level business validations (e.g. date in the past) - further validations that use
   * external APIs such as - category of law checks - duplicate claim checks - fee calculations. Any
   * errors encountered during the validation process are added to the submission validation
   * context.
   *
   * @param submissionId the ID of the submission to which the claim belongs
   * @param claim the claim object to validate
   * @param feeDetailsResponseMap a map containing FeeDetailsResponse and their corresponding
   *     feeCodes
   * @param areaOfLaw the area of law for the parent submission: some validations change depending
   *     on the area of law.
   */
  private void validateClaim(
      UUID submissionId,
      ClaimResponse claim,
      List<ClaimResponse> submissionClaims,
      Map<String, FeeDetailsResponseWrapper> feeDetailsResponseMap,
      AreaOfLaw areaOfLaw,
      String officeCode,
      SubmissionValidationContext context) {

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
    FeeDetailsResponseWrapper feeDetailsResponseWrapper =
        feeDetailsResponseMap.get(claim.getFeeCode());
    if (feeDetailsResponseWrapper.isError()) {
      log.error(
          "Fee Scheme Platform API has returned an unexpected error for feeCode: {}",
          claim.getFeeCode());
      context.addClaimError(
          claim.getId(),
          ClaimValidationError.TECHNICAL_ERROR_FEE_CALCULATION_SERVICE,
          claim.getFeeCode());
      return;
    }
    if (feeDetailsResponseWrapper.getFeeDetailsResponse() == null) {
      log.error("Fee details response returned null for fee code: {}", claim.getFeeCode());
      context.addClaimError(
          claim.getId(),
          ClaimValidationError.INVALID_CATEGORY_OF_LAW_AND_FEE_CODE,
          claim.getFeeCode());
      return;
    }
    String feeCalculationType = feeDetailsResponseWrapper.getFeeDetailsResponse().getFeeType();
    claimValidator.stream()
        .sorted(
            Comparator.comparingInt(ClaimValidator::priority)) // Ensure validators are run in order
        .forEach(
            x -> {
              switch (x) {
                case BasicClaimValidator validator -> validator.validate(claim, context);
                case ClaimWithAreaOfLawValidator validator ->
                    validator.validate(claim, context, areaOfLaw, feeCalculationType);
                case EffectiveCategoryOfLawClaimValidator validator ->
                    validator.validate(
                        claim, context, areaOfLaw, officeCode, feeDetailsResponseMap);
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

    // fee calculation validation - done last after every other claim validation
    feeCalculationService.validateFeeCalculation(
        submissionId, claim, context, feeDetailsResponseWrapper.getFeeDetailsResponse());

    // Check claim status and record metric
    recordClaimMetric(claim, context);
  }

  private void recordClaimMetric(ClaimResponse claim, SubmissionValidationContext context) {
    Optional<ClaimValidationReport> claimReportOptional = context.getClaimReport(claim.getId());
    if (claimReportOptional.isEmpty()) {
      eventServiceMetricService.incrementTotalClaimsValidatedAndValid();
      return;
    }

    // Claim could have either errors or warnings so record both
    if (claimReportOptional.get().getMessages().stream()
        .anyMatch(x -> x.getType().equals(ValidationMessageType.ERROR))) {
      eventServiceMetricService.incrementTotalClaimsValidatedAndErrorsFound();
    }

    if (claimReportOptional.get().getMessages().stream()
        .anyMatch(x -> x.getType().equals(ValidationMessageType.WARNING))) {
      eventServiceMetricService.incrementTotalClaimsValidatedAndWarningsFound();
    }
  }
}
