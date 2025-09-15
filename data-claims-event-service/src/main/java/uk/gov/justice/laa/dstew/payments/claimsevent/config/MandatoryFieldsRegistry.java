package uk.gov.justice.laa.dstew.payments.claimsevent.config;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@Getter
public class MandatoryFieldsRegistry {
  private final Map<String, List<String>> mandatoryFieldsByAreaOfLaw =
      Map.of(
          "CIVIL",
              List.of(
                  "uniqueFileNumber",
                  "feeSchemeCode",
                  "caseConcludedDate",
                  "outcomeCode",
                  "isPostalApplicationAccepted",
                  "travelWaitingCostsAmount",
                  "clientForename",
                  "clientSurname",
                  "clientDateOfBirth",
                  "uniqueClientNumber",
                  "clientPostCode",
                  "genderCode",
                  "ethnicityCode",
                  "disabilityCode",
                  "clientTypeCode",
                  "homeOfficeClientNumber",
                  "claReferenceNumber",
                  "claExemptionCode",
                  "adviceTime",
                  "travelTime",
                  "waitingTime",
                  "netCounselCostsAmount",
                  "caseId",
                  "matterTypeCode",
                  "netProfitCostsAmount"),
          "CRIME",
              List.of(
                  "caseConcludedDate",
                  "stageReachedCode",
                  "netProfitCostsAmount",
                  "disbursementsVatAmount"),
          "MEDIATION",
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
                  "client2Forename",
                  "client2Surname",
                  "client2DateOfBirth",
                  "client2Ucn",
                  "client2Postcode",
                  "client2GenderCode",
                  "client2EthnicityCode",
                  "client2DisabilityCode",
                  "client2IsLegallyAided",
                  "caseId",
                  "matterTypeCode",
                  "uniqueCaseId"));
}
