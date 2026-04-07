package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.fee.scheme.model.FeeDetailsResponseV2;

/** A service responsible for validating data items related to category of law. */
@Slf4j
@Service
@AllArgsConstructor
public class CategoryOfLawValidationService {

  private final FeeSchemePlatformRestClient feeSchemePlatformRestClient;

  /**
   * Validates whether the claim's fee code is associated with a category of law that the provider
   * is authorised to use.
   *
   * <p>The validation process checks the fee details response associated with the claim's fee code.
   * If the external fee details service returns an error, the claim is flagged for retry. If the
   * fee details are returned successfully, the method verifies that the category of law codes in
   * the response intersect with the provider's authorised categories.
   *
   * <p>If no category of law codes are provided, or if there is no intersection with the provider's
   * authorised categories, the claim is marked with the appropriate validation error. When a match
   * exists, the resolved valid category of law code is recorded in the validation context.
   *
   * @param claim the claim being validated
   * @param feeDetailsResponseMap a mapping of fee codes to their corresponding fee details
   * @param providerCategoriesOfLaw the list of category of law codes the provider is authorised for
   * @param context the validation context used to track errors, retries, and resolved values
   */
  public void validateCategoriesOfLaw(
      ClaimResponse claim,
      Map<String, FeeDetailsResponseWrapper> feeDetailsResponseMap,
      List<String> providerCategoriesOfLaw,
      SubmissionValidationContext context) {

    log.debug("Validating categories of law for claim {}", claim.getId());

    FeeDetailsResponseWrapper feeDetailsResponseWrapper =
        feeDetailsResponseMap.get(claim.getFeeCode());

    if (feeDetailsResponseWrapper.isError()) {
      context.flagForRetry(claim.getId());
    } else if (feeDetailsResponseWrapper.getFeeDetailsResponse() != null) {
      validateProviderCategoriesOfLaw(
          claim,
          providerCategoriesOfLaw,
          feeDetailsResponseWrapper.getFeeDetailsResponse().getCategoryOfLawCodes(),
          context);
    }
    log.debug("Categories of law validation completed for claim {}", claim.getId());
  }

  /**
   * Validates that the claim's fee code is associated with at least one category of law for which
   * the provider is authorised. If the claim does not specify any applicable categories of law, or
   * none of them match the provider's authorised categories, an appropriate validation error is
   * recorded in the {@link SubmissionValidationContext}.
   *
   * <p>When a valid authorised category is found, it is stored in the validation context against
   * the claim’s fee code.
   *
   * @param claim the claim being validated, containing identifiers and the fee code
   * @param providerCategoriesOfLaw the list of category-of-law codes the provider is authorised for
   * @param categoryOfLawCodes the list of category-of-law codes applicable to the claim's fee code
   * @param context the validation context used to track valid category codes and errors
   */
  private void validateProviderCategoriesOfLaw(
      ClaimResponse claim,
      List<String> providerCategoriesOfLaw,
      List<String> categoryOfLawCodes,
      SubmissionValidationContext context) {

    if (CollectionUtils.isEmpty(categoryOfLawCodes)) {
      context.addClaimError(
          claim.getId(),
          ClaimValidationError.INVALID_CATEGORY_OF_LAW_AND_FEE_CODE,
          claim.getFeeCode());
      return;
    }

    Optional<String> authorisedCategoryOfLaw =
        categoryOfLawCodes.stream().filter(providerCategoriesOfLaw::contains).findFirst();
    if (authorisedCategoryOfLaw.isEmpty()) {
      context.putAuthorisedCategoryOfLawCode(claim.getFeeCode(), null);
      context.addClaimError(
          claim.getId(), ClaimValidationError.INVALID_CATEGORY_OF_LAW_NOT_AUTHORISED_FOR_PROVIDER);
    } else {
      context.putAuthorisedCategoryOfLawCode(claim.getFeeCode(), authorisedCategoryOfLaw.get());
    }
  }

  /**
   * Build a lookup of feeCode -> FeeDetailsResponseWrapper ({@link FeeDetailsResponseWrapper})
   * where feeCode are the unique fee codes found in the list of claims, and
   * FeeDetailsResponseWrapper represents the response from the fee details endpoint of the Fee
   * Scheme Platform API.
   *
   * <p>FeeDetailsResponse may be null if none were found corresponding to the fee code.
   *
   * @param claims the list of claims (from the submission)
   * @return the feeCode -> FeeDetailsResponseWrapper
   */
  public Map<String, FeeDetailsResponseWrapper> getFeeDetailsResponseForAllFeeCodesInClaims(
      List<ClaimResponse> claims) {
    Set<String> uniqueFeeCodes =
        claims.stream().map(ClaimResponse::getFeeCode).collect(Collectors.toSet());

    return uniqueFeeCodes.stream()
        .collect(Collectors.toMap(feeCode -> feeCode, this::getFeeDetails));
  }

  private FeeDetailsResponseWrapper getFeeDetails(String feeCode) {
    if (!StringUtils.hasText(feeCode)) {
      return FeeDetailsResponseWrapper.withFeeDetailsResponse(null);
    }
    try {
      FeeDetailsResponseV2 response = feeSchemePlatformRestClient.getFeeDetails(feeCode).getBody();

      if (response == null) {
        log.error("Get fee details returned empty response for fee code: {}", feeCode);
      }
      return FeeDetailsResponseWrapper.withFeeDetailsResponse(response);

    } catch (WebClientResponseException ex) {
      log.error("Get fee details returned {} for fee code: {}", ex.getStatusCode(), feeCode);
      return FeeDetailsResponseWrapper.withFeeDetailsResponse(null);

    } catch (Exception ex) {
      log.error("Get fee details resulted in error for fee code {}", feeCode, ex);
      return FeeDetailsResponseWrapper.error();
    }
  }
}
