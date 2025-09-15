package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResultSet;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationReport;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

@ExtendWith(MockitoExtension.class)
class ClaimValidationServiceTest {

  @Mock private DataClaimsRestClient dataClaimsRestClient;

  @Mock private CategoryOfLawValidationService categoryOfLawValidationService;

  @Mock private DuplicateClaimValidationService duplicateClaimValidationService;

  @Mock private FeeCalculationService feeCalculationService;

  @InjectMocks private ClaimValidationService claimValidationService;

  @Nested
  @DisplayName("validateClaims")
  class ValidateClaimsTests {

    @Test
    @DisplayName("Validates category of law, duplicates and fee calculation for all claims")
    void validateCategoryOfLawAndDuplicatesAndFeeCalculation() {
      ClaimResponse claim1 =
          new ClaimResponse().id("claim1").feeCode("feeCode1").status(ClaimStatus.READY_TO_PROCESS);
      ClaimResponse claim2 =
          new ClaimResponse().id("claim2").feeCode("feeCode2").status(ClaimStatus.READY_TO_PROCESS);
      List<ClaimResponse> claims = List.of(claim1, claim2);
      List<String> providerCategoriesOfLaw = List.of("categoryOfLaw1");
      Map<String, CategoryOfLawResult> categoryOfLawLookup = Collections.emptyMap();

      SubmissionResponse submissionResponse =
          new SubmissionResponse()
              .submissionId(new UUID(1, 1))
              .areaOfLaw("areaOfLaw")
              .officeAccountNumber("officeAccountNumber");

      ClaimResultSet claimResultSet = new ClaimResultSet();
      claimResultSet.content(claims);

      when(dataClaimsRestClient.getClaims(
              submissionResponse.getOfficeAccountNumber(),
              submissionResponse.getSubmissionId().toString(),
              null,
              null,
              null,
              null,
              null,
              null))
          .thenReturn(ResponseEntity.ok(claimResultSet));

      when(categoryOfLawValidationService.getCategoryOfLawLookup(claims))
          .thenReturn(categoryOfLawLookup);

      SubmissionValidationContext context = new SubmissionValidationContext();
      context.addClaimReports(
          List.of(
              new ClaimValidationReport(claim1.getId()),
              new ClaimValidationReport(claim2.getId())));

      claimValidationService.validateClaims(submissionResponse, providerCategoriesOfLaw, context);

      verify(categoryOfLawValidationService, times(1))
          .validateCategoryOfLaw(claim1, categoryOfLawLookup, providerCategoriesOfLaw, context);
      verify(categoryOfLawValidationService, times(1))
          .validateCategoryOfLaw(claim2, categoryOfLawLookup, providerCategoriesOfLaw, context);

      verify(duplicateClaimValidationService, times(1))
          .validateDuplicateClaims(claim1, claims, "areaOfLaw", "officeAccountNumber", context);
      verify(duplicateClaimValidationService, times(1))
          .validateDuplicateClaims(claim2, claims, "areaOfLaw", "officeAccountNumber", context);

      verify(feeCalculationService, times(1)).validateFeeCalculation(claim1, context);
      verify(feeCalculationService, times(1)).validateFeeCalculation(claim2, context);
    }
  }
}
