package uk.gov.justice.laa.dstew.payments.claimsevent.mapper;

import java.util.Objects;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.BoltOnPatch;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.FeeCalculationPatch;
import uk.gov.justice.laa.fee.scheme.model.FeeCalculationResponse;

@Mapper(
    componentModel = "spring")
public interface FeeCalculationPatchMapper {

  @Mapping(target = ".", source = "feeCalculationResponse.feeCalculation")
  @Mapping(target = "netWaitingCostsAmount", source = "feeCalculationResponse.feeCalculation.netWaitingCosts")
  @Mapping(target = "boltOnDetails",
      source = "feeCalculationResponse.feeCalculation.boltOnFeeDetails")
  FeeCalculationPatch mapToFeeCalculationPatch(FeeCalculationResponse feeCalculationResponse);

  /**
   * Adds scheme ID and escape case flag to the bolt on details afterwards. Mapstruct doesn't
   * like setting properties on boltOnDetails from two different sources so this is the workaround.
   * @param source the source FeeCalculationResponse
   * @param target the target FeeCalculationPatch
   */
  @AfterMapping
  default void mapSchemeAndEscapeFlag(FeeCalculationResponse source, @MappingTarget FeeCalculationPatch target) {
    // Additional logic after mapping
    if(Objects.isNull(target.getBoltOnDetails())) {
      target.setBoltOnDetails(new BoltOnPatch());
    }
    target.getBoltOnDetails().setSchemeId(source.getSchemeId());
    target.getBoltOnDetails().setEscapeCaseFlag(source.getEscapeCaseFlag());
  }

}
