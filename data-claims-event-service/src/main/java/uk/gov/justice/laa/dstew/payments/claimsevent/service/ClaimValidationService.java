package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.EVENT_SERVICE;

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
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionClaim;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ProviderDetailsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.MandatoryFieldsRegistry;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.EventServiceIllegalArgumentException;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.strategy.DuplicateClaimValidationStrategy;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.strategy.StrategyTypes;
import uk.gov.justice.laa.dstew.payments.claimsevent.util.ClaimEffectiveDateUtil;
import uk.gov.justice.laa.dstew.payments.claimsevent.util.UniqueFileNumberUtil;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.JsonSchemaValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.provider.model.FirmOfficeContractAndScheduleDetails;
import uk.gov.justice.laa.provider.model.FirmOfficeContractAndScheduleLine;
import uk.gov.justice.laa.provider.model.ProviderFirmOfficeContractAndScheduleDto;

/**
 * A service for validating submitted claims that are ready to process. Validation errors will
 * result in claims being marked as invalid and all validation errors will be reported against the
 * claim.
 */
@Slf4j
@Service
@AllArgsConstructor
public class ClaimValidationService {

  public static final String OLDEST_DATE_ALLOWED_1 = "1995-01-01";
  public static final String MIN_REP_ORDER_DATE = "2016-04-01";
  public static final String MIN_BIRTH_DATE = "1900-01-01";
  private final CategoryOfLawValidationService categoryOfLawValidationService;
  private final ProviderDetailsRestClient providerDetailsRestClient;
  private final FeeCalculationService feeCalculationService;
  private final DataClaimsRestClient dataClaimsRestClient;
  private final JsonSchemaValidator jsonSchemaValidator;
  private final MandatoryFieldsRegistry mandatoryFieldsRegistry;
  private final ClaimEffectiveDateUtil claimEffectiveDateUtil;
  private final Map<String, DuplicateClaimValidationStrategy> strategies;

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
    List<ValidationMessagePatch> schemaMessages = jsonSchemaValidator.validate("claim", claim);
    context.addClaimMessages(claim.getId(), schemaMessages);

    checkMandatoryFields(claim, areaOfLaw, context);
    validateUniqueFileNumber(claim, context);
    validateStageReachedCode(claim, areaOfLaw, context);
    validateDisbursementsVatAmount(claim, areaOfLaw, context);
    String caseStartDate = claim.getCaseStartDate();
    checkDateInPast(claim, "Case Start Date", caseStartDate, OLDEST_DATE_ALLOWED_1, context);
    checkDateInPast(
        claim, "Case Concluded Date", claim.getCaseConcludedDate(), OLDEST_DATE_ALLOWED_1, context);
    checkDateInPast(
        claim, "Transfer Date", claim.getTransferDate(), OLDEST_DATE_ALLOWED_1, context);
    checkDateInPast(
        claim,
        "Representation Order Date",
        claim.getRepresentationOrderDate(),
        MIN_REP_ORDER_DATE,
        context);
    checkDateInPast(
        claim, "Client Date of Birth", claim.getClientDateOfBirth(), MIN_BIRTH_DATE, context);
    checkDateInPast(
        claim, "Client2 Date of Birth", claim.getClient2DateOfBirth(), MIN_BIRTH_DATE, context);
    validateMatterType(claim, areaOfLaw, context);

    try {
      LocalDate effectiveDate = claimEffectiveDateUtil.getEffectiveDate(claim);
      List<String> effectiveCategoriesOfLaw =
          getEffectiveCategoriesOfLaw(officeCode, areaOfLaw, effectiveDate);
      // Get effective category of law lookup
      categoryOfLawValidationService.validateCategoryOfLaw(
          claim, categoryOfLawLookup, effectiveCategoriesOfLaw, context);
    } catch (EventServiceIllegalArgumentException e) {
      log.debug(
          "Error getting effective date for category of law validation: {}. Continuing with claim"
              + " validation",
          e.getMessage());
    }

    // duplicates bases on area of law
    validateDuplicateClaims(claim, submissionClaims, areaOfLaw, officeCode, context);

    // fee calculation validation
    feeCalculationService.validateFeeCalculation(submissionId, claim, context);
  }

  /**
   * Checks if all mandatory fields for a given area of law are populated in the provided
   * ClaimResponse object. If a mandatory field is missing or invalid, an error is added to the
   * submission validation context.
   *
   * @param claim the ClaimResponse object containing data that needs to be validated
   * @param areaOfLaw the area of law for which mandatory fields need to be checked
   */
  private void checkMandatoryFields(
      ClaimResponse claim, String areaOfLaw, SubmissionValidationContext context) {
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
          context.addClaimError(
              claim.getId(),
              String.format("%s is required for area of law: %s", fieldName, areaOfLaw),
              EVENT_SERVICE);
        }

      } catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
        throw new IllegalStateException(
            "Error accessing property in ClaimResponse: " + fieldName, e);
      }
    }
  }

  private void validateFieldWithRegex(
      ClaimResponse claim,
      String areaOfLaw,
      String fieldValue,
      String fieldName,
      String regex,
      SubmissionValidationContext context) {
    if (regex != null && fieldValue != null && !fieldValue.matches(regex)) {
      context.addClaimError(
          claim.getId(),
          String.format(
              "%s (%s): does not match the regex pattern %s (provided value: %s)",
              fieldName, areaOfLaw, regex, fieldValue),
          EVENT_SERVICE);
    }
  }

  private void validateStageReachedCode(
      ClaimResponse claim, String areaOfLaw, SubmissionValidationContext context) {
    String regex =
        switch (areaOfLaw) {
          case "CIVIL" -> "^[a-zA-Z0-9]{2}$";
          case "CRIME" -> "^[A-Z]{4}$";
          default -> null;
        };

    validateFieldWithRegex(
        claim, areaOfLaw, claim.getStageReachedCode(), "stage_reached_code", regex, context);
  }

  private void validateMatterType(
      ClaimResponse claim, String areaOfLaw, SubmissionValidationContext context) {
    String regex =
        switch (areaOfLaw) {
          case "CIVIL" -> "^[a-zA-Z0-9]{1,4}[-:][a-zA-Z0-9]{1,4}$";
          case "MEDIATION" -> "^[A-Z]{4}[-:][A-Z]{4}$";
          default -> null;
        };

    validateFieldWithRegex(
        claim, areaOfLaw, claim.getMatterTypeCode(), "matter_type_code", regex, context);
  }

  private void validateDisbursementsVatAmount(
      ClaimResponse claim, String areaOfLaw, SubmissionValidationContext context) {
    var disbursementsVatAmount = claim.getDisbursementsVatAmount();

    BigDecimal maxAllowed =
        switch (areaOfLaw) {
          case "CIVIL" -> BigDecimal.valueOf(99999.99);
          case "CRIME" -> BigDecimal.valueOf(999999.99);
          case "MEDIATION" -> BigDecimal.valueOf(999999999.99);
          default -> null;
        };

    if (maxAllowed != null
        && disbursementsVatAmount != null
        && disbursementsVatAmount.compareTo(maxAllowed) > 0) {
      context.addClaimError(
          claim.getId(),
          String.format(
              "disbursementsVatAmount (%s): must have a maximum value of %s (provided value: %s)",
              areaOfLaw, maxAllowed, disbursementsVatAmount),
          EVENT_SERVICE);
    }
  }

  /**
   * Validates the unique file number of the given claim to ensure it contains a valid and
   * non-future date in the format DDMMYY. If the date is invalid or in the future, an error is
   * added to the submission validation context.
   *
   * @param claim the claim object containing the unique file number to be validated
   */
  private void validateUniqueFileNumber(ClaimResponse claim, SubmissionValidationContext context) {
    String uniqueFileNumber = claim.getUniqueFileNumber();
    if (uniqueFileNumber != null && uniqueFileNumber.length() > 1) {
      try {
        LocalDate date = UniqueFileNumberUtil.parse(uniqueFileNumber);
        if (!date.isBefore(LocalDate.now())) {
          context.addClaimError(
              claim.getId(), ClaimValidationError.INVALID_DATE_IN_UNIQUE_FILE_NUMBER);
        }
      } catch (DateTimeParseException e) {
        context.addClaimError(
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
      ClaimResponse claim,
      String fieldName,
      String dateValueToCheck,
      String oldestDateAllowedStr,
      SubmissionValidationContext context) {
    if (dateValueToCheck != null) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      try {
        LocalDate oldestDateAllowed = LocalDate.parse(oldestDateAllowedStr, formatter);
        LocalDate date = LocalDate.parse(dateValueToCheck, formatter);
        if (date.isBefore(oldestDateAllowed) || date.isAfter(LocalDate.now())) {
          context.addClaimError(
              claim.getId(),
              String.format(
                  "Invalid date value for %s (Must be between %s and today): %s",
                  fieldName, oldestDateAllowedStr, dateValueToCheck),
              EVENT_SERVICE);
        }
      } catch (DateTimeParseException e) {
        context.addClaimError(
            claim.getId(),
            String.format("Invalid date value provided for %s: %s", fieldName, dateValueToCheck),
            EVENT_SERVICE);
      }
    }
  }

  private List<String> getEffectiveCategoriesOfLaw(
      String officeCode, String areaOfLaw, LocalDate effectiveDate) {
    return providerDetailsRestClient
        .getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate)
        .blockOptional()
        .stream()
        .map(ProviderFirmOfficeContractAndScheduleDto::getSchedules)
        .flatMap(List::stream)
        .map(FirmOfficeContractAndScheduleDetails::getScheduleLines)
        .flatMap(List::stream)
        .map(FirmOfficeContractAndScheduleLine::getCategoryOfLaw)
        .toList();
  }

  private void validateDuplicateClaims(
      final ClaimResponse claim,
      final List<ClaimResponse> submissionClaims,
      final String areaOfLaw,
      final String officeCode,
      final SubmissionValidationContext context) {
    switch (areaOfLaw) {
      case StrategyTypes.CRIME ->
          strategies
              .get(StrategyTypes.CRIME)
              .validateDuplicateClaims(claim, submissionClaims, officeCode, context);
      case StrategyTypes.CIVIL ->
          strategies
              .get(StrategyTypes.CIVIL)
              .validateDuplicateClaims(claim, submissionClaims, officeCode, context);
      default ->
          log.debug("No duplicate claim validation strategy found for area of law: {}", areaOfLaw);
    }
  }
}
