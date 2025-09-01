package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * A container for the result of a request to the category of law endpoint of the Fee Scheme
 * Platform API.
 */
@Getter
@EqualsAndHashCode
public class CategoryOfLawResult {

  private final String categoryOfLaw;
  private final boolean error;

  private CategoryOfLawResult(String categoryOfLaw, boolean error) {
    this.categoryOfLaw = categoryOfLaw;
    this.error = error;
  }

  public static CategoryOfLawResult withCategoryOfLaw(String categoryOfLaw) {
    return new CategoryOfLawResult(categoryOfLaw, false);
  }

  public static CategoryOfLawResult error() {
    return new CategoryOfLawResult(null, true);
  }
}
