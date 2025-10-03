package uk.gov.justice.laa.dstew.payments.claimsevent.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.FeeCalculationPatch;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.DataClaimsRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.client.FeeSchemePlatformRestClient;
import uk.gov.justice.laa.dstew.payments.claimsevent.mapper.FeeCalculationPatchMapper;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationResponse;
import uk.gov.justice.laa.fee.scheme.model.FeeDetailsResponse;

/**
 * Service responsible for updating the claim with fee calculation details.
 *
 * @author Jamie Briggs
 */
@Service
@RequiredArgsConstructor
public class FeeCalculationUpdaterService {

  private final DataClaimsRestClient dataClaimsRestClient;
  private final FeeSchemePlatformRestClient feeSchemePlatformRestClient;
  private final FeeCalculationPatchMapper feeCalculationPatchMapper;

  /**
   * Update the claim with the fee calculation details.
   *
   * @param submissionId the ID of the submission to update the claim in
   * @param claim the claim to update
   * @param feeCalculationResponse the fee calculation response to update the claim with
   */
  public void updateClaimWithFeeCalculationDetails(
      final UUID submissionId,
      final ClaimResponse claim,
      final FeeCalculationResponse feeCalculationResponse) {
    FeeDetailsResponse feeDetails =
        feeSchemePlatformRestClient.getFeeDetails(claim.getFeeCode()).getBody();
    FeeCalculationPatch feeCalculationPatch =
        feeCalculationPatchMapper.mapToFeeCalculationPatch(feeCalculationResponse, feeDetails);
    ClaimPatch claimPatch =
        ClaimPatch.builder().id(claim.getId()).feeCalculationResponse(feeCalculationPatch).build();
    dataClaimsRestClient.updateClaim(submissionId, UUID.fromString(claim.getId()), claimPatch);
  }
}
