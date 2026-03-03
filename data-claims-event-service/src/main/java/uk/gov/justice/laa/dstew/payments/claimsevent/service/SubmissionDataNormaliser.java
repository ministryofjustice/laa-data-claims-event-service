package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionOutcome;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200Response;
import uk.gov.justice.laa.dstew.payments.claimsevent.exception.SubmissionDataNormalisationException;

/**
 * Normalises the bulk submission data by recursively trimming all String fields in the DTO objects.
 * This ensures whitespaces do not cause parsing or validation failures during subsequent mapping.
 *
 * <p>Fields listed in {@link #UPPERCASE_FIELDS} are additionally converted to upper case after
 * trimming. The map is keyed by the exact DTO class, ensuring that field name matches are scoped
 * precisely — a field named {@code gender} on an unrelated class will not be uppercased
 * unintentionally.
 *
 * <p>This normaliser does not alter numeric, boolean, enum, or other typed values.
 */
@Service
public class SubmissionDataNormaliser {

  /**
   * Defines the fields to be uppercased after trimming, scoped explicitly by DTO class. Field names
   * must match the Java bean property name exactly (camelCase). To add further fields, add an entry
   * for the relevant class or extend an existing one.
   */
  static final Map<Class<?>, Set<String>> UPPERCASE_FIELDS =
      Map.of(
          BulkSubmissionOutcome.class,
          Set.of(
              "gender",
              "client2Gender",
              "disability",
              "client2Disability",
              "clientType",
              "typeOfAdvice"));

  private String normaliseString(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  /**
   * Normalises the entire {@link GetBulkSubmission200Response} DTO object, recursively trimming all
   * String fields and cleaning nested DTO structures such as schedules, offices, outcomes,
   * matter-starts, and immigration CLR entries.
   *
   * @param response the {@code GetBulkSubmission200Response} DTO retrieved from the claims api
   * @return the same DTO instance with String values normalised
   */
  public GetBulkSubmission200Response normalise(GetBulkSubmission200Response response) {
    if (response == null) {
      return null;
    }
    normaliseObject(response, new HashSet<>());
    return response;
  }

  /**
   * Recursively normalises any object, trimming all String fields. Supports: POJOs / DTOs, Lists,
   * Maps
   *
   * @param object the object to normalise
   * @param visited tracks visited objects to avoid infinite recursion
   */
  private void normaliseObject(Object object, Set<Object> visited) {
    if (object == null || visited.contains(object)) {
      return;
    }
    visited.add(object);

    // 1. Handle List
    if (object instanceof List<?> list) {
      list.forEach(element -> normaliseObject(element, visited));
      return;
    }

    // 2. Handle Map<String, String> (only immigrationClr)
    if (object instanceof Map<?, ?> map) {
      normaliseMap((Map<Object, Object>) map);
      return;
    }

    // 3. Only normalise DTOs (ignore Java core classes)
    if (object.getClass().getPackageName().startsWith("java.")) {
      return;
    }

    Arrays.stream(object.getClass().getDeclaredFields())
        .forEach(field -> normaliseFieldValue(object, field, visited));
  }

  private void normaliseFieldValue(Object object, Field field, Set<Object> visited) {
    if (object == null || field == null) {
      return;
    }
    try {
      field.setAccessible(true);
      Object value = field.get(object);
      if (value instanceof String s) {
        String normalised = normaliseString(s);
        Set<String> upperFields = UPPERCASE_FIELDS.getOrDefault(object.getClass(), Set.of());
        if (normalised != null && upperFields.contains(field.getName())) {
          normalised = normalised.toUpperCase(Locale.ENGLISH);
        }
        field.set(object, normalised);
      } else if (isNormalisableObject(value)) {
        normaliseObject(value, visited);
      }
    } catch (IllegalAccessException e) {
      throw new SubmissionDataNormalisationException(
          "Unable to normalise field '%s' on class '%s': %s"
              .formatted(field.getName(), object.getClass().getSimpleName(), e.getMessage()),
          e);
    }
  }

  private void normaliseMap(Map<Object, Object> map) {
    Map<Object, Object> copy = new LinkedHashMap<>();

    for (Map.Entry<Object, Object> entry : map.entrySet()) {
      Object rawKey = entry.getKey();
      Object rawVal = entry.getValue();

      String key = rawKey instanceof String s ? normaliseString(s) : null;
      if (key == null) {
        continue; // skip blank/null keys
      }

      String val = rawVal instanceof String s ? normaliseString(s) : null;

      copy.put(key, val);
    }

    map.clear();
    map.putAll(copy);
  }

  // Determines whether an object should be recursively traversed
  private boolean isNormalisableObject(Object value) {
    if (value == null) {
      return false;
    }

    return value instanceof List
        || value instanceof Map
        || value.getClass().isArray()
        || !value.getClass().getPackageName().startsWith("java.");
  }
}
