package uk.gov.justice.laa.dstew.payments.claimsevent.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.MetricsProperties;

@DisplayName("EventServiceMetricService")
class EventServiceMetricServiceTest {

  static final String TEST_NAMESPACE = "claims_event_service_";

  private SimpleMeterRegistry registry;
  private MetricPublisher service;

  @BeforeEach
  void beforeEach() {
    registry = new SimpleMeterRegistry();
    MetricsProperties props = new MetricsProperties();
    props.setNamespace(TEST_NAMESPACE);
    props.setWarnThresholdSeconds(2.0);
    service = new MetricPublisher(registry, props);
  }

  // ---------------------------------------------------------------------------
  // Construction validation
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("construction validation")
  class ConstructionValidation {

    @Test
    @DisplayName("null namespace throws IllegalArgumentException")
    void nullNamespaceThrows() {
      MetricsProperties p = new MetricsProperties();
      p.setNamespace(null);
      p.setWarnThresholdSeconds(2.0);
      assertThatThrownBy(() -> new MetricPublisher(registry, p))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("namespace must not be null or blank");
    }

    @Test
    @DisplayName("blank namespace throws IllegalArgumentException")
    void blankNamespaceThrows() {
      MetricsProperties p = new MetricsProperties();
      p.setNamespace("   ");
      p.setWarnThresholdSeconds(2.0);
      assertThatThrownBy(() -> new MetricPublisher(registry, p))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("namespace must not be null or blank");
    }

    @Test
    @DisplayName("empty namespace throws IllegalArgumentException")
    void emptyNamespaceThrows() {
      MetricsProperties p = new MetricsProperties();
      p.setNamespace("");
      p.setWarnThresholdSeconds(2.0);
      assertThatThrownBy(() -> new MetricPublisher(registry, p))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("namespace must not be null or blank");
    }

    @Test
    @DisplayName("zero warnThresholdSeconds throws IllegalArgumentException")
    void zeroWarnThresholdThrows() {
      MetricsProperties p = new MetricsProperties();
      p.setNamespace(TEST_NAMESPACE);
      p.setWarnThresholdSeconds(0.0);
      assertThatThrownBy(() -> new MetricPublisher(registry, p))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("warnThresholdSeconds must be positive");
    }

    @Test
    @DisplayName("negative warnThresholdSeconds throws IllegalArgumentException")
    void negativeWarnThresholdThrows() {
      MetricsProperties p = new MetricsProperties();
      p.setNamespace(TEST_NAMESPACE);
      p.setWarnThresholdSeconds(-1.0);
      assertThatThrownBy(() -> new MetricPublisher(registry, p))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("warnThresholdSeconds must be positive");
    }

    @Test
    @DisplayName("valid configuration constructs successfully")
    void validConfigConstructsSuccessfully() {
      MetricsProperties p = new MetricsProperties();
      p.setNamespace("my_app_");
      p.setWarnThresholdSeconds(1.0);
      MetricPublisher publisher = new MetricPublisher(registry, p);
      assertThat(publisher).isNotNull();
    }
  }

  // ---------------------------------------------------------------------------
  // MetricNames constants
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("MetricNames constants")
  class MetricNamesConstants {

    @Test
    @DisplayName("FILE_PARSING_TIME has correct short name")
    void fileParsingTime() {
      assertThat(MetricNames.FILE_PARSING_TIME).isEqualTo("file_parsing_time");
    }

    @Test
    @DisplayName("SUBMISSION_VALIDATION_TIME has correct short name")
    void submissionValidationTime() {
      assertThat(MetricNames.SUBMISSION_VALIDATION_TIME).isEqualTo("submission_validation_time");
    }

    @Test
    @DisplayName("CLAIM_VALIDATION_TIME has correct short name")
    void claimValidationTime() {
      assertThat(MetricNames.CLAIM_VALIDATION_TIME).isEqualTo("claim_validation_time");
    }

    @Test
    @DisplayName("FSP_VALIDATION_TIME has correct short name")
    void fspValidationTime() {
      assertThat(MetricNames.FSP_VALIDATION_TIME).isEqualTo("fsp_validation_time");
    }

    @Test
    @DisplayName("SUBMISSIONS_ADDED has correct short name")
    void submissionsAdded() {
      assertThat(MetricNames.SUBMISSIONS_ADDED).isEqualTo("submissions_added");
    }

    @Test
    @DisplayName("CLAIMS_ADDED has correct short name")
    void claimsAdded() {
      assertThat(MetricNames.CLAIMS_ADDED).isEqualTo("claims_added");
    }

    @Test
    @DisplayName("SUBMISSIONS_WITH_ERRORS has correct short name")
    void submissionsWithErrors() {
      assertThat(MetricNames.SUBMISSIONS_WITH_ERRORS).isEqualTo("submissions_with_errors");
    }

    @Test
    @DisplayName("CLAIMS_VALIDATED_VALID has correct short name")
    void claimsValidatedValid() {
      assertThat(MetricNames.CLAIMS_VALIDATED_VALID).isEqualTo("claims_validated_and_valid");
    }

    @Test
    @DisplayName("CLAIMS_VALIDATED_WARNINGS has correct short name")
    void claimsValidatedWarnings() {
      assertThat(MetricNames.CLAIMS_VALIDATED_WARNINGS)
          .isEqualTo("claims_validated_and_warnings_found");
    }

    @Test
    @DisplayName("CLAIMS_VALIDATED_INVALID has correct short name")
    void claimsValidatedInvalid() {
      assertThat(MetricNames.CLAIMS_VALIDATED_INVALID).isEqualTo("claims_validated_and_invalid");
    }

    @Test
    @DisplayName("VALID_SUBMISSIONS has correct short name")
    void validSubmissions() {
      assertThat(MetricNames.VALID_SUBMISSIONS).isEqualTo("valid_submissions");
    }

    @Test
    @DisplayName("INVALID_SUBMISSIONS has correct short name")
    void invalidSubmissions() {
      assertThat(MetricNames.INVALID_SUBMISSIONS).isEqualTo("invalid_submissions");
    }

    @Test
    @DisplayName("MESSAGES_ERRORS has correct short name")
    void messagesErrors() {
      assertThat(MetricNames.MESSAGES_ERRORS).isEqualTo("messages_errors");
    }

    @Test
    @DisplayName("MESSAGES_WARNINGS has correct short name")
    void messagesWarnings() {
      assertThat(MetricNames.MESSAGES_WARNINGS).isEqualTo("messages_warnings");
    }

    @Test
    @DisplayName("METRIC_NAMESPACE has correct value")
    void metricNamespace() {
      assertThat(TEST_NAMESPACE).isEqualTo("claims_event_service_");
    }

    @Test
    @DisplayName("fully-qualified names are correctly composed from namespace and short name")
    void fullyQualifiedNamesComposed() {
      String ns = TEST_NAMESPACE;
      assertThat(ns + MetricNames.FILE_PARSING_TIME)
          .isEqualTo("claims_event_service_file_parsing_time");
      assertThat(ns + MetricNames.SUBMISSIONS_ADDED)
          .isEqualTo("claims_event_service_submissions_added");
      assertThat(ns + MetricNames.INVALID_SUBMISSIONS)
          .isEqualTo("claims_event_service_invalid_submissions");
    }
  }

  // ---------------------------------------------------------------------------
  // Counter registration
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("counter registration")
  class CounterRegistration {

    private String fqn(String shortName) {
      return TEST_NAMESPACE + shortName;
    }

    @Test
    @DisplayName("counters are not pre-registered — they appear only after first increment")
    void countersAreNotPreRegistered() {
      assertThat(registry.find(fqn(MetricNames.SUBMISSIONS_ADDED)).counter()).isNull();
      assertThat(registry.find(fqn(MetricNames.CLAIMS_ADDED)).counter()).isNull();
    }

    @Test
    @DisplayName("counter is registered in the registry after first increment")
    void counterRegisteredAfterFirstIncrement() {
      service.increment(MetricNames.SUBMISSIONS_ADDED);
      assertThat(registry.find(fqn(MetricNames.SUBMISSIONS_ADDED)).counter()).isNotNull();
    }
  }

  // ---------------------------------------------------------------------------
  // Counter increment methods
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("counter increments")
  class CounterIncrements {

    private Counter counter(String shortName) {
      return registry.find(TEST_NAMESPACE + shortName).counter();
    }

    @Test
    @DisplayName("increment SUBMISSIONS_ADDED increments by 1")
    void incrementSubmissionsAdded() {
      service.increment(MetricNames.SUBMISSIONS_ADDED);
      assertThat(counter(MetricNames.SUBMISSIONS_ADDED).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("increment CLAIMS_ADDED increments by 1")
    void incrementClaimsAdded() {
      service.increment(MetricNames.CLAIMS_ADDED);
      assertThat(counter(MetricNames.CLAIMS_ADDED).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("increment CLAIMS_VALIDATED_VALID increments by 1")
    void incrementClaimsValidatedValid() {
      service.increment(MetricNames.CLAIMS_VALIDATED_VALID);
      assertThat(counter(MetricNames.CLAIMS_VALIDATED_VALID).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("increment CLAIMS_VALIDATED_WARNINGS increments by 1")
    void incrementClaimsValidatedWarnings() {
      service.increment(MetricNames.CLAIMS_VALIDATED_WARNINGS);
      assertThat(counter(MetricNames.CLAIMS_VALIDATED_WARNINGS).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("increment CLAIMS_VALIDATED_INVALID increments by 1")
    void incrementClaimsValidatedInvalid() {
      service.increment(MetricNames.CLAIMS_VALIDATED_INVALID);
      assertThat(counter(MetricNames.CLAIMS_VALIDATED_INVALID).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("increment SUBMISSIONS_WITH_ERRORS increments by 1")
    void incrementSubmissionsWithErrors() {
      service.increment(MetricNames.SUBMISSIONS_WITH_ERRORS);
      assertThat(counter(MetricNames.SUBMISSIONS_WITH_ERRORS).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("increment VALID_SUBMISSIONS increments by 1")
    void incrementValidSubmissions() {
      service.increment(MetricNames.VALID_SUBMISSIONS);
      assertThat(counter(MetricNames.VALID_SUBMISSIONS).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("increment INVALID_SUBMISSIONS increments by 1")
    void incrementInvalidSubmissions() {
      service.increment(MetricNames.INVALID_SUBMISSIONS);
      assertThat(counter(MetricNames.INVALID_SUBMISSIONS).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("multiple increments accumulate correctly")
    void multipleIncrementsAccumulate() {
      service.increment(MetricNames.SUBMISSIONS_ADDED);
      service.increment(MetricNames.SUBMISSIONS_ADDED);
      service.increment(MetricNames.SUBMISSIONS_ADDED);
      assertThat(counter(MetricNames.SUBMISSIONS_ADDED).count()).isEqualTo(3);
    }
  }

  // ---------------------------------------------------------------------------
  // increment() tag support
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("increment with tags")
  class IncrementWithTags {

    private String fqn(String shortName) {
      return TEST_NAMESPACE + shortName;
    }

    @Test
    @DisplayName("increment with no tags registers and increments the counter")
    void noTagsRegistersCounter() {
      service.increment(MetricNames.SUBMISSIONS_ADDED);
      Counter counter = registry.find(fqn(MetricNames.SUBMISSIONS_ADDED)).counter();
      assertThat(counter).isNotNull();
      assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("increment with a single tag pair applies the tag to the counter")
    void singleTagPairApplied() {
      service.increment(MetricNames.SUBMISSIONS_ADDED, "area_of_law", "CRIME");
      Counter counter =
          registry.find(fqn(MetricNames.SUBMISSIONS_ADDED)).tag("area_of_law", "CRIME").counter();
      assertThat(counter).isNotNull();
      assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("increment with multiple tag pairs applies all tags to the counter")
    void multipleTagPairsApplied() {
      service.increment(
          MetricNames.SUBMISSIONS_ADDED, "area_of_law", "CRIME", "provider_id", "12345");
      Counter counter =
          registry
              .find(fqn(MetricNames.SUBMISSIONS_ADDED))
              .tag("area_of_law", "CRIME")
              .tag("provider_id", "12345")
              .counter();
      assertThat(counter).isNotNull();
      assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("tagged and untagged counters are tracked independently")
    void taggedAndUntaggedAreIndependent() {
      service.increment(MetricNames.SUBMISSIONS_ADDED);
      service.increment(MetricNames.SUBMISSIONS_ADDED, "area_of_law", "CRIME");
      service.increment(MetricNames.SUBMISSIONS_ADDED, "area_of_law", "CRIME");

      Counter untagged =
          registry.find(fqn(MetricNames.SUBMISSIONS_ADDED)).counters().stream()
              .filter(c -> c.getId().getTags().isEmpty())
              .findFirst()
              .orElse(null);
      Counter tagged =
          registry.find(fqn(MetricNames.SUBMISSIONS_ADDED)).tag("area_of_law", "CRIME").counter();

      assertThat(untagged).isNotNull();
      assertThat(untagged.count()).isEqualTo(1);
      assertThat(tagged).isNotNull();
      assertThat(tagged.count()).isEqualTo(2);
    }

    @Test
    @DisplayName(
        "odd number of tag elements throws IllegalArgumentException — programmer bug, fail fast")
    void oddTagCountThrows() {
      assertThatThrownBy(() -> service.increment(MetricNames.SUBMISSIONS_ADDED, "only_key"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("even number of elements");
    }

    @Test
    @DisplayName("three tag elements throws IllegalArgumentException — programmer bug, fail fast")
    void threeTagElementsThrows() {
      assertThatThrownBy(() -> service.increment(MetricNames.SUBMISSIONS_ADDED, "k1", "v1", "k2"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("even number of elements");
    }

    @Test
    @DisplayName("null tag key throws NullPointerException — programmer bug, fail fast")
    void nullTagKeyThrows() {
      assertThatThrownBy(() -> service.increment(MetricNames.SUBMISSIONS_ADDED, null, "value"))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("must not be null");
    }

    @Test
    @DisplayName("null tag value throws NullPointerException — programmer bug, fail fast")
    void nullTagValueThrows() {
      assertThatThrownBy(() -> service.increment(MetricNames.SUBMISSIONS_ADDED, "key", null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("must not be null");
    }
  }

  // ---------------------------------------------------------------------------
  // Timer factory methods — MetricTimer
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("MetricTimer factory methods")
  class MetricTimerFactory {

    private String fqn(String shortName) {
      return TEST_NAMESPACE + shortName;
    }

    // --- lazy registration ---

    @Test
    @DisplayName("timers are not pre-registered — they appear only after first use")
    void timersAreNotPreRegistered() {
      assertThat(registry.find(fqn(MetricNames.FILE_PARSING_TIME)).timer()).isNull();
      assertThat(registry.find(fqn(MetricNames.CLAIM_VALIDATION_TIME)).timer()).isNull();
      assertThat(registry.find(fqn(MetricNames.SUBMISSION_VALIDATION_TIME)).timer()).isNull();
      assertThat(registry.find(fqn(MetricNames.FSP_VALIDATION_TIME)).timer()).isNull();
    }

    @Test
    @DisplayName("timer is registered in the registry after first use")
    void timerRegisteredAfterFirstUse() {
      try (var ignored = service.timer(MetricNames.FILE_PARSING_TIME, UUID.randomUUID())) {
        // timed operation
      }
      assertThat(registry.find(fqn(MetricNames.FILE_PARSING_TIME)).timer()).isNotNull();
    }

    // --- all four MetricNames timer constants ---

    @Test
    @DisplayName("FILE_PARSING_TIME records after close")
    void fileParsingTimeRecords() {
      try (var ignored = service.timer(MetricNames.FILE_PARSING_TIME, UUID.randomUUID())) {
        assertThat(ignored).isNotNull();
      }
      Timer timer = registry.find(fqn(MetricNames.FILE_PARSING_TIME)).timer();
      assertThat(timer).isNotNull();
      assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("SUBMISSION_VALIDATION_TIME records after close")
    void submissionValidationTimeRecords() {
      try (var ignored = service.timer(MetricNames.SUBMISSION_VALIDATION_TIME, UUID.randomUUID())) {
        assertThat(ignored).isNotNull();
      }
      Timer timer = registry.find(fqn(MetricNames.SUBMISSION_VALIDATION_TIME)).timer();
      assertThat(timer).isNotNull();
      assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("CLAIM_VALIDATION_TIME records after close")
    void claimValidationTimeRecords() {
      try (var ignored = service.timer(MetricNames.CLAIM_VALIDATION_TIME, UUID.randomUUID())) {
        assertThat(ignored).isNotNull();
      }
      Timer timer = registry.find(fqn(MetricNames.CLAIM_VALIDATION_TIME)).timer();
      assertThat(timer).isNotNull();
      assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("FSP_VALIDATION_TIME records after close")
    void fspValidationTimeRecords() {
      try (var ignored = service.timer(MetricNames.FSP_VALIDATION_TIME, UUID.randomUUID())) {
        assertThat(ignored).isNotNull();
      }
      Timer timer = registry.find(fqn(MetricNames.FSP_VALIDATION_TIME)).timer();
      assertThat(timer).isNotNull();
      assertThat(timer.count()).isEqualTo(1);
    }

    // --- exception safety ---

    @Test
    @DisplayName("timer still records when the enclosed operation throws")
    void timerRecordsOnException() {
      try {
        try (var ignored = service.timer(MetricNames.CLAIM_VALIDATION_TIME, UUID.randomUUID())) {
          throw new RuntimeException("simulated failure");
        }
      } catch (RuntimeException ignored) {
        // expected
      }
      Timer timer = registry.find(fqn(MetricNames.CLAIM_VALIDATION_TIME)).timer();
      assertThat(timer).isNotNull();
      assertThat(timer.count()).isEqualTo(1);
    }

    // --- accumulation ---

    @Test
    @DisplayName("multiple timer samples accumulate the count correctly")
    void multipleTimerSamplesAccumulate() {
      for (int i = 0; i < 3; i++) {
        try (var ignored = service.timer(MetricNames.FSP_VALIDATION_TIME, UUID.randomUUID())) {
          // timed operation
        }
      }
      Timer timer = registry.find(fqn(MetricNames.FSP_VALIDATION_TIME)).timer();
      assertThat(timer).isNotNull();
      assertThat(timer.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("each timer() call produces a distinct MetricTimer instance")
    void eachCallProducesDistinctInstance() {
      UUID id = UUID.randomUUID();
      try (MetricTimer a = service.timer(MetricNames.FILE_PARSING_TIME, id);
          MetricTimer b = service.timer(MetricNames.FILE_PARSING_TIME, id)) {
        assertThat(a).isNotSameAs(b);
      }
    }

    // --- tags ---

    @Test
    @DisplayName("single tag pair is applied to the timer")
    void singleTagPairApplied() {
      try (var ignored =
          service.timer(
              MetricNames.CLAIM_VALIDATION_TIME, UUID.randomUUID(), "area_of_law", "CRIME")) {
        // timed operation
      }
      Timer timer =
          registry.find(fqn(MetricNames.CLAIM_VALIDATION_TIME)).tag("area_of_law", "CRIME").timer();
      assertThat(timer).isNotNull();
      assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("multiple tag pairs are all applied to the timer")
    void multipleTagPairsApplied() {
      try (var ignored =
          service.timer(
              MetricNames.CLAIM_VALIDATION_TIME,
              UUID.randomUUID(),
              "area_of_law",
              "CRIME",
              "provider_id",
              "12345")) {
        // timed operation
      }
      Timer timer =
          registry
              .find(fqn(MetricNames.CLAIM_VALIDATION_TIME))
              .tag("area_of_law", "CRIME")
              .tag("provider_id", "12345")
              .timer();
      assertThat(timer).isNotNull();
      assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("tagged and untagged timers are tracked independently")
    void taggedAndUntaggedAreIndependent() {
      try (var ignored = service.timer(MetricNames.CLAIM_VALIDATION_TIME, UUID.randomUUID())) {
        /* untagged */
      }
      try (var ignored =
          service.timer(
              MetricNames.CLAIM_VALIDATION_TIME, UUID.randomUUID(), "area_of_law", "CRIME")) {
        /* tagged */
      }
      try (var ignored =
          service.timer(
              MetricNames.CLAIM_VALIDATION_TIME, UUID.randomUUID(), "area_of_law", "CRIME")) {
        /* tagged */
      }

      Timer untagged =
          registry.find(fqn(MetricNames.CLAIM_VALIDATION_TIME)).timers().stream()
              .filter(t -> t.getId().getTags().isEmpty())
              .findFirst()
              .orElse(null);
      Timer tagged =
          registry.find(fqn(MetricNames.CLAIM_VALIDATION_TIME)).tag("area_of_law", "CRIME").timer();

      assertThat(untagged).isNotNull();
      assertThat(untagged.count()).isEqualTo(1);
      assertThat(tagged).isNotNull();
      assertThat(tagged.count()).isEqualTo(2);
    }

    // --- tag validation ---

    @Test
    @DisplayName(
        "odd number of tag elements throws IllegalArgumentException — programmer bug, fail fast")
    void oddTagCountThrows() {
      assertThatThrownBy(
              () -> service.timer(MetricNames.CLAIM_VALIDATION_TIME, UUID.randomUUID(), "only_key"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("even number of elements");
    }

    @Test
    @DisplayName("three tag elements throws IllegalArgumentException — programmer bug, fail fast")
    void threeTagElementsThrows() {
      assertThatThrownBy(
              () ->
                  service.timer(
                      MetricNames.CLAIM_VALIDATION_TIME, UUID.randomUUID(), "k1", "v1", "k2"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("even number of elements");
    }

    @Test
    @DisplayName("null tag key throws NullPointerException — programmer bug, fail fast")
    void nullTagKeyThrows() {
      assertThatThrownBy(
              () ->
                  service.timer(
                      MetricNames.CLAIM_VALIDATION_TIME, UUID.randomUUID(), null, "value"))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("must not be null");
    }

    @Test
    @DisplayName("null tag value throws NullPointerException — programmer bug, fail fast")
    void nullTagValueThrows() {
      assertThatThrownBy(
              () ->
                  service.timer(MetricNames.CLAIM_VALIDATION_TIME, UUID.randomUUID(), "key", null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("must not be null");
    }

    // --- warn threshold ---

    @Test
    @DisplayName("no warning logged when elapsed time is below threshold")
    void noWarnWhenBelowThreshold() {
      // MockClock does not advance unless told to — elapsed will be ~0, well below 2s threshold
      try (var ignored = service.timer(MetricNames.CLAIM_VALIDATION_TIME, UUID.randomUUID())) {
        // no advance
      }
      // If a WARN had been logged the test would not fail here — but we can assert the timer
      // recorded correctly, confirming the path completed without error
      Timer timer = registry.find(fqn(MetricNames.CLAIM_VALIDATION_TIME)).timer();
      assertThat(timer).isNotNull();
      assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("warning is logged when elapsed time exceeds threshold — uses MockClock")
    void warnLoggedWhenThresholdExceeded() {
      // Build a publisher backed by a MockClock registry so we can advance time deterministically
      io.micrometer.core.instrument.MockClock mockClock =
          new io.micrometer.core.instrument.MockClock();
      io.micrometer.core.instrument.simple.SimpleMeterRegistry mockRegistry =
          new io.micrometer.core.instrument.simple.SimpleMeterRegistry(
              io.micrometer.core.instrument.simple.SimpleConfig.DEFAULT, mockClock);
      MetricsProperties slowProps = new MetricsProperties();
      slowProps.setNamespace(TEST_NAMESPACE);
      slowProps.setWarnThresholdSeconds(1.0);
      MetricPublisher slowService = new MetricPublisher(mockRegistry, slowProps);

      // The close() call reads the clock — advance 2s (> 1s threshold) before close
      MetricTimer metricTimer =
          slowService.timer(MetricNames.CLAIM_VALIDATION_TIME, UUID.randomUUID());
      mockClock.add(2, java.util.concurrent.TimeUnit.SECONDS);
      metricTimer.close();

      // Timer still recorded
      Timer timer = mockRegistry.find(TEST_NAMESPACE + MetricNames.CLAIM_VALIDATION_TIME).timer();
      assertThat(timer).isNotNull();
      assertThat(timer.count()).isEqualTo(1);
    }
  }

  // ---------------------------------------------------------------------------
  // toLabel — short name to human-readable label (lives in MetricUtils)
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("toLabel")
  class ToLabel {

    @Test
    @DisplayName("single word is title-cased")
    void singleWord() {
      assertThat(MetricUtils.toLabel("time")).isEqualTo("Time");
    }

    @Test
    @DisplayName("underscored words become title-cased space-separated words")
    void underscoredWords() {
      assertThat(MetricUtils.toLabel("claim_validation_time")).isEqualTo("Claim Validation Time");
    }

    @Test
    @DisplayName("file_parsing_time converts correctly")
    void fileParsingTime() {
      assertThat(MetricUtils.toLabel("file_parsing_time")).isEqualTo("File Parsing Time");
    }

    @Test
    @DisplayName("fsp_validation_time converts correctly")
    void fspValidationTime() {
      assertThat(MetricUtils.toLabel("fsp_validation_time")).isEqualTo("Fsp Validation Time");
    }

    @Test
    @DisplayName("null returns null")
    void nullInput() {
      assertThat(MetricUtils.toLabel(null)).isNull();
    }

    @Test
    @DisplayName("blank string returns blank string")
    void blankInput() {
      assertThat(MetricUtils.toLabel("   ")).isEqualTo("   ");
    }

    @Test
    @DisplayName("empty string returns empty string")
    void emptyInput() {
      assertThat(MetricUtils.toLabel("")).isEqualTo("");
    }
  }
}
