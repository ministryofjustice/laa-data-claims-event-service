package uk.gov.justice.laa.dstew.payments.claimsevent.util;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.laa.dstew.payments.claimsevent.util.StringCaseUtil.toTitleCase;

import org.junit.jupiter.api.Test;

class StringCaseUtilTest {

  @Test
  void toTitleCaseTest() {
    String invalidString = "";
    String snakeCaseString = "snake_case";
    String camelCaseString = "camelCase";
    String snakeCaseWithExcludedLower = "excluded_of_snake";
    String camelCaseWithExcludedCaps = "isNilSubmission";

    assertThat(toTitleCase(invalidString)).isEqualTo(invalidString);
    assertThat(toTitleCase(null)).isEqualTo(null);
    assertThat(toTitleCase(snakeCaseString)).isEqualTo("Snake Case");
    assertThat(toTitleCase(camelCaseString)).isEqualTo("Camel Case");
    assertThat(toTitleCase(snakeCaseWithExcludedLower)).isEqualTo("Excluded of Snake");
    assertThat(toTitleCase(camelCaseWithExcludedCaps)).isEqualTo("Is NIL Submission");
  }
}
