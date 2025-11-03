package uk.gov.justice.laa.dstew.payments.claimsevent.mapper;

import java.util.Objects;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BoltOnPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.FeeCalculationPatch;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationResponse;
import uk.gov.justice.laa.fee.scheme.model.FeeDetailsResponse;

/**
 * Maps Fee Calculation Response to Fee Calculation Patch.
 *
 * @author Jamie Briggs
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface FeeCalculationPatchMapper {

  @Mapping(target = ".", source = "feeCalculationResponse")
  @Mapping(target = ".", source = "feeCalculationResponse.feeCalculation")
  @Mapping(
      target = "travelAndWaitingCostsAmount",
      source = "feeCalculationResponse.feeCalculation.travelAndWaitingCostAmount")
  @Mapping(
      target = "netWaitingCostsAmount",
      source = "feeCalculationResponse.feeCalculation.netWaitingCostsAmount")
  @Mapping(
      target = "boltOnDetails",
      source = "feeCalculationResponse.feeCalculation.boltOnFeeDetails")
  @Mapping(target = "feeType", source = "feeDetailsResponse.feeType")
  @Mapping(target = "feeCodeDescription", source = "feeDetailsResponse.feeCodeDescription")
  @Mapping(target = "categoryOfLaw", source = "feeDetailsResponse.categoryOfLawCode")
  @Mapping(target = "calculatedFeeDetailId", ignore = true)
  @Mapping(target = "claimSummaryFeeId", ignore = true)
  FeeCalculationPatch mapToFeeCalculationPatch(
      FeeCalculationResponse feeCalculationResponse, FeeDetailsResponse feeDetailsResponse);

  /**
   * Adds scheme ID and escape case flag to the bolt on details afterwards. Mapstruct doesn't like
   * setting properties on boltOnDetails from two different sources so this is the workaround.
   *
   * @param source the source FeeCalculationResponse
   * @param target the target FeeCalculationPatch
   */
  @AfterMapping
  default void mapSchemeAndEscapeFlag(
      FeeCalculationResponse source, @MappingTarget FeeCalculationPatch target) {
    // Additional logic after mapping
    if (Objects.isNull(target.getBoltOnDetails())) {
      target.setBoltOnDetails(new BoltOnPatch());
    }
    target.getBoltOnDetails().setSchemeId(source.getSchemeId());
    target.getBoltOnDetails().setEscapeCaseFlag(source.getEscapeCaseFlag());
  }
}
