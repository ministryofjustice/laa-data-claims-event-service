package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionClaim;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.EventServiceIllegalArgumentException;
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

    Map<String, CategoryOfLawResult> categoryOfLawLookup =
        categoryOfLawValidationService.getCategoryOfLawLookup(submissionClaims);
    submissionClaims.stream()
        .filter(claim -> ClaimStatus.READY_TO_PROCESS.equals(claim.getStatus()))
        .forEach(
            claim ->
                validateClaim(
                    submission.getSubmissionId(),
                    claim,
                    submissionClaims,
                    categoryOfLawLookup,
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
   * @param categoryOfLawLookup a map containing category of law codes and their corresponding
   *     descriptions
   * @param areaOfLaw the area of law for the parent submission: some validations change depending
   *     on the area of law.
   */
  private void validateClaim(
      UUID submissionId,
      ClaimResponse claim,
      List<ClaimResponse> submissionClaims,
      Map<String, CategoryOfLawResult> categoryOfLawLookup,
      String areaOfLaw,
      String officeCode,
      SubmissionValidationContext context) {

    // Includes:
    // - JSON scheme validation
    // - Mandatory field validations
    // - UFN
    // - Stage reached validation
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
                    validator.validate(claim, context, areaOfLaw, officeCode, categoryOfLawLookup);
                case DuplicateClaimValidator validator ->
                    validator.validate(claim, context, areaOfLaw, officeCode, submissionClaims);
                default -> throw new EventServiceIllegalArgumentException("Unknown validator used");
              }
            });

    // fee calculation validation - done last after every other claim validation
    feeCalculationService.validateFeeCalculation(submissionId, claim, context);
  }


  private void validateScheduleReference(
      ClaimResponse claim, String areaOfLaw, SubmissionValidationContext context) {
    String regex = null;
    if (areaOfLaw.equals("CIVIL")) {
      regex = "^[a-zA-Z0-9/.\\-]{1,20}$";
    }
    validateFieldWithRegex(
        claim, areaOfLaw, claim.getScheduleReference(), "schedule_reference", regex, context);
  }
}
