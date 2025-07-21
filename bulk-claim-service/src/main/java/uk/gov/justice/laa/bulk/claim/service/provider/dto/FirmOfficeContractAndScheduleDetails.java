package uk.gov.justice.laa.bulk.claim.service.provider.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * A data transfer object (DTO) representing the details of a firm's office contract and its
 * schedule.
 *
 * <p>The information from this record was taken from the Provider Details API.
 *
 * @param contractType The type of the contract.
 * @param contractDescription A description of the contract.
 * @param contractNumber The unique number associated with the contract.
 * @param contractReference The reference number for the contract.
 * @param contractStatus The current status of the contract.
 * @param contractAuthorizationStatus The authorization status of the contract.
 * @param contractStartDate The start date of the contract.
 * @param contractEndDate The end date of the contract.
 * @param areaOfLaw The area of law relevant to the schedule.
 * @param scheduleType The type of the schedule.
 * @param scheduleNumber The unique number associated with the schedule.
 * @param scheduleContractNumber The contract number associated with the schedule.
 * @param scheduleContractReference The reference number for the schedule contract.
 * @param scheduleAuthorizationStatus The authorization status of the schedule.
 * @param scheduleStatus The current status of the schedule.
 * @param scheduleStartDate The start date of the schedule.
 * @param scheduleEndDate The end date of the schedule.
 * @param scheduleLines A list of lines associated with the schedule.
 * @param nmsAuths A list of NMS authorization details associated with the schedule.
 * @author Jamie Briggs
 */
public record FirmOfficeContractAndScheduleDetails(
    String contractType,
    String contractDescription,
    String contractNumber,
    String contractReference,
    String contractStatus,
    String contractAuthorizationStatus,
    LocalDate contractStartDate,
    LocalDate contractEndDate,
    String areaOfLaw,
    String scheduleType,
    String scheduleNumber,
    String scheduleContractNumber,
    String scheduleContractReference,
    String scheduleAuthorizationStatus,
    String scheduleStatus,
    LocalDate scheduleStartDate,
    LocalDate scheduleEndDate,
    List<FirmOfficeContractAndScheduleLine> scheduleLines,
    List<NmsAuthDetails> nmsAuths) {}
