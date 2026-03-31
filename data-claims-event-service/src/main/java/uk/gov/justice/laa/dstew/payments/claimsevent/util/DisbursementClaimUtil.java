package uk.gov.justice.laa.dstew.payments.claimsevent.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.FeeCalculationType;

/**
 * Utility class providing disbursement-claim-specific helper methods shared across validation
 * components.
 */
public final class DisbursementClaimUtil {

  /**
   * The number of calendar months that must separate two submission periods for a disbursement
   * claim to fall outside the duplicate-detection window.
   */
  public static final int MAXIMUM_MONTHS_DIFFERENCE = 3;

  /**
   * The number of months added to the base period when calculating the submission cutoff date. The
   * cutoff falls in the month <em>following</em> the base period.
   */
  private static final int CUTOFF_MONTH_OFFSET = 1;

  /**
   * The day of the month on which the submission cutoff falls. Disbursement claims must be
   * submitted by the {@value}th of the cutoff month.
   */
  private static final int CUTOFF_DAY_OF_MONTH = 20;

  private DisbursementClaimUtil() {}

  /**
   * Returns {@code true} if the given fee type represents a disbursement-only claim.
   *
   * @param feeType the fee calculation type string to evaluate
   * @return {@code true} if {@code feeType} equals {@link FeeCalculationType#DISB_ONLY}
   */
  public static boolean isDisbursementClaim(String feeType) {
    return Objects.equals(feeType, FeeCalculationType.DISB_ONLY.getValue());
  }

  /**
   * Calculates the submission cutoff date for a given disbursement submission period. The cutoff is
   * the {@value CUTOFF_DAY_OF_MONTH}th day of the month following the given period, and represents
   * the deadline by which a disbursement claim for that period must be submitted.
   *
   * <p>For example, a submission period of MAY-2025 yields a cutoff of 20 JUN-2025.
   *
   * @param submissionPeriod the submission period for which the cutoff is calculated
   * @return the cutoff date ({@value CUTOFF_DAY_OF_MONTH}th of the month following {@code
   *     submissionPeriod})
   */
  public static LocalDate submissionPeriodCutoffDate(YearMonth submissionPeriod) {
    return submissionPeriod.plusMonths(CUTOFF_MONTH_OFFSET).atDay(CUTOFF_DAY_OF_MONTH);
  }
}
