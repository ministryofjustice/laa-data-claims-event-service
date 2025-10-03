package uk.gov.justice.laa.dstew.payments.claimsevent.mapper;

import java.util.Objects;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BoltOnPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.FeeCalculationPatch;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationResponse;

/**
 * Maps Fee Calculation Response to Fee Calculation Patch.
 *
 * @author Jamie Briggs
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface FeeCalculationPatchMapper {

  @Mapping(target = ".", source = "feeCalculationResponse.feeCalculation")
  @Mapping(
      target = "netWaitingCostsAmount",
      source = "feeCalculationResponse.feeCalculation.netWaitingCosts")
  @Mapping(
      target = "boltOnDetails",
      source = "feeCalculationResponse.feeCalculation.boltOnFeeDetails")
  @Mapping(target = "calculatedFeeDetailId", ignore = true)
  @Mapping(target = "claimSummaryFeeId", ignore = true)
  @Mapping(target = "feeCodeDescription", ignore = true)
  @Mapping(target = "feeType", ignore = true)
  @Mapping(target = "categoryOfLaw", ignore = true)
  @Mapping(target = "travelAndWaitingCostsAmount", ignore = true)
  FeeCalculationPatch mapToFeeCalculationPatch(FeeCalculationResponse feeCalculationResponse);

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
