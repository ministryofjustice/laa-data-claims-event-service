package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.EVENT_SERVICE;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.FeeCalculationPatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.mapper.FeeCalculationPatchMapper;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationResponse;
import uk.gov.justice.laa.fee.scheme.model.FeeDetailsResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fee calculation updater service test")
class FeeCalculationUpdaterServiceTest {

  @Mock FeeSchemePlatformRestClient feeSchemePlatformRestClient;
  @Mock DataClaimsRestClient dataClaimsRestClient;
  @Mock FeeCalculationPatchMapper feeCalculationPatchMapper;

  @InjectMocks FeeCalculationUpdaterService feeCalculationUpdaterService;

  @Test
  @DisplayName("Should update claim")
  void shouldUpdateClaim() {
    // Given
    final UUID submissionId = new UUID(1, 1);
    final UUID claimId = new UUID(2, 2);
    final ClaimResponse claimResponse =
        new ClaimResponse().id(claimId.toString()).feeCode("feeCode");
    final FeeCalculationResponse feeCalculationResponse = new FeeCalculationResponse();

    final FeeDetailsResponse feeDetailsResponse =
        new FeeDetailsResponse().feeCodeDescription("feeCodeDescription");

    final FeeCalculationPatch feeCalculationPatch = new FeeCalculationPatch().claimId(claimId);
    when(feeCalculationPatchMapper.mapToFeeCalculationPatch(
            feeCalculationResponse, feeDetailsResponse))
        .thenReturn(feeCalculationPatch);

    // When
    feeCalculationUpdaterService.updateClaimWithFeeCalculationDetails(
        submissionId, claimResponse, feeCalculationResponse, feeDetailsResponse);

    // Then
    verify(dataClaimsRestClient, times(1))
        .updateClaim(
            submissionId,
            claimId,
            ClaimPatch.builder()
                .id(claimId.toString())
                .feeCalculationResponse(feeCalculationPatch)
                .createdByUserId(EVENT_SERVICE)
                .build());
  }
}
