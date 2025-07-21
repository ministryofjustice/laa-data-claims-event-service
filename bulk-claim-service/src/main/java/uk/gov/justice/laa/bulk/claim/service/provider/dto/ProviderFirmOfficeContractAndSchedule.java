package uk.gov.justice.laa.bulk.claim.service.provider.dto;

import java.util.List;

/**
 * A data transfer object (DTO) that represents the contract and schedule details of an office
 * associated with a provider firm.
 *
 * <p>The information from this record was taken from the Provider Details API.
 *
 * @param firm A summary of the provider firm.
 * @param office A summary of the office associated with the provider firm.
 * @param pds A flag indicating if the Provider Direct Services (PDS) is applicable.
 * @param schedules A list of detailed records containing contract and schedule information for the
 *     firm office.
 * @author Jamie Briggs
 */
public record ProviderFirmOfficeContractAndSchedule(
    ProviderFirmSummary firm,
    ProviderFirmOfficeSummary office,
    boolean pds,
    List<FirmOfficeContractAndScheduleDetails> schedules) {}
