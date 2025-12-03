package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResultSet;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimStatus;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionClaim;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.SubmissionResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.BasicClaimValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.ClaimValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.ClaimWithAreaOfLawValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.DuplicateClaimValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.EffectiveCategoryOfLawClaimValidator;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim.MandatoryFieldClaimValidator;
import uk.gov.justice.laa.fee.scheme.model.FeeDetailsResponse;

@ExtendWith(MockitoExtension.class)
class ClaimValidationServiceTest {

  public static final int CLAIM_VALIDATION_BATCH_SIZE = 100;
  ClaimValidationService claimValidationService;

  @Mock CategoryOfLawValidationService categoryOfLawValidationService;
  @Mock DataClaimsRestClient dataClaimsRestClient;
  @Mock private BulkClaimUpdater bulkClaimUpdater;

  public interface StubBasicClaimValidator extends ClaimValidator, BasicClaimValidator {}

  public interface StubClaimWithAreaOfLawValidator
      extends ClaimValidator, ClaimWithAreaOfLawValidator {}

  @Mock StubBasicClaimValidator basicClaimValidator;
  @Mock StubClaimWithAreaOfLawValidator claimWithAreaOfLawValidator;
  @Mock EffectiveCategoryOfLawClaimValidator effectiveCategoryOfLawClaimValidator;
  @Mock MandatoryFieldClaimValidator mandatoryFieldClaimValidator;
  @Mock DuplicateClaimValidator duplicateClaimValidator;

  @BeforeEach
  void beforeEach() {
    claimValidationService =
        new ClaimValidationService(
            categoryOfLawValidationService,
            dataClaimsRestClient,
            bulkClaimUpdater,
            Arrays.asList(
                basicClaimValidator,
                claimWithAreaOfLawValidator,
                effectiveCategoryOfLawClaimValidator,
                mandatoryFieldClaimValidator,
                duplicateClaimValidator),
            CLAIM_VALIDATION_BATCH_SIZE);

    lenient().when(basicClaimValidator.priority()).thenReturn(1);
    lenient().when(claimWithAreaOfLawValidator.priority()).thenReturn(1);
    lenient().when(effectiveCategoryOfLawClaimValidator.priority()).thenReturn(1);
    lenient().when(duplicateClaimValidator.priority()).thenReturn(1);
  }

  @Test
  @DisplayName("Should call all validators")
  void shouldCallAllValidators() {
    UUID submissionId = new UUID(0, 0);
    UUID claimId = new UUID(1, 1);
    UUID claimIdTwo = new UUID(1, 2);
    SubmissionResponse submissionResponse =
        SubmissionResponse.builder()
            .submissionId(submissionId)
            .areaOfLaw(AreaOfLaw.LEGAL_HELP)
            .officeAccountNumber("officeAccountNumber")
            .build()
            .addClaimsItem(
                new SubmissionClaim().status(ClaimStatus.READY_TO_PROCESS).claimId(claimId))
            .addClaimsItem(
                new SubmissionClaim().status(ClaimStatus.READY_TO_PROCESS).claimId(claimIdTwo));
    SubmissionValidationContext context = new SubmissionValidationContext();

    ClaimResponse claimOne =
        new ClaimResponse()
            .id(claimId.toString())
            .feeCode("feeCode1")
            .status(ClaimStatus.READY_TO_PROCESS);
    ClaimResponse claimTwo =
        new ClaimResponse()
            .id(claimIdTwo.toString())
            .feeCode("feeCode1")
            .status(ClaimStatus.READY_TO_PROCESS);
    ClaimResultSet claimResultSet =
        ClaimResultSet.builder()
            .number(1)
            .totalPages(1)
            .totalElements(2)
            .content(Arrays.asList(claimOne, claimTwo))
            .build();
    when(dataClaimsRestClient.getClaims(
            "officeAccountNumber",
            String.valueOf(submissionId),
            Collections.emptyList(),
            null,
            null,
            null,
            null,
            null,
            0,
            CLAIM_VALIDATION_BATCH_SIZE,
            "id,asc"))
        .thenReturn(ResponseEntity.ok(claimResultSet));
    HashMap<String, FeeDetailsResponseWrapper> feeDetailsResponseMap = new HashMap<>();
    FeeDetailsResponse feeDetailsResponse =
        new FeeDetailsResponse().categoryOfLawCode("categoryOfLaw1").feeType("feeType");
    feeDetailsResponseMap.put(
        "feeCode1", FeeDetailsResponseWrapper.withFeeDetailsResponse(feeDetailsResponse));
    List<ClaimResponse> claimsList = Arrays.asList(claimOne, claimTwo);
    when(categoryOfLawValidationService.getFeeDetailsResponseForAllFeeCodesInClaims(claimsList))
        .thenReturn(feeDetailsResponseMap);

    // When
    claimValidationService.validateAndUpdateClaims(submissionResponse, context);

    // Then
    verify(basicClaimValidator, times(1)).validate(claimOne, context);
    verify(basicClaimValidator, times(1)).validate(claimTwo, context);
    verify(claimWithAreaOfLawValidator, times(1)).validate(claimOne, context, AreaOfLaw.LEGAL_HELP);
    verify(claimWithAreaOfLawValidator, times(1)).validate(claimTwo, context, AreaOfLaw.LEGAL_HELP);
    verify(mandatoryFieldClaimValidator, times(1))
        .validate(claimOne, context, AreaOfLaw.LEGAL_HELP, "feeType");
    verify(mandatoryFieldClaimValidator, times(1))
        .validate(claimTwo, context, AreaOfLaw.LEGAL_HELP, "feeType");
    verify(effectiveCategoryOfLawClaimValidator, times(1))
        .validate(
            claimOne, context, AreaOfLaw.LEGAL_HELP, "officeAccountNumber", feeDetailsResponseMap);
    verify(effectiveCategoryOfLawClaimValidator, times(1))
        .validate(
            claimTwo, context, AreaOfLaw.LEGAL_HELP, "officeAccountNumber", feeDetailsResponseMap);
    verify(duplicateClaimValidator, times(1))
        .validate(
            claimOne, context, AreaOfLaw.LEGAL_HELP, "officeAccountNumber", claimsList, "feeType");
    verify(duplicateClaimValidator, times(1))
        .validate(
            claimTwo, context, AreaOfLaw.LEGAL_HELP, "officeAccountNumber", claimsList, "feeType");

    verify(bulkClaimUpdater)
        .updateClaims(
            eq(submissionId),
            eq(claimsList),
            eq(AreaOfLaw.LEGAL_HELP),
            eq(context),
            eq(feeDetailsResponseMap));
  }
}
