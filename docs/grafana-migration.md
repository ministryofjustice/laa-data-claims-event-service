# Grafana Dashboard — Prometheus Query Migration

## Background

`EventServiceMetricService` has been migrated from the raw Prometheus Java client
(`io.prometheus.metrics`) to Micrometer (`io.micrometer.core.instrument`).

The metric **names** are unchanged. However, the way Micrometer and the raw Prometheus client
format counter metric names in the Prometheus scrape output differs in one case:

| Metric type | Raw Prometheus client suffix | Micrometer suffix |
|---|---|---|
| Counter | `_total` (when explicitly added) or none | Always appends `_total` automatically |
| Timer/Summary | `_count`, `_sum`, `_created`, quantile labels | `_count`, `_sum`, `_max`, `{quantile}` labels |

Most existing Grafana counter queries already include `_total` and will continue to work
unchanged. **One panel is missing the `_total` suffix** and must be updated.

---

## Required Changes

### Panel: "Submissions validated and errors found with the submission"

This is the only panel whose PromQL query does not include the `_total` suffix.
Micrometer automatically appends `_total` to all counter names, so the metric name at the
scrape endpoint changes from `claims_event_service_submissions_with_errors` to
`claims_event_service_submissions_with_errors_total`.

| | Value |
|---|---|
| **Panel title** | Submissions validated and errors found with the submission |
| **Old query** | `sum(increase(claims_event_service_submissions_with_errors{container='$container'}[$__range]))` |
| **New query** | `sum(increase(claims_event_service_submissions_with_errors_total{container='$container'}[$__range]))` |

---

## Panels Unaffected (no changes required)

These panels already use the `_total` suffix, which is what Micrometer produces. No changes
are needed.

| Panel title | Query (unchanged) |
|---|---|
| Total submissions created by event service | `sum(increase(claims_event_service_submissions_added_total{container='$container'}[$__range]))` |
| Total claims created by event service | `sum(increase(claims_event_service_claims_added_total{container='$container'}[$__range]))` |
| Total valid submissions | `sum(increase(claims_event_service_valid_submissions_total{container='$container'}[$__range]))` |
| Total invalid submissions | `sum(increase(claims_event_service_invalid_submissions_total{container='$container'}[$__range]))` |
| Claims validated and has no warnings or errors | `sum(increase(claims_event_service_claims_validated_and_valid_total{container='$container'}[$__range]))` |
| Claims validated and has warnings | `sum(increase(claims_event_service_claims_validated_and_warnings_found_total{container='$container'}[$__range]))` |
| Claims validated and has errors | `sum(increase(claims_event_service_claims_validated_and_invalid_total{container='$container'}[$__range]))` |

---

## New Metrics Now Available

The following metrics are new and not yet in the Grafana dashboard. They are available at
`/actuator/prometheus` immediately after deployment and can be added to the dashboard as
needed.

### Timers (all expose `_count`, `_sum`, `_max` and P50/P90/P95/P99 quantile labels)

| Metric name | Description |
|---|---|
| `claims_event_service_file_parsing_time` | Time taken to parse a bulk upload file from the SQS message |
| `claims_event_service_submission_validation_time` | End-to-end time to validate a submission including FSP calls |
| `claims_event_service_claim_validation_time` | Time taken to validate a single claim |
| `claims_event_service_fsp_validation_time` | Time taken for a Fee Scheme Platform fee calculation |

Example PromQL to show P99 claim validation latency:
```
histogram_quantile(0.99, sum(rate(claims_event_service_claim_validation_time_bucket{container='$container'}[$__rate_interval])) by (le))
```

### Labelled counters

| Metric name | Tags | Description |
|---|---|---|
| `claims_event_service_messages_errors_total` | `error_source`, `type` (Claim/Submission), `message` | Count of validation error messages by type and source |
| `claims_event_service_messages_warnings_total` | `error_source`, `type` (Claim/Submission), `message` | Count of validation warning messages by type and source |

---

## How to Apply the Change in Grafana

1. Open the **LAA DSTEW Claims Event Service Dashboard** in Grafana.
2. Click **Edit** on the panel titled **"Submissions validated and errors found with the submission"**.
3. In the **Metrics browser / query editor**, replace the expression shown in the
   **Old query** row above with the expression shown in the **New query** row.
4. Click **Apply**, then **Save dashboard**.

