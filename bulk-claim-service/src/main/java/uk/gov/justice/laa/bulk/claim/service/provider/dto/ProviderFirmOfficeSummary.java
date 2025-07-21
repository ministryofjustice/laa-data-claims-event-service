package uk.gov.justice.laa.bulk.claim.service.provider.dto;

/**
 * A data transfer object (DTO) that represents a summary of an office associated with a provider
 * firm.
 *
 * <p>The information from this record was taken from the Provider Details API.
 *
 * @param firmOfficeId The firm office ID.
 * @param ccmsFirmOfficeId The firm office ID in CCMS.
 * @param firmOfficeCode The firm office code.
 * @param officeName The office name.
 * @param officeCodeAlt The office code alternative.
 * @param type The office type.
 * @author Jamie Briggs
 */
public record ProviderFirmOfficeSummary(
    int firmOfficeId,
    int ccmsFirmOfficeId,
    String firmOfficeCode,
    String officeName,
    String officeCodeAlt,
    String type) {}
