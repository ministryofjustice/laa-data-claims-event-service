package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import java.util.List;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * Registry containing lists of fields that should be excluded from certain validation rules. Used
 * to manage special cases in claim validation logic.
 */
@Component
@Getter
public class ExclusionsRegistry {

  /**
   * List of field names that should be excluded from mandatory field validation for
   * disbursement-only claims.
   */
  private final List<String> disbursementOnlyExclusions =
      List.of(
          "travelWaitingCostsAmount",
          "adviceTime",
          "travelTime",
          "waitingTime",
          "netCounselCostsAmount",
          "netProfitCostsAmount",
          "isVatApplicable");
}
