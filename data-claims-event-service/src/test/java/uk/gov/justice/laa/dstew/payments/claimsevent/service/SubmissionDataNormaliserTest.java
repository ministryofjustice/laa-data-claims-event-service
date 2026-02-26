package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionMatterStart;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BulkSubmissionOutcome;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.CategoryCode;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200Response;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200ResponseDetails;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200ResponseDetailsOffice;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.GetBulkSubmission200ResponseDetailsSchedule;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.MediationType;

@ExtendWith(MockitoExtension.class)
class SubmissionDataNormaliserTest {

  private SubmissionDataNormaliser normaliser;

  @BeforeEach
  void setUp() {
    normaliser = new SubmissionDataNormaliser();
  }

  @Test
  @DisplayName("normalise(null) -> null")
  void normalise_nullResponse_returnsNull() {
    assertNull(normaliser.normalise(null));
  }

  @Test
  @DisplayName("GetBulkSubmission200ResponseDetails: office and schedule String fields trimmed")
  void normalise_details_office_schedule() {
    GetBulkSubmission200ResponseDetails details = new GetBulkSubmission200ResponseDetails();

    GetBulkSubmission200ResponseDetailsOffice office =
        new GetBulkSubmission200ResponseDetailsOffice();
    office.setAccount(" 2P554H ");
    details.setOffice(office);

    GetBulkSubmission200ResponseDetailsSchedule schedule =
        new GetBulkSubmission200ResponseDetailsSchedule();
    schedule.setSubmissionPeriod(" JAN-2020 ");
    schedule.setAreaOfLaw(" CRIME LOWER ");
    schedule.setScheduleNum(" ABC/20000L/10 ");
    details.setSchedule(schedule);

    GetBulkSubmission200Response resp = new GetBulkSubmission200Response();
    resp.setDetails(details);

    normaliser.normalise(resp);

    assertEquals("2P554H", resp.getDetails().getOffice().getAccount());
    assertEquals("JAN-2020", resp.getDetails().getSchedule().getSubmissionPeriod());
    assertEquals("CRIME LOWER", resp.getDetails().getSchedule().getAreaOfLaw());
    assertEquals("ABC/20000L/10", resp.getDetails().getSchedule().getScheduleNum());
  }

  @Test
  @DisplayName(
      "BulkSubmissionOutcome: all String fields trimmed, blanks become null, numeric/boolean preserved")
  void normalise_BulkSubmissionOutcome() {

    BulkSubmissionOutcome outcome = new BulkSubmissionOutcome();

    outcome.setMatterType(" MT ");
    outcome.setFeeCode(" F01 ");
    outcome.setCaseRefNumber(" REF-123 ");
    outcome.setCaseStartDate(" 2024-01-01 ");
    outcome.setCaseId(" CID ");
    outcome.setCaseStageLevel(" L2 ");
    outcome.setUfn(" UFN-1 ");
    outcome.setProcurementArea(" PA ");
    outcome.setAccessPoint(" AP ");
    outcome.setClientForename(" John ");
    outcome.setClientSurname(" Doe ");
    outcome.setClientDateOfBirth(" 2000-01-01 ");
    outcome.setUcn(" UCN ");
    outcome.setClaRefNumber(" CLA-1 ");
    outcome.setClaExemption(" EX ");
    outcome.setGender(" M ");
    outcome.setEthnicity(" E ");
    outcome.setDisability(" D ");
    outcome.setClientPostCode(" PC1 1PC ");
    outcome.setWorkConcludedDate(" 2024-02-01 ");
    outcome.setOutcomeCode(" OUTC ");
    outcome.setClaimType(" CT ");
    outcome.setTypeOfAdvice(" TA ");
    outcome.setScheduleRef(" SCH-1 ");
    outcome.setCmrhOral(" Y ");
    outcome.setCmrhTelephone(" N ");
    outcome.setAitHearingCentre(" AIT-1 ");
    outcome.setHoUcn(" HOU-1 ");
    outcome.setTransferDate(" 2024-03-01 ");
    outcome.setDeliveryLocation(" LOC ");
    outcome.setPriorAuthorityRef(" PAR-123 ");
    outcome.setMeetingsAttended(" M1,M2 ");
    outcome.setMhtRefNumber(" MHT-1 ");
    outcome.setStageReached(" S1 ");
    outcome.setFollowOnWork(" FOW ");
    outcome.setExemptionCriteriaSatisfied(" ECS ");
    outcome.setExclCaseFundingRef(" ECF-1 ");
    outcome.setSurgeryDate(" 2024-03-02 ");
    outcome.setLineNumber(" 42 ");
    outcome.setCrimeMatterType(" CMT ");
    outcome.setFeeScheme(" FS ");
    outcome.setRepOrderDate(" 2024-01-15 ");
    outcome.setPoliceStation(" PS ");
    outcome.setDsccNumber(" DSCC ");
    outcome.setMaatId(" MAAT ");
    outcome.setOutreach(" OUTR ");
    outcome.setReferral(" REF ");
    outcome.setClient2Forename(" Jane ");
    outcome.setClient2Surname(" Roe ");
    outcome.setClient2DateOfBirth(" 1999-12-31 ");
    outcome.setClient2Ucn(" UCN2 ");
    outcome.setClient2PostCode(" PC2 2PC ");
    outcome.setClient2Gender(" F ");
    outcome.setClient2Ethnicity(" E2 ");
    outcome.setClient2Disability(" D2 ");
    outcome.setUniqueCaseId(" UCID ");
    outcome.setStandardFeeCat(" SFC ");
    outcome.setCourtLocationHpcds(" HPCDS-1 ");
    outcome.setLocalAuthorityNumber(" LA-1 ");
    outcome.setPaNumber(" PA-1 ");
    outcome.setMedConcludedDate(" 2024-04-04 ");

    // Blank-only -> null
    outcome.setMatterType("   ");
    outcome.setSchemeId("   ");
    outcome.setCrimeMatterType("   ");
    outcome.setOutcomeCode("   ");
    outcome.setMaatId("   ");

    // All numeric and boolean fields should remain unchanged
    outcome.setAdviceTime(10);
    outcome.setTravelTime(20);
    outcome.setWaitingTime(30);
    outcome.setProfitCost(new BigDecimal("123.45"));
    outcome.setValueOfCosts(new BigDecimal("234.56"));
    outcome.setDisbursementsAmount(new BigDecimal("12.34"));
    outcome.setCounselCost(new BigDecimal("56.78"));
    outcome.setDisbursementsVat(new BigDecimal("9.99"));
    outcome.setTravelWaitingCosts(new BigDecimal("15.00"));
    outcome.setVatIndicator(Boolean.TRUE);
    outcome.setLondonNonlondonRate(Boolean.FALSE);
    outcome.setToleranceIndicator(Boolean.TRUE);
    outcome.setTravelCosts(new BigDecimal("7.77"));
    outcome.setLegacyCase(Boolean.FALSE);
    outcome.setAdjournedHearingFee(99);
    outcome.setPostalApplAccp(Boolean.TRUE);
    outcome.setSubstantiveHearing(Boolean.FALSE);
    outcome.setHoInterview(3);
    outcome.setDetentionTravelWaitingCosts(new BigDecimal("1.11"));
    outcome.setJrFormFilling(new BigDecimal("2.22"));
    outcome.setAdditionalTravelPayment(Boolean.TRUE);
    outcome.setMedicalReportsClaimed(2);
    outcome.setDesiAccRep(1);
    outcome.setNationalRefMechanismAdvice(Boolean.FALSE);
    outcome.setNoOfClients(5);
    outcome.setNoOfSurgeryClients(6);
    outcome.setIrcSurgery(Boolean.TRUE);
    outcome.setCostsDamagesRecovered(new BigDecimal("1000.00"));
    outcome.setEligibleClient(Boolean.TRUE);
    outcome.setExcessTravelCosts(new BigDecimal("3.33"));
    outcome.setDutySolicitor(Boolean.FALSE);
    outcome.setYouthCourt(Boolean.TRUE);
    outcome.setNoOfSuspects(7);
    outcome.setNoOfPoliceStation(8);
    outcome.setNumberOfMediationSessions(9);
    outcome.setMediationTime(60);
    outcome.setClientLegallyAided(Boolean.TRUE);
    outcome.setClient2LegallyAided(Boolean.FALSE);
    outcome.setClient2PostalApplAccp(Boolean.TRUE);

    GetBulkSubmission200ResponseDetails details = new GetBulkSubmission200ResponseDetails();
    details.setOutcomes(new ArrayList<>(List.of(outcome)));
    GetBulkSubmission200Response resp = new GetBulkSubmission200Response();
    resp.setDetails(details);

    normaliser.normalise(resp);

    BulkSubmissionOutcome n = resp.getDetails().getOutcomes().get(0);

    // Assertions for all String fields with trimmed whitespaces
    assertEquals("F01", n.getFeeCode());
    assertEquals("REF-123", n.getCaseRefNumber());
    assertEquals("2024-01-01", n.getCaseStartDate());
    assertEquals("CID", n.getCaseId());
    assertEquals("L2", n.getCaseStageLevel());
    assertEquals("UFN-1", n.getUfn());
    assertEquals("PA", n.getProcurementArea());
    assertEquals("AP", n.getAccessPoint());
    assertEquals("John", n.getClientForename());
    assertEquals("Doe", n.getClientSurname());
    assertEquals("2000-01-01", n.getClientDateOfBirth());
    assertEquals("UCN", n.getUcn());
    assertEquals("CLA-1", n.getClaRefNumber());
    assertEquals("EX", n.getClaExemption());
    assertEquals("M", n.getGender());
    assertEquals("E", n.getEthnicity());
    assertEquals("D", n.getDisability());
    assertEquals("PC1 1PC", n.getClientPostCode());
    assertEquals("2024-02-01", n.getWorkConcludedDate());
    assertEquals("CT", n.getClaimType());
    assertEquals("TA", n.getTypeOfAdvice());
    assertEquals("SCH-1", n.getScheduleRef());
    assertEquals("Y", n.getCmrhOral());
    assertEquals("N", n.getCmrhTelephone());
    assertEquals("AIT-1", n.getAitHearingCentre());
    assertEquals("HOU-1", n.getHoUcn());
    assertEquals("2024-03-01", n.getTransferDate());
    assertEquals("LOC", n.getDeliveryLocation());
    assertEquals("PAR-123", n.getPriorAuthorityRef());
    assertEquals("M1,M2", n.getMeetingsAttended());
    assertEquals("MHT-1", n.getMhtRefNumber());
    assertEquals("S1", n.getStageReached());
    assertEquals("FOW", n.getFollowOnWork());
    assertEquals("ECS", n.getExemptionCriteriaSatisfied());
    assertEquals("ECF-1", n.getExclCaseFundingRef());
    assertEquals("2024-03-02", n.getSurgeryDate());
    assertEquals("42", n.getLineNumber());
    assertEquals("FS", n.getFeeScheme());
    assertEquals("2024-01-15", n.getRepOrderDate());
    assertEquals("PS", n.getPoliceStation());
    assertEquals("OUTR", n.getOutreach());
    assertEquals("REF", n.getReferral());
    assertEquals("Jane", n.getClient2Forename());
    assertEquals("Roe", n.getClient2Surname());
    assertEquals("1999-12-31", n.getClient2DateOfBirth());
    assertEquals("UCN2", n.getClient2Ucn());
    assertEquals("PC2 2PC", n.getClient2PostCode());
    assertEquals("F", n.getClient2Gender());
    assertEquals("E2", n.getClient2Ethnicity());
    assertEquals("D2", n.getClient2Disability());
    assertEquals("UCID", n.getUniqueCaseId());
    assertEquals("SFC", n.getStandardFeeCat());
    assertEquals("HPCDS-1", n.getCourtLocationHpcds());
    assertEquals("LA-1", n.getLocalAuthorityNumber());
    assertEquals("PA-1", n.getPaNumber());
    assertEquals("2024-04-04", n.getMedConcludedDate());

    // Assertions for all String fields with blanks -> null
    assertNull(n.getMatterType());
    assertNull(n.getOutcomeCode());
    assertNull(n.getCrimeMatterType());
    assertNull(n.getMaatId());
    assertNull(n.getSchemeId());

    // Assertions for numeric and boolean fields - remained unchanged
    assertEquals(10, n.getAdviceTime());
    assertEquals(20, n.getTravelTime());
    assertEquals(30, n.getWaitingTime());
    assertEquals(new BigDecimal("123.45"), n.getProfitCost());
    assertEquals(new BigDecimal("234.56"), n.getValueOfCosts());
    assertEquals(new BigDecimal("12.34"), n.getDisbursementsAmount());
    assertEquals(new BigDecimal("56.78"), n.getCounselCost());
    assertEquals(new BigDecimal("9.99"), n.getDisbursementsVat());
    assertEquals(new BigDecimal("15.00"), n.getTravelWaitingCosts());
    assertEquals(Boolean.TRUE, n.getVatIndicator());
    assertEquals(Boolean.FALSE, n.getLondonNonlondonRate());
    assertEquals(Boolean.TRUE, n.getToleranceIndicator());
    assertEquals(new BigDecimal("7.77"), n.getTravelCosts());
    assertEquals(Boolean.FALSE, n.getLegacyCase());
    assertEquals(99, n.getAdjournedHearingFee());
    assertEquals(Boolean.TRUE, n.getPostalApplAccp());
    assertEquals(Boolean.FALSE, n.getSubstantiveHearing());
    assertEquals(3, n.getHoInterview());
    assertEquals(new BigDecimal("1.11"), n.getDetentionTravelWaitingCosts());
    assertEquals(new BigDecimal("2.22"), n.getJrFormFilling());
    assertEquals(Boolean.TRUE, n.getAdditionalTravelPayment());
    assertEquals(2, n.getMedicalReportsClaimed());
    assertEquals(1, n.getDesiAccRep());
    assertEquals(Boolean.FALSE, n.getNationalRefMechanismAdvice());
    assertEquals(5, n.getNoOfClients());
    assertEquals(6, n.getNoOfSurgeryClients());
    assertEquals(Boolean.TRUE, n.getIrcSurgery());
    assertEquals(new BigDecimal("1000.00"), n.getCostsDamagesRecovered());
    assertEquals(Boolean.TRUE, n.getEligibleClient());
    assertEquals(new BigDecimal("3.33"), n.getExcessTravelCosts());
    assertEquals(Boolean.FALSE, n.getDutySolicitor());
    assertEquals(Boolean.TRUE, n.getYouthCourt());
    assertEquals(7, n.getNoOfSuspects());
    assertEquals(8, n.getNoOfPoliceStation());
    assertEquals(9, n.getNumberOfMediationSessions());
    assertEquals(60, n.getMediationTime());
    assertEquals(Boolean.TRUE, n.getClientLegallyAided());
    assertEquals(Boolean.FALSE, n.getClient2LegallyAided());
    assertEquals(Boolean.TRUE, n.getClient2PostalApplAccp());
  }

  @Test
  @DisplayName(
      "BulkSubmissionMatterStart: all String fields trimmed/blanks become null; enums/integers preserved")
  void normalise_BulkSubmissionMatterStart() {
    BulkSubmissionMatterStart matterStart = new BulkSubmissionMatterStart();
    matterStart.setScheduleRef("  SCH-99 ");
    matterStart.setProcurementArea("  PA ");
    matterStart.setAccessPoint("  AP ");
    matterStart.setDeliveryLocation("  LOC ");

    // numeric/enum fields that should remain unchanged
    matterStart.setNumberOfMatterStarts(11);
    matterStart.setCategoryCode(CategoryCode.CON); // example enum
    matterStart.setMediationType(MediationType.MDAC_ALL_ISSUES_CO); // example enum

    GetBulkSubmission200ResponseDetails details = new GetBulkSubmission200ResponseDetails();
    details.setMatterStarts(new ArrayList<>(List.of(matterStart)));
    GetBulkSubmission200Response resp = new GetBulkSubmission200Response();
    resp.setDetails(details);

    normaliser.normalise(resp);

    BulkSubmissionMatterStart n = resp.getDetails().getMatterStarts().get(0);

    assertEquals("SCH-99", n.getScheduleRef());
    assertEquals("PA", n.getProcurementArea());
    assertEquals("AP", n.getAccessPoint());
    assertEquals("LOC", n.getDeliveryLocation());

    assertEquals(11, n.getNumberOfMatterStarts());
    assertEquals(CategoryCode.CON, n.getCategoryCode());
    assertEquals(MediationType.MDAC_ALL_ISSUES_CO, n.getMediationType());

    // blank -> null behavior
    n.setAccessPoint("   ");
    normaliser.normalise(resp);
    assertNull(n.getAccessPoint());
  }

  @Test
  @DisplayName("immigrationClr maps: keys and values are trimmed; blanks become null")
  void normalise_immigrationClr() {
    Map<String, String> immClrData1 = new LinkedHashMap<>();
    immClrData1.put("  Key1 ", "  Val1  ");
    immClrData1.put(" Key2 ", "   "); // value should become null
    immClrData1.put("K3", null); // stays null

    Map<String, String> immClrData2 = new LinkedHashMap<>();
    immClrData2.put("\tCode ", "\n  Value  ");
    immClrData2.put("Another", "  123  ");

    List<Map<String, String>> list = new ArrayList<>();
    list.add(immClrData1);
    list.add(immClrData2);

    GetBulkSubmission200ResponseDetails details = new GetBulkSubmission200ResponseDetails();
    details.setImmigrationClr(list);
    GetBulkSubmission200Response resp = new GetBulkSubmission200Response();
    resp.setDetails(details);

    normaliser.normalise(resp);

    List<Map<String, String>> normalised = resp.getDetails().getImmigrationClr();
    assertEquals(2, normalised.size());

    Map<String, String> n1 = normalised.get(0);
    assertEquals(3, n1.size());
    assertTrue(n1.containsKey("Key1"));
    assertEquals("Val1", n1.get("Key1"));
    assertTrue(n1.containsKey("Key2"));
    assertNull(n1.get("Key2"));
    assertTrue(n1.containsKey("K3"));
    assertNull(n1.get("K3"));

    Map<String, String> n2 = normalised.get(1);
    assertEquals("Value", n2.get("Code"));
    assertEquals("123", n2.get("Another"));
  }
}
