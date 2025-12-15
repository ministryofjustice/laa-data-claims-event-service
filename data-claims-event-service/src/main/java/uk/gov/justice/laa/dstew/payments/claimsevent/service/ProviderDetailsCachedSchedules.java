package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import uk.gov.justice.laa.provider.model.ProviderFirmOfficeContractAndScheduleDto;

/** Holder for a cached PDA response along with coverage and expiry metadata. */
record ProviderDetailsCachedSchedules(
    ProviderFirmOfficeContractAndScheduleDto value,
    List<ProviderDetailsCoverageWindow> windows,
    Instant expiresAt,
    boolean negative) {

  static ProviderDetailsCachedSchedules positive(
      ProviderFirmOfficeContractAndScheduleDto value,
      List<ProviderDetailsCoverageWindow> windows,
      Duration timeToLive) {
    return new ProviderDetailsCachedSchedules(
        value, windows, Instant.now().plus(timeToLive), false);
  }

  static ProviderDetailsCachedSchedules negative(Duration timeToLive) {
    return new ProviderDetailsCachedSchedules(
        null, List.of(), Instant.now().plus(timeToLive), true);
  }

  boolean isNegative() {
    return negative;
  }

  boolean isValid() {
    return expiresAt == null || Instant.now().isBefore(expiresAt);
  }

  boolean covers(LocalDate effectiveDate) {
    return windows.stream()
        .anyMatch(
            window ->
                !effectiveDate.isBefore(window.start()) && !effectiveDate.isAfter(window.end()));
  }

  ProviderDetailsCachedSchedules refresh(Duration timeToLive) {
    if (negative) {
      return this;
    }
    return new ProviderDetailsCachedSchedules(
        value, windows, Instant.now().plus(timeToLive), false);
  }
}
