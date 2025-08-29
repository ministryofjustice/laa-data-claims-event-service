package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimFields;

/** Service responsible for validating whether a claim is a duplicate. */
@Slf4j
@Service
public class DuplicateClaimValidationService {

  /**
   * Validates whether a claim has been previously submitted, and is therefore a duplicate.
   *
   * @param claim the submitted claim
   */
  public void validateDuplicateClaims(ClaimFields claim) {
    log.debug("Validating duplicated for claim {}", claim.getId());
    // TODO: Duplicate validation. See https://dsdmoj.atlassian.net/browse/CCMSPUI-790.
    log.debug("Duplicate validation completed for claim {}", claim.getId());
  }
}
