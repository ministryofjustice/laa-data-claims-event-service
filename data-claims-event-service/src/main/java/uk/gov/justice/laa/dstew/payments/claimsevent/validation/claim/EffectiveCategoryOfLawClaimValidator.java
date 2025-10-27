package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ProviderDetailsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.EventServiceIllegalArgumentException;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.CategoryOfLawValidationService;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.FeeDetailsResponseWrapper;
import uk.gov.justice.laa.dstew.payments.claimsevent.util.ClaimEffectiveDateUtil;
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
public class EffectiveCategoryOfLawClaimValidator implements ClaimValidator {

  private final CategoryOfLawValidationService categoryOfLawValidationService;
  private final ClaimEffectiveDateUtil claimEffectiveDateUtil;
  private final ProviderDetailsRestClient providerDetailsRestClient;

  protected EffectiveCategoryOfLawClaimValidator(
      CategoryOfLawValidationService categoryOfLawValidationService,
      ClaimEffectiveDateUtil claimEffectiveDateUtil,
      ProviderDetailsRestClient providerDetailsRestClient) {
    this.categoryOfLawValidationService = categoryOfLawValidationService;
    this.claimEffectiveDateUtil = claimEffectiveDateUtil;
    this.providerDetailsRestClient = providerDetailsRestClient;
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
      String areaOfLaw,
      String officeCode,
      Map<String, FeeDetailsResponseWrapper> feeDetailsResponseMap) {
    try {
      LocalDate effectiveDate = claimEffectiveDateUtil.getEffectiveDate(claim);
      List<String> effectiveCategoriesOfLaw =
          getEffectiveCategoriesOfLaw(officeCode, areaOfLaw, effectiveDate);
      // Get effective category of law lookup
      categoryOfLawValidationService.validateCategoryOfLaw(
          claim, feeDetailsResponseMap, effectiveCategoriesOfLaw, context);
    } catch (EventServiceIllegalArgumentException e) {
      log.debug(
          "Error getting effective date for category of law validation: {}. Continuing with claim"
              + " validation",
          e.getMessage());
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
}
