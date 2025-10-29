package uk.gov.justice.laa.dstew.payments.claimsevent.validation.claim;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.gov.justice.laa.dstew.payments.claimsevent.ValidationServiceTestUtils.getClaimMessages;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.ClassPathResource;
import uk.gov.justice.laa.dstew.payments.claimsdata.model.ClaimResponse;
import uk.gov.justice.laa.dstew.payments.claimsevent.config.SchemaValidationConfig;
import uk.gov.justice.laa.dstew.payments.claimsevent.validation.SubmissionValidationContext;

@DisplayName("Matter type claim with area of law validator test")
class MatterTypeClaimValidatorTest {

  MatterTypeClaimValidator validator;

  @BeforeEach
  void beforeEach() throws IOException {
    SchemaValidationConfig config =
        new SchemaValidationConfig(
            new ObjectMapper(),
            new ClassPathResource("schemas/submission-fields.schema.json"),
            new ClassPathResource("schemas/claim-fields.schema.json"));
    validator = new MatterTypeClaimValidator(config.schemaValidationErrorMessages());
  }

  @ParameterizedTest(
      name = "{index} => claimId={0}, matterType={1}, areaOfLaw={2}, regex={3}, expectError={4}")
  @CsvSource({
    "1, BadMatterType, CIVIL, '^[a-zA-Z0-9]{1,4}[-:][a-zA-Z0-9]{1,4}$', true, Each Matter Type Code 1 and 2 must be 4 characters",
    "2, ab12:bc24, CIVIL, '^[a-zA-Z0-9]{1,4}[-:][a-zA-Z0-9]{1,4}$', false, NA",
    "3, AB-CD, CIVIL, '^[a-zA-Z0-9]{1,4}[-:][a-zA-Z0-9]{1,4}$', false, NA",
    "4, ABCD:EFGH, MEDIATION, '^[A-Z]{4}[-:][A-Z]{4}$', false, NA",
    "5, AB12:CD34, MEDIATION, '^[A-Z]{4}[-:][A-Z]{4}$', true, Each Matter Type Code 1 and 2 must be 4 uppercase characters",
    "6, AB-CD, MEDIATION, '^[A-Z]{4}[-:][A-Z]{4}$', true, Each Matter Type Code 1 and 2 must be 4 uppercase characters",
  })
  void checkMatterType(
      int claimIdBit,
      String matterTypeCode,
      String areaOfLaw,
      String regex,
      boolean expectError,
      String expectedDisplayMessage) {
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
      assertThat(getClaimMessages(context, claimId.toString()).getFirst().getDisplayMessage())
          .isEqualTo(expectedDisplayMessage);
    } else {
      assertThat(getClaimMessages(context, claimId.toString()).isEmpty()).isTrue();
    }
  }
}
