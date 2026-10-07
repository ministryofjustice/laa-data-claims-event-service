package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.EventServiceIllegalArgumentException;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.CategoryOfLawValidationService;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.FeeDetailsResponseWrapper;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.ProviderDetailsService;
import uk.gov.justice.laa.dstew.payments.claimsevent.util.ClaimEffectiveDateUtil;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laadata.providers.model.FirmOfficeContractAndScheduleDetails;
import uk.gov.justice.laadata.providers.model.FirmOfficeContractAndScheduleLine;
import uk.gov.justice.laadata.providers.model.ProviderFirmOfficeContractAndScheduleDto;

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
   * @param providerDetailsService the provider details service (owns caching/retry for PDA calls)
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
   * @param officeCode the office code
   * @param feeDetailsResponseMap a map containing FeeDetailsResponse and their corresponding
   *     feeCodes
   */
  public void validate(
      ClaimResponse claim,
      SubmissionValidationContext context,
      String officeCode,
      Map<String, FeeDetailsResponseWrapper> feeDetailsResponseMap) {
    LocalDate effectiveDate = null;
    try {
      effectiveDate = ClaimEffectiveDateUtil.getEffectiveDate(claim);
      List<String> effectiveCategoriesOfLaw =
          getEffectiveCategoriesOfLaw(officeCode, effectiveDate);
      // Get effective category of law lookup
      categoryOfLawValidationService.validateCategoriesOfLaw(
          claim, feeDetailsResponseMap, effectiveCategoriesOfLaw, context);
    } catch (EventServiceIllegalArgumentException e) {
      log.info(
          "Error getting effective date for category of law validation: {}. Continuing with claim"
              + " validation",
          e.getMessage());
    } catch (WebClientResponseException ex) {
      log.error(
          "Error calling provider details API: Status={}, Message={}, officeCode={}, effectiveDate={},"
              + "Please check if the API endpoint is configured correctly.",
          ex.getStatusCode(),
          ex.getMessage(),
          officeCode,
          effectiveDate,
          ex);
      handleProviderDetailsApiError(context, claim.getId());
    } catch (Exception ex) {
      log.error(
          "Unexpected error during category of law validation for officeCode={}, effectiveDate={}",
          officeCode,
          effectiveDate,
          ex);
      handleProviderDetailsApiError(context, claim.getId());
    }
  }

  /**
   * Retrieves the category-of-law codes effective for the given office and date.
   *
   * <p>The PDA API requires an explicit {@code effectiveDate} (omitting it defaults server-side to
   * "today"), so it is always passed through to {@link ProviderDetailsService}, which owns caching
   * and retry behaviour for this call.
   *
   * @param officeCode the office code
   * @param effectiveDate the claim's effective date; must not be {@code null}
   * @return distinct category-of-law codes for the office/date, possibly empty when no schedules
   *     are returned
   */
  private List<String> getEffectiveCategoriesOfLaw(String officeCode, LocalDate effectiveDate) {
    List<FirmOfficeContractAndScheduleDetails> schedules =
        providerDetailsService
            .getProviderFirmSchedules(officeCode, effectiveDate)
            .blockOptional()
            .map(ProviderFirmOfficeContractAndScheduleDto::getSchedules)
            .orElse(Collections.emptyList());

    return getEffectiveCategoriesOfLawForSchedules(schedules);
  }

  private void handleProviderDetailsApiError(SubmissionValidationContext context, String claimId) {
    context.addClaimError(claimId, ClaimValidationError.TECHNICAL_ERROR_PROVIDER_DETAILS_API);
  }

  /**
   * Extracts distinct category-of-law codes from the provided schedule details.
   *
   * <p>Business purpose: used to determine which category-of-law codes apply for a set of schedules
   * (typically schedules that are effective for a given date/office) so callers can validate a
   * claim's category of law.
   *
   * <p>Behaviour and assumptions:
   *
   * <ul>
   *   <li>Null or empty input returns an empty list (no exception).
   *   <li>Null scheduleLines are ignored.
   *   <li>Null category codes are ignored.
   *   <li>Duplicate category codes are removed; first-seen order is preserved.
   * </ul>
   *
   * @param officeContractAndScheduleDetails list of schedule details, may be null
   * @return non-null (possibly empty) list of distinct category-of-law codes preserving first-seen
   *     order
   */
  private List<String> getEffectiveCategoriesOfLawForSchedules(
      List<FirmOfficeContractAndScheduleDetails> officeContractAndScheduleDetails) {
    if (ObjectUtils.isEmpty(officeContractAndScheduleDetails)) {
      return Collections.emptyList();
    }
    return officeContractAndScheduleDetails.stream()
        .map(FirmOfficeContractAndScheduleDetails::getScheduleLines)
        .filter(Objects::nonNull)
        .flatMap(List::stream)
        .map(FirmOfficeContractAndScheduleLine::getCategoryOfLaw)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
  }
}
