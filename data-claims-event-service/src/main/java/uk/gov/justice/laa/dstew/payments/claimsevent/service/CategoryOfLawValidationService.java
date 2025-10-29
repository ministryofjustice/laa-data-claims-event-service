package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.fee.scheme.model.FeeDetailsResponse;

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
   * @param feeDetailsResponseMap a map containing FeeDetailsResponse and their corresponding
   *     feeCodes
   */
  public void validateCategoryOfLaw(
      ClaimResponse claim,
      Map<String, FeeDetailsResponseWrapper> feeDetailsResponseMap,
      List<String> providerCategoriesOfLaw,
      SubmissionValidationContext context) {

    log.debug("Validating category of law for claim {}", claim.getId());

    FeeDetailsResponseWrapper feeDetailsResponseWrapper =
        feeDetailsResponseMap.get(claim.getFeeCode());

    if (feeDetailsResponseWrapper.isError()) {
      context.flagForRetry(claim.getId());
    } else {
      String categoryOfLaw =
          feeDetailsResponseWrapper.getFeeDetailsResponse().getCategoryOfLawCode();

      if (categoryOfLaw == null) {
        context.addClaimError(
            claim.getId(),
            ClaimValidationError.INVALID_CATEGORY_OF_LAW_AND_FEE_CODE,
            claim.getFeeCode());
      } else if (!providerCategoriesOfLaw.contains(categoryOfLaw)) {
        context.addClaimError(
            claim.getId(),
            ClaimValidationError.INVALID_CATEGORY_OF_LAW_NOT_AUTHORISED_FOR_PROVIDER);
      }
    }
    log.debug("Category of law validation completed for claim {}", claim.getId());
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
    try {
      FeeDetailsResponse response = feeSchemePlatformRestClient.getFeeDetails(feeCode).getBody();

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
