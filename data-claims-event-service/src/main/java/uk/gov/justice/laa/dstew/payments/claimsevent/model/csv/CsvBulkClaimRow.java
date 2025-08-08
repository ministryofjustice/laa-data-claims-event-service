package uk.gov.justice.laa.dstew.payments.claimsevent.model.csv;

import java.util.Map;

/**
 * Record representing a row in a bulk claim submission CSV file.
 *
 * @param header the @{link CsvHeader} value for the row.
 * @param values a map of key value pairs for each column within the row.
 */
public record CsvBulkClaimRow(CsvHeader header, Map<String, String> values) {}
