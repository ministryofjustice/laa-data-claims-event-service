package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.EventServiceIllegalArgumentException;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.CategoryOfLawValidationService;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.FeeDetailsResponseWrapper;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.ProviderDetailsService;
import uk.gov.justice.laa.dstew.payments.claimsevent.util.ClaimEffectiveDateUtil;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.provider.model.FirmOfficeContractAndScheduleDetails;
import uk.gov.justice.laa.provider.model.FirmOfficeContractAndScheduleLine;
import uk.gov.justice.laa.provider.model.ProviderFirmOfficeContractAndScheduleDto;

/**
 * Validates that a claim's effective category of law is valid.
 *
 * @author Jamie Briggs
 * @see ClaimResponse
 * @see SubmissionValidationContext
 */
@Component
@Slf4j
public final class EffectiveCategoryOfLawClaimValidator implements ClaimValidator {

  private final CategoryOfLawValidationService categoryOfLawValidationService;
  private final ProviderDetailsService providerDetailsService;

  /**
   * Constructs an instance of {@link EffectiveCategoryOfLawClaimValidator}.
   *
   * @param categoryOfLawValidationService the category of law validation service
   * @param providerDetailsService the provider details rest client
   */
  public EffectiveCategoryOfLawClaimValidator(
      CategoryOfLawValidationService categoryOfLawValidationService,
      ProviderDetailsService providerDetailsService) {
    this.categoryOfLawValidationService = categoryOfLawValidationService;
    this.providerDetailsService = providerDetailsService;
  }

  @Override
  public int priority() {
    return 1000;
  }

  /**
   * Validates that a claim's effective category of law is valid.
   *
   * @param claim the claim to validate
   * @param context the validation context to add errors to
   * @param areaOfLaw the area of law
   * @param officeCode the office code
   * @param feeDetailsResponseMap a map containing FeeDetailsResponse and their corresponding
   *     feeCodes
   */
  public void validate(
      ClaimResponse claim,
      SubmissionValidationContext context,
      AreaOfLaw areaOfLaw,
      String officeCode,
      Map<String, FeeDetailsResponseWrapper> feeDetailsResponseMap) {
    LocalDate effectiveDate = null;
    try {
      effectiveDate = ClaimEffectiveDateUtil.getEffectiveDate(claim);
      List<String> effectiveCategoriesOfLaw =
          getEffectiveCategoriesOfLaw(officeCode, areaOfLaw.getValue(), effectiveDate);
      // Get effective category of law lookup
      categoryOfLawValidationService.validateCategoryOfLaw(
          claim, feeDetailsResponseMap, effectiveCategoriesOfLaw, context);
    } catch (EventServiceIllegalArgumentException e) {
      log.info(
          "Error getting effective date for category of law validation: {}. Continuing with claim"
              + " validation",
          e.getMessage());
    } catch (WebClientResponseException ex) {
      log.error(
          "Error calling provider details API: Status={}, Message={}, officeCode={}, areaOfLaw={}, effectiveDate={},"
              + "Please check if the API endpoint is configured correctly.",
          ex.getStatusCode(),
          ex.getMessage(),
          officeCode,
          areaOfLaw.getValue(),
          effectiveDate,
          ex);
      handleProviderDetailsApiError(context, claim.getId());
    } catch (Exception ex) {
      log.error(
          "Unexpected error during category of law validation for officeCode={}, areaOfLaw={}, effectiveDate={}",
          officeCode,
          areaOfLaw.getValue(),
          effectiveDate,
          ex);
      handleProviderDetailsApiError(context, claim.getId());
    }
  }

  private List<String> getEffectiveCategoriesOfLaw(
      String officeCode, String areaOfLaw, LocalDate effectiveDate) {
    return providerDetailsService
        .getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate)
        .blockOptional()
        .map(this::extractCategoriesFromSchedules)
        .orElse(Collections.emptyList());
  }

  private List<String> extractCategoriesFromSchedules(
      ProviderFirmOfficeContractAndScheduleDto schedulesDto) {
    return schedulesDto.getSchedules().stream()
        .map(FirmOfficeContractAndScheduleDetails::getScheduleLines)
        .flatMap(List::stream)
        .map(FirmOfficeContractAndScheduleLine::getCategoryOfLaw)
        .toList();
  }

  private void handleProviderDetailsApiError(SubmissionValidationContext context, String claimId) {
    context.addClaimError(claimId, ClaimValidationError.TECHNICAL_ERROR_PROVIDER_DETAILS_API);
  }
}
