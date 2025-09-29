package uk.gov.justice.laa.dstew.payments.claimsevent.validation.submission;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.assertContextClaimError;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.ProviderDetailsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationError;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.provider.model.FirmOfficeContractAndScheduleDetails;
import uk.gov.justice.laa.provider.model.FirmOfficeContractAndScheduleLine;
import uk.gov.justice.laa.provider.model.ProviderFirmOfficeContractAndScheduleDto;

@ExtendWith(MockitoExtension.class)
@DisplayName("Submission Provider Contract Validator Test")
class SubmissionProviderContractValidatorTest {

  @Mock private ProviderDetailsRestClient providerDetailsRestClient;

  private SubmissionProviderContractValidator validator;

  @BeforeEach
  void beforeEach() {
    validator = new SubmissionProviderContractValidator(providerDetailsRestClient);
  }

  @Test
  @DisplayName("Should have no errors if provider contract exists")
  void shouldHaveNoErrorsIfProviderContractExists() {
    // Given
    SubmissionResponse submissionResponse =
        SubmissionResponse.builder()
            .officeAccountNumber("officeCode")
            .areaOfLaw("areaOfLawCode")
            .build();
    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();

    when(providerDetailsRestClient.getProviderFirmSchedules("officeCode", "areaOfLawCode"))
        .thenReturn(
            Mono.just(
                new ProviderFirmOfficeContractAndScheduleDto()
                    .addSchedulesItem(
                        new FirmOfficeContractAndScheduleDetails()
                            .addScheduleLinesItem(
                                new FirmOfficeContractAndScheduleLine()
                                    .categoryOfLaw("categoryOfLawCode")))));
    // When
    validator.validate(submissionResponse, submissionValidationContext);
    // Then
    assertFalse(submissionValidationContext.hasErrors());
  }

  @Test
  @DisplayName("Should have errors when empty response")
  void shouldHaveErrorsWhenEmptyResponse() {
    // Given
    SubmissionResponse submissionResponse =
        SubmissionResponse.builder()
            .officeAccountNumber("officeCode")
            .areaOfLaw("areaOfLawCode")
            .build();
    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();

    when(providerDetailsRestClient.getProviderFirmSchedules("officeCode", "areaOfLawCode"))
        .thenReturn(Mono.empty());
    // When
    validator.validate(submissionResponse, submissionValidationContext);
    // Then
    assertTrue(submissionValidationContext.hasErrors());
    assertContextClaimError(
        submissionValidationContext, ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER);
  }

  @Test
  @DisplayName("Should have errors when no schedules")
  void shouldHaveErrorsWhenNoSchedules() {
    // Given
    SubmissionResponse submissionResponse =
        SubmissionResponse.builder()
            .officeAccountNumber("officeCode")
            .areaOfLaw("areaOfLawCode")
            .build();
    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();

    when(providerDetailsRestClient.getProviderFirmSchedules("officeCode", "areaOfLawCode"))
        .thenReturn(Mono.just(new ProviderFirmOfficeContractAndScheduleDto()));
    // When
    validator.validate(submissionResponse, submissionValidationContext);
    // Then
    assertTrue(submissionValidationContext.hasErrors());
    assertContextClaimError(
        submissionValidationContext, ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER);
  }

  @Test
  @DisplayName("Should have errors when no schedule lines in schedule")
  void shouldHaveErrorsWhenNoScheduleLinesInSchedule() {
    // Given
    SubmissionResponse submissionResponse =
        SubmissionResponse.builder()
            .officeAccountNumber("officeCode")
            .areaOfLaw("areaOfLawCode")
            .build();
    SubmissionValidationContext submissionValidationContext = new SubmissionValidationContext();

    when(providerDetailsRestClient.getProviderFirmSchedules("officeCode", "areaOfLawCode"))
        .thenReturn(
            Mono.just(
                new ProviderFirmOfficeContractAndScheduleDto()
                    .addSchedulesItem(new FirmOfficeContractAndScheduleDetails())));
    // When
    validator.validate(submissionResponse, submissionValidationContext);
    // Then
    assertTrue(submissionValidationContext.hasErrors());
    assertContextClaimError(
        submissionValidationContext, ClaimValidationError.INVALID_AREA_OF_LAW_FOR_PROVIDER);
  }
}
