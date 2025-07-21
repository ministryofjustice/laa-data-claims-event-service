package uk.gov.justice.laa.bulk.claim.service.provider.dto;

import java.time.LocalDate;

/**
 * A data transfer object (DTO) representing a line item in a firm's office contract and schedule.
 *
 * <p>The information from this record was taken from the Provider Details API.
 *
 * @param areaOfLaw The area of law relevant to the contract line.
 * @param categoryOfLaw The category of law relevant to the contract line.
 * @param description A description of the contract line.
 * @param devolvedPowersStatus The status of devolved powers for this contract line.
 * @param dpTypeOfChange The type of change to devolved powers.
 * @param dpReasonForChange The reason for the change to devolved powers.
 * @param dpDateOfChange The date of change to devolved powers.
 * @param remainderWorkFlag A flag indicating whether there is remaining work.
 * @param minimumCasesAllowedCount The minimum number of cases allowed.
 * @param maximumCasesAllowedCount The maximum number of cases allowed.
 * @param minimumToleranceCount The minimum tolerance count.
 * @param maximumToleranceCount The maximum tolerance count.
 * @param minimumLicenseCount The minimum license count.
 * @param maximumLicenseCount The maximum license count.
 * @param workInProgressCount The count of work in progress.
 * @param outreach Outreach details related to this contract line.
 * @param cancelFlag A flag indicating if the contract line has been canceled.
 * @param cancelReason The reason for canceling the contract line.
 * @param cancelDate The date the contract line was canceled.
 * @param closedDate The date the contract line was closed.
 * @param closedReason The reason for closing the contract line.
 * @author Jamie Briggs
 */
public record FirmOfficeContractAndScheduleLine(
    String areaOfLaw,
    String categoryOfLaw,
    String description,
    String devolvedPowersStatus,
    String dpTypeOfChange,
    String dpReasonForChange,
    String dpDateOfChange,
    String remainderWorkFlag,
    String minimumCasesAllowedCount,
    String maximumCasesAllowedCount,
    String minimumToleranceCount,
    String maximumToleranceCount,
    String minimumLicenseCount,
    String maximumLicenseCount,
    String workInProgressCount,
    String outreach,
    String cancelFlag,
    String cancelReason,
    LocalDate cancelDate,
    LocalDate closedDate,
    String closedReason) {}
