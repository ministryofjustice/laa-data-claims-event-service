package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static uk.gov.justice.laa.dstew.payments.claimsevent.validation.ClaimValidationSource.EVENT_SERVICE;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.MandatoryFieldsRegistry;
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
@RequiredArgsConstructor
public class MandatoryFieldClaimValidator implements ClaimValidator, ClaimWithAreaOfLawValidator {

  private final MandatoryFieldsRegistry mandatoryFieldsRegistry;

  @Override
  public void validate(ClaimResponse claim, SubmissionValidationContext context, String areaOfLaw) {
    Map<String, List<String>> mandatoryFieldsByAreaOfLaw =
        mandatoryFieldsRegistry.getMandatoryFieldsByAreaOfLaw();
    List<String> mandatoryFields = mandatoryFieldsByAreaOfLaw.get(areaOfLaw);
    if (Objects.isNull(mandatoryFields)) {
      return;
    }
    for (String fieldName : mandatoryFields) {
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
