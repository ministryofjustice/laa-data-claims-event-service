package uk.gov.justice.laa.dstew.payments.claimsevent.maintenance;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ListenerActivityTracker")
class ListenerActivityTrackerTest {

  @Test
  @DisplayName("Increments and decrements the in-flight count")
  void incrementsAndDecrements() {
    ListenerActivityTracker tracker = new ListenerActivityTracker();

    assertThat(tracker.getInFlightCount()).isZero();

    tracker.increment();
    tracker.increment();
    assertThat(tracker.getInFlightCount()).isEqualTo(2);

    tracker.decrement();
    assertThat(tracker.getInFlightCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("Never goes negative")
  void neverGoesNegative() {
    ListenerActivityTracker tracker = new ListenerActivityTracker();

    tracker.decrement();

    assertThat(tracker.getInFlightCount()).isZero();
  }
}
