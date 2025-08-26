package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimFields;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.fee.scheme.model.CategoryOfLawResponse;

/** A service responsible for validating data items related to category of law. */
@Service
@AllArgsConstructor
public class CategoryOfLawValidationService {

  private final SubmissionValidationContext submissionValidationContext;

  private final FeeSchemePlatformRestClient feeSchemePlatformRestClient;

  /**
   * Validates that a valid category of law exists for the fee code provided in the claim.
   *
   * @param claim the submitted claim
   * @param categoryOfLawLookup the lookup of feeCode -> categoryOfLaw for all claims in the
   *     submission
   */
  public void validateCategoryOfLaw(
      ClaimFields claim,
      Map<String, String> categoryOfLawLookup,
      List<String> providerCategoriesOfLaw) {

    String categoryOfLaw = categoryOfLawLookup.get(claim.getFeeCode());

    if (categoryOfLaw == null) {
      submissionValidationContext.addClaimError(
          claim.getId(), ClaimValidationError.INVALID_CATEGORY_OF_LAW_AND_FEE_CODE);
    } else if (!providerCategoriesOfLaw.contains(categoryOfLaw)) {
      submissionValidationContext.addClaimError(
          claim.getId(), ClaimValidationError.INVALID_CATEGORY_OF_LAW_NOT_AUTHORISED_FOR_PROVIDER);
    }
  }

  /**
   * Build a lookup of feeCode -> categoryOfLaw where feeCode are the unique fee codes found in the
   * list of claims, and categoryOfLaw are the corresponding category of law that has been retrieved
   * from the Fee Scheme Platform API.
   *
   * <p>Category of law may be null if none were found corresponding to the fee code.
   *
   * @param claims the list of claims (from the submission)
   * @return the feeCode -> categoryOfLaw lookup
   */
  public Map<String, String> getCategoryOfLawLookup(List<ClaimFields> claims) {
    Set<String> uniqueFeeCodes =
        claims.stream().map(ClaimFields::getFeeCode).collect(Collectors.toSet());

    return Flux.fromIterable(uniqueFeeCodes)
        .flatMap(
            feeCode ->
                feeSchemePlatformRestClient
                    .getCategoryOfLaw(feeCode)
                    .map(CategoryOfLawResponse::getCategoryOfLawCode)
                    .map(categoryOfLawResponse -> Map.entry(feeCode, categoryOfLawResponse))
                    .onErrorResume(
                        WebClientResponseException.NotFound.class,
                        ex -> Mono.just(new AbstractMap.SimpleEntry<>(feeCode, null))))
        .collectMap(Map.Entry::getKey, Map.Entry::getValue)
        .block();
  }
}
