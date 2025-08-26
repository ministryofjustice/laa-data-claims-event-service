package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimFields;

/**
 * Service responsible for validating the fee calculation response from the Fee Scheme Platform API.
 */
@Service
public class FeeCalculationService {

  /**
   * Calculates the fee for the claim using the Fee Scheme Platform API, and handles any returned
   * validation errors.
   *
   * @param claim the submitted claim
   */
  public void validateFeeCalculation(ClaimFields claim) {
    // TODO: Calculate fee. See https://dsdmoj.atlassian.net/browse/CCMSPUI-825.
  }
}
