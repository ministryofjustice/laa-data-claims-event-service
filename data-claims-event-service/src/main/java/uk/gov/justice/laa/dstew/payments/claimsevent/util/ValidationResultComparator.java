package uk.gov.justice.laa.dstew.payments.claimsevent.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.dstew.payments.claims.validation.core.model.ValidationIssue;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ValidationMessagePatch;

/**
 * Compares new validation issues produced by the new validator against existing validation messages
 * produced by the old validator, logging any differences as WARN. Intended as a migration aid — it
 * never modifies either result.
 *
 * <p>A pair is considered matched when display message, technical message and severity are all
 * equal. Anything left unmatched is logged as only-in-new or only-in-existing.
 */
@Slf4j
public final class ValidationResultComparator {

  private ValidationResultComparator() {}

  /**
   * Compares new validation issues against existing validation messages for a single entity (claim
   * or submission).
   *
   * @param label a human-readable identifier used in log messages (e.g. "Claim abc-123")
   * @param newIssues issues produced by the new validator; may be null or empty
   * @param existingMessages messages produced by the old validator; may be null or empty
   */
  public static void compare(
      String label,
      List<ValidationIssue> newIssues,
      List<ValidationMessagePatch> existingMessages) {

    List<ValidationIssue> unmatched = toMutableList(newIssues);
    List<ValidationMessagePatch> unmatchedExisting = toMutableList(existingMessages);

    if (unmatched.isEmpty() && unmatchedExisting.isEmpty()) {
      log.debug("{} validators matched: both produced no issues", label);
      return;
    }

    removeExactMatches(unmatched, unmatchedExisting);

    if (unmatched.isEmpty() && unmatchedExisting.isEmpty()) {
      log.debug("{} validators matched exactly", label);
      return;
    }

    unmatched.forEach(
        ni -> log.warn("[VALIDATOR-DRY_RUN] {} only in new validator: {}", label, describeNew(ni)));
    unmatchedExisting.forEach(
        em ->
            log.warn(
                "[VALIDATOR-DRY-RUN] {} only in existing validator: {}",
                label,
                describeExisting(em)));
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  private static void removeExactMatches(
      List<ValidationIssue> newIssues, List<ValidationMessagePatch> existing) {
    Iterator<ValidationIssue> it = newIssues.iterator();
    while (it.hasNext()) {
      ValidationIssue ni = it.next();
      ValidationMessagePatch match = findExact(ni, existing);
      if (match != null) {
        it.remove();
        existing.remove(match);
      }
    }
  }

  private static ValidationMessagePatch findExact(
      ValidationIssue ni, List<ValidationMessagePatch> existing) {
    return existing.stream()
        .filter(
            em ->
                Objects.equals(ni.getMessage(), em.getDisplayMessage())
                    && Objects.equals(ni.getTechnicalMessage(), em.getTechnicalMessage())
                    && Objects.equals(severityName(ni), typeName(em)))
        .findFirst()
        .orElse(null);
  }

  private static String severityName(ValidationIssue ni) {
    return ni.getSeverity() == null ? null : ni.getSeverity().name();
  }

  private static String typeName(ValidationMessagePatch em) {
    return em.getType() == null ? null : em.getType().name();
  }

  private static String describeNew(ValidationIssue ni) {
    return String.format(
        "code=[%s] severity=[%s] message=[%s] technical=[%s]",
        ni.getCode(), ni.getSeverity(), ni.getMessage(), ni.getTechnicalMessage());
  }

  private static String describeExisting(ValidationMessagePatch em) {
    return String.format(
        "source=[%s] severity=[%s] message=[%s] technical=[%s]",
        em.getSource(), em.getType(), em.getDisplayMessage(), em.getTechnicalMessage());
  }

  private static <T> List<T> toMutableList(List<T> source) {
    return source == null ? new ArrayList<>() : new ArrayList<>(source);
  }
}
