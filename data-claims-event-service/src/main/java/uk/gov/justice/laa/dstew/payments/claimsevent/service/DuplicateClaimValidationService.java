package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimFields;

/** Service responsible for validating whether a claim is a duplicate. */
@Service
public class DuplicateClaimValidationService {

  /**
   * Validates whether a claim has been previously submitted, and is therefore a duplicate.
   *
   * @param claim the submitted claim
   */
  public void validateDuplicateClaims(ClaimFields claim) {
    // TODO: Duplicate validation. See https://dsdmoj.atlassian.net/browse/CCMSPUI-790.
  }
}
