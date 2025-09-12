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
                  "case_concluded_date",
                  "stage_reached_code",
                  "net_profit_costs_amount",
                  "disbursements_vat_amount"),
          "MEDIATION",
              List.of(
                  "outreach_location",
                  "referral_source",
                  "client_forename",
                  "client_surname",
                  "client_date_of_birth",
                  "unique_client_number",
                  "client_postcode",
                  "gender_code",
                  "ethnicity_code",
                  "disability_code",
                  "is_legally_aided",
                  "client_2_forename",
                  "client_2_surname",
                  "client_2_date_of_birth",
                  "client_2_ucn",
                  "client_2_postcode",
                  "client_2_gender_code",
                  "client_2_ethnicity_code",
                  "client_2_disability_code",
                  "client_2_is_legally_aided",
                  "case_id",
                  "matter_type_code",
                  "unique_case_id"));
}
