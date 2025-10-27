package uk.gov.justice.laa.dstew.payments.claimsevent.validation.model;

/**
 * Represents a validation error message consisting of a key and a value.
 *
 * <p>This class is used to encapsulate validation error details, where the key denotes the specific
 * validation field or category, and the value provides a description or reason for the validation
 * error.
 *
 * <p>It is intended to be immutable and used in scenarios that require error reporting or debugging
 * within validation workflows.
 */
public record ValidationErrorMessage(String key, String value) {}
