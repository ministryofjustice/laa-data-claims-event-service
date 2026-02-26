package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionMatterStart;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionOutcome;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200Response;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200ResponseDetails;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200ResponseDetailsOffice;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200ResponseDetailsSchedule;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.MatterStartPost;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionPost;
import uk.gov.justice.laa.dstew.payments.claimsevent.mapper.BulkSubmissionMapper;

/**
 * Normalises incoming bulk submission data retrieved from the upstream Bulk Submission API.
 *
 * <p>This normaliser operates on:
 *
 * <ul>
 *   <li>{@link GetBulkSubmission200Response}
 *   <li>{@link GetBulkSubmission200ResponseDetails}
 *   <li>{@link GetBulkSubmission200ResponseDetailsOffice}
 *   <li>{@link GetBulkSubmission200ResponseDetailsSchedule}
 *   <li>{@link BulkSubmissionOutcome}
 *   <li>{@link BulkSubmissionMatterStart}
 * </ul>
 *
 * <p>Normalisation occurs BEFORE mapping into:
 *
 * <ul>
 *   <li>{@link SubmissionPost}
 *   <li>{@link ClaimPost}
 *   <li>{@link MatterStartPost}
 * </ul>
 *
 * <p><strong>Normalisation rules applied:</strong>
 *
 * <ul>
 *   <li>All {@link String} fields are trimmed
 *   <li>Blank strings become {@code null}
 *   <li>No parsing of numerics/dates/booleans occurs here — that responsibility is intentionally
 *       left to the Claims API mapper
 *   <li>Nested DTOs, lists, and string maps are normalised recursively
 * </ul>
 *
 * <p>This ensures that the {@link BulkSubmissionMapper} does not encounter whitespace or
 * blank-string values that would otherwise cause parsing failures.
 */
@Service
public class SubmissionDataNormaliser {

  private String normaliseString(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  /**
   * Normalises the entire upstream {@link GetBulkSubmission200Response} object, recursively
   * trimming all String fields and cleaning nested DTO structures such as schedules, offices,
   * outcomes, matter-starts, and immigration CLR entries.
   *
   * @param response the {@code GetBulkSubmission200Response} DTO retrieved from the Bulk Submission
   *     API
   * @return the same DTO instance with String values normalised
   */
  public GetBulkSubmission200Response normalise(GetBulkSubmission200Response response) {
    if (response == null) {
      return null;
    }

    if (response.getDetails() != null) {
      normalise(response.getDetails());
    }

    return response;
  }

  private void normalise(GetBulkSubmission200ResponseDetails details) {

    if (details.getOffice() != null) {
      normalise(details.getOffice());
    }

    if (details.getSchedule() != null) {
      normalise(details.getSchedule());
    }

    if (details.getOutcomes() != null) {
      details.setOutcomes(details.getOutcomes().stream().map(this::normalise).toList());
    }

    if (details.getMatterStarts() != null) {
      details.setMatterStarts(details.getMatterStarts().stream().map(this::normalise).toList());
    }

    if (details.getImmigrationClr() != null) {
      details.setImmigrationClr(
          details.getImmigrationClr().stream().map(this::normaliseMap).toList());
    }
  }

  // Office normalisation
  private void normalise(GetBulkSubmission200ResponseDetailsOffice office) {
    office.setAccount(normaliseString(office.getAccount()));
  }

  // Schedule normalisation
  private void normalise(GetBulkSubmission200ResponseDetailsSchedule schedule) {
    schedule.setSubmissionPeriod(normaliseString(schedule.getSubmissionPeriod()));
    schedule.setAreaOfLaw(normaliseString(schedule.getAreaOfLaw()));
    schedule.setScheduleNum(normaliseString(schedule.getScheduleNum()));
  }

  // Outcome normalisation
  private BulkSubmissionOutcome normalise(BulkSubmissionOutcome outcome) {
    if (outcome == null) {
      return null;
    }

    outcome.setMatterType(normaliseString(outcome.getMatterType()));
    outcome.setFeeCode(normaliseString(outcome.getFeeCode()));
    outcome.setCaseRefNumber(normaliseString(outcome.getCaseRefNumber()));
    outcome.setCaseStartDate(normaliseString(outcome.getCaseStartDate()));
    outcome.setCaseId(normaliseString(outcome.getCaseId()));
    outcome.setCaseStageLevel(normaliseString(outcome.getCaseStageLevel()));
    outcome.setUfn(normaliseString(outcome.getUfn()));
    outcome.setProcurementArea(normaliseString(outcome.getProcurementArea()));
    outcome.setAccessPoint(normaliseString(outcome.getAccessPoint()));
    outcome.setClientForename(normaliseString(outcome.getClientForename()));
    outcome.setClientSurname(normaliseString(outcome.getClientSurname()));
    outcome.setClientDateOfBirth(normaliseString(outcome.getClientDateOfBirth()));
    outcome.setUcn(normaliseString(outcome.getUcn()));
    outcome.setClaRefNumber(normaliseString(outcome.getClaRefNumber()));
    outcome.setClaExemption(normaliseString(outcome.getClaExemption()));
    outcome.setGender(normaliseString(outcome.getGender()));
    outcome.setEthnicity(normaliseString(outcome.getEthnicity()));
    outcome.setDisability(normaliseString(outcome.getDisability()));
    outcome.setClientPostCode(normaliseString(outcome.getClientPostCode()));
    outcome.setWorkConcludedDate(normaliseString(outcome.getWorkConcludedDate()));
    outcome.setClientType(normaliseString(outcome.getClientType()));
    outcome.setOutcomeCode(normaliseString(outcome.getOutcomeCode()));
    outcome.setClaimType(normaliseString(outcome.getClaimType()));
    outcome.setTypeOfAdvice(normaliseString(outcome.getTypeOfAdvice()));
    outcome.setScheduleRef(normaliseString(outcome.getScheduleRef()));
    outcome.setCmrhOral(normaliseString(outcome.getCmrhOral()));
    outcome.setCmrhTelephone(normaliseString(outcome.getCmrhTelephone()));
    outcome.setAitHearingCentre(normaliseString(outcome.getAitHearingCentre()));
    outcome.setHoUcn(normaliseString(outcome.getHoUcn()));
    outcome.setTransferDate(normaliseString(outcome.getTransferDate()));
    outcome.setDeliveryLocation(normaliseString(outcome.getDeliveryLocation()));
    outcome.setPriorAuthorityRef(normaliseString(outcome.getPriorAuthorityRef()));
    outcome.setMeetingsAttended(normaliseString(outcome.getMeetingsAttended()));
    outcome.setMhtRefNumber(normaliseString(outcome.getMhtRefNumber()));
    outcome.setStageReached(normaliseString(outcome.getStageReached()));
    outcome.setFollowOnWork(normaliseString(outcome.getFollowOnWork()));
    outcome.setExemptionCriteriaSatisfied(normaliseString(outcome.getExemptionCriteriaSatisfied()));
    outcome.setExclCaseFundingRef(normaliseString(outcome.getExclCaseFundingRef()));
    outcome.setSurgeryDate(normaliseString(outcome.getSurgeryDate()));
    outcome.setLineNumber(normaliseString(outcome.getLineNumber()));
    outcome.setCrimeMatterType(normaliseString(outcome.getCrimeMatterType()));
    outcome.setFeeScheme(normaliseString(outcome.getFeeScheme()));
    outcome.setRepOrderDate(normaliseString(outcome.getRepOrderDate()));
    outcome.setPoliceStation(normaliseString(outcome.getPoliceStation()));
    outcome.setDsccNumber(normaliseString(outcome.getDsccNumber()));
    outcome.setMaatId(normaliseString(outcome.getMaatId()));
    outcome.setSchemeId(normaliseString(outcome.getSchemeId()));
    outcome.setOutreach(normaliseString(outcome.getOutreach()));
    outcome.setReferral(normaliseString(outcome.getReferral()));
    outcome.setClient2Forename(normaliseString(outcome.getClient2Forename()));
    outcome.setClient2Surname(normaliseString(outcome.getClient2Surname()));
    outcome.setClient2DateOfBirth(normaliseString(outcome.getClient2DateOfBirth()));
    outcome.setClient2Ucn(normaliseString(outcome.getClient2Ucn()));
    outcome.setClient2PostCode(normaliseString(outcome.getClient2PostCode()));
    outcome.setClient2Gender(normaliseString(outcome.getClient2Gender()));
    outcome.setClient2Ethnicity(normaliseString(outcome.getClient2Ethnicity()));
    outcome.setClient2Disability(normaliseString(outcome.getClient2Disability()));
    outcome.setUniqueCaseId(normaliseString(outcome.getUniqueCaseId()));
    outcome.setStandardFeeCat(normaliseString(outcome.getStandardFeeCat()));
    outcome.setCourtLocationHpcds(normaliseString(outcome.getCourtLocationHpcds()));
    outcome.setLocalAuthorityNumber(normaliseString(outcome.getLocalAuthorityNumber()));
    outcome.setPaNumber(normaliseString(outcome.getPaNumber()));
    outcome.setMedConcludedDate(normaliseString(outcome.getMedConcludedDate()));

    return outcome;
  }

  // Matter start normalisation
  private BulkSubmissionMatterStart normalise(BulkSubmissionMatterStart ms) {
    if (ms == null) {
      return null;
    }

    ms.setScheduleRef(normaliseString(ms.getScheduleRef()));
    ms.setProcurementArea(normaliseString(ms.getProcurementArea()));
    ms.setAccessPoint(normaliseString(ms.getAccessPoint()));
    ms.setDeliveryLocation(normaliseString(ms.getDeliveryLocation()));

    return ms;
  }

  // Map normalisation - used for Immigration CLR data
  private Map<String, String> normaliseMap(Map<String, String> map) {
    Map<String, String> result = new LinkedHashMap<>();

    for (Map.Entry<String, String> entry : map.entrySet()) {
      String key = normaliseString(entry.getKey());
      String value = normaliseString(entry.getValue());
      result.put(key, value);
    }

    return result;
  }
}
