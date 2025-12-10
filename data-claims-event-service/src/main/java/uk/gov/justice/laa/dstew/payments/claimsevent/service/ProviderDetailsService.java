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
import uk.gov.justice.laa.provider.model.FirmOfficeContractAndScheduleDetails;
import uk.gov.justice.laa.provider.model.ProviderFirmOfficeContractAndScheduleDto;

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
  // Prevent concurrent cache-miss calls for the same office/area.
  private final Map<String, Mono<ProviderFirmOfficeContractAndScheduleDto>> inFlightCalls =
      new ConcurrentHashMap<>();

  // Short-lived negative cache to avoid hammering PDA when no schedules exist for a key.
  private static final Duration NEGATIVE_CACHE_TTL = Duration.ofMinutes(5);
  // Positive cache window for successful schedule responses.
  private static final Duration POSITIVE_CACHE_TTL = Duration.ofMinutes(10);

  /**
   * Retrieves the provider firm office contract and schedule information for a given office, area
   * of law, and effective date.
   *
   * <p>This method delegates the call to the underlying client and applies a retry mechanism
   * defined by the {@code pdaRetry} configuration.
   *
   * @param officeCode the unique code identifying the office (must not be {@code null})
   * @param areaOfLaw the area of law for which schedules are requested (must not be {@code null})
   * @param effectiveDate the date from which the schedule should be effective (must not be {@code
   *     null})
   * @return a {@link Mono} emitting {@link ProviderFirmOfficeContractAndScheduleDto} containing the
   *     contract and schedule details for the specified parameters
   * @throws IllegalArgumentException if any of the parameters are invalid
   */
  public Mono<ProviderFirmOfficeContractAndScheduleDto> getProviderFirmSchedules(
      String officeCode, String areaOfLaw, LocalDate effectiveDate) {
    String cacheKey = cacheKey(officeCode, areaOfLaw);
    String negativeKey = negativeCacheKey(officeCode, areaOfLaw, effectiveDate);
    ProviderDetailsCachedSchedules cached = scheduleCache.get(cacheKey);
    ProviderDetailsCachedSchedules cachedNegative = negativeCache.get(negativeKey);
    if (cached != null) {
      if (!cached.isValid()) {
        log.debug("ProviderDetails cache expired for key {}", cacheKey);
        scheduleCache.remove(cacheKey);
      } else if (cached.isNegative()) {
        log.debug("ProviderDetails negative cache hit for key {}", cacheKey);
        return Mono.empty();
      } else {
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
          scheduleCache.put(cacheKey, cached.refresh(POSITIVE_CACHE_TTL));
          return Mono.just(cached.value());
        }
        log.debug(
            "ProviderDetails cache miss for key {}: date {} not covered", cacheKey, effectiveDate);
      }
    } else if (cachedNegative != null) {
      if (!cachedNegative.isValid()) {
        log.debug("ProviderDetails negative cache expired for key {}", negativeKey);
        negativeCache.remove(negativeKey);
      } else {
        log.debug("ProviderDetails negative cache hit for key {}", negativeKey);
        return Mono.empty();
      }
    } else {
      log.debug("ProviderDetails cache miss for key {}", cacheKey);
    }

    Retry retry = retryRegistry.retry("pdaRetry");
    return inFlightCalls
        .computeIfAbsent(
            cacheKey,
            key ->
                providerDetailsRestClient
                    .getProviderFirmSchedules(officeCode, areaOfLaw, effectiveDate)
                    .doOnSubscribe(
                        subscription ->
                            log.debug(
                                "Calling PDA getProviderFirmSchedules for officeCode {}, areaOfLaw {}, effectiveDate {}",
                                officeCode,
                                areaOfLaw,
                                effectiveDate))
                    .map(
                        dto -> {
                          cacheWindows(cacheKey, dto);
                          return dto;
                        })
                    .switchIfEmpty(cacheNegative(negativeKey))
                    .transformDeferred(RetryOperator.of(retry))
                    .cache())
        .doFinally(signalType -> inFlightCalls.remove(cacheKey));
  }

  private Mono<ProviderFirmOfficeContractAndScheduleDto> cacheNegative(String negativeKey) {
    negativeCache.put(negativeKey, ProviderDetailsCachedSchedules.negative(NEGATIVE_CACHE_TTL));
    return Mono.empty();
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

  private void mergeOrAdd(
      List<ProviderDetailsCoverageWindow> windows, ProviderDetailsCoverageWindow next) {
    if (windows.isEmpty()) {
      windows.add(next);
      return;
    }
    ProviderDetailsCoverageWindow last = windows.getLast();
    if (!next.start().isAfter(last.end().plusDays(1))) {
      windows.set(
          windows.size() - 1,
          new ProviderDetailsCoverageWindow(last.start(), max(last.end(), next.end())));
    } else {
      windows.add(next);
    }
  }

  private void mergeLists(
      List<ProviderDetailsCoverageWindow> target, List<ProviderDetailsCoverageWindow> source) {
    source.forEach(window -> mergeOrAdd(target, window));
  }

  private LocalDate max(LocalDate left, LocalDate right) {
    return left.isAfter(right) ? left : right;
  }

  private String cacheKey(String officeCode, String areaOfLaw) {
    return officeCode + "|" + areaOfLaw;
  }

  private String negativeCacheKey(String officeCode, String areaOfLaw, LocalDate effectiveDate) {
    return officeCode + "|" + areaOfLaw + "|" + effectiveDate;
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
                        mergedDto, mergedWindows, POSITIVE_CACHE_TTL));
              } else {
                scheduleCache.put(
                    cacheKey,
                    ProviderDetailsCachedSchedules.positive(dto, newWindows, POSITIVE_CACHE_TTL));
              }
            });
  }

  private List<ProviderDetailsCoverageWindow> mergeWindows(
      List<ProviderDetailsCoverageWindow> existing, List<ProviderDetailsCoverageWindow> incoming) {
    return Stream.concat(existing.stream(), incoming.stream())
        .sorted(Comparator.comparing(ProviderDetailsCoverageWindow::start))
        .collect(ArrayList::new, this::mergeOrAdd, this::mergeLists);
  }

  private ProviderFirmOfficeContractAndScheduleDto mergeSchedules(
      ProviderFirmOfficeContractAndScheduleDto existing,
      ProviderFirmOfficeContractAndScheduleDto incoming) {
    ProviderFirmOfficeContractAndScheduleDto merged = new ProviderFirmOfficeContractAndScheduleDto();
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
