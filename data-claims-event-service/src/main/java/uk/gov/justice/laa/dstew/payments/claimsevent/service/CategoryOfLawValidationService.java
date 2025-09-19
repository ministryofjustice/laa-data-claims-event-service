package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.fee.scheme.model.CategoryOfLawResponse;

/** A service responsible for validating data items related to category of law. */
@Slf4j
@Service
@AllArgsConstructor
public class CategoryOfLawValidationService {

  private final FeeSchemePlatformRestClient feeSchemePlatformRestClient;

  /**
   * Validates that a valid category of law exists for the fee code provided in the claim.
   *
   * @param claim the submitted claim
   * @param categoryOfLawLookup the lookup of feeCode -> categoryOfLaw for all claims in the
   *     submission
   */
  public void validateCategoryOfLaw(
      ClaimResponse claim,
      Map<String, CategoryOfLawResult> categoryOfLawLookup,
      List<String> providerCategoriesOfLaw,
      SubmissionValidationContext context) {

    log.debug("Validating category of law for claim {}", claim.getId());

    CategoryOfLawResult categoryOfLawResult = categoryOfLawLookup.get(claim.getFeeCode());

    if (categoryOfLawResult.isError()) {
      context.flagForRetry(claim.getId());
    } else {
      String categoryOfLaw = categoryOfLawResult.getCategoryOfLaw();

      if (categoryOfLaw == null) {
        context.addClaimError(
            claim.getId(), ClaimValidationError.INVALID_CATEGORY_OF_LAW_AND_FEE_CODE);
      } else if (!providerCategoriesOfLaw.contains(categoryOfLaw)) {
        context.addClaimError(
            claim.getId(),
            ClaimValidationError.INVALID_CATEGORY_OF_LAW_NOT_AUTHORISED_FOR_PROVIDER);
      }
    }
    log.debug("Category of law validation completed for claim {}", claim.getId());
  }

  /**
   * Build a lookup of feeCode -> categoryOfLaw ({@link CategoryOfLawResult}) where feeCode are the
   * unique fee codes found in the list of claims, and categoryOfLaw result represents the response
   * from the category of law endpoint of the Fee Scheme Platform API.
   *
   * <p>Category of law may be null if none were found corresponding to the fee code.
   *
   * @param claims the list of claims (from the submission)
   * @return the feeCode -> categoryOfLaw lookup
   */
  public Map<String, CategoryOfLawResult> getCategoryOfLawLookup(List<ClaimResponse> claims) {
    Set<String> uniqueFeeCodes =
        claims.stream().map(ClaimResponse::getFeeCode).collect(Collectors.toSet());

    Map<String, CategoryOfLawResult> categoryOfLawLookup = new HashMap<>();

    uniqueFeeCodes.forEach(
        feeCode -> {
          ResponseEntity<CategoryOfLawResponse> categoryOfLawResponse =
              feeSchemePlatformRestClient.getCategoryOfLaw(feeCode);
          if (categoryOfLawResponse.getStatusCode().is2xxSuccessful()) {
            categoryOfLawLookup.put(
                feeCode,
                CategoryOfLawResult.withCategoryOfLaw(
                    categoryOfLawResponse.getBody().getCategoryOfLawCode()));
          } else if (categoryOfLawResponse.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
            log.debug("Get category of law returned 404 for fee code: {}", feeCode);
            categoryOfLawLookup.put(feeCode, CategoryOfLawResult.withCategoryOfLaw(null));
          } else {
            log.debug(
                "Get category of law resulted in error for fee code {} with status: {}",
                feeCode,
                categoryOfLawResponse.getStatusCode());
            categoryOfLawLookup.put(feeCode, CategoryOfLawResult.error());
          }
        });

    return categoryOfLawLookup;
  }
}
