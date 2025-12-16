package uk.gov.justice.laa.dstew.payments.claimsevent.service.model;

import java.time.LocalDate;

/** Coverage window for a provider schedule, inclusive of start and end dates. */
public record ProviderDetailsCoverageWindow(LocalDate start, LocalDate end) {}
