package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200Response;

/**
 * Normalises the bulk submission data by recursively trimming all String fields in the DTO objects.
 * This ensures whitespaces do not cause parsing or validation failures during subsequent mapping.
 *
 * <p>This method does not alter numbers, booleans, enums, dates or other typed values.
 */
@Service
public class SubmissionDataNormaliser {

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
   * Maps<String, String>
   *
   * @param object the object to normalise
   * @param visited tracks visited objects to avoid infinite recursion
   */
  private void normaliseObject(Object object, Set<Object> visited) {
    if (object == null) return;

    // Avoid cycles for infinite recursion
    if (visited.contains(object)) return;
    visited.add(object);

    Class<?> clazz = object.getClass();

    // 1. Handle List
    if (object instanceof List<?> list) {
      for (Object element : list) {
        normaliseObject(element, visited);
      }
      return;
    }

    // 2. Handle Map<String, String> (only immigrationClr)
    if (object instanceof Map<?, ?> map) {
      normaliseMap((Map<Object, Object>) map);
      return;
    }

    // 3. Only normalise DTOs (ignore Java core classes)
    if (clazz.getPackageName().startsWith("java.")) {
      return;
    }

    // 4. Process fields
    for (Field field : clazz.getDeclaredFields()) {
      field.setAccessible(true);
      try {
        Object value = field.get(object);

        if (value instanceof String s) {
          field.set(object, normaliseString(s));
        } else if (isNormalisableObject(value)) {
          normaliseObject(value, visited);
        }
        // otherwise ignore numeric, boolean, enum etc

      } catch (IllegalAccessException e) {
        throw new RuntimeException("Unable to normalise field: " + field.getName(), e);
      }
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
    if (value == null) return false;

    return value instanceof List
        || value instanceof Map
        || value.getClass().isArray()
        || !value.getClass().getPackageName().startsWith("java.");
  }
}
