package uk.gov.justice.laa.bulk.claim.util;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;

/** Provides validation for DTO objects based on defined annotation constraints. */
public class ValidationUtil {
  private static final Validator validator =
      Validation.buildDefaultValidatorFactory().getValidator();

  /**
   * Generic validation for objects against defined constraints on object properties.
   *
   * @param obj subject of validation
   * @param <T> generic constraint
   */
  public static <T> void validate(T obj) {
    Set<ConstraintViolation<T>> violations = validator.validate(obj);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }
}
