package uk.gov.justice.laa.dstew.payments.claimsevent.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.EventServiceIllegalArgumentException;

/**
 * Utility class for calculating the effective date of a claim.
 *
 * @author Jamie Briggs
 */
@Component
public final class ClaimEffectiveDateUtil {

  /**
   * Gets the effective date for a claim based on what fields are available.
   *
   * <p>The effective date is calculated in the following order:
   *
   * <ul>
   *   <li>Use case start date if available.
   *   <li>Fall back to representation order date if available.
   *   <li>Otherwise, use unique file number.
   * </ul>
   *
   * @param claimResponse the claim to calculate the effective date for
   * @return the effective date for the claim
   * @throws EventServiceIllegalArgumentException if dates are unavailable or invalid
   */
  public LocalDate getEffectiveDate(final ClaimResponse claimResponse)
      throws EventServiceIllegalArgumentException {

    if (StringUtils.isNotEmpty(claimResponse.getCaseStartDate())) {
      return parseDate(claimResponse.getCaseStartDate(), "case start date");
    }

    if (StringUtils.isNotEmpty(claimResponse.getRepresentationOrderDate())) {
      return parseDate(claimResponse.getRepresentationOrderDate(), "representation order date");
    }

    if (StringUtils.isNotEmpty(claimResponse.getUniqueFileNumber())) {
      return UniqueFileNumberUtil.parse(claimResponse.getUniqueFileNumber());
    }

    throw new EventServiceIllegalArgumentException(
        "No fields available to determine effective date of claim ID: " + claimResponse.getId());
  }

  private LocalDate parseDate(final String dateStr, final String fieldName) {
    try {
      return LocalDate.parse(dateStr);
    } catch (DateTimeParseException e) {
      throw new EventServiceIllegalArgumentException(
          String.format("Invalid date format for %s: %s", fieldName, dateStr));
    }
  }
}
