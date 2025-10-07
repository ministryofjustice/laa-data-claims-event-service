package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * Registry holding mappings of mandatory field names grouped by area of law.
 *
 * <p>Used to validate presence of required fields depending on the specified area of law.
 */
@Component
@Getter
public class MandatoryFieldsRegistry {

  private final List<String> civilMandatoryFields =
      List.of(
          "uniqueFileNumber",
          "caseStartDate",
          "caseConcludedDate",
          "outcomeCode",
          "travelWaitingCostsAmount",
          "clientForename",
          "clientSurname",
          "clientDateOfBirth",
          "uniqueClientNumber",
          "clientPostcode",
          "genderCode",
          "ethnicityCode",
          "disabilityCode",
          "adviceTime",
          "travelTime",
          "waitingTime",
          "netCounselCostsAmount",
          "caseId",
          "caseReferenceNumber",
          "scheduleReference",
          "matterTypeCode",
          "netProfitCostsAmount");

  private final List<String> crimeMandatoryFields =
      List.of(
          "caseConcludedDate",
          "stageReachedCode",
          "netProfitCostsAmount",
          "disbursementsVatAmount");

  private final List<String> mediationMandatoryFields =
      List.of(
          "outreachLocation",
          "referralSource",
          "clientForename",
          "clientSurname",
          "clientDateOfBirth",
          "uniqueClientNumber",
          "clientPostcode",
          "genderCode",
          "ethnicityCode",
          "disabilityCode",
          "isLegallyAided",
          "caseId",
          "caseStartDate",
          "caseReferenceNumber",
          "scheduleReference",
          "matterTypeCode",
          "uniqueCaseId");

  private final Map<String, List<String>> mandatoryFieldsByAreaOfLaw =
      Map.of(
          "CIVIL", civilMandatoryFields,
          "LEGAL HELP", civilMandatoryFields, // Same fields are mandatory for CIVIL and LEGAL HELP
          "CRIME", crimeMandatoryFields,
          "CRIME LOWER",
              crimeMandatoryFields, // Same fields are mandatory for CRIME and CRIME LOWER
          "MEDIATION", mediationMandatoryFields);
}
