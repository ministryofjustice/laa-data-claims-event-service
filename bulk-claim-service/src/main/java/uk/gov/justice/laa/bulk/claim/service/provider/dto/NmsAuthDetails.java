package uk.gov.justice.laa.bulk.claim.service.provider.dto;

/**
 * A data transfer object (DTO) that represents the details of NMS (National Mediation Service)
 * authorisation information.
 *
 * <p>The information from this record was taken from the Provider Details API.</p>
 *
 * @param description A brief description of the authorisation details.
 * @param minMatterStarts The minimum number of matter starts required.
 * @param maxMatterStarts The maximum number of matter starts allowed.
 * @param authorisedLitigator The name of the authorised litigator.
 * @param supervision Details about any required supervision.
 * @param serviceCombinations Allowed combinations of services.
 * @param typeOfPresence The type of presence required at the outreach office.
 * @param lawSocietyChildrenFlag A flag indicating if the law society children provisions apply.
 * @param advLawSocFamVioFlag A flag indicating if the law society family violence provisions apply.
 * @param advLawSocFamNoVioFlag A flag indicating if the law society family non-violence
 *                              provisions apply.
 * @param resAccrSpecDomAbuseFlag A flag indicating if the residence accreditation for domestic
 *                                abuse applies.
 * @param resAccrSpecOtherFlag A flag indicating if the residence accreditation for other issues
 *                             applies.
 * @param consortiaId The ID associated with any consortia.
 * @param authorisationStatus The current status of the authorisation.
 * @param withdrawalType The type of withdrawal, if applicable.
 * @param withdrawalReason The reason for withdrawal, if applicable.
 * @param attributeCategory The category of attributes associated with the authorisation.
 * @author Jamie Briggs
 */
public record NmsAuthDetails(
    String description,
    int minMatterStarts,
    int maxMatterStarts,
    String authorisedLitigator,
    String supervision,
    String serviceCombinations,
    String typeOfPresence,
    String lawSocietyChildrenFlag,
    String advLawSocFamVioFlag,
    String advLawSocFamNoVioFlag,
    String resAccrSpecDomAbuseFlag,
    String resAccrSpecOtherFlag,
    String consortiaId,
    String authorisationStatus,
    String withdrawalType,
    String withdrawalReason,
    String attributeCategory
) {

}
