package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import uk.gov.justice.laa.fee.scheme.model.FeeDetailsResponse;

/**
 * A container for the result of a request to the fee details endpoint of the Fee Scheme Platform
 * API.
 */
@Getter
@EqualsAndHashCode
public class FeeDetailsResponseWrapper {

  private final FeeDetailsResponse feeDetailsResponse;
  private final boolean error;

  private FeeDetailsResponseWrapper(FeeDetailsResponse feeDetailsResponse, boolean error) {
    this.feeDetailsResponse = feeDetailsResponse;
    this.error = error;
  }

  public static FeeDetailsResponseWrapper withFeeDetailsResponse(
      FeeDetailsResponse feeDetailsResponse) {
    return new FeeDetailsResponseWrapper(feeDetailsResponse, false);
  }

  public static FeeDetailsResponseWrapper error() {
    return new FeeDetailsResponseWrapper(null, true);
  }
}
