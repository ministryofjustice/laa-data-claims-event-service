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
  @DisplayName("normalise(null) returns null")
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

    GetBulkSubmission200Response response = new GetBulkSubmission200Response();
    response.setDetails(details);

    normaliser.normalise(response);

    assertEquals("2P554H", response.getDetails().getOffice().getAccount());
    assertEquals("JAN-2020", response.getDetails().getSchedule().getSubmissionPeriod());
    assertEquals("CRIME LOWER", response.getDetails().getSchedule().getAreaOfLaw());
    assertEquals("ABC/20000L/10", response.getDetails().getSchedule().getScheduleNum());
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
    GetBulkSubmission200Response response = new GetBulkSubmission200Response();
    response.setDetails(details);

    normaliser.normalise(response);

    BulkSubmissionOutcome actualOutcome = response.getDetails().getOutcomes().getFirst();

    // Assertions for all String fields with trimmed whitespaces
    assertEquals("F01", actualOutcome.getFeeCode());
    assertEquals("REF-123", actualOutcome.getCaseRefNumber());
    assertEquals("2024-01-01", actualOutcome.getCaseStartDate());
    assertEquals("CID", actualOutcome.getCaseId());
    assertEquals("L2", actualOutcome.getCaseStageLevel());
    assertEquals("UFN-1", actualOutcome.getUfn());
    assertEquals("PA", actualOutcome.getProcurementArea());
    assertEquals("AP", actualOutcome.getAccessPoint());
    assertEquals("John", actualOutcome.getClientForename());
    assertEquals("Doe", actualOutcome.getClientSurname());
    assertEquals("2000-01-01", actualOutcome.getClientDateOfBirth());
    assertEquals("UCN", actualOutcome.getUcn());
    assertEquals("CLA-1", actualOutcome.getClaRefNumber());
    assertEquals("EX", actualOutcome.getClaExemption());
    assertEquals("M", actualOutcome.getGender());
    assertEquals("E", actualOutcome.getEthnicity());
    assertEquals("D", actualOutcome.getDisability());
    assertEquals("PC1 1PC", actualOutcome.getClientPostCode());
    assertEquals("2024-02-01", actualOutcome.getWorkConcludedDate());
    assertEquals("CT", actualOutcome.getClaimType());
    assertEquals("TA", actualOutcome.getTypeOfAdvice());
    assertEquals("SCH-1", actualOutcome.getScheduleRef());
    assertEquals("Y", actualOutcome.getCmrhOral());
    assertEquals("N", actualOutcome.getCmrhTelephone());
    assertEquals("AIT-1", actualOutcome.getAitHearingCentre());
    assertEquals("HOU-1", actualOutcome.getHoUcn());
    assertEquals("2024-03-01", actualOutcome.getTransferDate());
    assertEquals("LOC", actualOutcome.getDeliveryLocation());
    assertEquals("PAR-123", actualOutcome.getPriorAuthorityRef());
    assertEquals("M1,M2", actualOutcome.getMeetingsAttended());
    assertEquals("MHT-1", actualOutcome.getMhtRefNumber());
    assertEquals("S1", actualOutcome.getStageReached());
    assertEquals("FOW", actualOutcome.getFollowOnWork());
    assertEquals("ECS", actualOutcome.getExemptionCriteriaSatisfied());
    assertEquals("ECF-1", actualOutcome.getExclCaseFundingRef());
    assertEquals("2024-03-02", actualOutcome.getSurgeryDate());
    assertEquals("42", actualOutcome.getLineNumber());
    assertEquals("FS", actualOutcome.getFeeScheme());
    assertEquals("2024-01-15", actualOutcome.getRepOrderDate());
    assertEquals("PS", actualOutcome.getPoliceStation());
    assertEquals("OUTR", actualOutcome.getOutreach());
    assertEquals("REF", actualOutcome.getReferral());
    assertEquals("Jane", actualOutcome.getClient2Forename());
    assertEquals("Roe", actualOutcome.getClient2Surname());
    assertEquals("1999-12-31", actualOutcome.getClient2DateOfBirth());
    assertEquals("UCN2", actualOutcome.getClient2Ucn());
    assertEquals("PC2 2PC", actualOutcome.getClient2PostCode());
    assertEquals("F", actualOutcome.getClient2Gender());
    assertEquals("E2", actualOutcome.getClient2Ethnicity());
    assertEquals("D2", actualOutcome.getClient2Disability());
    assertEquals("UCID", actualOutcome.getUniqueCaseId());
    assertEquals("SFC", actualOutcome.getStandardFeeCat());
    assertEquals("HPCDS-1", actualOutcome.getCourtLocationHpcds());
    assertEquals("LA-1", actualOutcome.getLocalAuthorityNumber());
    assertEquals("PA-1", actualOutcome.getPaNumber());
    assertEquals("2024-04-04", actualOutcome.getMedConcludedDate());

    // Assertions for all String fields with blanks -> null
    assertNull(actualOutcome.getMatterType());
    assertNull(actualOutcome.getOutcomeCode());
    assertNull(actualOutcome.getCrimeMatterType());
    assertNull(actualOutcome.getMaatId());
    assertNull(actualOutcome.getSchemeId());

    // Assertions for numeric and boolean fields - remained unchanged
    assertEquals(10, actualOutcome.getAdviceTime());
    assertEquals(20, actualOutcome.getTravelTime());
    assertEquals(30, actualOutcome.getWaitingTime());
    assertEquals(new BigDecimal("123.45"), actualOutcome.getProfitCost());
    assertEquals(new BigDecimal("234.56"), actualOutcome.getValueOfCosts());
    assertEquals(new BigDecimal("12.34"), actualOutcome.getDisbursementsAmount());
    assertEquals(new BigDecimal("56.78"), actualOutcome.getCounselCost());
    assertEquals(new BigDecimal("9.99"), actualOutcome.getDisbursementsVat());
    assertEquals(new BigDecimal("15.00"), actualOutcome.getTravelWaitingCosts());
    assertEquals(Boolean.TRUE, actualOutcome.getVatIndicator());
    assertEquals(Boolean.FALSE, actualOutcome.getLondonNonlondonRate());
    assertEquals(Boolean.TRUE, actualOutcome.getToleranceIndicator());
    assertEquals(new BigDecimal("7.77"), actualOutcome.getTravelCosts());
    assertEquals(Boolean.FALSE, actualOutcome.getLegacyCase());
    assertEquals(99, actualOutcome.getAdjournedHearingFee());
    assertEquals(Boolean.TRUE, actualOutcome.getPostalApplAccp());
    assertEquals(Boolean.FALSE, actualOutcome.getSubstantiveHearing());
    assertEquals(3, actualOutcome.getHoInterview());
    assertEquals(new BigDecimal("1.11"), actualOutcome.getDetentionTravelWaitingCosts());
    assertEquals(new BigDecimal("2.22"), actualOutcome.getJrFormFilling());
    assertEquals(Boolean.TRUE, actualOutcome.getAdditionalTravelPayment());
    assertEquals(2, actualOutcome.getMedicalReportsClaimed());
    assertEquals(1, actualOutcome.getDesiAccRep());
    assertEquals(Boolean.FALSE, actualOutcome.getNationalRefMechanismAdvice());
    assertEquals(5, actualOutcome.getNoOfClients());
    assertEquals(6, actualOutcome.getNoOfSurgeryClients());
    assertEquals(Boolean.TRUE, actualOutcome.getIrcSurgery());
    assertEquals(new BigDecimal("1000.00"), actualOutcome.getCostsDamagesRecovered());
    assertEquals(Boolean.TRUE, actualOutcome.getEligibleClient());
    assertEquals(new BigDecimal("3.33"), actualOutcome.getExcessTravelCosts());
    assertEquals(Boolean.FALSE, actualOutcome.getDutySolicitor());
    assertEquals(Boolean.TRUE, actualOutcome.getYouthCourt());
    assertEquals(7, actualOutcome.getNoOfSuspects());
    assertEquals(8, actualOutcome.getNoOfPoliceStation());
    assertEquals(9, actualOutcome.getNumberOfMediationSessions());
    assertEquals(60, actualOutcome.getMediationTime());
    assertEquals(Boolean.TRUE, actualOutcome.getClientLegallyAided());
    assertEquals(Boolean.FALSE, actualOutcome.getClient2LegallyAided());
    assertEquals(Boolean.TRUE, actualOutcome.getClient2PostalApplAccp());
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
    GetBulkSubmission200Response response = new GetBulkSubmission200Response();
    response.setDetails(details);

    normaliser.normalise(response);

    BulkSubmissionMatterStart n = response.getDetails().getMatterStarts().getFirst();

    assertEquals("SCH-99", n.getScheduleRef());
    assertEquals("PA", n.getProcurementArea());
    assertEquals("AP", n.getAccessPoint());
    assertEquals("LOC", n.getDeliveryLocation());

    assertEquals(11, n.getNumberOfMatterStarts());
    assertEquals(CategoryCode.CON, n.getCategoryCode());
    assertEquals(MediationType.MDAC_ALL_ISSUES_CO, n.getMediationType());

    // blank -> null behavior
    n.setAccessPoint("   ");
    normaliser.normalise(response);
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
    GetBulkSubmission200Response response = new GetBulkSubmission200Response();
    response.setDetails(details);

    normaliser.normalise(response);

    List<Map<String, String>> normalised = response.getDetails().getImmigrationClr();
    assertEquals(2, normalised.size());

    Map<String, String> n1 = normalised.getFirst();
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
