package uk.gov.justice.laa.bulk.claim.service.provider.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A data transfer object (DTO) that represents a summary of a provider firm.
 *
 * <p>The information from this record was taken from the Provider Details API.</p>
 *
 * @param firmNumber           The firm number.
 * @param firmId               The firm ID.
 * @param ccmsFirmId           The firm ID in CCMS.
 * @param parentFirmId         The parent firm ID.
 * @param firmName             The name of the firm.
 * @param firmType             The type of the firm.
 * @param constitutionalStatus The consitutional status of the firm.
 * @param solicitorAdvocate    A flag indicating if the firm is a solicitor or advocate.
 * @param advocateLevel        The level of the firm as an advocate.
 * @param barCouncilRoll       The Bar Council roll.
 * @param companyHouseNumber   The company house number.
 * @author Jamie Briggs
 */
public record ProviderFirmSummary(
    String firmNumber,
    int firmId,
    int ccmsFirmId,
    Integer parentFirmId,
    String firmName,
    String firmType,
    String constitutionalStatus,
    @JsonProperty("solicitorAdvocateYN") String solicitorAdvocate,
    String advocateLevel,
    String barCouncilRoll,
    String companyHouseNumber) {

}
