package uk.gov.justice.laa.dstew.payments.claimsevent.service.model;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import uk.gov.justice.laa.provider.model.ProviderFirmOfficeContractAndScheduleDto;

/** Holder for a cached PDA response along with coverage and expiry metadata. */
public record ProviderDetailsCachedSchedules(
    ProviderFirmOfficeContractAndScheduleDto value,
    List<ProviderDetailsCoverageWindow> windows,
    Instant expiresAt,
    boolean negative) {

  /** Creates a positive cache entry with an expiry derived from the supplied TTL. */
  public static ProviderDetailsCachedSchedules positive(
      ProviderFirmOfficeContractAndScheduleDto value,
      List<ProviderDetailsCoverageWindow> windows,
      Duration timeToLive) {
    return new ProviderDetailsCachedSchedules(
        value, windows, Instant.now().plus(timeToLive), false);
  }

  /** Creates a negative cache entry (no schedules) with the given TTL. */
  public static ProviderDetailsCachedSchedules negative(Duration timeToLive) {
    return new ProviderDetailsCachedSchedules(
        null, List.of(), Instant.now().plus(timeToLive), true);
  }

  public boolean isNegative() {
    return negative;
  }

  public boolean isValid() {
    return expiresAt == null || Instant.now().isBefore(expiresAt);
  }

  /** Returns true if the supplied date falls within any cached coverage window. */
  public boolean covers(LocalDate effectiveDate) {
    return windows.stream()
        .anyMatch(
            window ->
                !effectiveDate.isBefore(window.start()) && !effectiveDate.isAfter(window.end()));
  }

  /** Refreshes the expiry for positive entries; negative entries remain unchanged. */
  public ProviderDetailsCachedSchedules refresh(Duration timeToLive) {
    return negative
        ? this
        : new ProviderDetailsCachedSchedules(value, windows, Instant.now().plus(timeToLive), false);
  }
}
