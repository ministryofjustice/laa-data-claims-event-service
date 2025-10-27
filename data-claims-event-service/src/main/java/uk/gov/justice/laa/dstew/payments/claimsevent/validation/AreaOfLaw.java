package uk.gov.justice.laa.dstew.payments.claimsevent.validation;

import lombok.Getter;

/**
 * Represents different areas of law for legal claims processing. This enum provides standardized
 * values for categorizing legal cases.
 */
@Getter
public enum AreaOfLaw {
  LEGAL_HELP("LEGAL HELP"),
  CRIME_LOWER("CRIME LOWER"),
  MEDIATION("MEDIATION");

  private final String value;

  /**
   * Constructs an AreaOfLaw enum with the specified value.
   *
   * @param value the string representation of the areaOfLaw
   */
  AreaOfLaw(String value) {
    this.value = value;
  }

  /**
   * Converts a string value to its corresponding AreaOfLaw enum constant.
   *
   * @param value the string value to convert
   * @return the matching AreaOfLaw enum constant
   * @throws IllegalArgumentException if no matching area of law is found
   */
  public static AreaOfLaw fromValue(String value) {
    for (AreaOfLaw areaOfLaw : values()) {
      if (areaOfLaw.value.equals(value)) {
        return areaOfLaw;
      }
    }
    throw new IllegalArgumentException("Unknown area of law: " + value);
  }
}
