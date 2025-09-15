package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.MandatoryFieldsRegistry;
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

  public static final String OLDEST_DATE_ALLOWED_1 = "1995-01-01";
  public static final String MIN_REP_ORDER_DATE = "2016-04-01";
  public static final String MIN_BIRTH_DATE = "1900-01-01";
  private final CategoryOfLawValidationService categoryOfLawValidationService;
  private final DuplicateClaimValidationService duplicateClaimValidationService;
  private final FeeCalculationService feeCalculationService;
  private final SubmissionValidationContext submissionValidationContext;
  private final JsonSchemaValidator jsonSchemaValidator;
  private final MandatoryFieldsRegistry mandatoryFieldsRegistry;

  /**
   * Validate a list of claims in a submission.
   *
   * @param claims the claims in a submission
   * @param areaOfLaw the Area of Law that this submission belongs to: used to apply conditional
   *     validations
   */
  public void validateClaims(
      List<ClaimResponse> claims, List<String> providerCategoriesOfLaw, String areaOfLaw) {
    Map<String, CategoryOfLawResult> categoryOfLawLookup =
        categoryOfLawValidationService.getCategoryOfLawLookup(claims);
    claims.forEach(
        claim -> validateClaim(claim, categoryOfLawLookup, providerCategoriesOfLaw, areaOfLaw));
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
   * @param areaOfLaw the area of law for the parent submission: some validations change depending
   *     on the area of law.
   */
  private void validateClaim(
      ClaimResponse claim,
      Map<String, CategoryOfLawResult> categoryOfLawLookup,
      List<String> providerCategoriesOfLaw,
      String areaOfLaw) {
    List<ValidationMessagePatch> schemaMessages = jsonSchemaValidator.validate("claim", claim);
    submissionValidationContext.addClaimMessages(claim.getId(), schemaMessages);

    //    checkMandatoryFields(claim, areaOfLaw);
    validateUniqueFileNumber(claim);
    validateStageReachedCode(claim, areaOfLaw);
    validateDisbursementsVatAmount(claim, areaOfLaw);
    checkDateInPast(claim, "Case Start Date", claim.getCaseStartDate(), OLDEST_DATE_ALLOWED_1);
    checkDateInPast(
        claim, "Case Concluded Date", claim.getCaseConcludedDate(), OLDEST_DATE_ALLOWED_1);
    checkDateInPast(claim, "Transfer Date", claim.getTransferDate(), OLDEST_DATE_ALLOWED_1);
    checkDateInPast(
        claim, "Representation Order Date", claim.getRepresentationOrderDate(), MIN_REP_ORDER_DATE);
    checkDateInPast(claim, "Client Date of Birth", claim.getClientDateOfBirth(), MIN_BIRTH_DATE);
    checkDateInPast(claim, "Client2 Date of Birth", claim.getClient2DateOfBirth(), MIN_BIRTH_DATE);
    validateMatterType(claim, areaOfLaw);
    categoryOfLawValidationService.validateCategoryOfLaw(
        claim, categoryOfLawLookup, providerCategoriesOfLaw);
    duplicateClaimValidationService.validateDuplicateClaims(claim);
    feeCalculationService.validateFeeCalculation(claim);
  }

  /**
   * Checks if all mandatory fields for a given area of law are populated in the provided
   * ClaimResponse object. If a mandatory field is missing or invalid, an error is added to the
   * submission validation context.
   *
   * @param claim the ClaimResponse object containing data that needs to be validated
   * @param areaOfLaw the area of law for which mandatory fields need to be checked
   */
  private void checkMandatoryFields(ClaimResponse claim, String areaOfLaw) {
    List<String> mandatoryFields =
        mandatoryFieldsRegistry.getMandatoryFieldsByAreaOfLaw().get(areaOfLaw);
    for (String fieldName : mandatoryFields) {
      try {
        // Look up getter method for the property
        PropertyDescriptor pd = new PropertyDescriptor(fieldName, ClaimResponse.class);
        Method getter = pd.getReadMethod();

        if (getter == null) {
          throw new IllegalStateException("No getter for field in ClaimResponse: " + fieldName);
        }

        Object value = getter.invoke(claim);

        if (value == null || (value instanceof String s && s.trim().isEmpty())) {
          submissionValidationContext.addClaimError(
              claim.getId(),
              String.format("%s is required for area of law: %s", fieldName, areaOfLaw));
        }

      } catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
        throw new IllegalStateException(
            "Error accessing property in ClaimResponse: " + fieldName, e);
      }
    }
  }

  private void validateFieldWithRegex(
      ClaimResponse claim, String areaOfLaw, String fieldValue, String fieldName, String regex) {
    if (regex != null && fieldValue != null && !fieldValue.matches(regex)) {
      submissionValidationContext.addClaimError(
          claim.getId(),
          String.format(
              "%s (%s): does not match the regex pattern %s (provided value: %s)",
              fieldName, areaOfLaw, regex, fieldValue));
    }
  }

  private void validateStageReachedCode(ClaimResponse claim, String areaOfLaw) {
    String regex =
        switch (areaOfLaw) {
          case "CIVIL" -> "^[a-zA-Z0-9]{2}$";
          case "CRIME" -> "^[A-Z]{4}$";
          default -> null;
        };

    validateFieldWithRegex(
        claim, areaOfLaw, claim.getStageReachedCode(), "stage_reached_code", regex);
  }

  private void validateMatterType(ClaimResponse claim, String areaOfLaw) {
    String regex =
        switch (areaOfLaw) {
          case "CIVIL" -> "^[a-zA-Z0-9]{1,4}[-:][a-zA-Z0-9]{1,4}$";
          case "MEDIATION" -> "^[A-Z]{4}[-:][A-Z]{4}$";
          default -> null;
        };

    validateFieldWithRegex(claim, areaOfLaw, claim.getMatterTypeCode(), "matter_type_code", regex);
  }

  private void validateDisbursementsVatAmount(ClaimResponse claim, String areaOfLaw) {
    var disbursementsVatAmount = claim.getDisbursementsVatAmount();

    BigDecimal maxAllowed =
        switch (areaOfLaw) {
          case "CIVIL" -> BigDecimal.valueOf(99999.99);
          case "CRIME" -> BigDecimal.valueOf(999999.99);
          case "MEDIATION" -> BigDecimal.valueOf(999999999.99);
          default -> null;
        };

    if (maxAllowed != null && disbursementsVatAmount.compareTo(maxAllowed) > 0) {
      submissionValidationContext.addClaimError(
          claim.getId(),
          String.format(
              "disbursementsVatAmount (%s): must have a maximum value of %s (provided value: %s)",
              areaOfLaw, maxAllowed, disbursementsVatAmount));
    }
  }

  /**
   * Validates the unique file number of the given claim to ensure it contains a valid and
   * non-future date in the format DDMMYY. If the date is invalid or in the future, an error is
   * added to the submission validation context.
   *
   * @param claim the claim object containing the unique file number to be validated
   */
  private void validateUniqueFileNumber(ClaimResponse claim) {
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
   * @param dateValueToCheck The date value to validate in the format "yyyy-MM-dd".
   */
  private void checkDateInPast(
      ClaimResponse claim, String fieldName, String dateValueToCheck, String oldestDateAllowedStr) {
    if (dateValueToCheck != null) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
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
