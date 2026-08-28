package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

  private List<String> getEffectiveCategoriesOfLaw(String officeCode, LocalDate effectiveDate) {
    return providerDetailsService
        .getProviderFirmSchedules(officeCode, effectiveDate)
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

  private List<String> getEffectiveCategoriesOfLawForOfficeByEffectiveDate(String officeCode, LocalDate effectiveDate) {
    // Load in ALL the provider schedules for the office code from the API handle any errors that may occur
    Optional<ProviderFirmOfficeContractAndScheduleDto> officeSchedules =  providerDetailsService
            .getProviderFirmSchedules(officeCode, null).blockOptional();

    // iterate the schedules and return all schedules where the effective date is between the start and end date of the schedule
    List<FirmOfficeContractAndScheduleDetails> filteredEffectiveSchedulesByEffectiveDate = filterEffectiveSchedulesByEffectiveDate(officeSchedules.orElse(null), effectiveDate);

    // no schedules then throw an error
    if (filteredEffectiveSchedulesByEffectiveDate.isEmpty()) {
      throw new EventServiceIllegalArgumentException("Effective date " + effectiveDate + " is not within any of the schedules for office code "+ officeCode);
    }

    // iterate the filtered schedules and return all the category of law codes for the schedules
    return getEffectiveCategoriesOfLawForSchedules(filteredEffectiveSchedulesByEffectiveDate);
  }

  /**
   * Filters the schedules in the supplied DTO to those that are effective for the supplied
   * effectiveDate. A schedule is considered effective if the effectiveDate is within the
   * inclusive interval [contractStartDate, contractEndDate], using the same semantics as
   * {@link #isEffectiveDateWithinSchedule(FirmOfficeContractAndScheduleDetails, java.time.LocalDate)}.
   * <p>
   * Business purpose: determine which provider schedules apply for a claim's effective date so
   * callers can extract category-of-law codes for validation.
   *
   * @param schedulesDto DTO containing the list of schedules to be filtered; if {@code null}
   *                     this method returns an empty list.
   * @param effectiveDate the date to test for schedule effectiveness; if {@code null} this
   *                      method treats it as not contained in any schedule and returns an empty list.
   * @return a non-null, possibly empty, unmodifiable list of schedules from {@code schedulesDto.getSchedules()}
   *         that are effective on {@code effectiveDate}. The returned list preserves the iteration
   *         order of {@code schedulesDto.getSchedules()}.
   * <p>
   * Important assumptions/constraints:
   * - Null or empty input schedules are treated as no matches and result in an empty list.
   * - Individual null schedule elements are ignored.
   * - The returned list is unmodifiable;
   */
  private List<FirmOfficeContractAndScheduleDetails> filterEffectiveSchedulesByEffectiveDate(
      ProviderFirmOfficeContractAndScheduleDto schedulesDto, LocalDate effectiveDate) {
    if (schedulesDto == null || ObjectUtils.isEmpty(schedulesDto.getSchedules()) || effectiveDate == null) {
      return Collections.emptyList();
    }

    return schedulesDto.getSchedules().stream()
        .filter(schedule -> isEffectiveDateWithinSchedule(schedule, effectiveDate))
        .toList();
  }

  /**
   * Determine whether the provided effectiveDate falls within the inclusive contract period
   * defined by the schedule's contractStartDate and contractEndDate.
   * <p>
   * Business purpose: used when filtering schedules to those that are effective for a given
   * claim effective date so that category-of-law codes can be derived.
   * <p>
   * Semantics/assumptions:
   * - contractStartDate and contractEndDate are inclusive bounds.
   * - a null contractStartDate is treated as unbounded on the lower side.
   * - a null contractEndDate is treated as unbounded on the upper side.
   * - a null effectiveDate is considered not within the schedule (returns false).
   * - a null schedule is considered not containing the date (returns false).
   *
   * @param schedule the schedule whose contract start/end define the inclusive interval; may be null
   * @param effectiveDate the date to test; if null the method returns false
   * @return true if effectiveDate is within [contractStartDate or -infty, contractEndDate or +infty], false otherwise
   */
  private boolean isEffectiveDateWithinSchedule(FirmOfficeContractAndScheduleDetails schedule, LocalDate effectiveDate) {
    if (effectiveDate == null || schedule == null) {
      return false;
    }

    LocalDate start = schedule.getContractStartDate();
    LocalDate end = schedule.getContractEndDate();

    if (start != null && effectiveDate.isBefore(start)) {
      return false;
    }
    return end == null || !effectiveDate.isAfter(end);
  }

  /**
   * Extracts distinct category-of-law codes from the provided schedule details.
   *
   * <p>Business purpose: used to determine which category-of-law codes apply for a set of
   * schedules (typically schedules that are effective for a given date/office) so callers can
   * validate a claim's category of law.
   *
   * <p>Behaviour and assumptions:
   * <ul>
   *   <li>Null or empty input returns an empty list (no exception).
   *   <li>Null scheduleLines are ignored.
   *   <li>Null category codes are ignored.
   *   <li>Duplicate category codes are removed; first-seen order is preserved.
   * </ul>
   *
   * @param officeContractAndScheduleDetails list of schedule details, may be null
   * @return non-null (possibly empty) list of distinct category-of-law codes preserving first-seen order
   */
  private List<String> getEffectiveCategoriesOfLawForSchedules(List<FirmOfficeContractAndScheduleDetails> officeContractAndScheduleDetails) {
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
