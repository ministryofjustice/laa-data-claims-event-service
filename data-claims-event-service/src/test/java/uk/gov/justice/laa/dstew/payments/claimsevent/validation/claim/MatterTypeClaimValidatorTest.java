package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.getClaimMessages;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.AreaOfLaw;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

@DisplayName("Matter type claim with area of law validator test")
class MatterTypeClaimValidatorTest {

  MatterTypeClaimValidator validator = new MatterTypeClaimValidator();

  @ParameterizedTest(
      name = "{index} => claimId={0}, matterType={1}, areaOfLaw={2}, regex={3}, expectError={4}")
  @CsvSource({
    "1, BadMatterType, LEGAL_HELP, '^[a-zA-Z0-9]{1,4}[-:][a-zA-Z0-9]{1,4}$', true",
    "2, ab12:bc24, LEGAL_HELP, '^[a-zA-Z0-9]{1,4}[-:][a-zA-Z0-9]{1,4}$', false",
    "3, AB-CD, LEGAL_HELP, '^[a-zA-Z0-9]{1,4}[-:][a-zA-Z0-9]{1,4}$', false",
    "4, ABCD:EFGH, MEDIATION, '^[A-Z]{4}[-:][A-Z]{4}$', false",
    "5, AB12:CD34, MEDIATION, '^[A-Z]{4}[-:][A-Z]{4}$', true",
    "6, AB-CD, MEDIATION, '^[A-Z]{4}[-:][A-Z]{4}$', true",
  })
  void checkMatterType(
      int claimIdBit,
      String matterTypeCode,
      AreaOfLaw areaOfLaw,
      String regex,
      boolean expectError) {
    UUID claimId = new UUID(claimIdBit, claimIdBit);
    ClaimResponse claim = new ClaimResponse().id(claimId.toString()).matterTypeCode(matterTypeCode);

    SubmissionValidationContext context = new SubmissionValidationContext();

    // Run validation
    validator.validate(claim, context, areaOfLaw);

    if (expectError) {
      String expectedMessage =
          String.format(
              "matter_type_code (%s): does not match the regex pattern %s (provided value: %s)",
              areaOfLaw, regex, matterTypeCode);
      assertThat(getClaimMessages(context, claimId.toString()).getFirst().getTechnicalMessage())
          .isEqualTo(expectedMessage);
    } else {
      assertThat(getClaimMessages(context, claimId.toString()).isEmpty()).isTrue();
    }
  }
}
