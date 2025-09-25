package uk.gov.justice.laa.dstew.payments.claimsevent.validation.submission;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ProviderDetailsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.provider.model.FirmOfficeContractAndScheduleDetails;
import uk.gov.justice.laa.provider.model.FirmOfficeContractAndScheduleLine;
import uk.gov.justice.laa.provider.model.ProviderFirmOfficeContractAndScheduleDto;

/**
 * Validates that the provider contract is valid for the given office and area of law.
 *
 * @author Jamie Briggs
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubmissionProviderContractValidator implements SubmissionValidator {

  private final ProviderDetailsRestClient providerDetailsRestClient;

  @Override
  public void validate(final SubmissionResponse submission, SubmissionValidationContext context) {
    UUID submissionId = submission.getSubmissionId();
    String officeCode = submission.getOfficeAccountNumber();
    String areaOfLaw = submission.getAreaOfLaw();
    List<String> providerCategoriesOfLaw = getProviderCategoriesOfLaw(officeCode, areaOfLaw);

    log.debug("Validating provider contract for submission {}", submissionId);
    if (providerCategoriesOfLaw.isEmpty()) {
      context.addSubmissionValidationError(ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER);
    }
  }

  @Override
  public int priority() {
    return 10;
  }

  private List<String> getProviderCategoriesOfLaw(String officeCode, String areaOfLaw) {
    return providerDetailsRestClient
        .getProviderFirmSchedules(officeCode, areaOfLaw)
        .blockOptional()
        .stream()
        .map(ProviderFirmOfficeContractAndScheduleDto::getSchedules)
        .flatMap(List::stream)
        .map(FirmOfficeContractAndScheduleDetails::getScheduleLines)
        .flatMap(List::stream)
        .map(FirmOfficeContractAndScheduleLine::getCategoryOfLaw)
        .toList();
  }


}
