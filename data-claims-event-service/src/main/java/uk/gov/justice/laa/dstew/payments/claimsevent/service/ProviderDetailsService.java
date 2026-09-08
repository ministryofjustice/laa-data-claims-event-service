package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ProviderDetailsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.model.ProviderDetailsCachedSchedules;
import uk.gov.justice.laa.dstew.payments.claimsevent.service.model.ProviderDetailsCoverageWindow;
import uk.gov.justice.laadata.providers.model.FirmOfficeContractAndScheduleDetails;
import uk.gov.justice.laadata.providers.model.ProviderFirmOfficeContractAndScheduleDto;

/**
 * Service layer for ProviderDetailsRestClient, in order to apply the retry backoff.
 *
 * @author Jose Carlos Arinero Adam
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ProviderDetailsService {

  private final ProviderDetailsRestClient providerDetailsRestClient;
  private final RetryRegistry retryRegistry;
  private final Map<String, ProviderDetailsCachedSchedules> scheduleCache =
      new ConcurrentHashMap<>();
  private final Map<String, ProviderDetailsCachedSchedules> negativeCache =
      new ConcurrentHashMap<>();
  // Prevent concurrent cache-miss calls for the same office.
  private final Map<String, Mono<ProviderFirmOfficeContractAndScheduleDto>> inFlightCalls =
      new ConcurrentHashMap<>();

  // Short-lived negative cache to avoid hammering PDA when no schedules exist for a key.
  private static final Duration NEGATIVE_CACHE_TIME_TO_LIVE = Duration.ofMinutes(5);
  // Positive cache window for successful schedule responses.
  private static final Duration POSITIVE_CACHE_TIME_TO_LIVE = Duration.ofMinutes(10);

  /**
   * Clears all cached provider schedule data (positive and negative caches, and in-flight call
   * tracking).
   *
   * <p>Intended for test isolation: because this service is a Spring singleton whose caches
   * merge/accumulate schedule data per office code across calls, reusing the same office code in
   * multiple tests within the same Spring context can leak schedule data cached by an earlier test
   * into a later one. Call this between tests that reuse the same office code with different
   * expected provider schedules.
   */
  public void clearCaches() {
    scheduleCache.clear();
    negativeCache.clear();
    inFlightCalls.clear();
  }

  /**
   * Retrieves the provider firm office contract and schedule information for a given office and
   * effective date.
   *
   * <p>This method delegates the call to the underlying client and applies a retry mechanism
   * defined by the {@code pdaRetry} configuration.
   *
   * @param officeCode the unique code identifying the office (must not be {@code null})
   * @param effectiveDate the date from which the schedule should be effective (must not be {@code
   *     null})
   * @return a {@link Mono} emitting {@link ProviderFirmOfficeContractAndScheduleDto} containing the
   *     contract and schedule details for the specified parameters
   * @throws IllegalArgumentException if any of the parameters are invalid
   */
  public Mono<ProviderFirmOfficeContractAndScheduleDto> getProviderFirmSchedules(
      String officeCode, LocalDate effectiveDate) {
    String cacheKey = cacheKey(officeCode);
    String negativeKey = negativeCacheKey(officeCode, effectiveDate);
    Optional<Mono<ProviderFirmOfficeContractAndScheduleDto>> negativeCacheHit =
        handleNegativeCache(negativeKey);
    if (negativeCacheHit.isPresent()) {
      return negativeCacheHit.get();
    }

    Optional<Mono<ProviderFirmOfficeContractAndScheduleDto>> positiveCacheHit =
        handlePositiveCache(cacheKey, effectiveDate);
    if (positiveCacheHit.isPresent()) {
      return positiveCacheHit.get();
    }

    return fetchAndCache(officeCode, effectiveDate, cacheKey, negativeKey);
  }

  private Mono<ProviderFirmOfficeContractAndScheduleDto> cacheNegative(String negativeKey) {
    negativeCache.put(
        negativeKey, ProviderDetailsCachedSchedules.negative(NEGATIVE_CACHE_TIME_TO_LIVE));
    return Mono.empty();
  }

  /** Returns a cached negative result if it is still valid, otherwise clears it. */
  private Optional<Mono<ProviderFirmOfficeContractAndScheduleDto>> handleNegativeCache(
      String negativeKey) {
    ProviderDetailsCachedSchedules cachedNegative = negativeCache.get(negativeKey);
    if (cachedNegative == null) {
      return Optional.empty();
    }
    if (!cachedNegative.isValid()) {
      log.debug("ProviderDetails negative cache expired for key {}", negativeKey);
      negativeCache.remove(negativeKey);
      return Optional.empty();
    }
    log.debug("ProviderDetails negative cache hit for key {}", negativeKey);
    return Optional.of(Mono.empty());
  }

  /**
   * Returns a cached positive result when valid and covering the requested date; refreshes TTL on
   * hit.
   */
  private Optional<Mono<ProviderFirmOfficeContractAndScheduleDto>> handlePositiveCache(
      String cacheKey, LocalDate effectiveDate) {
    ProviderDetailsCachedSchedules cached = scheduleCache.get(cacheKey);
    if (cached == null) {
      log.debug("ProviderDetails cache miss for key {}", cacheKey);
      return Optional.empty();
    }
    if (!cached.isValid()) {
      log.debug("ProviderDetails cache expired for key {}", cacheKey);
      scheduleCache.remove(cacheKey);
      return Optional.empty();
    }
    if (cached.isNegative()) {
      log.debug("ProviderDetails negative cache hit for key {}", cacheKey);
      return Optional.of(Mono.empty());
    }
    log.debug(
        "ProviderDetails coverage windows for key {} when checking effective date {}: {}",
        cacheKey,
        effectiveDate,
        cached.windows());
    if (cached.covers(effectiveDate)) {
      log.debug(
          "ProviderDetails cache hit for key {} covering effective date {}",
          cacheKey,
          effectiveDate);
      scheduleCache.put(cacheKey, cached.refresh(POSITIVE_CACHE_TIME_TO_LIVE));
      // The cached value may be merged from multiple prior calls for different effective dates
      // (see cacheWindows). Filter to only the schedule lines actually effective on the date
      // being requested now, so a cache hit returns exactly what a fresh, date-correct lookup
      // would return - this is what makes the cache safe to reuse across claims with different
      // effective dates in the same submission.
      return Optional.of(Mono.just(filterToEffectiveDate(cached.value(), effectiveDate)));
    }
    log.debug(
        "ProviderDetails cache miss for key {}: date {} not covered", cacheKey, effectiveDate);
    return Optional.empty();
  }

  /** Invokes PDA, then caches positive or negative results, sharing in-flight calls. */
  private Mono<ProviderFirmOfficeContractAndScheduleDto> fetchAndCache(
      String officeCode, LocalDate effectiveDate, String cacheKey, String negativeKey) {
    Retry retry = retryRegistry.retry("pdaRetry");
    return inFlightCalls
        .computeIfAbsent(
            cacheKey,
            key ->
                providerDetailsRestClient
                    .getProviderFirmSchedules(officeCode, effectiveDate)
                    .doOnSubscribe(
                        subscription ->
                            log.debug(
                                "Calling PDA getProviderFirmSchedules for officeCode {}, effectiveDate {}",
                                officeCode,
                                effectiveDate))
                    .map(
                        dto -> {
                          cacheWindows(cacheKey, dto);
                          // Cache the raw response for future coverage-window merging, but return
                          // only the schedule lines effective on the requested date - keeps the
                          // fresh-fetch path and the cache-hit path date-correct in the same way.
                          return filterToEffectiveDate(dto, effectiveDate);
                        })
                    .switchIfEmpty(Mono.defer(() -> cacheNegative(negativeKey)))
                    .transformDeferred(RetryOperator.of(retry))
                    .cache())
        .doFinally(signalType -> inFlightCalls.remove(cacheKey));
  }

  /**
   * Builds the coverage windows from schedule and contract dates. Uses multiple windows to avoid
   * false positives when there are gaps between contracts. Null starts/ends are treated as open
   * ranges.
   */
  private Optional<List<ProviderDetailsCoverageWindow>> computeCoverage(
      ProviderFirmOfficeContractAndScheduleDto dto) {
    if (dto.getSchedules() == null || dto.getSchedules().isEmpty()) {
      return Optional.empty();
    }

    List<ProviderDetailsCoverageWindow> windows =
        dto.getSchedules().stream()
            .map(this::toWindow)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .sorted(Comparator.comparing(ProviderDetailsCoverageWindow::start))
            .collect(ArrayList::new, this::mergeOrAdd, this::mergeLists);

    if (windows.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(windows);
  }

  /** Builds a coverage window for a schedule if end is not before start. */
  private Optional<ProviderDetailsCoverageWindow> toWindow(
      FirmOfficeContractAndScheduleDetails schedule) {
    LocalDate start =
        Stream.of(schedule.getScheduleStartDate())
            .filter(Objects::nonNull)
            .min(Comparator.naturalOrder())
            .orElse(LocalDate.MIN);
    LocalDate end =
        Stream.of(schedule.getScheduleEndDate())
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(LocalDate.MAX);
    if (end.isBefore(start)) {
      return Optional.empty();
    }
    return Optional.of(new ProviderDetailsCoverageWindow(start, end));
  }

  /** Merges an adjacent/overlapping window into the tail of the list or adds a new window. */
  private void mergeOrAdd(
      List<ProviderDetailsCoverageWindow> windows, ProviderDetailsCoverageWindow next) {
    if (windows.isEmpty()) {
      windows.add(next);
      return;
    }
    ProviderDetailsCoverageWindow last = windows.getLast();
    // Avoid overflowing when last.end() is LocalDate.MAX. Treat a MAX end as unbounded so any
    // subsequent window should be merged.
    boolean adjacentOrOverlapping;
    if (last.end().equals(LocalDate.MAX)) {
      adjacentOrOverlapping = true;
    } else {
      adjacentOrOverlapping = !next.start().isAfter(last.end().plusDays(1));
    }

    if (adjacentOrOverlapping) {
      windows.set(
          windows.size() - 1,
          new ProviderDetailsCoverageWindow(last.start(), max(last.end(), next.end())));
    } else {
      windows.add(next);
    }
  }

  /** Merges all windows from source into target in order. */
  private void mergeLists(
      List<ProviderDetailsCoverageWindow> target, List<ProviderDetailsCoverageWindow> source) {
    source.forEach(window -> mergeOrAdd(target, window));
  }

  /** Returns the max of two dates. */
  private LocalDate max(LocalDate left, LocalDate right) {
    return left.isAfter(right) ? left : right;
  }

  /**
   * Returns a copy of the given DTO containing only the schedule entries that are effective on
   * {@code effectiveDate}, using the same schedule-level date semantics as {@link
   * #toWindow(FirmOfficeContractAndScheduleDetails)} (null start = unbounded past, null end =
   * unbounded future).
   *
   * <p>This is what guarantees a cache hit (which may be merged from responses fetched for several
   * different effective dates) returns exactly what a fresh, date-correct lookup for {@code
   * effectiveDate} would return - regardless of what other dates have previously been queried for
   * this office.
   *
   * @param dto the source DTO; if {@code null} an empty result is returned
   * @param effectiveDate the date to filter schedules against; if {@code null} no schedules match
   * @return a new DTO with the same firm/office/pds metadata and only the effective schedules
   */
  private ProviderFirmOfficeContractAndScheduleDto filterToEffectiveDate(
      ProviderFirmOfficeContractAndScheduleDto dto, LocalDate effectiveDate) {
    ProviderFirmOfficeContractAndScheduleDto filtered =
        new ProviderFirmOfficeContractAndScheduleDto();
    if (dto == null) {
      filtered.setSchedules(List.of());
      return filtered;
    }
    filtered.setFirm(dto.getFirm());
    filtered.setOffice(dto.getOffice());
    filtered.setPds(dto.getPds());
    List<FirmOfficeContractAndScheduleDetails> schedules = dto.getSchedules();
    List<FirmOfficeContractAndScheduleDetails> effectiveSchedules =
        schedules == null
            ? List.of()
            : schedules.stream()
                .filter(schedule -> isScheduleEffectiveOn(schedule, effectiveDate))
                .toList();
    filtered.setSchedules(effectiveSchedules);
    return filtered;
  }

  /**
   * Determines whether a schedule's own start/end dates cover the given effective date.
   *
   * @param schedule the schedule to test; {@code null} is never effective
   * @param effectiveDate the date to test; {@code null} never matches
   * @return true if {@code effectiveDate} falls within [scheduleStartDate, scheduleEndDate]
   *     inclusive, treating a null bound as unbounded on that side
   */
  private boolean isScheduleEffectiveOn(
      FirmOfficeContractAndScheduleDetails schedule, LocalDate effectiveDate) {
    if (schedule == null || effectiveDate == null) {
      return false;
    }
    LocalDate start = schedule.getScheduleStartDate();
    LocalDate end = schedule.getScheduleEndDate();
    if (start != null && effectiveDate.isBefore(start)) {
      return false;
    }
    return end == null || !effectiveDate.isAfter(end);
  }

  /** Creates a cache key for positive cache lookups. */
  private String cacheKey(String officeCode) {
    return officeCode;
  }

  /** Creates a cache key for negative cache scoped to the effective date. */
  private String negativeCacheKey(String officeCode, LocalDate effectiveDate) {
    return officeCode + "|" + effectiveDate;
  }

  /** Cache the response along with its merged coverage windows. */
  private void cacheWindows(String cacheKey, ProviderFirmOfficeContractAndScheduleDto dto) {
    computeCoverage(dto)
        .ifPresent(
            newWindows -> {
              ProviderDetailsCachedSchedules existing = scheduleCache.get(cacheKey);
              if (existing != null && !existing.isNegative()) {
                List<ProviderDetailsCoverageWindow> mergedWindows =
                    mergeWindows(existing.windows(), newWindows);
                ProviderFirmOfficeContractAndScheduleDto mergedDto =
                    mergeSchedules(existing.value(), dto);
                scheduleCache.put(
                    cacheKey,
                    ProviderDetailsCachedSchedules.positive(
                        mergedDto, mergedWindows, POSITIVE_CACHE_TIME_TO_LIVE));
              } else {
                scheduleCache.put(
                    cacheKey,
                    ProviderDetailsCachedSchedules.positive(
                        dto, newWindows, POSITIVE_CACHE_TIME_TO_LIVE));
              }
            });
  }

  /** Merges cached and incoming coverage windows into a single ordered list. */
  private List<ProviderDetailsCoverageWindow> mergeWindows(
      List<ProviderDetailsCoverageWindow> existing, List<ProviderDetailsCoverageWindow> incoming) {
    return Stream.concat(existing.stream(), incoming.stream())
        .sorted(Comparator.comparing(ProviderDetailsCoverageWindow::start))
        .collect(ArrayList::new, this::mergeOrAdd, this::mergeLists);
  }

  /** Combines cached and incoming DTO data, appending schedule lists. */
  private ProviderFirmOfficeContractAndScheduleDto mergeSchedules(
      ProviderFirmOfficeContractAndScheduleDto existing,
      ProviderFirmOfficeContractAndScheduleDto incoming) {
    ProviderFirmOfficeContractAndScheduleDto merged =
        new ProviderFirmOfficeContractAndScheduleDto();
    merged.setFirm(
        incoming.getFirm() != null
            ? incoming.getFirm()
            : existing != null ? existing.getFirm() : null);
    merged.setOffice(
        incoming.getOffice() != null
            ? incoming.getOffice()
            : existing != null ? existing.getOffice() : null);
    merged.setPds(
        incoming.getPds() != null
            ? incoming.getPds()
            : existing != null ? existing.getPds() : null);

    List<FirmOfficeContractAndScheduleDetails> schedules = new ArrayList<>();
    if (existing != null && existing.getSchedules() != null) {
      schedules.addAll(existing.getSchedules());
    }
    if (incoming.getSchedules() != null) {
      schedules.addAll(incoming.getSchedules());
    }
    merged.setSchedules(schedules);
    return merged;
  }
}
