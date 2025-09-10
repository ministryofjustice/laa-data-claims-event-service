package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimFields;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.JsonSchemaValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/**
 * A service for validating submitted claims that are ready to process. Validation errors will
 * result in claims being marked as invalid and all validation errors will be reported against the
 * claim.
 */
@Service
@AllArgsConstructor
public class ClaimValidationService {

  public static final String OLDEST_DATE_ALLOWED_1 = "01/01/1995";
  public static final String MIN_REP_ORDER_DATE = "01/04/2016";
  public static final String MIN_BIRTH_DATE = "01/01/1900";
  private final CategoryOfLawValidationService categoryOfLawValidationService;
  private final DuplicateClaimValidationService duplicateClaimValidationService;
  private final FeeCalculationService feeCalculationService;
  private final SubmissionValidationContext submissionValidationContext;
  private final JsonSchemaValidator jsonSchemaValidator;

  /**
   * Validate a list of claims in a submission.
   *
   * @param claims the claims in a submission
   */
  public void validateClaims(List<ClaimFields> claims, List<String> providerCategoriesOfLaw) {
    Map<String, CategoryOfLawResult> categoryOfLawLookup =
        categoryOfLawValidationService.getCategoryOfLawLookup(claims);
    claims.forEach(claim -> validateClaim(claim, categoryOfLawLookup, providerCategoriesOfLaw));
  }

  /**
   * Validates the provided claim by performing various checks such as: - JSON schema validation , -
   * field level business validations (e.g. date in the past) - further validations that use
   * external APIs such as - category of law checks - duplicate claim checks - fee calculations. Any
   * errors encountered during the validation process are added to the submission validation
   * context.
   *
   * @param claim the claim object to validate
   * @param categoryOfLawLookup a map containing category of law codes and their corresponding
   *     descriptions
   * @param providerCategoriesOfLaw a list of categories of law applicable to the provider
   */
  private void validateClaim(
      ClaimFields claim,
      Map<String, CategoryOfLawResult> categoryOfLawLookup,
      List<String> providerCategoriesOfLaw) {
    submissionValidationContext.addClaimErrors(
        claim.getId(), jsonSchemaValidator.validate("claim", claim));

    validateUniqueFileNumber(claim);
    checkDateInPast(claim, "Case Start Date", claim.getCaseStartDate(), OLDEST_DATE_ALLOWED_1);
    checkDateInPast(claim, "Case Concluded Date", claim.getCaseConcludedDate(), OLDEST_DATE_ALLOWED_1);
    checkDateInPast(claim, "Transfer Date", claim.getTransferDate(), OLDEST_DATE_ALLOWED_1);
    checkDateInPast(claim, "Representation Order Date", claim.getRepresentationOrderDate(), MIN_REP_ORDER_DATE);
    checkDateInPast(claim, "Client Date of Birth", claim.getClientDateOfBirth(), MIN_BIRTH_DATE);
    checkDateInPast(claim, "Client2 Date of Birth", claim.getClient2DateOfBirth(), MIN_BIRTH_DATE);
    categoryOfLawValidationService.validateCategoryOfLaw(
        claim, categoryOfLawLookup, providerCategoriesOfLaw);
    duplicateClaimValidationService.validateDuplicateClaims(claim);
    feeCalculationService.validateFeeCalculation(claim);
  }

  /**
   * Validates the unique file number of the given claim to ensure it contains a valid and
   * non-future date in the format DDMMYY. If the date is invalid or in the future, an error is
   * added to the submission validation context.
   *
   * @param claim the claim object containing the unique file number to be validated
   */
  private void validateUniqueFileNumber(ClaimFields claim) {
    String uniqueFileNumber = claim.getUniqueFileNumber();
    if (uniqueFileNumber != null && uniqueFileNumber.length() > 1) {
      String datePart = uniqueFileNumber.substring(0, 6); // DDMMYY
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyy");
      try {
        LocalDate date = LocalDate.parse(datePart, formatter);
        if (!date.isBefore(LocalDate.now())) {
          submissionValidationContext.addClaimError(
              claim.getId(), ClaimValidationError.INVALID_DATE_IN_UNIQUE_FILE_NUMBER);
        }
      } catch (DateTimeParseException e) {
        submissionValidationContext.addClaimError(
            claim.getId(), ClaimValidationError.INVALID_DATE_IN_UNIQUE_FILE_NUMBER);
      }
    }
  }

  /**
   * Validates whether the provided date value is within the valid range (01/01/1995 to today's
   * date). If the date is invalid or falls outside the range, an error is added to the submission
   * validation context.
   *
   * @param claim The claim object associated with the date being checked.
   * @param fieldName The name of the field associated with the date being validated.
   * @param dateValueToCheck The date value to validate in the format "dd/MM/yyyy".
   */
  private void checkDateInPast(ClaimFields claim, String fieldName, String dateValueToCheck, String oldestDateAllowedStr) {
    if (dateValueToCheck != null) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
      try {
        LocalDate oldestDateAllowed = LocalDate.parse(oldestDateAllowedStr, formatter);
        LocalDate date = LocalDate.parse(dateValueToCheck, formatter);
        if (date.isBefore(oldestDateAllowed) || date.isAfter(LocalDate.now())) {
          submissionValidationContext.addClaimError(
              claim.getId(),
              String.format(
                  "Invalid date value for %s (Must be between %s and today): %s",
                  fieldName, oldestDateAllowedStr, dateValueToCheck));
        }
      } catch (DateTimeParseException e) {
        submissionValidationContext.addClaimError(
            claim.getId(),
            String.format("Invalid date value provided for %s: %s", fieldName, dateValueToCheck));
      }
    }
  }
}
