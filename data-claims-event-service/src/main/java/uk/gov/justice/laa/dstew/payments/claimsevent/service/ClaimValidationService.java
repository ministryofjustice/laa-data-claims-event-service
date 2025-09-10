package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;

/**
 * A service for validating submitted claims that are ready to process. Validation errors will
 * result in claims being marked as invalid and all validation errors will be reported against the
 * claim.
 */
@Service
@AllArgsConstructor
public class ClaimValidationService {

  private final CategoryOfLawValidationService categoryOfLawValidationService;
  private final DuplicateClaimValidationService duplicateClaimValidationService;
  private final FeeCalculationService feeCalculationService;

  /**
   * Validate a list of claims in a submission.
   *
   * @param claims the claims in a submission
   */
  public void validateClaims(List<ClaimResponse> claims, List<String> providerCategoriesOfLaw) {
    Map<String, CategoryOfLawResult> categoryOfLawLookup =
        categoryOfLawValidationService.getCategoryOfLawLookup(claims);
    claims.forEach(claim -> validateClaim(claim, categoryOfLawLookup, providerCategoriesOfLaw));
  }

  /**
   * Validate a claim.
   *
   * <p>Validates that:
   *
   * <ul>
   *   <li>The claim's fee code is associated with a valid category of law
   *   <li>The claim is not a duplicate
   *   <li>The claim passes fee calculation
   * </ul>
   *
   * @param claim the submitted claim
   * @param categoryOfLawLookup the lookup of fee code -> category of law for the submission
   */
  private void validateClaim(
      ClaimResponse claim,
      Map<String, CategoryOfLawResult> categoryOfLawLookup,
      List<String> providerCategoriesOfLaw) {
    categoryOfLawValidationService.validateCategoryOfLaw(
        claim, categoryOfLawLookup, providerCategoriesOfLaw);
    duplicateClaimValidationService.validateDuplicateClaims(claim);
    feeCalculationService.validateFeeCalculation(claim);
  }
}
