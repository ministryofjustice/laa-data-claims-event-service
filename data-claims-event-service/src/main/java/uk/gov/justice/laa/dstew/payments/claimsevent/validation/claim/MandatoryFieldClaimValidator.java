package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.EVENT_SERVICE;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.ExclusionsRegistry;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.MandatoryFieldsRegistry;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

/**
 * Checks if all mandatory fields for a given area of law are populated in the provided
 * ClaimResponse object. If a mandatory field is missing or invalid, an error is added to the
 * submission validation context.
 *
 * @author Jamie Briggs
 * @see ClaimResponse
 * @see SubmissionValidationContext
 * @see ClaimWithAreaOfLawValidator
 */
@Component
@Slf4j
public final class MandatoryFieldClaimValidator implements ClaimValidator {

  public static final String FEE_CALCULATION_TYPE_DISB_ONLY = "DISB_ONLY";
  private final MandatoryFieldsRegistry mandatoryFieldsRegistry;
  private final ExclusionsRegistry exclusionsRegistry;

  public MandatoryFieldClaimValidator(
      MandatoryFieldsRegistry mandatoryFieldsRegistry, ExclusionsRegistry exclusionsRegistry) {
    this.exclusionsRegistry = exclusionsRegistry;
    this.mandatoryFieldsRegistry = mandatoryFieldsRegistry;
  }

  /**
   * Validates mandatory fields for a given area of law.
   *
   * @param claim the claim to validate
   * @param context the validation context to add errors to
   * @param areaOfLaw the area of law to validate
   * @param feeCalculationType the fee calculation type to validate
   */
  public void validate(
      ClaimResponse claim,
      SubmissionValidationContext context,
      String areaOfLaw,
      String feeCalculationType) {
    Map<String, List<String>> mandatoryFieldsByAreaOfLaw =
        mandatoryFieldsRegistry.getMandatoryFieldsByAreaOfLaw();
    List<String> mandatoryFields = mandatoryFieldsByAreaOfLaw.get(areaOfLaw);
    if (Objects.isNull(mandatoryFields)) {
      return;
    }
    boolean isDisbursementLegalHelpClaim =
        FEE_CALCULATION_TYPE_DISB_ONLY.equals(feeCalculationType)
            && AreaOfLaw.LEGAL_HELP.getValue().equals(areaOfLaw);
    List<String> disbursementExclusions = exclusionsRegistry.getDisbursementOnlyExclusions();

    for (String fieldName : mandatoryFields) {
      if (isDisbursementLegalHelpClaim && disbursementExclusions.contains(fieldName)) {
        // Skip validation for excluded fields when disbursement-only
        log.debug("Skipping validation for excluded field: {}", fieldName);
        continue;
      }
      try {
        // Look up getter method for the property
        PropertyDescriptor pd = new PropertyDescriptor(fieldName, ClaimResponse.class);
        Method getter = pd.getReadMethod();

        if (getter == null) {
          throw new IllegalStateException("No getter for field in ClaimResponse: " + fieldName);
        }

        Object value = getter.invoke(claim);

        if (value == null || (value instanceof String s && s.trim().isEmpty())) {
          context.addClaimError(
              claim.getId(),
              String.format("%s is required for area of law: %s", fieldName, areaOfLaw),
              EVENT_SERVICE);
        }

      } catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
        throw new IllegalStateException(
            "Error accessing property in ClaimResponse: " + fieldName, e);
      }
    }
  }

  @Override
  public int priority() {
    return 10;
  }
}
