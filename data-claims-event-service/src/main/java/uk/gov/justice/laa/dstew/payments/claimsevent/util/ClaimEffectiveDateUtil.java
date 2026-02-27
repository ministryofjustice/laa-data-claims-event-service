package uk.gov.justice.laa.dstew.payments.claimsevent.util;

import java.time.LocalDate;
import java.util.Objects;
import org.springframework.util.StringUtils;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.EventServiceIllegalArgumentException;

/**
 * Utility class for calculating the effective date of a claim.
 *
 * @author Jamie Briggs
 */
public final class ClaimEffectiveDateUtil {

  /**
   * The fee code that identifies a PROD (Preparation of Defence) claim. Claims with this fee code
   * use a different effective date resolution order — see {@link #getEffectiveDate(ClaimResponse)}.
   */
  private static final String PROD_FEE_CODE = "PROD";

  private ClaimEffectiveDateUtil() {}

  /**
   * Gets the effective date for a claim based on what fields are available.
   *
   * <p>For claims with a fee code of {@value #PROD_FEE_CODE}, the effective date is resolved in the
   * following order:
   *
   * <ol>
   *   <li>Case Concluded Date, if present.
   *   <li>Case Start Date, if present.
   * </ol>
   *
   * <p>For all other claims, the effective date is resolved in the following order:
   *
   * <ol>
   *   <li>Case Start Date, if present.
   *   <li>Representation Order Date, if present.
   *   <li>Date derived from the Unique File Number.
   * </ol>
   *
   * @param claimResponse the claim to calculate the effective date for
   * @return the effective date for the claim
   * @throws EventServiceIllegalArgumentException if no date fields are available, or if a date
   *     field is present but cannot be parsed
   */
  public static LocalDate getEffectiveDate(final ClaimResponse claimResponse)
      throws EventServiceIllegalArgumentException {

    if (Objects.equals(claimResponse.getFeeCode(), PROD_FEE_CODE)
        && StringUtils.hasText(claimResponse.getCaseConcludedDate())) {
      return DateUtil.parseDate(claimResponse.getCaseConcludedDate(), "case concluded date");
    }

    if (StringUtils.hasText(claimResponse.getCaseStartDate())) {
      return DateUtil.parseDate(claimResponse.getCaseStartDate(), "case start date");
    }

    if (StringUtils.hasText(claimResponse.getRepresentationOrderDate())) {
      return DateUtil.parseDate(
          claimResponse.getRepresentationOrderDate(), "representation order date");
    }

    if (StringUtils.hasText(claimResponse.getUniqueFileNumber())) {
      return UniqueFileNumberUtil.parse(claimResponse.getUniqueFileNumber());
    }

    throw new EventServiceIllegalArgumentException(
        "No fields available to determine effective date of claim ID: " + claimResponse.getId());
  }
}
