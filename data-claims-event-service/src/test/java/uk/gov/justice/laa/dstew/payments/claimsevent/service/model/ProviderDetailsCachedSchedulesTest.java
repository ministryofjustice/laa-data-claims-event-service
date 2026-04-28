package uk.gov.justice.laa.dstew.payments.claimsevent.service.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.provider.model.ProviderFirmOfficeContractAndScheduleDto;

class ProviderDetailsCachedSchedulesTest {

  private static final Duration TTL = Duration.ofMinutes(5);
  private static final List<ProviderDetailsCoverageWindow> WINDOWS =
      List.of(
          new ProviderDetailsCoverageWindow(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)));

  @Test
  void positiveCacheIsValidAndCoversWindow() {
    ProviderFirmOfficeContractAndScheduleDto dto = new ProviderFirmOfficeContractAndScheduleDto();

    ProviderDetailsCachedSchedules cache =
        ProviderDetailsCachedSchedules.positive(dto, WINDOWS, TTL);

    assertThat(cache.isNegative()).isFalse();
    assertThat(cache.isValid()).isTrue();
    assertThat(cache.covers(LocalDate.of(2024, 1, 15))).isTrue();
    assertThat(cache.value()).isSameAs(dto);
    assertThat(cache.windows()).containsExactlyElementsOf(WINDOWS);
  }

  @Test
  void expiredCacheIsNotValid() {
    ProviderDetailsCachedSchedules expired =
        ProviderDetailsCachedSchedules.positive(
            new ProviderFirmOfficeContractAndScheduleDto(), WINDOWS, Duration.ZERO);

    assertThat(expired.isValid()).isFalse();
  }

  @Test
  void negativeCacheRefreshDoesNotChangeInstance() {
    ProviderDetailsCachedSchedules negative =
        ProviderDetailsCachedSchedules.negative(Duration.ofSeconds(1));

    ProviderDetailsCachedSchedules refreshed = negative.refresh(TTL);

    assertThat(refreshed).isSameAs(negative);
    assertThat(refreshed.isNegative()).isTrue();
  }

  @Test
  void positiveRefreshReturnsNewExpiry() {
    ProviderFirmOfficeContractAndScheduleDto dto = new ProviderFirmOfficeContractAndScheduleDto();
    ProviderDetailsCachedSchedules original =
        ProviderDetailsCachedSchedules.positive(dto, WINDOWS, Duration.ofSeconds(1));

    Instant originalExpiry = original.expiresAt();

    ProviderDetailsCachedSchedules refreshed = original.refresh(TTL);

    assertThat(refreshed).isNotSameAs(original);
    assertThat(refreshed.expiresAt()).isAfter(originalExpiry);
    assertThat(refreshed.value()).isSameAs(dto);
    assertThat(refreshed.isNegative()).isFalse();
  }
}
